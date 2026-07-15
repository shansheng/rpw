<script lang="ts" setup>
import type { OrganizationApi } from '#/api/rpw/organization';
import type { SystemUserApi } from '#/api/system/user';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Avatar,
  Button,
  Card,
  Col,
  Empty,
  Input,
  message,
  Pagination,
  Row,
} from 'antdv-next';

import { getOrganizationTree } from '#/api/rpw/organization';
import { getUserPage } from '#/api/system/user';
import OrgTree from './org-tree.vue';

interface Props {
  cancelText?: string;
  confirmText?: string;
  multiple?: boolean;
  title?: string;
  value?: number[];
}

defineOptions({ name: 'UserSelectModal' });

withDefaults(defineProps<Props>(), {
  title: '选择用户',
  multiple: true,
  value: () => [],
  confirmText: '确定',
  cancelText: '取消',
});

const emit = defineEmits<{
  cancel: [];
  closed: [];
  confirm: [value: SystemUserApi.User[]];
  'update:value': [value: number[]];
}>();

// ===== 左侧组织树 =====
interface OrgTreeNode {
  key: number;
  title: string;
  raw: OrganizationApi.Organization;
  children?: OrgTreeNode[];
}

const orgTreeData = ref<OrgTreeNode[]>([]);

function orgTypeTag(n: OrganizationApi.Organization): string {
  if (n.nodeType === 2) return '【部门】';
  return { 1: '【局】', 2: '【公司】', 3: '【项目】' }[n.orgLevel ?? 0] ?? '';
}

function toOrgTree(nodes: OrganizationApi.Organization[]): OrgTreeNode[] {
  return nodes.map((n) => ({
    key: n.id!,
    title: `${orgTypeTag(n)} ${n.orgName}`,
    raw: n,
    children: n.children?.length ? toOrgTree(n.children) : undefined,
  }));
}

const selectedOrgId = ref<number | undefined>(undefined);

// ===== 用户数据 =====
const userList = ref<SystemUserApi.User[]>([]); // 已知用户缓存（含已选）
const selectedUserIds = ref<string[]>([]); // 已选用户 ID（字符串）

const leftListState = ref({
  searchValue: '',
  dataSource: [] as SystemUserApi.User[],
  pagination: { current: 1, pageSize: 10, total: 0 },
});

const rightListState = ref({
  searchValue: '',
  dataSource: [] as SystemUserApi.User[],
  pagination: { current: 1, pageSize: 10, total: 0 },
});

// 左侧展示：剔除已选中的用户，避免与右侧重复
const leftDisplay = computed(() =>
  leftListState.value.dataSource.filter(
    (u) => !selectedUserIds.value.includes(String(u.id)),
  ),
);

// 右侧展示：已选中用户（按搜索 + 分页）
const rightDisplay = computed(() => rightListState.value.dataSource);

const [Modal, modalApi] = useVbenModal({
  onCancel: handleCancel,
  onClosed: handleClosed,
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      resetData();
      return;
    }
    const data = modalApi.getData();
    if (!data) {
      return;
    }
    modalApi.lock();
    try {
      // 组织树
      orgTreeData.value = toOrgTree(await getOrganizationTree());

      // 待选用户首页
      await loadUserData(1, leftListState.value.pagination.pageSize);

      // 回显已选用户
      if (data.userIds?.length) {
        selectedUserIds.value = data.userIds.map(String);
        const { list } = await getUserPage({
          pageNo: 1,
          pageSize: 100,
          userIds: data.userIds,
        });
        const userMap = new Map(userList.value.map((u) => [u.id, u]));
        list.forEach((u) => {
          if (!userMap.has(u.id)) {
            userMap.set(u.id, u);
          }
        });
        userList.value = [...userMap.values()];
        updateRightListData();
      }
    } finally {
      modalApi.unlock();
    }
  },
  destroyOnClose: true,
});

/** 加载待选用户（左侧列表） */
async function loadUserData(pageNo: number, pageSize: number) {
  const { list, total } = await getUserPage({
    pageNo,
    pageSize,
    deptId: selectedOrgId.value,
    username: leftListState.value.searchValue || undefined,
  });
  leftListState.value.dataSource = list;
  leftListState.value.pagination.total = total;
  leftListState.value.pagination.current = pageNo;
  leftListState.value.pagination.pageSize = pageSize;

  const newUsers = list.filter(
    (u) => !userList.value.some((x) => x.id === u.id),
  );
  if (newUsers.length > 0) {
    userList.value.push(...newUsers);
  }
}

/** 根据已选 + 搜索 + 分页，刷新右侧列表 */
function updateRightListData() {
  const uniqueIds = [...new Set(selectedUserIds.value)];
  let selected = userList.value.filter((u) =>
    uniqueIds.includes(String(u.id)),
  );
  if (rightListState.value.searchValue) {
    const kw = rightListState.value.searchValue.toLowerCase();
    selected = selected.filter((u) =>
      (u.nickname ?? u.username).toLowerCase().includes(kw),
    );
  }
  rightListState.value.pagination.total = selected.length;
  const { current, pageSize } = rightListState.value.pagination;
  const start = (current - 1) * pageSize;
  rightListState.value.dataSource = selected.slice(start, start + pageSize);
}

function isSelected(u: SystemUserApi.User): boolean {
  return selectedUserIds.value.includes(String(u.id));
}

/** 左侧点击：切换选中 */
function toggleSelect(u: SystemUserApi.User) {
  if (!u.id && u.id !== 0) return;
  const id = String(u.id);
  if (selectedUserIds.value.includes(id)) {
    selectedUserIds.value = selectedUserIds.value.filter((x) => x !== id);
  } else {
    selectedUserIds.value = [...selectedUserIds.value, id];
  }
  emit('update:value', selectedUserIds.value.map(Number));
  updateRightListData();
}

/** 右侧点击 X：移除选中 */
function removeSelect(u: SystemUserApi.User) {
  if (!u.id && u.id !== 0) return;
  const id = String(u.id);
  selectedUserIds.value = selectedUserIds.value.filter((x) => x !== id);
  emit('update:value', selectedUserIds.value.map(Number));
  updateRightListData();
}

/** 组织树节点点击 → 按组织过滤待选用户 */
function handleOrgSelect(org?: OrganizationApi.Organization) {
  selectedOrgId.value = org?.id;
  leftListState.value.pagination.current = 1;
  loadUserData(1, leftListState.value.pagination.pageSize);
}

/** 左侧搜索 */
function onLeftSearch(value: string) {
  leftListState.value.searchValue = value;
  leftListState.value.pagination.current = 1;
  loadUserData(1, leftListState.value.pagination.pageSize);
}

/** 左侧分页 */
function onLeftPage({ current, pageSize }: { current: number; pageSize: number }) {
  loadUserData(current, pageSize);
}

/** 右侧搜索 */
function onRightSearch(value: string) {
  rightListState.value.searchValue = value;
  rightListState.value.pagination.current = 1;
  updateRightListData();
}

/** 右侧分页 */
function onRightPage({ current, pageSize }: { current: number; pageSize: number }) {
  rightListState.value.pagination.current = current;
  rightListState.value.pagination.pageSize = pageSize;
  updateRightListData();
}

/** 重置 */
function resetData() {
  userList.value = [];
  selectedUserIds.value = [];
  selectedOrgId.value = undefined;
  leftListState.value = {
    searchValue: '',
    dataSource: [],
    pagination: { current: 1, pageSize: 10, total: 0 },
  };
  rightListState.value = {
    searchValue: '',
    dataSource: [],
    pagination: { current: 1, pageSize: 10, total: 0 },
  };
}

/** 确认 */
function handleConfirm() {
  if (selectedUserIds.value.length === 0) {
    message.warning('请选择用户');
    return;
  }
  emit(
    'confirm',
    userList.value.filter((u) =>
      selectedUserIds.value.includes(String(u.id)),
    ),
  );
  modalApi.close();
}

/** 取消 */
function handleCancel() {
  emit('cancel');
  modalApi.close();
}

/** 关闭完成 */
function handleClosed() {
  emit('closed');
  resetData();
}
</script>

<template>
  <Modal class="!w-[900px] max-w-[94vw]" :title="title">
    <Row :gutter="[12, 12]" class="h-[560px]">
      <!-- 左列：组织树 -->
      <Col :span="10" class="h-full">
        <Card
          class="flex h-full flex-col overflow-hidden"
          :body-style="{ padding: '8px' }"
        >
          <div class="mb-2 shrink-0 font-semibold">组织</div>
          <div class="min-h-0 flex-1 overflow-auto">
            <OrgTree :tree-data="orgTreeData" @select="handleOrgSelect" />
          </div>
        </Card>
      </Col>

      <!-- 中列：待选择人员 -->
      <Col :span="7" class="h-full">
        <Card
          class="flex h-full flex-col overflow-hidden"
          :body-style="{ padding: '8px' }"
        >
          <div class="mb-2 flex shrink-0 items-center gap-2">
            <span class="shrink-0 whitespace-nowrap font-semibold">待选择</span>
            <span class="shrink-0 whitespace-nowrap text-xs text-gray-400">
              ({{ leftListState.pagination.total }} 人)
            </span>
            <Input
              :model-value="leftListState.searchValue"
              placeholder="搜索用户名"
              allow-clear
              size="small"
              class="ml-auto w-1/2"
              @update:value="onLeftSearch"
            />
          </div>
          <div class="min-h-0 flex-1 overflow-auto">
            <div
              v-for="u in leftDisplay"
              :key="u.id"
              class="flex cursor-pointer items-center gap-2 rounded p-2 hover:bg-gray-100 dark:hover:bg-gray-700"
              @click="toggleSelect(u)"
            >
              <Avatar v-if="u.avatar" class="size-7" :src="u.avatar" />
              <Avatar v-else class="size-7">
                {{ (u.nickname || u.username)?.substring(0, 1) }}
              </Avatar>
              <div class="min-w-0 flex-1">
                <div class="truncate">{{ u.nickname }}</div>
                <div class="truncate text-xs text-gray-400">
                  {{ u.username
                  }}<span v-if="u.mobile"> · {{ u.mobile }}</span>
                </div>
              </div>
              <IconifyIcon
                v-if="isSelected(u)"
                icon="lucide:check"
                class="size-4 shrink-0 text-primary"
              />
            </div>
            <Empty
              v-if="!leftDisplay.length"
              description="暂无用户"
            />
          </div>
          <div class="mt-2 shrink-0 text-right">
            <Pagination
              size="small"
              :current="leftListState.pagination.current"
              :page-size="leftListState.pagination.pageSize"
              :total="leftListState.pagination.total"
              :show-size-changer="true"
              :show-total="(t: number) => `共 ${t} 条`"
              @change="onLeftPage"
            />
          </div>
        </Card>
      </Col>

      <!-- 右列：已选中人员 -->
      <Col :span="7" class="h-full">
        <Card
          class="flex h-full flex-col overflow-hidden"
          :body-style="{ padding: '8px' }"
        >
          <div class="mb-2 flex shrink-0 items-center gap-2">
            <span class="shrink-0 whitespace-nowrap font-semibold">已选中</span>
            <span class="shrink-0 whitespace-nowrap text-xs text-gray-400">
              ({{ rightListState.pagination.total }} 人)
            </span>
            <Input
              :model-value="rightListState.searchValue"
              placeholder="搜索已选用户"
              allow-clear
              size="small"
              class="ml-auto w-1/2"
              @update:value="onRightSearch"
            />
          </div>
          <div class="min-h-0 flex-1 overflow-auto">
            <div
              v-for="u in rightDisplay"
              :key="u.id"
              class="flex items-center gap-2 rounded p-2 hover:bg-gray-100 dark:hover:bg-gray-700"
            >
              <Avatar v-if="u.avatar" class="size-7" :src="u.avatar" />
              <Avatar v-else class="size-7">
                {{ (u.nickname || u.username)?.substring(0, 1) }}
              </Avatar>
              <div class="min-w-0 flex-1">
                <div class="truncate">{{ u.nickname }}</div>
                <div class="truncate text-xs text-gray-400">
                  {{ u.username
                  }}<span v-if="u.mobile"> · {{ u.mobile }}</span>
                </div>
              </div>
              <IconifyIcon
                icon="lucide:x"
                class="size-4 shrink-0 cursor-pointer text-gray-400 hover:text-red-500"
                @click="removeSelect(u)"
              />
            </div>
            <Empty
              v-if="!rightDisplay.length"
              description="尚未选择用户"
            />
          </div>
          <div class="mt-2 shrink-0 text-right">
            <Pagination
              size="small"
              :current="rightListState.pagination.current"
              :page-size="rightListState.pagination.pageSize"
              :total="rightListState.pagination.total"
              :show-size-changer="true"
              :show-total="(t: number) => `共 ${t} 条`"
              @change="onRightPage"
            />
          </div>
        </Card>
      </Col>
    </Row>
    <template #footer>
      <Button
        type="primary"
        :disabled="selectedUserIds.length === 0"
        @click="handleConfirm"
      >
        {{ confirmText }}
      </Button>
      <Button @click="handleCancel">{{ cancelText }}</Button>
    </template>
  </Modal>
</template>

<style lang="scss" scoped>
:deep(.ant-card-body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}
</style>
