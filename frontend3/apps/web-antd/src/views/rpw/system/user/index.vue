<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { SysUserApi } from '#/api/rpw/system/user';

import { onMounted, ref } from 'vue';

import { confirm, DocAlert, Page, useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteSysUser,
  getSysUserPage,
  resetSysUserPassword,
  updateSysUserStatus,
} from '#/api/rpw/system/user';
import { getOrganizationTree } from '#/api/rpw/organization';
import { getRoleSimpleList } from '#/api/rpw/system/role';
import { $t } from '#/locales';

import { useGridColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

defineOptions({ name: 'SysUser' });

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

/** 组织ID -> 名称 映射（实时查询用） */
const orgMap = ref<Record<number, string>>({});
const orgNameOf = (id?: number): string => {
  if (id == null) return '-';
  return orgMap.value[id] ?? `#${id}`;
};

/** 角色ID -> 名称 映射（列表展示用户已绑定角色用） */
const roleMap = ref<Record<number, string>>({});
const roleNameOf = (ids?: number[]): string => {
  if (!ids || ids.length === 0) return '-';
  return ids.map((id) => roleMap.value[id] ?? `#${id}`).join('、');
};

async function loadOrgMap() {
  try {
    const tree = await getOrganizationTree();
    const map: Record<number, string> = {};
    const walk = (nodes: any[]) => {
      for (const n of nodes ?? []) {
        map[n.id] = n.orgName;
        if (n.children?.length) walk(n.children);
      }
    };
    walk(tree);
    orgMap.value = map;
  } catch {
    orgMap.value = {};
  }
}

/** 角色ID -> 名称 映射（列表展示用户已绑定角色用） */
async function loadRoleMap() {
  try {
    const roles = await getRoleSimpleList();
    const map: Record<number, string> = {};
    for (const r of roles ?? []) {
      if (r.id != null) map[r.id] = r.name ?? '';
    }
    roleMap.value = map;
  } catch {
    roleMap.value = {};
  }
}

/** 刷新表格 */
function handleRefresh() {
  gridApi.query();
}

/** 新增用户 */
function handleCreate() {
  formModalApi.setData({}).open();
}

/** 编辑用户 */
function handleEdit(row: SysUserApi.SysUser) {
  formModalApi.setData(row).open();
}

/** 重置密码 */
async function handleResetPwd(row: SysUserApi.SysUser) {
  const ok = await confirm({
    title: '重置密码',
    content: `确定将「${row.realName || row.username}」的密码重置为默认口令 123456 吗？`,
  });
  if (!ok) return;
  await resetSysUserPassword(row.id!);
  message.success('密码已重置为 123456');
}

/** 启用/禁用切换 */
async function handleToggleStatus(row: SysUserApi.SysUser) {
  const next = row.status === 1 ? 0 : 1;
  const ok = await confirm({
    title: next === 1 ? '启用用户' : '禁用用户',
    content: `确定${next === 1 ? '启用' : '禁用'}「${row.realName || row.username}」吗？`,
  });
  if (!ok) return;
  await updateSysUserStatus(row.id!, next);
  message.success('操作成功');
  handleRefresh();
}

/** 删除用户 */
async function handleDelete(row: SysUserApi.SysUser) {
  const ok = await confirm({
    title: '删除用户',
    content: `确定删除「${row.realName || row.username}」吗？删除后不可恢复。`,
  });
  if (!ok) return;
  await deleteSysUser(row.id!);
  message.success('删除成功');
  handleRefresh();
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
  },
  gridOptions: {
    columns: useGridColumns(orgNameOf, roleNameOf),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const res = await getSysUserPage({
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
          return { list: res.records, total: res.total };
        },
      },
    },
    pagerConfig: {
      pageSize: 10,
    },
    rowConfig: {
      keyField: 'id',
      isHover: true,
    },
    checkboxConfig: {
      highlight: true,
    },
    toolbarConfig: {
      refresh: true,
      search: true,
    },
  } as VxeTableGridOptions<SysUserApi.SysUser>,
});

onMounted(() => {
  loadOrgMap();
  loadRoleMap();
});
</script>

<template>
  <Page auto-content-height>
    <template #doc>
      <DocAlert title="【系统】用户管理" url="https://doc.iocoder.cn/" />
    </template>

    <FormModal @success="handleRefresh" />
    <Grid table-title="用户列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '新增用户',
              type: 'primary',
              icon: ACTION_ICON.ADD,
              onClick: handleCreate,
            },
          ]"
        />
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '编辑',
              type: 'link',
              icon: ACTION_ICON.EDIT,
              onClick: handleEdit.bind(null, row),
            },
            {
              label: '重置密码',
              type: 'link',
              icon: ACTION_ICON.REFRESH,
              onClick: handleResetPwd.bind(null, row),
            },
            {
              label: row.status === 1 ? '禁用' : '启用',
              type: 'link',
              icon: row.status === 1 ? ACTION_ICON.CLOSE : ACTION_ICON.ADD,
              onClick: handleToggleStatus.bind(null, row),
            },
            {
              label: '删除',
              type: 'link',
              icon: ACTION_ICON.DELETE,
              danger: true,
              onClick: handleDelete.bind(null, row),
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
