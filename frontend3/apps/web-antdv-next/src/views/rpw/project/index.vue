<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { ProjectApi } from '#/api/rpw/project';

import { Page } from '@vben/common-ui';
import { useVbenModal } from '#/components/modal';

import { message } from 'antdv-next';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteProject,
  getProjectList,
  updateProjectStatus,
} from '#/api/rpw/project';
import { $t } from '#/locales';

import { useGridColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';
import StatusForm from './modules/status.vue';

defineOptions({ name: 'Project' });

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

const [StatusModal, statusModalApi] = useVbenModal({
  connectedComponent: StatusForm,
  destroyOnClose: true,
});

/** 刷新表格 */
function handleRefresh() {
  gridApi.query();
}

/** 创建项目 */
function handleCreate() {
  formModalApi.setData(null).open();
}

/** 编辑项目 */
function handleEdit(row: ProjectApi.Project) {
  formModalApi.setData(row).open();
}

/** 删除项目 */
async function handleDelete(row: ProjectApi.Project) {
  const hideLoading = message.loading({
    content: $t('ui.actionMessage.deleting', [row.projectName]),
    duration: 0,
  });
  try {
    await deleteProject(row.id!);
    message.success($t('ui.actionMessage.deleteSuccess', [row.projectName]));
    handleRefresh();
  } finally {
    hideLoading();
  }
}

/** 更新项目状态 */
function handleUpdateStatus(row: ProjectApi.Project) {
  statusModalApi.setData(row).open();
}

/** 状态变更成功 */
async function handleStatusSuccess(row: ProjectApi.Project, status: number) {
  const hideLoading = message.loading({
    content: '状态更新中...',
    duration: 0,
  });
  try {
    await updateProjectStatus(row.id!, status);
    message.success($t('ui.actionMessage.operationSuccess'));
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
          const list = await getProjectList(formValues);
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
  } as VxeTableGridOptions<ProjectApi.Project>,
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <StatusModal @success="handleStatusSuccess" />
    <Grid table-title="项目列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: $t('ui.actionTitle.create', ['项目']),
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['rpw:project:create'],
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
              auth: ['rpw:project:update'],
              onClick: handleEdit.bind(null, row),
            },
            {
              label: '更新状态',
              type: 'link',
              icon: ACTION_ICON.EDIT,
              auth: ['rpw:project:update'],
              onClick: handleUpdateStatus.bind(null, row),
            },
            {
              label: $t('common.delete'),
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['rpw:project:delete'],
              popConfirm: {
                title: $t('ui.actionMessage.deleteConfirm', [row.projectName]),
                confirm: handleDelete.bind(null, row),
              },
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
