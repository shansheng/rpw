<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { SubcontractChange } from '#/api/rpw/resource-plan/subcontract-change';

import { Page } from '@vben/common-ui';

import { message } from 'antdv-next';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { subcontractChangeApi, STATUS_LABEL } from '#/api/rpw/resource-plan/subcontract-change';
import { router } from '#/router';
import { $t } from '#/locales';

import { useGridColumns, useGridFormSchema } from './data';

defineOptions({ name: 'RpwResourcePlanSubcontractChange' });

/** 刷新表格 */
function handleRefresh() {
  gridApi.query();
}

/** 新增变更（跳转创建页） */
function handleCreate() {
  router.push({ path: '/rpw/resource-plan/subcontract-change/create' });
}

/** 取消变更 */
async function handleCancel(row: SubcontractChange) {
  const hideLoading = message.loading({ content: '取消中...', duration: 0 });
  try {
    await subcontractChangeApi.cancel(row.id!);
    message.success('已取消');
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
        query: async (_params: any, formValues: any) => {
          const resp = await subcontractChangeApi.page(formValues);
          return { list: resp.list ?? [], total: resp.total ?? 0 };
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
  } as VxeTableGridOptions<SubcontractChange>,
});

function statusText(status?: string): string {
  return status ? STATUS_LABEL[status] ?? status : '—';
}
</script>

<template>
  <Page auto-content-height>
    <Grid table-title="分包计划变更">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '新增变更',
              type: 'primary',
              icon: ACTION_ICON.ADD,
              onClick: handleCreate,
            },
          ]"
        />
      </template>
      <template #status="{ row }">
        <span>{{ statusText(row.status) }}</span>
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '取消',
              type: 'link',
              danger: true,
              disabled: row.status !== 'RUNNING',
              icon: ACTION_ICON.DELETE,
              popConfirm: {
                title: '确定取消该变更申请？',
                confirm: handleCancel.bind(null, row),
              },
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
