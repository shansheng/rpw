<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { OrganizationApi } from '#/api/rpw/organization';
import type { SystemUserApi } from '#/api/system/user';

import { onMounted, ref } from 'vue';

import { getOrganizationTree } from '#/api/rpw/organization';

import { confirm, DocAlert, Page, useVbenModal } from '@vben/common-ui';
import { DICT_TYPE } from '@vben/constants';
import { getDictLabel } from '@vben/hooks';
import { downloadFileFromBlobPart, isEmpty } from '@vben/utils';

import { Card, message } from 'antdv-next';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteUser,
  deleteUserList,
  exportUser,
  getUserPage,
  updateUserStatus,
} from '#/api/system/user';
import { getSimpleRoleList } from '#/api/system/role';
import { $t } from '#/locales';
import OrgTree from './components/org-tree.vue';

import { useGridColumns, useGridFormSchema } from './data';
import AssignRoleForm from './modules/assign-role-form.vue';
import Form from './modules/form.vue';
import ImportForm from './modules/import-form.vue';
import ResetPasswordForm from './modules/reset-password-form.vue';

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

const [ResetPasswordModal, resetPasswordModalApi] = useVbenModal({
  connectedComponent: ResetPasswordForm,
  destroyOnClose: true,
});

const [AssignRoleModal, assignRoleModalApi] = useVbenModal({
  connectedComponent: AssignRoleForm,
  destroyOnClose: true,
});

const [ImportModal, importModalApi] = useVbenModal({
  connectedComponent: ImportForm,
  destroyOnClose: true,
});

/** 刷新表格 */
function handleRefresh() {
  gridApi.query();
}

/** 导出表格 */
async function handleExport() {
  const data = await exportUser(await gridApi.formApi.getValues());
  downloadFileFromBlobPart({ fileName: '用户.xls', source: data });
}

/** 组织树（用户管理左侧） */
interface OrgTreeNode {
  key: number;
  title: string;
  raw: OrganizationApi.Organization;
  children?: OrgTreeNode[];
}

function orgTypeTag(n: OrganizationApi.Organization): string {
  if (n.nodeType === 2) return '【部门】';
  return { 1: '【局】', 2: '【公司】', 3: '【项目】' }[n.orgLevel ?? 0] ?? '';
}

function toOrgTree(nodes: OrganizationApi.Organization[]): OrgTreeNode[] {
  return nodes.map((n) => {
    if (n.id != null) orgNodeMap.value.set(n.id, n);
    return {
      key: n.id!,
      title: `${orgTypeTag(n)} ${n.orgName}`,
      raw: n,
      children: n.children?.length ? toOrgTree(n.children) : undefined,
    };
  });
}

const orgTreeData = ref<OrgTreeNode[]>([]);
const orgNodeMap = ref<Map<number, OrganizationApi.Organization>>(new Map());
const searchOrgId = ref<number | undefined>(undefined);

async function loadOrgTree() {
  orgNodeMap.value = new Map();
  orgTreeData.value = toOrgTree(await getOrganizationTree());
}

/** 选择组织节点 → 按 orgId 过滤用户（后端 /page 的 deptId 映射到 orgId） */
async function handleOrgSelect(org?: OrganizationApi.Organization) {
  searchOrgId.value = org?.id;
  handleRefresh();
}

/** 组织ID -> 名称 映射（列表「归属组织」列展示用） */
const orgNameOf = (orgId?: number): string => {
  if (orgId == null || orgId === 0) return '-';
  return orgNodeMap.value.get(orgId)?.orgName ?? `#${orgId}`;
};

onMounted(loadOrgTree);

/** 左侧组织树宽度拖拽调节（鼠标拖动分隔条改变左列像素宽） */
const leftWidth = ref(260);
let resizeStartX = 0;
let resizeStartW = 0;

function startResize(e: MouseEvent) {
  resizeStartX = e.clientX;
  resizeStartW = leftWidth.value;
  document.addEventListener('mousemove', onResize);
  document.addEventListener('mouseup', stopResize);
  document.body.style.userSelect = 'none';
  document.body.style.cursor = 'col-resize';
}

function onResize(e: MouseEvent) {
  const delta = e.clientX - resizeStartX;
  leftWidth.value = Math.min(Math.max(resizeStartW + delta, 180), 640);
}

function stopResize() {
  document.removeEventListener('mousemove', onResize);
  document.removeEventListener('mouseup', stopResize);
  document.body.style.userSelect = '';
  document.body.style.cursor = '';
}

/** 角色ID -> 名称 映射（列表「角色」列展示用） */
const roleMap = ref<Record<number, string>>({});
const roleNameOf = (roleIds?: number[]): string => {
  if (!roleIds?.length) return '-';
  return roleIds.map((id) => roleMap.value[id] ?? `#${id}`).join('、');
};
getSimpleRoleList().then((list) => {
  const map: Record<number, string> = {};
  for (const r of list ?? []) {
    map[r.id] = r.name;
  }
  roleMap.value = map;
});

/** 创建用户 */
function handleCreate() {
  formModalApi.setData(null).open();
}

/** 导入用户 */
function handleImport() {
  importModalApi.open();
}

/** 编辑用户 */
function handleEdit(row: SystemUserApi.User) {
  formModalApi.setData(row).open();
}

/** 删除用户 */
async function handleDelete(row: SystemUserApi.User) {
  const hideLoading = message.loading({
    content: $t('ui.actionMessage.deleting', [row.username]),
    duration: 0,
  });
  try {
    await deleteUser(row.id!);
    message.success($t('ui.actionMessage.deleteSuccess', [row.username]));
    handleRefresh();
  } finally {
    hideLoading();
  }
}

/** 批量删除用户 */
async function handleDeleteBatch() {
  await confirm($t('ui.actionMessage.deleteBatchConfirm'));
  const hideLoading = message.loading({
    content: $t('ui.actionMessage.deletingBatch'),
    duration: 0,
  });
  try {
    await deleteUserList(checkedIds.value);
    checkedIds.value = [];
    message.success($t('ui.actionMessage.deleteSuccess'));
    handleRefresh();
  } finally {
    hideLoading();
  }
}

const checkedIds = ref<number[]>([]);
function handleRowCheckboxChange({
  records,
}: {
  records: SystemUserApi.User[];
}) {
  checkedIds.value = records.map((item) => item.id!);
}

/** 重置密码 */
function handleResetPassword(row: SystemUserApi.User) {
  resetPasswordModalApi.setData(row).open();
}

/** 分配角色 */
function handleAssignRole(row: SystemUserApi.User) {
  assignRoleModalApi.setData(row).open();
}

/** 更新用户状态 */
async function handleStatusChange(
  newStatus: number,
  row: SystemUserApi.User,
): Promise<boolean | undefined> {
  try {
    await confirm(
      `你要将${row.username}的状态切换为【${getDictLabel(DICT_TYPE.COMMON_STATUS, newStatus)}】吗？`,
    );
  } catch {
    return false;
  }
  // 更新用户状态
  await updateUserStatus(row.id!, newStatus);
  // 提示并返回成功
  message.success($t('ui.actionMessage.operationSuccess'));
  return true;
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
  },
  gridOptions: {
    columns: useGridColumns(handleStatusChange, roleNameOf, orgNameOf),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
            return await getUserPage({
              pageNo: page.currentPage,
              pageSize: page.pageSize,
              ...formValues,
              deptId: searchOrgId.value,
            });
        },
      },
    },
    rowConfig: {
      keyField: 'id',
      isHover: true,
    },
    toolbarConfig: {
      refresh: true,
      search: true,
    },
  } as VxeTableGridOptions<SystemUserApi.User>,
  gridEvents: {
    checkboxAll: handleRowCheckboxChange,
    checkboxChange: handleRowCheckboxChange,
  },
});
</script>

<template>
  <Page auto-content-height>
  
  
    <FormModal @success="handleRefresh" />
    <ResetPasswordModal @success="handleRefresh" />
    <AssignRoleModal @success="handleRefresh" />
    <ImportModal @success="handleRefresh" />

    <div class="flex h-full w-full">
      <!-- 左侧组织树（可拖拽调宽） -->
      <Card
        class="mr-2 flex h-full shrink-0 flex-col overflow-hidden"
        :style="{ width: leftWidth + 'px' }"
      >
        <div class="mb-2 shrink-0 font-semibold">组织</div>
        <div class="min-h-0 flex-1 overflow-auto">
          <OrgTree :tree-data="orgTreeData" @select="handleOrgSelect" />
        </div>
      </Card>
      <!-- 拖拽分隔条 -->
      <div
        class="group relative w-1 shrink-0 cursor-col-resize bg-gray-200 transition-colors hover:bg-primary dark:bg-gray-700"
        @mousedown="startResize"
      >
        <div class="absolute inset-y-0 -left-1.5 -right-1.5" />
      </div>
      <!-- 右侧用户列表 -->
      <div class="min-w-0 flex-1">
        <Grid table-title="用户列表">
          <template #toolbar-tools>
            <TableAction
              :actions="[
                {
                  label: $t('ui.actionTitle.create', ['用户']),
                  type: 'primary',
                  icon: ACTION_ICON.ADD,
                  auth: ['system:user:create'],
                  onClick: handleCreate,
                },
                {
                  label: $t('ui.actionTitle.export'),
                  type: 'primary',
                  icon: ACTION_ICON.DOWNLOAD,
                  auth: ['system:user:export'],
                  onClick: handleExport,
                },
                {
                  label: $t('ui.actionTitle.import', ['用户']),
                  type: 'primary',
                  icon: ACTION_ICON.UPLOAD,
                  auth: ['system:user:import'],
                  onClick: handleImport,
                },
                {
                  label: $t('ui.actionTitle.deleteBatch'),
                  type: 'primary',
                  danger: true,
                  icon: ACTION_ICON.DELETE,
                  disabled: isEmpty(checkedIds),
                  auth: ['system:user:delete'],
                  onClick: handleDeleteBatch,
                },
              ]"
            />
          </template>
          <template #actions="{ row }">
            <TableAction
              :actions="[
                {
                  label: $t('common.edit'),
                  type: 'link',
                  icon: ACTION_ICON.EDIT,
                  auth: ['system:user:update'],
                  onClick: handleEdit.bind(null, row),
                },
                {
                  label: $t('common.delete'),
                  type: 'link',
                  danger: true,
                  icon: ACTION_ICON.DELETE,
                  auth: ['system:user:delete'],
                  popConfirm: {
                    title: $t('ui.actionMessage.deleteConfirm', [row.username]),
                    confirm: handleDelete.bind(null, row),
                  },
                },
              ]"
              :drop-down-actions="[
                {
                  label: '分配角色',
                  type: 'link',
                  auth: ['system:permission:assign-user-role'],
                  onClick: handleAssignRole.bind(null, row),
                },
                {
                  label: '重置密码',
                  type: 'link',
                  auth: ['system:user:update-password'],
                  onClick: handleResetPassword.bind(null, row),
                },
              ]"
            />
          </template>
        </Grid>
      </div>
    </div>
  </Page>
</template>
