<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { ReportApi } from '#/api/rpw/report';

import { onMounted, ref } from 'vue';

import { DocAlert, Page, useVbenModal } from '@vben/common-ui';

import { message, Modal, Table } from 'ant-design-vue';

import { TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteReportConfig,
  getReportConfigs,
  previewReport,
} from '#/api/rpw/report';
import { $t } from '#/locales';

defineOptions({ name: 'ReportManage' });

// 报表配置列表必须带 userId 查询；当前登录用户 id 暂以常量占位
const CURRENT_USER_ID = 1;

function useGridColumns(): VxeTableGridOptions<ReportApi.ReportConfigVO>['columns'] {
  return [
    { field: 'id', title: 'ID', width: 80 },
    { field: 'reportName', title: '报表名称', minWidth: 160 },
    { field: 'reportType', title: '报表类型', width: 140 },
    {
      field: 'isDefault',
      title: '默认',
      width: 100,
      formatter: ({ cellValue }) => (cellValue === 1 ? '是' : '否'),
    },
    {
      field: 'createTime',
      title: '创建时间',
      width: 180,
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
          const list = (await getReportConfigs({ userId: CURRENT_USER_ID })) ?? [];
          return { list, total: list.length };
        },
      },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<ReportApi.ReportConfigVO>,
});

const previewOpen = ref(false);
const previewColumns = ref<any[]>([]);
const previewData = ref<any[]>([]);
const previewTitle = ref('');

async function handlePreview(row: ReportApi.ReportConfigVO) {
  const res = await previewReport({
    reportType: row.reportType!,
    configJson: row.configJson!,
  });
  previewColumns.value = (res.fields ?? []).map((f) => ({
    title: f.label || f.field,
    dataIndex: f.field,
    key: f.field,
    ellipsis: true,
  }));
  previewData.value = res.data ?? [];
  previewTitle.value = row.reportName || '报表预览';
  previewOpen.value = true;
}

function handleDelete(row: ReportApi.ReportConfigVO) {
  Modal.confirm({
    title: '确认删除该报表配置？',
    okType: 'danger',
    async onOk() {
      await deleteReportConfig(row.id!);
      message.success('删除成功');
      gridApi.query();
    },
  });
}

onMounted(() => gridApi.query());
</script>

<template>
  <Page auto-content-height>
    <template #doc>
      <DocAlert title="【报表与看板】报表" url="https://doc.iocoder.cn/" />
    </template>

    <Grid table-title="报表配置">
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '预览',
              type: 'link',
              onClick: handlePreview.bind(null, row),
            },
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
      v-model:open="previewOpen"
      :title="previewTitle"
      :footer="null"
      width="80%"
    >
      <Table
        :columns="previewColumns"
        :data-source="previewData"
        row-key="id"
        size="small"
        :scroll="{ x: 'max-content' }"
      />
    </Modal>
  </Page>
</template>
