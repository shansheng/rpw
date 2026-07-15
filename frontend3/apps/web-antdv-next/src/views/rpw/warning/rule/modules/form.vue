<script lang="ts" setup>
import type { WarningRuleApi } from '#/api/rpw/warning/rule';

import { computed, ref } from 'vue';

import { useVbenModal } from '#/components/modal';

import { message } from 'antdv-next';

import { useVbenForm } from '#/adapter/form';
import {
  createWarningRule,
  getWarningRule,
  updateWarningRule,
} from '#/api/rpw/warning/rule';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

defineOptions({ name: 'WarningRuleForm' });

const emit = defineEmits(['success']);
const formData = ref<WarningRuleApi.WarningRule>();
const getTitle = computed(() => {
  return formData.value?.id
    ? $t('ui.actionTitle.edit', ['预警规则'])
    : $t('ui.actionTitle.create', ['预警规则']);
});

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: {
      class: 'w-full',
    },
    formItemClass: 'col-span-2',
    labelWidth: 100,
  },
  layout: 'horizontal',
  schema: [],
  showDefaultActions: false,
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    modalApi.lock();
    // 提交表单
    const data = (await formApi.getValues()) as WarningRuleApi.WarningRule;
    try {
      await (formData.value?.id
        ? updateWarningRule(data)
        : createWarningRule(data));
      // 关闭并提示
      await modalApi.close();
      emit('success');
      message.success($t('ui.actionMessage.operationSuccess'));
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      formData.value = undefined;
      return;
    }
    formApi.setState({ schema: useFormSchema() });
    const data = modalApi.getData<WarningRuleApi.WarningRule>();
    if (!data || !data.id) {
      await formApi.setValues({ enabled: 1, notifyWecom: 0 });
      return;
    }
    // 加载数据
    modalApi.lock();
    try {
      formData.value = await getWarningRule(data.id);
      // 设置到 values
      await formApi.setValues(formData.value);
    } finally {
      modalApi.unlock();
    }
  },
});
</script>

<template>
  <Modal :title="getTitle" class="w-1/3">
    <Form class="mx-4" />
  </Modal>
</template>
