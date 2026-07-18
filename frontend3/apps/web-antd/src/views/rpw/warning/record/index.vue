<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { WarningRecordApi } from '#/api/rpw/warning/record';

import { onMounted, ref } from 'vue';

import { DocAlert, Page } from '@vben/common-ui';
import { useVbenModal } from '@vben/common-ui';

import { Card, Statistic, message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  getWarningRecordPage,
  getWarningRecordStatistics,
} from '#/api/rpw/warning/record';
import { checkWarningRule } from '#/api/rpw/warning/rule';

import { useGridColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

defineOptions({ name: 'WarningRecord' });

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

/** 统计信息 */
const statistics = ref<WarningRecordApi.WarningRecordStatistics>({});
async function loadStatistics() {
  statistics.value = await getWarningRecordStatistics();
}

/** 刷新表格 */
function handleRefresh() {
  gridApi.query();
  loadStatistics();
}

/** 处理预警记录 */
function handleProcess(row: WarningRecordApi.WarningRecord) {
  formModalApi.setData({ ids: [row.id!] }).open();
}

/** 批量处理预警记录 */
function handleBatchProcess() {
  const rows = gridApi.grid.getCheckboxRecords() as WarningRecordApi.WarningRecord[];
  if (!rows || rows.length === 0) {
    message.warning('请选择需要处理的预警记录');
    return;
  }
  const ids = rows.map((row) => row.id!);
  formModalApi.setData({ ids }).open();
}

/** 立即检查（手动触发后台生成预警） */
async function handleCheck() {
  const hideLoading = message.loading({
    content: '正在检查预警...',
    duration: 0,
  });
  try {
    const res = await checkWarningRule({});
    message.success(res || '检查完成');
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
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const res = await getWarningRecordPage({
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
          return { list: res.records, total: res.total };
        },
      },
    },
    pagerConfig: {
      pageSize: 10,
    },
    rowConfig: {
      keyField: 'id',
      isHover: true,
    },
    checkboxConfig: {
      highlight: true,
    },
    toolbarConfig: {
      refresh: true,
      search: true,
    },
  } as VxeTableGridOptions<WarningRecordApi.WarningRecord>,
  gridEvents: {
    checkboxChange: handleRefreshSelection,
    checkboxAll: handleRefreshSelection,
  },
});

const checkedIds = ref<number[]>([]);
function handleRefreshSelection({
  records,
}: {
  records: WarningRecordApi.WarningRecord[];
}) {
  checkedIds.value = records.map((item) => item.id!);
}

onMounted(() => {
  loadStatistics();
});
</script>

<template>
  <Page auto-content-height>
    <template #doc>
      <DocAlert title="【预警】预警记录" url="https://doc.iocoder.cn/" />
    </template>

    <div class="mb-4 grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-7">
      <Card>
        <Statistic title="预警总数" :value="statistics.totalCount || 0" />
      </Card>
      <Card>
        <Statistic
          title="待处理"
          :value="statistics.pendingCount || 0"
          :value-style="{ color: '#cf1322' }"
        />
      </Card>
      <Card>
        <Statistic
          title="已解决"
          :value="statistics.resolvedCount || 0"
          :value-style="{ color: '#3f8600' }"
        />
      </Card>
      <Card>
        <Statistic title="已忽略" :value="statistics.ignoredCount || 0" />
      </Card>
      <Card>
        <Statistic
          title="红色"
          :value="statistics.redCount || 0"
          :value-style="{ color: '#cf1322' }"
        />
      </Card>
      <Card>
        <Statistic
          title="橙色"
          :value="statistics.orangeCount || 0"
          :value-style="{ color: '#fa8c16' }"
        />
      </Card>
      <Card>
        <Statistic
          title="黄色"
          :value="statistics.yellowCount || 0"
          :value-style="{ color: '#d48806' }"
        />
      </Card>
    </div>

    <FormModal @success="handleRefresh" />
    <Grid table-title="预警记录列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '立即检查',
              type: 'primary',
              icon: ACTION_ICON.SEARCH,
              onClick: handleCheck,
            },
            {
              label: '批量处理',
              type: 'primary',
              icon: ACTION_ICON.EDIT,
              auth: ['warning:record:update'],
              disabled: checkedIds.length === 0,
              onClick: handleBatchProcess,
            },
          ]"
        />
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '处理',
              type: 'link',
              icon: ACTION_ICON.EDIT,
              auth: ['warning:record:update'],
              ifShow: row.status === 'PENDING',
              onClick: handleProcess.bind(null, row),
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
