<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { WorkflowApi } from '#/api/rpw/workflow';

import { ref } from 'vue';

import { DocAlert, Page } from '@vben/common-ui';

import { message, Modal } from 'antdv-next';

import { TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  getApprovalHistory,
  getMyProcesses,
} from '#/api/rpw/workflow';
import { $t } from '#/locales';

defineOptions({ name: 'WorkflowMy' });

function planTypeLabel(v?: string) {
  return (v && WorkflowApi.PLAN_TYPE_LABELS[v]) || v || '-';
}

function useGridColumns(): VxeTableGridOptions<WorkflowApi.MyProcessDTO>['columns'] {
  return [
    { field: 'processInstanceId', title: '流程实例ID', minWidth: 220 },
    { field: 'processDefinitionKey', title: '流程标识', minWidth: 160 },
    {
      field: 'planType',
      title: '业务类型',
      width: 160,
      formatter: ({ cellValue }) => planTypeLabel(cellValue as string),
    },
    { field: 'planId', title: '业务ID', width: 120 },
    {
      field: 'status',
      title: '状态',
      width: 120,
      formatter: ({ cellValue }) => {
        const map: Record<string, string> = {
          RUNNING: '运行中',
          COMPLETED: '已完成',
        };
        return map[cellValue as string] ?? cellValue;
      },
    },
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
          const list = await getMyProcesses();
          return { list, total: list.length };
        },
      },
    },
    rowConfig: { keyField: 'processInstanceId', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<WorkflowApi.MyProcessDTO>,
});

const detailOpen = ref(false);
const detailTasks = ref<WorkflowApi.ApprovalHistoryDTO[]>([]);
const detailId = ref('');

async function handleDetail(row: WorkflowApi.MyProcessDTO) {
  detailId.value = row.processInstanceId!;
  const planType = row.planType;
  const planId = row.planId;
  if (planType && planId != null) {
    detailTasks.value = await getApprovalHistory(planType, planId);
  } else {
    detailTasks.value = [];
  }
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
      :title="`审批进度 - ${detailId}`"
      :footer="null"
      width="640px"
    >
      <div class="space-y-2 p-1">
        <div
          v-for="(task, idx) in detailTasks"
          :key="idx"
          class="rounded border border-gray-200 p-2 text-sm"
        >
          <div class="font-medium">{{ task.taskName }}</div>
          <div class="text-gray-500">
            处理人：{{ task.assignee }} ｜ {{ task.startTime }} ~ {{ task.endTime }}
          </div>
        </div>
        <div v-if="detailTasks.length === 0" class="text-gray-400">暂无审批历史</div>
      </div>
    </Modal>
  </Page>
</template>
