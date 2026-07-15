<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { ResourcePlanHardware } from '#/api/rpw/resource-plan';

import { DocAlert, Page } from '@vben/common-ui';
import { useVbenModal } from '#/components/modal';

import { message } from 'antdv-next';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { hardwareApi } from '#/api/rpw/resource-plan';
import { $t } from '#/locales';

import { useGridColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

defineOptions({ name: 'RpwResourcePlanHardware' });

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
function handleEdit(row: ResourcePlanHardware) {
  formModalApi.setData(row).open();
}

/** 删除资源计划 */
async function handleDelete(row: ResourcePlanHardware) {
  const hideLoading = message.loading({
    content: $t('ui.actionMessage.deleting', [row.hardwareName]),
    duration: 0,
  });
  try {
    await hardwareApi.remove(row.id!);
    message.success($t('ui.actionMessage.deleteSuccess', [row.hardwareName]));
    handleRefresh();
  } finally {
    hideLoading();
  }
}

/** 提交资源计划 */
async function handleSubmit(row: ResourcePlanHardware) {
  const hideLoading = message.loading({
    content: '提交中...',
    duration: 0,
  });
  try {
    await hardwareApi.submit(row.id!);
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
          const list = await hardwareApi.getList(formValues);
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
  } as VxeTableGridOptions<ResourcePlanHardware>,
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <Grid table-title="五金资源计划">
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
                title: $t('ui.actionMessage.deleteConfirm', [row.hardwareName]),
                confirm: handleDelete.bind(null, row),
              },
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
