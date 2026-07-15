<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { OrganizationApi } from '#/api/rpw/organization';

import { Page } from '@vben/common-ui';
import { useVbenModal } from '#/components/modal';

import { message } from 'antdv-next';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteOrganization, getOrganizationList } from '#/api/rpw/organization';
import { $t } from '#/locales';

import { useGridColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

defineOptions({ name: 'Organization' });

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

/** 刷新表格 */
function handleRefresh() {
  gridApi.query();
}

/** 创建组织 */
function handleCreate() {
  formModalApi.setData(null).open();
}

/** 编辑组织 */
function handleEdit(row: OrganizationApi.Organization) {
  formModalApi.setData(row).open();
}

/** 删除组织 */
async function handleDelete(row: OrganizationApi.Organization) {
  const hideLoading = message.loading({
    content: $t('ui.actionMessage.deleting', [row.orgName]),
    duration: 0,
  });
  try {
    await deleteOrganization(row.id!);
    message.success($t('ui.actionMessage.deleteSuccess', [row.orgName]));
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
          const list = await getOrganizationList(formValues);
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
  } as VxeTableGridOptions<OrganizationApi.Organization>,
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <Grid table-title="组织列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: $t('ui.actionTitle.create', ['组织']),
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['rpw:organization:create'],
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
              auth: ['rpw:organization:update'],
              onClick: handleEdit.bind(null, row),
            },
            {
              label: $t('common.delete'),
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['rpw:organization:delete'],
              popConfirm: {
                title: $t('ui.actionMessage.deleteConfirm', [row.orgName]),
                confirm: handleDelete.bind(null, row),
              },
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
