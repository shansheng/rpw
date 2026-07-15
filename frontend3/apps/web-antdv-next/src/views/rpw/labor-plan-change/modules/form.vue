<script lang="ts" setup>
import type { LaborPlanChangeApi } from '#/api/rpw/labor-plan-change';

import { computed } from 'vue';

import { useVbenModal } from '#/components/modal';

import { message } from 'antdv-next';

import { useVbenForm } from '#/adapter/form';
import { createLaborPlanChange } from '#/api/rpw/labor-plan-change';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

defineOptions({ name: 'LaborPlanChangeForm' });

const emit = defineEmits(['success']);

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: {
      class: 'w-full',
    },
    formItemClass: 'col-span-2',
    labelWidth: 110,
  },
  layout: 'horizontal',
  schema: useFormSchema(),
  showDefaultActions: false,
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    modalApi.lock();
    const values = (await formApi.getValues()) as LaborPlanChangeApi.LaborPlanChange;
    try {
      await createLaborPlanChange(values);
      await modalApi.close();
      emit('success');
      message.success($t('ui.actionMessage.operationSuccess'));
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      return;
    }
    const data = modalApi.getData<Partial<LaborPlanChangeApi.LaborPlanChange>>();
    await formApi.setValues(data ?? {});
  },
});

const getTitle = computed(() => '新增劳动力计划变更');
</script>

<template>
  <Modal :title="getTitle" class="w-1/2">
    <Form class="mx-4" />
  </Modal>
</template>
