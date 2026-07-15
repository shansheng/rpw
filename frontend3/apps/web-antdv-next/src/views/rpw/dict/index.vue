<script lang="ts" setup>
import type { DictApi } from '#/api/rpw/dict';

import { computed, onMounted, ref } from 'vue';

import { DocAlert, Page } from '@vben/common-ui';
import { useVbenModal } from '#/components/modal';

import { Button, Input, message, Modal, Select, Table } from 'antdv-next';

import { createDict, getTables, listDict, removeDict, updateDict } from '#/api/rpw/dict';
import { $t } from '#/locales';

defineOptions({ name: 'DictManage' });

const tables = ref<string[]>([]);
const tableName = ref<string | undefined>(undefined);
const rows = ref<DictApi.DictRow[]>([]);
const loading = ref(false);
const columns = ref<any[]>([]);

/** 编辑弹窗 */
const editOpen = ref(false);
const editRow = ref<DictApi.DictRow>({});

async function loadTables() {
  tables.value = (await getTables()) ?? [];
}

async function loadRows() {
  if (!tableName.value) {
    rows.value = [];
    columns.value = [];
    return;
  }
  loading.value = true;
  try {
    const list = (await listDict(tableName.value)) ?? [];
    rows.value = list;
    if (list.length) {
      const keys = Object.keys(list[0]).filter(
        (k) => k !== 'createTime' && k !== 'updateTime',
      );
      columns.value = keys.map((k) => ({
        title: k,
        dataIndex: k,
        key: k,
        ellipsis: true,
      }));
      columns.value.push({
        title: '操作',
        key: '__action',
        fixed: 'right',
        width: 140,
      });
    } else {
      columns.value = [{ title: '暂无数据', dataIndex: '_', key: '_' }];
    }
  } finally {
    loading.value = false;
  }
}

const editFields = computed(() => {
  if (!rows.value.length) return [];
  return Object.keys(rows.value[0]).filter(
    (k) => k !== 'id' && k !== 'createTime' && k !== 'updateTime',
  );
});

function handleEdit(row: DictApi.DictRow) {
  editRow.value = { ...row };
  editOpen.value = true;
}

function handleCreate() {
  editRow.value = {};
  editOpen.value = true;
}

async function handleSave() {
  if (!tableName.value) return;
  const { id, ...data } = editRow.value;
  if (id != null) {
    await updateDict(tableName.value, Number(id), data);
  } else {
    await createDict(tableName.value, data);
  }
  message.success('保存成功');
  editOpen.value = false;
  loadRows();
}

function handleDelete(row: DictApi.DictRow) {
  Modal.confirm({
    title: '确认删除该字典记录？',
    okType: 'danger',
    async onOk() {
      await removeDict(tableName.value!, Number(row.id));
      message.success('删除成功');
      loadRows();
    },
  });
}

onMounted(loadTables);
</script>

<template>
  <Page auto-content-height>
    <template #doc>
      <DocAlert title="【系统管理】字典管理" url="https://doc.iocoder.cn/" />
    </template>

    <div class="mb-4 flex items-center gap-2">
      <span>字典表：</span>
      <Select
        v-model:value="tableName"
        :options="tables.map((t) => ({ label: t, value: t }))"
        placeholder="请选择字典表"
        style="width: 260px"
        @change="loadRows"
      />
      <Button type="primary" :disabled="!tableName" @click="handleCreate">
        新增
      </Button>
    </div>

    <Table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      row-key="id"
      size="small"
      :scroll="{ x: 'max-content' }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === '__action'">
          <Button type="link" @click="handleEdit(record)">编辑</Button>
          <Button type="link" danger @click="handleDelete(record)">删除</Button>
        </template>
        <template v-else-if="typeof record[column.dataIndex] === 'object'">
          {{ JSON.stringify(record[column.dataIndex]) }}
        </template>
      </template>
    </Table>

    <Modal
      v-model:open="editOpen"
      :title="editRow.id ? '编辑字典' : '新增字典'"
      @ok="handleSave"
      width="520px"
    >
      <div class="space-y-3 p-1">
        <div
          v-for="f in editFields"
          :key="f"
          class="flex items-center gap-2"
        >
          <span class="w-32 text-right text-sm">{{ f }}</span>
          <Input v-model:value="editRow[f]" class="flex-1" />
        </div>
      </div>
    </Modal>
  </Page>
</template>
