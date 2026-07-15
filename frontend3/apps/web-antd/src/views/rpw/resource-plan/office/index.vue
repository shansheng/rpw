<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { ResourcePlanOffice } from '#/api/rpw/resource-plan';

import { DocAlert, Page, useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { officeApi } from '#/api/rpw/resource-plan';
import { $t } from '#/locales';

import { useGridColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

defineOptions({ name: 'RpwResourcePlanOffice' });

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

/** 刷新表格 */
function handleRefresh() {
  gridApi.query();
}

/** 创建资源计划 */
function handleCreate() {
  formModalApi.setData(null).open();
}

/** 编辑资源计划 */
function handleEdit(row: ResourcePlanOffice) {
  formModalApi.setData(row).open();
}

/** 删除资源计划 */
async function handleDelete(row: ResourcePlanOffice) {
  const hideLoading = message.loading({
    content: $t('ui.actionMessage.deleting', [row.officeName]),
    duration: 0,
  });
  try {
    await officeApi.remove(row.id!);
    message.success($t('ui.actionMessage.deleteSuccess', [row.officeName]));
    handleRefresh();
  } finally {
    hideLoading();
  }
}

/** 提交资源计划 */
async function handleSubmit(row: ResourcePlanOffice) {
  const hideLoading = message.loading({
    content: '提交中...',
    duration: 0,
  });
  try {
    await officeApi.submit(row.id!);
    message.success('提交成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
  },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    pagerConfig: false,
    proxyConfig: {
      ajax: {
        query: async (_params, formValues) => {
          const list = await officeApi.getList(formValues);
          return { list, total: list.length };
        },
      },
    },
    rowConfig: {
      keyField: 'id',
      isHover: true,
    },
    toolbarConfig: {
      refresh: true,
      search: true,
    },
  } as VxeTableGridOptions<ResourcePlanOffice>,
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <Grid table-title="办公资源计划">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: $t('ui.actionTitle.create', ['资源计划']),
              type: 'primary',
              icon: ACTION_ICON.ADD,
              onClick: handleCreate,
            },
          ]"
        />
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '提交',
              type: 'link',
              icon: ACTION_ICON.ADD,
              onClick: handleSubmit.bind(null, row),
            },
            {
              label: $t('common.edit'),
              type: 'link',
              icon: ACTION_ICON.EDIT,
              onClick: handleEdit.bind(null, row),
            },
            {
              label: $t('common.delete'),
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              popConfirm: {
                title: $t('ui.actionMessage.deleteConfirm', [row.officeName]),
                confirm: handleDelete.bind(null, row),
              },
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
