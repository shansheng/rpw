<script lang="ts" setup>
import type { ProjectApi } from '#/api/rpw/project';

import { computed, ref } from 'vue';

import { useVbenModal } from '#/components/modal';

const emit = defineEmits(['success']);

const rowData = ref<ProjectApi.Project>();
const status = ref<number>();

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: {
      class: 'w-full',
    },
    formItemClass: 'col-span-2',
    labelWidth: 80,
  },
  layout: 'horizontal',
  schema: [
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        allowClear: false,
        placeholder: '请选择状态',
        options: [
          { label: '1进行中', value: 1 },
          { label: '2已完工', value: 2 },
          { label: '3已暂停', value: 3 },
        ],
      },
      rules: 'required',
    },
  ],
  showDefaultActions: false,
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    const values = (await formApi.getValues()) as { status: number };
    emit('success', rowData.value, values.status);
    await modalApi.close();
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      rowData.value = undefined;
      return;
    }
    const data = modalApi.getData<ProjectApi.Project>();
    rowData.value = data;
    await formApi.setValues({ status: data?.status });
  },
});
</script>

<template>
  <Modal title="更新项目状态" class="w-1/4">
    <Form class="mx-4" />
  </Modal>
</template>
