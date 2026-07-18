<script lang="ts" setup>
import type { ResourcePlanSubcontract } from '#/api/rpw/resource-plan';
import type { OrganizationApi } from '#/api/rpw/organization';

import { computed, ref } from 'vue';

import { useVbenModal } from '#/components/modal';

import { message } from 'antdv-next';

import { useVbenForm } from '#/adapter/form';
import { subcontractApi } from '#/api/rpw/resource-plan';
import { $t } from '#/locales';

import { useFormSchema } from '../data';
import OrganizationSelectModal from './organization-select-modal.vue';

defineOptions({ name: 'RpwResourcePlanSubcontractForm' });

const emit = defineEmits(['success']);
const formData = ref<ResourcePlanSubcontract>();
const orgSelectVisible = ref(false);
const getTitle = computed(() => {
  return formData.value?.id
    ? $t('ui.actionTitle.edit', ['分包计划'])
    : $t('ui.actionTitle.create', ['分包计划']);
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

function openOrganizationSelect() {
  orgSelectVisible.value = true;
}

function handleOrganizationSelect(node: OrganizationApi.Organization) {
  formApi.setFieldValue('projectId', node.id);
  formApi.setFieldValue('projectName', node.orgName);
}

function buildSchema() {
  const schema = useFormSchema();
  const projectNameField = schema.find((f) => f.fieldName === 'projectName');
  if (projectNameField) {
    projectNameField.componentProps = {
      ...(projectNameField.componentProps || {}),
      placeholder: '点击选择项目',
      readonly: true,
      class: 'cursor-pointer',
      onClick: openOrganizationSelect,
    };
  }
  return schema;
}

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    modalApi.lock();
    // 提交表单
    const data = (await formApi.getValues()) as ResourcePlanSubcontract;
    // 新增时确保实际日期类字段为空（防止默认值）
    if (!formData.value?.id) {
      data.actualBidDate = data.actualBidDate || undefined;
      data.actualOnlineBidDate = data.actualOnlineBidDate || undefined;
      data.actualAwardDate = data.actualAwardDate || undefined;
    }
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
    formApi.setState({ schema: buildSchema() });
    const data = modalApi.getData<ResourcePlanSubcontract>();
    if (!data || !data.id) {
      // 新增：重置为空，确保实际日期类字段为空
      await formApi.resetForm();
      await formApi.setValues({
        projectId: undefined,
        projectName: undefined,
        specialtyEngineering: undefined,
        subcontractName: undefined,
        subcontractMode: undefined,
        teamSource: undefined,
        latestEntryDate: undefined,
        actualEntryDate: undefined,
        startPrepareBidDate: undefined,
        actualBidDate: undefined,
        plannedOnlineBidDate: undefined,
        actualOnlineBidDate: undefined,
        plannedAwardDate: undefined,
        actualAwardDate: undefined,
        mobilizationPeriod: undefined,
        remark: undefined,
      });
      return;
    }
    // 编辑：加载数据
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
    <OrganizationSelectModal
      v-model:visible="orgSelectVisible"
      @select="handleOrganizationSelect"
    />
  </Modal>
</template>
