<script lang="ts" setup>
import type { OrganizationApi } from '#/api/rpw/organization';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import {
  createOrganization,
  getOrganization,
  updateOrganization,
} from '#/api/rpw/organization';
import { $t } from '#/locales';

import { useFormSchema, type OrgFormCtx } from '../data';

defineOptions({ name: 'OrganizationForm' });

const emit = defineEmits(['success']);
const formData = ref<OrganizationApi.Organization>();
const isEdit = computed(() => !!formData.value?.id);

/** 表单 schema 上下文（决定节点类型/级别/是否展示项目字段） */
const ctx = ref<OrgFormCtx>({ nodeType: 1, expectedLevel: 1, isEdit: false });

const getTitle = computed(() =>
  isEdit.value ? $t('ui.actionTitle.edit', ['组织节点']) : $t('ui.actionTitle.create', ['组织节点']),
);

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: { class: 'w-full' },
    labelWidth: 90,
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
    const values = (await formApi.getValues()) as OrganizationApi.Organization;
    try {
      await (isEdit.value ? updateOrganization(values) : createOrganization(values));
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
    const data = modalApi.getData<
      Partial<OrganizationApi.Organization> & { parentNode?: OrganizationApi.Organization }
    >();

    if (data?.id) {
      formData.value = await getOrganization(data.id);
    } else {
      formData.value = { ...data };
    }

    // 推导节点类型与级别（用于动态表单）
    const parentNode = data?.parentNode;
    const nodeType = formData.value.nodeType ?? (data as any)?.nodeType ?? 1;
    let expectedLevel = 1;
    if (parentNode) {
      expectedLevel = nodeType === 2 ? parentNode.orgLevel! : parentNode.orgLevel! + 1;
    } else if (formData.value.orgLevel) {
      expectedLevel = formData.value.orgLevel;
    }
    ctx.value = { nodeType, expectedLevel, isEdit: isEdit.value };

    formApi.setState({ schema: useFormSchema(ctx.value) });

    if (formData.value.id) {
      await formApi.setValues(formData.value);
    } else {
      await formApi.setValues({
        nodeType,
        sort: 0,
        ...(parentNode ? { parentId: parentNode.id } : {}),
      });
    }
  },
});
</script>

<template>
  <Modal :title="getTitle" class="w-1/3">
    <Form class="mx-4" />
  </Modal>
</template>
