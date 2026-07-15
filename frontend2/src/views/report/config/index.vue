<template>
  <div class="p-6">
    <a-card>
      <template #title>报表配置</template>
      <template #extra>
        <a-button type="primary" @click="showAddModal">新增</a-button>
      </template>
      <a-form layout="inline" class="mb-4">
        <a-form-item label="名称">
          <a-input v-model:value="searchName" placeholder="请输入" style="width:200px" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="fetchData">搜索</a-button>
          <a-button style="margin-left:8px" @click="resetSearch">重置</a-button>
        </a-form-item>
      </a-form>
      <a-table
        :columns="columns"
        :data-source="dataList"
        :loading="loading"
        row-key="id"
        :pagination="pagination"
        @change="handleTableChange"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="showEditModal(record)">编辑</a>
              <a-popconfirm title="确认删除?" @confirm="handleDelete(record.id)">
                <a style="color:#ff4d4f">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
          <template v-else-if="column.key === 'enabled'">
            <a-badge :status="record.enabled ? 'success' : 'default'" :text="record.enabled ? '启用' : '禁用'" />
          </template>
        </template>
      </a-table>
    </a-card>
    <a-modal v-model:open="modalVisible" :title="modalTitle" @ok="handleModalOk" :confirm-loading="modalLoading" destroy-on-close width="600px">
      <a-form :model="form" :label-col="{ span: 5 }" autocomplete="off">
        <a-form-item label="名称" required>
          <a-input v-model:value="form.name" placeholder="请输入报表名称" />
        </a-form-item>
        <a-form-item label="类型">
          <a-input v-model:value="form.type" placeholder="请输入报表类型" />
        </a-form-item>
        <a-form-item label="模板">
          <a-input v-model:value="form.template" placeholder="请输入模板标识" />
        </a-form-item>
        <a-form-item label="调度">
          <a-input v-model:value="form.schedule" placeholder="如: 0 0 * * *" />
        </a-form-item>
        <a-form-item label="接收人">
          <a-select v-model:value="form.recipients" mode="tags" placeholder="输入接收人" style="width:100%" />
        </a-form-item>
        <a-form-item label="状态">
          <a-switch v-model:checked="form.enabled" checked-children="启用" un-checked-children="禁用" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { getConfigList, createConfig, updateConfig, deleteConfig } from '@/api/report'
import type { ReportConfig } from '@/api/report'
import type { TablePaginationConfig } from 'ant-design-vue/es/table'

const loading = ref(false)
const dataList = ref<ReportConfig[]>([])
const searchName = ref('')

const columns = [
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '类型', dataIndex: 'type', key: 'type', width: 120 },
  { title: '模板', dataIndex: 'template', key: 'template', width: 120 },
  { title: '调度', dataIndex: 'schedule', key: 'schedule', width: 140 },
  { title: '状态', key: 'enabled', width: 80 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 140 },
]

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getConfigList({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      name: searchName.value || undefined,
    })
    dataList.value = res.data.records
    pagination.total = res.data.total
  } catch {
    message.error('获取报表配置失败')
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  searchName.value = ''
  pagination.current = 1
  fetchData()
}

function handleTableChange(pag: TablePaginationConfig) {
  pagination.current = pag.current ?? 1
  pagination.pageSize = pag.pageSize ?? 10
  fetchData()
}

// --- modal ---
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const modalTitle = ref('')

const form = reactive<Partial<ReportConfig>>({
  name: '',
  type: '',
  template: '',
  schedule: '',
  recipients: [],
  enabled: true,
})

function showAddModal() {
  isEdit.value = false
  editId.value = null
  modalTitle.value = '新增报表配置'
  form.name = ''
  form.type = ''
  form.template = ''
  form.schedule = ''
  form.recipients = []
  form.enabled = true
  modalVisible.value = true
}

function showEditModal(record: ReportConfig) {
  isEdit.value = true
  editId.value = record.id
  modalTitle.value = '编辑报表配置'
  Object.assign(form, {
    name: record.name,
    type: record.type,
    template: record.template,
    schedule: record.schedule,
    recipients: record.recipients ?? [],
    enabled: record.enabled ?? true,
  })
  modalVisible.value = true
}

async function handleModalOk() {
  if (!form.name) {
    message.warning('请填写名称')
    return
  }
  modalLoading.value = true
  try {
    if (isEdit.value && editId.value) {
      await updateConfig(editId.value, form)
      message.success('更新成功')
    } else {
      await createConfig(form)
      message.success('创建成功')
    }
    modalVisible.value = false
    fetchData()
  } catch {
    message.error('操作失败')
  } finally {
    modalLoading.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await deleteConfig(id)
    message.success('删除成功')
    fetchData()
  } catch {
    message.error('删除失败')
  }
}

onMounted(fetchData)
</script>
