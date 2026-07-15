<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { WorkflowApi } from '#/api/rpw/workflow';

import { DocAlert, Page } from '@vben/common-ui';

import { TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getDoneTasks } from '#/api/rpw/workflow';
import { $t } from '#/locales';

defineOptions({ name: 'WorkflowDone' });

function planTypeLabel(v?: string) {
  return (v && WorkflowApi.PLAN_TYPE_LABELS[v]) || v || '-';
}

function useGridColumns(): VxeTableGridOptions<WorkflowApi.DoneTaskDTO>['columns'] {
  return [
    { field: 'taskName', title: '任务名称', minWidth: 160 },
    { field: 'assignee', title: '处理人', width: 140 },
    {
      field: 'planType',
      title: '业务类型',
      width: 160,
      formatter: ({ cellValue }) => planTypeLabel(cellValue as string),
    },
    { field: 'planId', title: '业务ID', width: 120 },
    {
      field: 'startTime',
      title: '开始时间',
      minWidth: 180,
      formatter: 'formatDateTime',
    },
    {
      field: 'endTime',
      title: '结束时间',
      minWidth: 180,
      formatter: 'formatDateTime',
    },
  ];
}

const [Grid] = useVbenVxeGrid({
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    pagerConfig: false,
    proxyConfig: {
      ajax: {
        query: async () => {
          const list = await getDoneTasks();
          return { list, total: list.length };
        },
      },
    },
    rowConfig: { keyField: 'taskId', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<WorkflowApi.DoneTaskDTO>,
});
</script>

<template>
  <Page auto-content-height>
    <template #doc>
      <DocAlert title="【工作流】已办任务" url="https://doc.iocoder.cn/" />
    </template>
    <Grid table-title="已办任务" />
  </Page>
</template>
