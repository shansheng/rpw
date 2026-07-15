<script lang="ts" setup>
import type { SysUserApi } from '#/api/rpw/system/user';

import { onMounted, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import {
  createSysUser,
  getSysUser,
  updateSysUser,
} from '#/api/rpw/system/user';
import { getOrganizationTree } from '#/api/rpw/organization';
import { getRoleSimpleList } from '#/api/rpw/system/role';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

defineOptions({ name: 'SysUserForm' });

const emit = defineEmits(['success']);
const formData = ref<SysUserApi.SysUser>();
const isEdit = ref(false);

/** 组织下拉选项（由组织树扁平化得到，带层级缩进） */
const orgOptions = ref<{ label: string; value: number }[]>([]);

/** 角色下拉选项（用户绑定角色时使用） */
const roleOptions = ref<{ label: string; value: number }[]>([]);

/** 递归扁平化组织树，生成带缩进的 Select 选项 */
function flattenOrgTree(
  nodes: any[],
  depth = 0,
  acc: { label: string; value: number }[] = [],
): { label: string; value: number }[] {
  for (const n of nodes ?? []) {
    const prefix = depth > 0 ? '　'.repeat(depth) : '';
    acc.push({ label: `${prefix}${n.orgName}`, value: n.id });
    if (n.children && n.children.length) {
      flattenOrgTree(n.children, depth + 1, acc);
    }
  }
  return acc;
}

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
    const values = (await formApi.getValues()) as SysUserApi.SysUser;
    // 密码留空：创建时后端用默认口令；修改时不修改密码（后端已处理）
    if (!values.password) {
      delete (values as any).password;
    }
    try {
      if (isEdit.value) {
        await updateSysUser(values);
      } else {
        await createSysUser(values);
      }
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
    // 构建组织下拉选项与角色下拉选项
    try {
      const [roles, tree] = await Promise.all([
        getRoleSimpleList(),
        getOrganizationTree(),
      ]);
      roleOptions.value = (roles ?? []).map((r) => ({
        label: r.name ?? '',
        value: r.id as number,
      }));
      orgOptions.value = flattenOrgTree(tree);
    } catch {
      orgOptions.value = [];
      roleOptions.value = [];
    }

    const data = modalApi.getData<SysUserApi.SysUser>();
    isEdit.value = !!data?.id;
    if (isEdit.value) {
      formData.value = await getSysUser(data.id!);
    } else {
      formData.value = {};
    }

    formApi.setState({ schema: useFormSchema(orgOptions.value, roleOptions.value) });

    if (isEdit.value) {
      // 编辑时不展示密码明文，留空表示不修改
      await formApi.setValues({ ...formData.value, password: '' });
    } else {
      await formApi.setValues({ status: 1, ...formData.value });
    }
  },
});

onMounted(() => {
  // 占位：实际数据在 onOpenChange 中加载
});
</script>

<template>
  <Modal :title="isEdit ? '编辑用户' : '新增用户'" class="w-1/3">
    <Form class="mx-4" />
  </Modal>
</template>
