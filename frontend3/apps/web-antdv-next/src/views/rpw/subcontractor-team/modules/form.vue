<script lang="ts" setup>
import type { SubcontractorTeamApi } from '#/api/rpw/subcontractor-team';

import { computed, ref } from 'vue';

import { useVbenModal } from '#/components/modal';

import { message } from 'antdv-next';

import { useVbenForm } from '#/adapter/form';
import {
  createSubcontractorTeam,
  getSubcontractorTeam,
  updateSubcontractorTeam,
} from '#/api/rpw/subcontractor-team';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

defineOptions({ name: 'SubcontractorTeamForm' });

const emit = defineEmits(['success']);
const formData = ref<SubcontractorTeamApi.SubcontractorTeam>();
const getTitle = computed(() => {
  return formData.value?.id
    ? $t('ui.actionTitle.edit', ['分包队伍统计'])
    : $t('ui.actionTitle.create', ['分包队伍统计']);
});

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: {
      class: 'w-full',
    },
    formItemClass: 'col-span-2',
    labelWidth: 150,
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
    const data = (await formApi.getValues()) as SubcontractorTeamApi.SubcontractorTeam;
    try {
      await (formData.value?.id ? updateSubcontractorTeam(data) : createSubcontractorTeam(data));
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
    const data = modalApi.getData<SubcontractorTeamApi.SubcontractorTeam>();
    if (!data || !data.id) {
      return;
    }
    modalApi.lock();
    try {
      formData.value = await getSubcontractorTeam(data.id);
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
