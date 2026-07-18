<script lang="ts" setup>
import type { ResourcePlanSubcontract } from '#/api/rpw/resource-plan';

import { computed, h, ref, watch } from 'vue';

import { Button, Empty, Input, Modal, Table, message } from 'ant-design-vue';

import { subcontractApi } from '#/api/rpw/resource-plan';

const props = defineProps<{
  visible: boolean;
  projectId?: number;
}>();

const emit = defineEmits<{
  (e: 'update:visible', visible: boolean): void;
  (e: 'select', plan: ResourcePlanSubcontract): void;
}>();

const plans = ref<ResourcePlanSubcontract[]>([]);
const loading = ref(false);
const keyword = ref('');

const open = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val),
});

const filteredPlans = computed(() => {
  const kw = keyword.value.trim();
  if (!kw) return plans.value;
  return plans.value.filter(
    (p) =>
      (p.subcontractName ?? '').includes(kw) ||
      (p.specialtyEngineering ?? '').includes(kw),
  );
});

async function loadPlans() {
  if (!props.projectId) {
    message.warning('请先选择项目');
    return;
  }
  loading.value = true;
  try {
    const list = await subcontractApi.getList({ projectId: props.projectId, pageSize: 9999 });
    plans.value = list ?? [];
    if (plans.value.length === 0) {
      message.info('该项目下暂无分包计划');
    }
  } catch (e) {
    message.error('加载分包计划失败');
    plans.value = [];
  } finally {
    loading.value = false;
  }
}

function handleSelect(record: ResourcePlanSubcontract) {
  emit('select', record);
  open.value = false;
}

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      keyword.value = '';
      loadPlans();
    }
  },
);

const columns = [
  { title: '分包计划ID', dataIndex: 'id', width: 100 },
  { title: '分包名称', dataIndex: 'subcontractName', width: 160 },
  { title: '专业工程', dataIndex: 'specialtyEngineering', width: 140 },
  { title: '分包模式', dataIndex: 'subcontractMode', width: 120 },
  { title: '队伍来源', dataIndex: 'teamSource', width: 120 },
  {
    title: '操作',
    key: 'action',
    width: 90,
    customRender: ({ record }: any) =>
      h(Button, { type: 'link', onClick: () => handleSelect(record) }, () => '选择'),
  },
];
</script>

<template>
  <Modal
    v-model:open="open"
    title="选择分包计划"
    width="760px"
    :footer="null"
  >
    <div class="mb-3 flex items-center gap-2">
      <Input
        v-model:value="keyword"
        placeholder="搜索分包名称/专业工程"
        allow-clear
        class="max-w-[280px]"
      />
      <span class="text-muted-foreground text-xs">共 {{ filteredPlans.length }} 条</span>
    </div>
    <div class="max-h-[420px] overflow-y-auto">
      <Table
        v-if="filteredPlans.length > 0"
        :columns="columns"
        :data-source="filteredPlans"
        :loading="loading"
        :pagination="false"
        row-key="id"
        size="small"
        :custom-row="
          (record: any) => ({
            onClick: () => handleSelect(record),
            style: 'cursor:pointer',
          })
        "
      />
      <Empty v-else description="该项目下暂无分包计划" />
    </div>
  </Modal>
</template>
