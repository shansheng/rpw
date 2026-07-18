<script lang="ts" setup>
import type { ResourcePlanSubcontract } from '#/api/rpw/resource-plan';

import { computed, h, ref, watch } from 'vue';

import { Modal, Table, message } from 'ant-design-vue';

import { DATE_TYPE_OPTIONS } from '#/api/rpw/resource-plan/subcontract-change';

const props = defineProps<{
  visible: boolean;
  /** 当前选中的分包计划（用于读取原日期） */
  plan?: ResourcePlanSubcontract | null;
}>();

const emit = defineEmits<{
  (e: 'update:visible', visible: boolean): void;
  (e: 'select', payload: { dateType: number; originalDate: string | undefined }): void;
}>();

const open = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val),
});

const rows = computed(() =>
  DATE_TYPE_OPTIONS.map((o) => ({
    dateType: o.value,
    label: o.label,
    originalDate: (props.plan as any)?.[o.field] as string | undefined,
  })),
);

function handleSelect(record: { dateType: number; originalDate: string | undefined }) {
  emit('select', { dateType: record.dateType, originalDate: record.originalDate });
  open.value = false;
}

watch(
  () => props.visible,
  (visible) => {
    if (visible && !props.plan) {
      message.warning('请先选择分包计划');
    }
  },
);

const columns = [
  { title: '日期类型', dataIndex: 'label', width: 200 },
  {
    title: '原日期（当前分包计划值）',
    dataIndex: 'originalDate',
    customRender: ({ text }: any) => text || '—',
  },
  {
    title: '操作',
    key: 'action',
    width: 90,
    customRender: ({ record }: any) =>
      h(
        'Button',
        { type: 'link', onClick: () => handleSelect(record) },
        () => '选择',
      ),
  },
];
</script>

<template>
  <Modal v-model:open="open" title="选择日期类型" width="560px" :footer="null">
    <div class="mb-2 text-muted-foreground text-xs">
      下列为所选分包计划的当前日期，选择一项作为本次变更调整的日期类型。
    </div>
    <Table
      :columns="columns"
      :data-source="rows"
      :pagination="false"
      row-key="dateType"
      size="small"
      :custom-row="
        (record: any) => ({
          onClick: () => handleSelect(record),
          style: 'cursor:pointer',
        })
      "
    />
  </Modal>
</template>
