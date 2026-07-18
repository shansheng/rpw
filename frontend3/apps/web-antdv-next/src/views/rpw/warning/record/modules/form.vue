<script lang="ts" setup>
import { computed, ref } from 'vue';

import { useVbenModal } from '#/components/modal';

import { message } from 'antdv-next';

import { useVbenForm } from '#/adapter/form';
import {
  batchHandleWarningRecord,
  handleWarningRecord,
} from '#/api/rpw/warning/record';
import { $t } from '#/locales';

import { useHandleFormSchema } from '../data';

defineOptions({ name: 'WarningRecordHandleForm' });

const emit = defineEmits(['success']);
const ids = ref<number[]>([]);
const getTitle = computed(() =>
  ids.value.length > 1 ? '批量处理预警' : '处理预警',
);

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: {
      class: 'w-full',
    },
    formItemClass: 'col-span-2',
    labelWidth: 100,
  },
  layout: 'horizontal',
  schema: useHandleFormSchema(),
  showDefaultActions: false,
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    modalApi.lock();
    const data = (await formApi.getValues()) as {
      status: string;
      handleRemark: string;
    };
    try {
      if (ids.value.length === 1) {
        await handleWarningRecord(
          ids.value[0]!,
          data.status,
          data.handleRemark,
        );
      } else {
        await batchHandleWarningRecord(
          ids.value,
          data.status,
          data.handleRemark,
        );
      }
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
      ids.value = [];
      return;
    }
    const data = modalApi.getData<{ ids: number[] }>();
    ids.value = data?.ids ?? [];
    await formApi.setValues({ status: undefined, handleRemark: undefined });
  },
});
</script>

<template>
  <Modal :title="getTitle" class="w-1/3">
    <Form class="mx-4" />
  </Modal>
</template>
