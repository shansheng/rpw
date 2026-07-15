<template>
  <div class="p-6">
    <a-card>
      <template #title>字典管理</template>
      <template #extra>
        <a-button type="primary" @click="showAddModal">新增</a-button>
      </template>
      <a-form layout="inline" class="mb-4">
        <a-form-item label="字典类型">
          <a-input v-model:value="searchDictType" placeholder="请输入" style="width:180px" />
        </a-form-item>
        <a-form-item label="字典标签">
          <a-input v-model:value="searchDictLabel" placeholder="请输入" style="width:180px" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="searchStatus" placeholder="全部" style="width:100px" allow-clear>
            <a-select-option :value="0">禁用</a-select-option>
            <a-select-option :value="1">启用</a-select-option>
          </a-select>
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
            <a-badge :status="record.status === 1 ? 'success' : 'default'" :text="record.status === 1 ? '启用' : '禁用'" />
          </template>
        </template>
      </a-table>
    </a-card>
    <a-modal v-model:open="modalVisible" :title="modalTitle" @ok="handleModalOk" :confirm-loading="modalLoading" destroy-on-close>
      <a-form :model="form" :label-col="{ span: 6 }" autocomplete="off">
        <a-form-item label="字典类型" required>
          <a-input v-model:value="form.dictType" placeholder="请输入字典类型编码" :disabled="isEdit" />
        </a-form-item>
        <a-form-item label="字典标签" required>
          <a-input v-model:value="form.dictLabel" placeholder="请输入字典标签" />
        </a-form-item>
        <a-form-item label="字典键值">
          <a-input v-model:value="form.dictCode" placeholder="请输入字典键值" />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="form.sort" :min="0" style="width:100%" />
        </a-form-item>
        <a-form-item label="状态">
          <a-radio-group v-model:value="form.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="form.remark" :rows="3" placeholder="请输入备注" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue'
import { message } from 'ant-design-vue'
import { getDictList, createDictItem, updateDictItem, deleteDictItem } from '@/api/dict'
import type { TablePaginationConfig } from 'ant-design-vue/es/table'

const loading = ref(false)
const dataList = ref<any[]>([])
const searchDictType = ref('')
const searchDictLabel = ref('')
const searchStatus = ref<number | undefined>(undefined)

const tableName = computed(() => searchDictType.value || undefined)

const columns = [
  { title: '字典类型', dataIndex: 'dictType', key: 'dictType', width: 160 },
  { title: '字典标签', dataIndex: 'dictLabel', key: 'dictLabel' },
  { title: '字典键值', dataIndex: 'dictCode', key: 'dictCode', width: 100 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 70 },
  { title: '状态', key: 'enabled', width: 70 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
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
  const tn = tableName.value
  if (!tn) return
  loading.value = true
  try {
    const res = await getDictList(tn)
    dataList.value = res.data
    pagination.total = res.data.length
  } catch {
    message.error('获取字典列表失败')
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  searchDictType.value = ''
  searchDictLabel.value = ''
  searchStatus.value = undefined
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

const form = reactive<Record<string, any>>({
  dictType: '',
  dictLabel: '',
  dictCode: '',
  sort: 0,
  status: 1,
  remark: '',
})

function showAddModal() {
  isEdit.value = false
  editId.value = null
  modalTitle.value = '新增字典'
  form.dictType = ''
  form.dictLabel = ''
  form.dictCode = ''
  form.sort = 0
  form.status = 1
  form.remark = ''
  modalVisible.value = true
}

function showEditModal(record: any) {
  isEdit.value = true
  editId.value = record.id
  modalTitle.value = '编辑字典'
  Object.assign(form, {
    dictType: record.dictType,
    dictLabel: record.dictLabel,
    dictCode: record.dictCode,
    sort: record.sort,
    status: record.status,
    remark: record.remark,
  })
  modalVisible.value = true
}

async function handleModalOk() {
  const tn = tableName.value
  if (!tn) {
    message.warning('请先输入字典类型')
    return
  }
  if (!form.dictType || !form.dictLabel) {
    message.warning('请填写必填项')
    return
  }
  modalLoading.value = true
  try {
    if (isEdit.value && editId.value) {
      await updateDictItem(tn, editId.value, form)
      message.success('更新成功')
    } else {
      await createDictItem(tn, form)
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
  const tn = tableName.value
  if (!tn) return
  try {
    await deleteDictItem(tn, id)
    message.success('删除成功')
    fetchData()
  } catch {
    message.error('删除失败')
  }
}

onMounted(fetchData)
</script>
