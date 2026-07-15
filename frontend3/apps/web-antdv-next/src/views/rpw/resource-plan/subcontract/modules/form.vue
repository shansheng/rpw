<script lang="ts" setup>
import type { ResourcePlanSubcontract } from '#/api/rpw/resource-plan';

import { computed, ref } from 'vue';

import { useVbenModal } from '#/components/modal';

import { message } from 'antdv-next';

import { useVbenForm } from '#/adapter/form';
import { subcontractApi } from '#/api/rpw/resource-plan';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

defineOptions({ name: 'RpwResourcePlanSubcontractForm' });

const emit = defineEmits(['success']);
const formData = ref<ResourcePlanSubcontract>();
const getTitle = computed(() => {
  return formData.value?.id
    ? $t('ui.actionTitle.edit', ['资源计划'])
    : $t('ui.actionTitle.create', ['资源计划']);
});

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: {
      class: 'w-full',
    },
    formItemClass: 'col-span-2',
    labelWidth: 110,
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
    const data = (await formApi.getValues()) as ResourcePlanSubcontract;
    try {
      await (formData.value?.id
        ? subcontractApi.update(data)
        : subcontractApi.create(data));
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
    const data = modalApi.getData<ResourcePlanSubcontract>();
    if (!data || !data.id) {
      return;
    }
    // 加载数据
    modalApi.lock();
    try {
      formData.value = await subcontractApi.get(data.id);
      // 设置到 values
      await formApi.setValues(formData.value);
    } finally {
      modalApi.unlock();
    }
  },
});
</script>

<template>
  <Modal :title="getTitle" class="w-1/2">
    <Form class="mx-4" />
  </Modal>
</template>
