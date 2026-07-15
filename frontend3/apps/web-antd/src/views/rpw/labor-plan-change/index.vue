<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { LaborPlanChangeApi } from '#/api/rpw/labor-plan-change';

import { ref } from 'vue';

import { DocAlert, Page, useVbenModal } from '@vben/common-ui';

import { InputNumber, message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  approveLaborPlanChange,
  getLaborPlanChangeList,
  rejectLaborPlanChange,
} from '#/api/rpw/labor-plan-change';
import { $t } from '#/locales';

import { useGridColumns } from './data';
import Form from './modules/form.vue';

defineOptions({ name: 'LaborPlanChange' });

const selectedLaborPlanId = ref<number | undefined>(undefined);

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

function handleRefresh() {
  gridApi.query();
}

function handleCreate() {
  formModalApi.setData({ laborPlanId: selectedLaborPlanId.value }).open();
}

async function handleApprove(row: LaborPlanChangeApi.LaborPlanChange) {
  await approveLaborPlanChange(row.id!);
  message.success('已通过');
  handleRefresh();
}

async function handleReject(row: LaborPlanChangeApi.LaborPlanChange) {
  await rejectLaborPlanChange(row.id!);
  message.success('已驳回');
  handleRefresh();
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    pagerConfig: false,
    proxyConfig: {
      ajax: {
        query: async () => {
          if (!selectedLaborPlanId.value) {
            return { list: [], total: 0 };
          }
          const list = await getLaborPlanChangeList(selectedLaborPlanId.value);
          return { list, total: list.length };
        },
      },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: false },
  } as VxeTableGridOptions<LaborPlanChangeApi.LaborPlanChange>,
});
</script>

<template>
  <Page auto-content-height>
    <template #doc>
      <DocAlert title="【工作流】人力计划变更" url="https://doc.iocoder.cn/" />
    </template>

    <div class="mb-4 flex items-center gap-2">
      <span>劳动力计划ID：</span>
      <InputNumber
        v-model:value="selectedLaborPlanId"
        :min="1"
        class="!w-40"
        placeholder="请输入"
      />
      <span class="text-gray-400 text-xs">输入劳动力计划ID后加载变更记录</span>
    </div>

    <FormModal @success="handleRefresh" />
    <Grid table-title="人力计划变更">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '新增变更',
              type: 'primary',
              icon: ACTION_ICON.ADD,
              onClick: handleCreate,
            },
          ]"
        />
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '通过',
              type: 'link',
              onClick: handleApprove.bind(null, row),
            },
            {
              label: '驳回',
              type: 'link',
              danger: true,
              onClick: handleReject.bind(null, row),
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
