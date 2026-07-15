<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { WorkflowApi } from '#/api/rpw/workflow';

import { ref } from 'vue';

import { DocAlert, Page } from '@vben/common-ui';

import { message, Modal } from 'ant-design-vue';

import { TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getMyProcessInstances, getProcessInstanceDetail } from '#/api/rpw/workflow';
import { $t } from '#/locales';

defineOptions({ name: 'WorkflowMy' });

function useGridColumns(): VxeTableGridOptions<WorkflowApi.ProcessInstanceDTO>['columns'] {
  return [
    { field: 'processInstanceId', title: '流程实例ID', minWidth: 220 },
    { field: 'processDefinitionKey', title: '流程标识', minWidth: 160 },
    { field: 'businessKey', title: '业务标识', minWidth: 160 },
    {
      field: 'status',
      title: '状态',
      width: 140,
      formatter: ({ cellValue }) => {
        const map: Record<string, string> = {
          RUNNING: '运行中',
          SUSPENDED: '已挂起',
          COMPLETED: '已完成',
        };
        return map[cellValue as string] ?? cellValue;
      },
    },
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
    {
      title: '操作',
      width: 120,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    pagerConfig: false,
    proxyConfig: {
      ajax: {
        query: async () => {
          const list = await getMyProcessInstances();
          return { list, total: list.length };
        },
      },
    },
    rowConfig: { keyField: 'processInstanceId', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<WorkflowApi.ProcessInstanceDTO>,
});

const detailOpen = ref(false);
const detailTasks = ref<Array<Record<string, any>>>([]);
const detailId = ref('');

async function handleDetail(row: WorkflowApi.ProcessInstanceDTO) {
  detailId.value = row.processInstanceId!;
  const detail = await getProcessInstanceDetail(row.processInstanceId!);
  detailTasks.value = detail?.historicTasks ?? [];
  detailOpen.value = true;
}
</script>

<template>
  <Page auto-content-height>
    <template #doc>
      <DocAlert title="【工作流】我发起的" url="https://doc.iocoder.cn/" />
    </template>

    <Grid table-title="我发起的流程">
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '详情',
              type: 'link',
              onClick: handleDetail.bind(null, row),
            },
          ]"
        />
      </template>
    </Grid>

    <Modal
      v-model:open="detailOpen"
      :title="`流程详情 - ${detailId}`"
      :footer="null"
      width="640px"
    >
      <div class="space-y-2 p-1">
        <div
          v-for="(task, idx) in detailTasks"
          :key="idx"
          class="rounded border border-gray-200 p-2 text-sm"
        >
          <div class="font-medium">{{ task.name }}</div>
          <div class="text-gray-500">
            处理人：{{ task.assignee }} ｜ {{ task.startTime }} ~ {{ task.endTime }}
          </div>
          <div v-if="task.comment" class="text-gray-600">
            意见：{{ task.comment }}
          </div>
        </div>
        <div v-if="detailTasks.length === 0" class="text-gray-400">暂无历史任务</div>
      </div>
    </Modal>
  </Page>
</template>
