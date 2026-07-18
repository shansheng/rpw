<script lang="ts" setup>
import type { ResourcePlanSubcontract } from '#/api/rpw/resource-plan';

import { computed, h, ref, watch } from 'vue';

import { Modal, Table, message } from 'antdv-next';

import { DATE_TYPE_OPTIONS } from '#/api/rpw/resource-plan/subcontract-change';

const props = defineProps<{
  visible: boolean;
  /** 当前选中的分包计划（用于读取原日期） */
  plan?: ResourcePlanSubcontract | null;
  /** 明细中已存在的日期类型，将被禁用（忽略） */
  existingTypes?: number[];
}>();

const emit = defineEmits<{
  (e: 'update:visible', visible: boolean): void;
  (e: 'select', payload: { dateTypes: number[] }): void;
}>();

const open = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val),
});

const existingSet = computed(() => new Set(props.existingTypes ?? []));

const rows = computed(() =>
  DATE_TYPE_OPTIONS.map((o) => ({
    dateType: o.value,
    label: o.label,
    originalDate: (props.plan as any)?.[o.field] as string | undefined,
    /** 已存在于明细中的类型禁用 */
    disabled: existingSet.value.has(o.value),
  })),
);

const selectedRowKeys = ref<number[]>([]);

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      if (!props.plan) {
        message.warning('请先选择分包计划');
      }
      // 默认勾选所有可选（未禁用）的日期类型，方便一键全选
      selectedRowKeys.value = rows.value
        .filter((r) => !r.disabled)
        .map((r) => r.dateType);
    }
  },
);

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: Array<string | number>) => {
    selectedRowKeys.value = keys.map((k) => Number(k));
  },
  getCheckboxProps: (record: any) => ({
    disabled: Boolean(record.disabled),
  }),
}));

function handleConfirm() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请至少选择一个日期类型');
    return;
  }
  emit('select', { dateTypes: [...selectedRowKeys.value] });
  open.value = false;
}

const columns = [
  { title: '日期类型', dataIndex: 'label', width: 160 },
  {
    title: '原日期（当前分包计划值）',
    dataIndex: 'originalDate',
    customRender: ({ text }: any) => text || '—',
  },
  {
    title: '状态',
    key: 'status',
    width: 90,
    customRender: ({ record }: any) =>
      record.disabled
        ? h('span', { style: 'color:#999' }, '已选')
        : '',
  },
];
</script>

<template>
  <Modal
    v-model:open="open"
    title="选择日期类型"
    width="560px"
    :ok-text="'确定'"
    @ok="handleConfirm"
  >
    <div class="mb-2 text-muted-foreground text-xs">
      勾选需要变更的日期类型，点击「确定」后将按所选类型插入明细；已存在的类型自动忽略（灰显）。
    </div>
    <Table
      :columns="columns"
      :data-source="rows"
      :pagination="false"
      row-key="dateType"
      size="small"
      :row-selection="rowSelection"
    />
  </Modal>
</template>
