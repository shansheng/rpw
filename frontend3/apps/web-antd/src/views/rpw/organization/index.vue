<script lang="ts" setup>
import type { OrganizationApi } from '#/api/rpw/organization';

import { onMounted, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';

import { Button, Descriptions, Empty, Modal, Tree, message } from 'ant-design-vue';

import { deleteOrganization, getOrganizationTree } from '#/api/rpw/organization';
import { $t } from '#/locales';

import Form from './modules/form.vue';

defineOptions({ name: 'Organization' });

interface TreeNode {
  key: number;
  title: string;
  raw: OrganizationApi.Organization;
  children?: TreeNode[];
}

const treeData = ref<TreeNode[]>([]);
const selectedKey = ref<number | null>(null);
const selectedNode = ref<OrganizationApi.Organization | null>(null);
const expandedKeys = ref<number[]>([]);
const nodeMap = ref<Map<number, OrganizationApi.Organization>>(new Map());

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

function typeTag(n: OrganizationApi.Organization): string {
  if (n.nodeType === 2) return '【部门】';
  return { 1: '【局】', 2: '【公司】', 3: '【项目】' }[n.orgLevel ?? 0] ?? '';
}

function statusText(s?: number): string {
  return s === 1 ? '进行中' : s === 2 ? '已完工' : s === 3 ? '已暂停' : '—';
}

function toTree(nodes: OrganizationApi.Organization[]): TreeNode[] {
  return nodes.map((n) => {
    if (n.id != null) nodeMap.value.set(n.id, n);
    return {
      key: n.id!,
      title: `${typeTag(n)} ${n.orgName}`,
      raw: n,
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

async function loadTree() {
  const tree = await getOrganizationTree();
  nodeMap.value = new Map();
  treeData.value = toTree(tree);
  expandedKeys.value = collectKeys(tree);
}

function onSelect(_keys: number[], info: any) {
  const key = info?.node?.key as number | undefined;
  const node = key != null ? nodeMap.value.get(key) : undefined;
  if (node?.id != null) {
    selectedKey.value = node.id;
    selectedNode.value = node;
  }
}

function handleRefresh() {
  selectedNode.value = null;
  selectedKey.value = null;
  loadTree();
}

function handleAddRoot() {
  formModalApi.setData({ parentId: null, nodeType: 1 }).open();
}

function handleAddChild() {
  if (!selectedNode.value) {
    message.warning('请先在左侧选择一个父节点');
    return;
  }
  formModalApi.setData({ parentNode: selectedNode.value, nodeType: 1 }).open();
}

function handleAddDept() {
  if (!selectedNode.value) {
    message.warning('请先在左侧选择一个组织节点');
    return;
  }
  formModalApi.setData({ parentNode: selectedNode.value, nodeType: 2 }).open();
}

function handleEdit() {
  if (!selectedNode.value) return;
  formModalApi.setData(selectedNode.value).open();
}

function handleDelete() {
  if (!selectedNode.value) return;
  const node = selectedNode.value;
  Modal.confirm({
    title: '确认删除',
    content: `确定删除「${typeTag(node)}${node.orgName}」吗？若存在子节点将被后端拒绝。`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deleteOrganization(node.id!);
      message.success($t('ui.actionMessage.deleteSuccess', [node.orgName]));
      handleRefresh();
    },
  });
}

onMounted(loadTree);
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <div class="flex h-full">
      <!-- 左：组织树 -->
      <div class="flex w-1/3 flex-col border-r p-3">
        <div class="mb-3 flex flex-wrap gap-2">
          <Button type="primary" size="small" @click="handleAddRoot">新增局</Button>
          <Button size="small" :disabled="!selectedNode" @click="handleAddChild">
            新增子组织
          </Button>
          <Button size="small" :disabled="!selectedNode" @click="handleAddDept">
            新增部门
          </Button>
          <Button size="small" @click="handleRefresh">刷新</Button>
        </div>
        <div class="flex-1 overflow-auto">
          <Tree
            v-if="treeData.length"
            v-model:expandedKeys="expandedKeys"
            :tree-data="treeData"
            :selected-keys="selectedKey ? [selectedKey] : []"
            :field-names="{ key: 'key', title: 'title', children: 'children' }"
            @select="onSelect"
          />
          <Empty v-else description="暂无组织数据" />
        </div>
      </div>

      <!-- 右：节点详情 -->
      <div class="flex-1 overflow-auto p-4">
        <template v-if="selectedNode">
          <div class="mb-3 flex items-center justify-between">
            <h2 class="text-lg font-semibold">
              {{ typeTag(selectedNode) }} {{ selectedNode.orgName }}
            </h2>
            <div class="flex gap-2">
              <Button size="small" @click="handleEdit">编辑</Button>
              <Button size="small" danger @click="handleDelete">删除</Button>
            </div>
          </div>
          <Descriptions bordered :column="1" size="small">
            <Descriptions.Item label="类型">{{ typeTag(selectedNode) }}</Descriptions.Item>
            <Descriptions.Item label="级别">{{ selectedNode.orgLevel }}</Descriptions.Item>
            <Descriptions.Item label="排序">{{ selectedNode.sort }}</Descriptions.Item>
            <Descriptions.Item
              v-if="selectedNode.nodeType === 1 && selectedNode.orgLevel === 3"
              label="项目编码"
            >
              {{ selectedNode.projectCode || '—' }}
            </Descriptions.Item>
            <Descriptions.Item
              v-if="selectedNode.nodeType === 1 && selectedNode.orgLevel === 3"
              label="状态"
            >
              {{ statusText(selectedNode.status) }}
            </Descriptions.Item>
            <Descriptions.Item
              v-if="selectedNode.nodeType === 1 && selectedNode.orgLevel === 3"
              label="计划开始"
            >
              {{ selectedNode.planStartDate || '—' }}
            </Descriptions.Item>
            <Descriptions.Item
              v-if="selectedNode.nodeType === 1 && selectedNode.orgLevel === 3"
              label="计划结束"
            >
              {{ selectedNode.planEndDate || '—' }}
            </Descriptions.Item>
            <Descriptions.Item label="ID">{{ selectedNode.id }}</Descriptions.Item>
          </Descriptions>
        </template>
        <Empty v-else description="请在左侧选择节点查看详情" />
      </div>
    </div>
  </Page>
</template>
