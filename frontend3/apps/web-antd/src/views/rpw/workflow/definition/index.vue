<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { WorkflowApi } from '#/api/rpw/workflow';

import { ref } from 'vue';

import { DocAlert, Page } from '@vben/common-ui';

import { Button, Input, message, Modal } from 'ant-design-vue';

import { TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteProcessDefinition,
  deployProcessDefinitionByXml,
  getProcessDefinitions,
} from '#/api/rpw/workflow';
import { $t } from '#/locales';

defineOptions({ name: 'WorkflowDefinition' });

function useGridColumns(): VxeTableGridOptions<WorkflowApi.ProcessDefinitionDTO>['columns'] {
  return [
    { field: 'name', title: '流程名称', minWidth: 160 },
    { field: 'key', title: '流程标识', minWidth: 160 },
    { field: 'version', title: '版本', width: 100 },
    { field: 'deploymentId', title: '部署ID', minWidth: 220 },
    {
      title: '操作',
      width: 140,
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
          const list = await getProcessDefinitions();
          return { list, total: list.length };
        },
      },
    },
    rowConfig: { keyField: 'deploymentId', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<WorkflowApi.ProcessDefinitionDTO>,
});

const deployOpen = ref(false);
const deployXml = ref('');
const deployName = ref('');

async function handleDeploy() {
  if (!deployXml.value) {
    message.warning('请输入 BPMN XML');
    return;
  }
  try {
    await deployProcessDefinitionByXml({
      xml: deployXml.value,
      name: deployName.value || undefined,
    });
    message.success('部署成功');
    deployOpen.value = false;
    deployXml.value = '';
    deployName.value = '';
    gridApi.query();
  } catch {
    // 错误由拦截器统一提示
  }
}

async function handleDelete(row: WorkflowApi.ProcessDefinitionDTO) {
  Modal.confirm({
    title: '确认删除该流程定义？',
    content: `部署ID：${row.deploymentId}`,
    okType: 'danger',
    async onOk() {
      await deleteProcessDefinition(row.deploymentId!, true);
      message.success('删除成功');
      gridApi.query();
    },
  });
}
</script>

<template>
  <Page auto-content-height>
    <template #doc>
      <DocAlert title="【工作流】流程定义" url="https://doc.iocoder.cn/" />
    </template>

    <Grid table-title="流程定义">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '部署(XML)',
              type: 'primary',
              onClick: () => (deployOpen = true),
            },
          ]"
        />
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '删除',
              type: 'link',
              danger: true,
              onClick: handleDelete.bind(null, row),
            },
          ]"
        />
      </template>
    </Grid>

    <Modal
      v-model:open="deployOpen"
      title="部署流程定义（XML）"
      :footer="null"
      width="640px"
    >
      <div class="space-y-3 p-1">
        <Input v-model:value="deployName" placeholder="流程名称（可选）" />
        <Input.TextArea
          v-model:value="deployXml"
          :rows="12"
          placeholder="请粘贴 BPMN XML 内容"
        />
        <div class="flex justify-end">
          <Button type="primary" @click="handleDeploy">部署</Button>
        </div>
      </div>
    </Modal>
  </Page>
</template>
