<script lang="ts" setup>
import type { OrganizationApi } from '#/api/rpw/organization';

import { computed, ref, watch } from 'vue';

import { useUserStore } from '@vben/stores';

import { Button, Empty, Modal, Tree, message } from 'antdv-next';

import { getOrganizationTree } from '#/api/rpw/organization';

interface TreeNode {
  key: number;
  title: string;
  raw: OrganizationApi.Organization;
  children?: TreeNode[];
  disabled?: boolean;
}

const props = defineProps<{
  visible: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:visible', visible: boolean): void;
  (e: 'select', node: OrganizationApi.Organization): void;
}>();

const userStore = useUserStore();
const userOrgId = (userStore.userInfo as any)?.orgId as number | undefined;

const treeData = ref<TreeNode[]>([]);
const nodeMap = ref<Map<number, OrganizationApi.Organization>>(new Map());
const expandedKeys = ref<number[]>([]);
const selectedKey = ref<number | null>(null);
const loading = ref(false);

const open = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val),
});

function typeTag(n: OrganizationApi.Organization): string {
  if (n.nodeType === 2) return '【部门】';
  return { 1: '【局】', 2: '【公司】', 3: '【项目】' }[n.orgLevel ?? 0] ?? '';
}

function toTree(nodes: OrganizationApi.Organization[]): TreeNode[] {
  return nodes.map((n) => {
    if (n.id != null) nodeMap.value.set(n.id, n);
    return {
      key: n.id!,
      title: `${typeTag(n)} ${n.orgName}`,
      raw: n,
      disabled: false,
      children: n.children?.length ? toTree(n.children) : undefined,
    };
  });
}

function collectKeys(nodes: OrganizationApi.Organization[], acc: number[] = []): number[] {
  for (const n of nodes) {
    if (n.id != null) acc.push(n.id);
    if (n.children) collectKeys(n.children, acc);
  }
  return acc;
}

/** 权限过滤：只保留用户 orgId 所在节点及其子树 */
function filterByPermission(nodes: OrganizationApi.Organization[]): OrganizationApi.Organization[] {
  if (userOrgId == null) return nodes;
  const result: OrganizationApi.Organization[] = [];
  for (const n of nodes) {
    if (n.id === userOrgId) {
      result.push(n);
    } else if (n.children && n.children.length > 0) {
      const filtered = filterByPermission(n.children);
      if (filtered.length > 0) {
        result.push({ ...n, children: filtered });
      }
    }
  }
  return result;
}

async function loadTree() {
  loading.value = true;
  try {
    const tree = await getOrganizationTree();
    const filtered = filterByPermission(tree);
    nodeMap.value = new Map();
    treeData.value = toTree(filtered);
    expandedKeys.value = collectKeys(filtered);
  } catch (error) {
    message.error('加载组织机构失败');
    treeData.value = [];
  } finally {
    loading.value = false;
  }
}

function onSelect(_keys: any, info: any) {
  const key = info?.node?.key as number | undefined;
  if (key != null) {
    selectedKey.value = key;
  }
}

function handleConfirm() {
  if (selectedKey.value == null) {
    message.warning('请选择一个项目');
    return;
  }
  const node = nodeMap.value.get(selectedKey.value);
  if (!node) {
    message.warning('未找到选中的组织节点');
    return;
  }
  // 只允许选择项目级（orgLevel=3）作为项目
  if (node.orgLevel !== 3) {
    message.warning('请选择项目级节点（orgLevel=3）');
    return;
  }
  emit('select', node);
  open.value = false;
}

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      selectedKey.value = null;
      loadTree();
    }
  },
  { immediate: true },
);
</script>

<template>
  <Modal
    v-model:open="open"
    title="选择项目"
    width="520px"
    :confirm-loading="loading"
    @ok="handleConfirm"
  >
    <div class="max-h-[400px] overflow-y-auto">
      <Tree
        v-if="treeData.length > 0"
        :tree-data="treeData"
        :expanded-keys="expandedKeys"
        :selected-keys="selectedKey ? [selectedKey] : []"
        :field-names="{ key: 'key', title: 'title' }"
        @select="onSelect"
        @expand="expandedKeys = $event as number[]"
      />
      <Empty v-else description="暂无权限内的组织机构" />
    </div>
    <template #footer>
      <Button @click="open = false">取消</Button>
      <Button type="primary" @click="handleConfirm">确定</Button>
    </template>
  </Modal>
</template>
