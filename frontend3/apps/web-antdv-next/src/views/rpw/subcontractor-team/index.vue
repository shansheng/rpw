<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { SubcontractorTeamApi } from '#/api/rpw/subcontractor-team';

import { Page } from '@vben/common-ui';
import { useVbenModal } from '#/components/modal';

import { message } from 'antdv-next';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteSubcontractorTeam,
  getSubcontractorTeamList,
} from '#/api/rpw/subcontractor-team';
import { $t } from '#/locales';

import { useGridColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

defineOptions({ name: 'SubcontractorTeam' });

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

/** 刷新表格 */
function handleRefresh() {
  gridApi.query();
}

/** 创建 */
function handleCreate() {
  formModalApi.setData(null).open();
}

/** 编辑 */
function handleEdit(row: SubcontractorTeamApi.SubcontractorTeam) {
  formModalApi.setData(row).open();
}

/** 删除 */
async function handleDelete(row: SubcontractorTeamApi.SubcontractorTeam) {
  const hideLoading = message.loading({
    content: $t('ui.actionMessage.deleting', [row.subcontractName]),
    duration: 0,
  });
  try {
    await deleteSubcontractorTeam(row.id!);
    message.success($t('ui.actionMessage.deleteSuccess', [row.subcontractName]));
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
          const list = await getSubcontractorTeamList(formValues);
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
  } as VxeTableGridOptions<SubcontractorTeamApi.SubcontractorTeam>,
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <Grid table-title="分包队伍统计">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: $t('ui.actionTitle.create', ['分包队伍统计']),
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['rpw:subcontractor-team:create'],
              onClick: handleCreate,
            },
          ]"
        />
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: $t('common.edit'),
              type: 'link',
              icon: ACTION_ICON.EDIT,
              auth: ['rpw:subcontractor-team:update'],
              onClick: handleEdit.bind(null, row),
            },
            {
              label: $t('common.delete'),
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['rpw:subcontractor-team:delete'],
              popConfirm: {
                title: $t('ui.actionMessage.deleteConfirm', [row.subcontractName]),
                confirm: handleDelete.bind(null, row),
              },
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
