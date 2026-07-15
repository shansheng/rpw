<template>
  <div style="padding:16px;">
    <a-card title="项目管理">
      <a-row :gutter="16" style="margin-bottom:16px;">
        <a-col :span="6"><a-input v-model:value="searchName" placeholder="搜索项目名称" @pressEnter="fetchData" /></a-col>
        <a-col :span="4">
          <a-select v-model:value="searchStatus" placeholder="状态" allow-clear style="width:100%;">
            <a-select-option value="1">启用</a-select-option>
            <a-select-option value="0">停用</a-select-option>
          </a-select>
        </a-col>
        <a-col><a-button type="primary" @click="fetchData">搜索</a-button><a-button style="margin-left:8px;" @click="searchName='';searchStatus=undefined;fetchData()">重置</a-button></a-col>
        <a-col flex="auto"><div style="text-align:right;"><a-button type="primary" @click="showAdd()">新增项目</a-button></div></a-col>
      </a-row>
      <a-table :columns="columns" :data-source="dataList" row-key="id" :loading="loading" :pagination="{ pageSize: 10, total: dataList.length }" bordered size="middle">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'orange'">{{ record.status === 1 ? '启用' : '停用' }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="showEdit(record)">编辑</a>
              <a-popconfirm title="确认删除?" @confirm="handleDelete(record.id)">
                <a style="color:#ff4d4f">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
    <a-modal v-model:open="modalVisible" :title="isEdit?'编辑项目':'新增项目'" @ok="handleOk" :confirm-loading="modalLoading" width="600px">
      <a-form :model="form" :label-col="{ span: 5 }">
        <a-form-item label="名称"><a-input v-model:value="form.projectName" placeholder="项目名称" /></a-form-item>
        <a-form-item label="公司ID"><a-input-number v-model:value="form.companyId" :min="1" style="width:100%" /></a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="form.status">
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">停用</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="计划开始"><a-date-picker v-model:value="form.planStartDate" style="width:100%" /></a-form-item>
        <a-form-item label="计划结束"><a-date-picker v-model:value="form.planEndDate" style="width:100%" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getList, create, update, remove } from '@/api/project'

const dataList = ref<any[]>([]), loading = ref(false), modalVisible = ref(false), modalLoading = ref(false), isEdit = ref(false)
const searchName = ref(''), searchStatus = ref<number | undefined>()
const form = ref<any>({ projectName: '', companyId: undefined, status: 1, planStartDate: undefined, planEndDate: undefined })

const columns = [
  { title: '项目名称', dataIndex: 'projectName', key: 'projectName' },
  { title: '公司ID', dataIndex: 'companyId', key: 'companyId', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '计划开始', dataIndex: 'planStartDate', key: 'planStartDate', width: 120 },
  { title: '计划结束', dataIndex: 'planEndDate', key: 'planEndDate', width: 120 },
  { title: '操作', key: 'action', width: 150 },
]

onMounted(() => fetchData())
const fetchData = async () => {
  loading.value = true
  try {
    const params: any = {}
    if (searchName.value) params.name = searchName.value
    if (searchStatus.value !== undefined) params.status = searchStatus.value
    const res = await getList(params)
    if (res.code === 200) dataList.value = res.data || []
  } catch {}
  finally { loading.value = false }
}
const showAdd = () => { isEdit.value = false; form.value = { projectName: '', companyId: undefined, status: 1, planStartDate: undefined, planEndDate: undefined }; modalVisible.value = true }
const showEdit = (record: any) => { isEdit.value = true; form.value = { ...record }; modalVisible.value = true }
const handleOk = async () => {
  modalLoading.value = true
  try { if (isEdit.value) { await update(form.value.id, form.value); message.success('更新成功') } else { await create(form.value); message.success('新增成功') } modalVisible.value = false; await fetchData() }
  catch (err: any) { message.error(err.message || '操作失败') }
  finally { modalLoading.value = false }
}
const handleDelete = async (id: number) => { try { await remove(id); message.success('删除成功'); await fetchData() } catch { message.error('删除失败') } }
</script>
