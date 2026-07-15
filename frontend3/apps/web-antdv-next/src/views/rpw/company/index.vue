<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { CompanyApi } from '#/api/rpw/company';

import { Page } from '@vben/common-ui';
import { useVbenModal } from '#/components/modal';

import { message } from 'antdv-next';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteCompany, getCompanyList } from '#/api/rpw/company';
import { $t } from '#/locales';

import { useGridColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

defineOptions({ name: 'Company' });

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

/** 刷新表格 */
function handleRefresh() {
  gridApi.query();
}

/** 创建公司 */
function handleCreate() {
  formModalApi.setData(null).open();
}

/** 编辑公司 */
function handleEdit(row: CompanyApi.Company) {
  formModalApi.setData(row).open();
}

/** 删除公司 */
async function handleDelete(row: CompanyApi.Company) {
  const hideLoading = message.loading({
    content: $t('ui.actionMessage.deleting', [row.companyName]),
    duration: 0,
  });
  try {
    await deleteCompany(row.id!);
    message.success($t('ui.actionMessage.deleteSuccess', [row.companyName]));
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
          const list = await getCompanyList(formValues);
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
  } as VxeTableGridOptions<CompanyApi.Company>,
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <Grid table-title="公司列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: $t('ui.actionTitle.create', ['公司']),
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['rpw:company:create'],
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
              auth: ['rpw:company:update'],
              onClick: handleEdit.bind(null, row),
            },
            {
              label: $t('common.delete'),
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['rpw:company:delete'],
              popConfirm: {
                title: $t('ui.actionMessage.deleteConfirm', [row.companyName]),
                confirm: handleDelete.bind(null, row),
              },
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
