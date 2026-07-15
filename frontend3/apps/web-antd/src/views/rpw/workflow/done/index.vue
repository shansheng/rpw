<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { WorkflowApi } from '#/api/rpw/workflow';

import { DocAlert, Page } from '@vben/common-ui';

import { TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getHistoryTasks } from '#/api/rpw/workflow';
import { $t } from '#/locales';

defineOptions({ name: 'WorkflowDone' });

function useGridColumns(): VxeTableGridOptions<WorkflowApi.HistoricTaskDTO>['columns'] {
  return [
    { field: 'name', title: '任务名称', minWidth: 160 },
    { field: 'assignee', title: '处理人', width: 140 },
    {
      field: 'startTime',
      title: '开始时间',
      width: 180,
      formatter: 'formatDateTime',
    },
    {
      field: 'endTime',
      title: '结束时间',
      width: 180,
      formatter: 'formatDateTime',
    },
    { field: 'processInstanceId', title: '流程实例ID', minWidth: 220 },
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
          const list = await getHistoryTasks();
          return { list, total: list.length };
        },
      },
    },
    rowConfig: { keyField: 'taskId', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<WorkflowApi.HistoricTaskDTO>,
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
