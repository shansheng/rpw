<script lang="ts" setup>
import type { OrganizationApi } from '#/api/rpw/organization';

import { computed, ref } from 'vue';

import { useVbenModal } from '#/components/modal';

import { message } from 'antdv-next';

import { useVbenForm } from '#/adapter/form';
import {
  createOrganization,
  getOrganization,
  updateOrganization,
} from '#/api/rpw/organization';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

defineOptions({ name: 'OrganizationForm' });

const emit = defineEmits(['success']);
const formData = ref<OrganizationApi.Organization>();
const getTitle = computed(() => {
  return formData.value?.id
    ? $t('ui.actionTitle.edit', ['组织'])
    : $t('ui.actionTitle.create', ['组织']);
});

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: {
      class: 'w-full',
    },
    formItemClass: 'col-span-2',
    labelWidth: 80,
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
    const data = (await formApi.getValues()) as OrganizationApi.Organization;
    try {
      await (formData.value?.id
        ? updateOrganization(data)
        : createOrganization(data));
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
    formApi.setState({ schema: useFormSchema(formApi) });
    const data = modalApi.getData<OrganizationApi.Organization>();
    if (!data || !data.id) {
      return;
    }
    // 加载数据
    modalApi.lock();
    try {
      formData.value = await getOrganization(data.id);
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
