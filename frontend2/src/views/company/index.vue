<template>
  <div style="padding:16px;">
    <a-card title="公司管理">
      <a-row :gutter="16" style="margin-bottom:16px;">
        <a-col :span="6"><a-input v-model:value="searchName" placeholder="搜索公司名称" @pressEnter="fetchData" /></a-col>
        <a-col><a-button type="primary" @click="fetchData">搜索</a-button><a-button style="margin-left:8px;" @click="searchName='';fetchData()">重置</a-button></a-col>
        <a-col flex="auto"><div style="text-align:right;"><a-button type="primary" @click="showAdd()">新增公司</a-button></div></a-col>
      </a-row>
      <a-table :columns="columns" :data-source="dataList" row-key="id" :loading="loading" :pagination="{ pageSize: 10, total: dataList.length }" bordered size="middle">
        <template #bodyCell="{ column, record }">
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
    <a-modal v-model:open="modalVisible" :title="isEdit?'编辑公司':'新增公司'" @ok="handleOk" :confirm-loading="modalLoading">
      <a-form :model="form" :label-col="{ span: 5 }">
        <a-form-item label="名称"><a-input v-model:value="form.companyName" placeholder="公司名称" /></a-form-item>
        <a-form-item label="组织ID"><a-input-number v-model:value="form.orgId" :min="1" style="width:100%" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getList, create, update, remove } from '@/api/company'

const dataList = ref<any[]>([]), loading = ref(false), modalVisible = ref(false), modalLoading = ref(false), isEdit = ref(false)
const searchName = ref('')
const form = ref<any>({ companyName: '', orgId: undefined })

const columns = [
  { title: '公司名称', dataIndex: 'companyName', key: 'companyName' },
  { title: '组织ID', dataIndex: 'orgId', key: 'orgId', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 180 },
  { title: '操作', key: 'action', width: 150 },
]

onMounted(() => fetchData())
const fetchData = async () => { loading.value = true; try { const res = await getList({ name: searchName.value || undefined }); if (res.code === 200) dataList.value = res.data || [] } catch {} finally { loading.value = false } }
const showAdd = () => { isEdit.value = false; form.value = { companyName: '', orgId: undefined }; modalVisible.value = true }
const showEdit = (record: any) => { isEdit.value = true; form.value = { ...record }; modalVisible.value = true }
const handleOk = async () => {
  modalLoading.value = true
  try { if (isEdit.value) { await update(form.value.id, form.value); message.success('更新成功') } else { await create(form.value); message.success('新增成功') } modalVisible.value = false; await fetchData() }
  catch (err: any) { message.error(err.message || '操作失败') }
  finally { modalLoading.value = false }
}
const handleDelete = async (id: number) => { try { await remove(id); message.success('删除成功'); await fetchData() } catch { message.error('删除失败') } }
</script>
