<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { WorkflowApi } from '#/api/rpw/workflow';

import { ref } from 'vue';

import { DocAlert, Page } from '@vben/common-ui';

import { Button, Input, message, Modal } from 'ant-design-vue';

import { TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  approveTask,
  getTaskDiagram,
  getTodoTasks,
  rejectTask,
} from '#/api/rpw/workflow';
import { $t } from '#/locales';

defineOptions({ name: 'WorkflowTodo' });

function useGridColumns(): VxeTableGridOptions<WorkflowApi.TaskDTO>['columns'] {
  return [
    { field: 'name', title: '任务名称', minWidth: 160 },
    { field: 'assignee', title: '处理人', width: 140 },
    {
      field: 'createTime',
      title: '创建时间',
      width: 180,
      formatter: 'formatDateTime',
    },
    { field: 'processInstanceId', title: '流程实例ID', minWidth: 220 },
    { field: 'businessKey', title: '业务标识', minWidth: 160 },
    {
      field: 'businessInfo',
      title: '业务类型',
      width: 140,
      formatter: ({ cellValue }) => (cellValue && cellValue.type) || '-',
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
          const list = await getTodoTasks();
          return { list, total: list.length };
        },
      },
    },
    rowConfig: { keyField: 'taskId', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<WorkflowApi.TaskDTO>,
});

const handleOpen = ref(false);
const diagramOpen = ref(false);
const currentTask = ref<WorkflowApi.TaskDTO>({});
const comment = ref('');
const diagramInfo = ref<Record<string, any>>({});

async function handleProcess(row: WorkflowApi.TaskDTO) {
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

async function handleDiagram(row: WorkflowApi.TaskDTO) {
  const info = await getTaskDiagram(row.taskId!);
  diagramInfo.value = info ?? {};
  diagramOpen.value = true;
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
            {
              label: '流程图',
              type: 'link',
              onClick: handleDiagram.bind(null, row),
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
          任务：{{ currentTask.name }}（{{ currentTask.taskId }}）
        </p>
        <Input.TextArea
          v-model:value="comment"
          :rows="3"
          placeholder="请输入审批意见"
        />
        <div class="flex justify-end gap-2">
          <Button type="primary" @click="doApprove">通过</Button>
          <Button danger @click="doReject">驳回</Button>
        </div>
      </div>
    </Modal>

    <Modal
      v-model:open="diagramOpen"
      title="流程图"
      :footer="null"
      width="520px"
    >
      <div class="space-y-2 p-1 text-sm">
        <p>流程实例ID：{{ diagramInfo.processInstanceId }}</p>
        <p>流程定义ID：{{ diagramInfo.processDefinitionId }}</p>
        <p>已完成节点：{{ (diagramInfo.activityIds || []).join(', ') }}</p>
        <p>当前活跃节点：{{ (diagramInfo.activeActivityIds || []).join(', ') }}</p>
      </div>
    </Modal>
  </Page>
</template>
