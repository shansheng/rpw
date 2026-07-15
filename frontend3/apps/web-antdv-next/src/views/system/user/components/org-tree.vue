<script lang="ts" setup>
import type { OrganizationApi } from '#/api/rpw/organization';

import { computed, ref, watch } from 'vue';

import { Empty, Tree } from 'antdv-next';

interface TreeNode {
  key: number;
  title: string;
  raw: OrganizationApi.Organization;
  children?: TreeNode[];
}

const props = defineProps<{ treeData: TreeNode[] }>();
const emit = defineEmits<{ select: [node?: OrganizationApi.Organization] }>();

function collectKeys(nodes: TreeNode[], acc: number[] = []): number[] {
  for (const n of nodes) {
    acc.push(n.key);
    if (n.children) collectKeys(n.children, acc);
  }
  return acc;
}

function buildMap(
  nodes: TreeNode[],
  map: Map<number, OrganizationApi.Organization> = new Map(),
): Map<number, OrganizationApi.Organization> {
  for (const n of nodes) {
    if (n.raw?.id != null) map.set(n.raw.id, n.raw);
    if (n.children) buildMap(n.children, map);
  }
  return map;
}

const expandedKeys = ref<number[]>([]);
const allKeys = computed(() => collectKeys(props.treeData));
watch(
  () => props.treeData,
  (v) => {
    expandedKeys.value = collectKeys(v);
  },
  { immediate: true },
);

function onSelect(_keys: number[], info: any) {
  const key = info?.node?.key as number | undefined;
  if (key == null) {
    emit('select', undefined);
    return;
  }
  emit('select', buildMap(props.treeData).get(key));
}
</script>

<template>
  <div class="h-full overflow-auto">
    <Tree
      v-if="treeData.length"
      v-model:expandedKeys="expandedKeys"
      :tree-data="treeData"
      :default-expanded-keys="allKeys"
      :field-names="{ key: 'key', title: 'title', children: 'children' }"
      @select="onSelect"
    />
    <Empty v-else description="暂无组织数据" />
  </div>
</template>
