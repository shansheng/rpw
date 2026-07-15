<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { WorkflowApi } from '#/api/rpw/workflow';

import { ref } from 'vue';

import { DocAlert, Page } from '@vben/common-ui';

import { Button, Input, message, Modal } from 'antdv-next';

import { TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  approveTask,
  getPendingTasks,
  rejectTask,
} from '#/api/rpw/workflow';
import { $t } from '#/locales';

defineOptions({ name: 'WorkflowTodo' });

function planTypeLabel(v?: string) {
  return (v && WorkflowApi.PLAN_TYPE_LABELS[v]) || v || '-';
}

function useGridColumns(): VxeTableGridOptions<WorkflowApi.PendingTaskDTO>['columns'] {
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
      field: 'createTime',
      title: '创建时间',
      minWidth: 180,
      formatter: 'formatDateTime',
    },
    {
      title: '操作',
      width: 160,
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
          const list = await getPendingTasks();
          return { list, total: list.length };
        },
      },
    },
    rowConfig: { keyField: 'taskId', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<WorkflowApi.PendingTaskDTO>,
});

const handleOpen = ref(false);
const currentTask = ref<WorkflowApi.PendingTaskDTO>({});
const comment = ref('');

async function handleProcess(row: WorkflowApi.PendingTaskDTO) {
  currentTask.value = row;
  comment.value = '';
  handleOpen.value = true;
}

async function doApprove() {
  try {
    await approveTask(currentTask.value.taskId!, comment.value || undefined);
    message.success('审批通过');
    handleOpen.value = false;
    gridApi.query();
  } catch {
    // 错误由拦截器统一提示
  }
}

async function doReject() {
  if (!comment.value) {
    message.warning('驳回必须填写审批意见');
    return;
  }
  try {
    await rejectTask(currentTask.value.taskId!, comment.value);
    message.success('已驳回');
    handleOpen.value = false;
    gridApi.query();
  } catch {
    // 错误由拦截器统一提示
  }
}
</script>

<template>
  <Page auto-content-height>
    <template #doc>
      <DocAlert title="【工作流】待办任务" url="https://doc.iocoder.cn/" />
    </template>

    <Grid table-title="待办任务">
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '办理',
              type: 'link',
              onClick: handleProcess.bind(null, row),
            },
          ]"
        />
      </template>
    </Grid>

    <Modal
      v-model:open="handleOpen"
      title="审批办理"
      :footer="null"
      width="480px"
    >
      <div class="space-y-3 p-1">
        <p class="text-sm text-gray-600">
          任务：{{ currentTask.taskName }}（{{ currentTask.taskId }}）
        </p>
        <p class="text-sm text-gray-600">
          业务：{{ planTypeLabel(currentTask.planType) }} #{{ currentTask.planId }}
        </p>
        <Input.TextArea
          v-model:value="comment"
          :rows="3"
          placeholder="请输入审批意见（驳回必填）"
        />
        <div class="flex justify-end gap-2">
          <Button type="primary" @click="doApprove">通过</Button>
          <Button danger @click="doReject">驳回</Button>
        </div>
      </div>
    </Modal>
  </Page>
</template>
