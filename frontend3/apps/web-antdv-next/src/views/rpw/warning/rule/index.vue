<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { WarningRuleApi } from '#/api/rpw/warning/rule';

import { DocAlert, Page } from '@vben/common-ui';
import { useVbenModal } from '#/components/modal';

import { message } from 'antdv-next';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  checkWarningRule,
  deleteWarningRule,
  getWarningRulePage,
  toggleWarningRule,
} from '#/api/rpw/warning/rule';
import { $t } from '#/locales';

import { useGridColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

defineOptions({ name: 'WarningRule' });

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

/** 刷新表格 */
function handleRefresh() {
  gridApi.query();
}

/** 创建预警规则 */
function handleCreate() {
  formModalApi.setData(null).open();
}

/** 编辑预警规则 */
function handleEdit(row: WarningRuleApi.WarningRule) {
  formModalApi.setData(row).open();
}

/** 删除预警规则 */
async function handleDelete(row: WarningRuleApi.WarningRule) {
  const hideLoading = message.loading({
    content: $t('ui.actionMessage.deleting', [row.ruleName]),
    duration: 0,
  });
  try {
    await deleteWarningRule(row.id!);
    message.success($t('ui.actionMessage.deleteSuccess', [row.ruleName]));
    handleRefresh();
  } finally {
    hideLoading();
  }
}

/** 启用/禁用预警规则 */
async function handleToggle(row: WarningRuleApi.WarningRule) {
  const nextEnabled = row.enabled === 1 ? 0 : 1;
  const hideLoading = message.loading({
    content: nextEnabled === 1 ? '正在启用...' : '正在禁用...',
    duration: 0,
  });
  try {
    await toggleWarningRule(row.id!, nextEnabled);
    message.success($t('ui.actionMessage.operationSuccess'));
    handleRefresh();
  } finally {
    hideLoading();
  }
}

/** 立即检查预警规则 */
async function handleCheck() {
  const hideLoading = message.loading({
    content: '正在检查预警...',
    duration: 0,
  });
  try {
    await checkWarningRule({});
    message.success('检查完成');
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
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const res = await getWarningRulePage({
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
          return { list: res.records, total: res.total };
        },
      },
    },
    pagerConfig: {
      pageSize: 10,
    },
    rowConfig: {
      keyField: 'id',
      isHover: true,
    },
    toolbarConfig: {
      refresh: true,
      search: true,
    },
  } as VxeTableGridOptions<WarningRuleApi.WarningRule>,
});
</script>

<template>
  <Page auto-content-height>
    <template #doc>
      <DocAlert title="【预警】预警规则" url="https://doc.iocoder.cn/" />
    </template>

    <FormModal @success="handleRefresh" />
    <Grid table-title="预警规则列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '立即检查',
              type: 'primary',
              icon: ACTION_ICON.SEARCH,
              onClick: handleCheck,
            },
            {
              label: $t('ui.actionTitle.create', ['预警规则']),
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['warning:rule:create'],
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
              auth: ['warning:rule:update'],
              onClick: handleEdit.bind(null, row),
            },
            {
              label: row.enabled === 1 ? '禁用' : '启用',
              type: 'link',
              icon: ACTION_ICON.EDIT,
              auth: ['warning:rule:update'],
              onClick: handleToggle.bind(null, row),
            },
            {
              label: $t('common.delete'),
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['warning:rule:delete'],
              popConfirm: {
                title: $t('ui.actionMessage.deleteConfirm', [row.ruleName]),
                confirm: handleDelete.bind(null, row),
              },
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
