<template>
  <div style="padding:16px;">
    <a-card title="组织架构">
      <a-button type="primary" @click="showAdd()" style="margin-bottom:16px;">新增组织</a-button>
      <a-table :columns="columns" :data-source="treeData" row-key="id" :loading="loading" :pagination="false" :defaultExpandAllRows="true" bordered size="middle">
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
    <a-modal v-model:open="modalVisible" :title="isEdit?'编辑组织':'新增组织'" @ok="handleOk" :confirm-loading="modalLoading">
      <a-form :model="form" :label-col="{ span: 5 }">
        <a-form-item label="名称"><a-input v-model:value="form.orgName" placeholder="组织名称" /></a-form-item>
        <a-form-item label="父级">
          <a-tree-select v-model:value="form.parentId" :tree-data="treeData" placeholder="顶级组织" allow-clear tree-default-expand-all :field-names="{ children:'children', label:'orgName', value:'id' }" />
        </a-form-item>
        <a-form-item label="部门"><a-input v-model:value="form.department" placeholder="部门名称" /></a-form-item>
        <a-form-item label="科室"><a-input v-model:value="form.section" placeholder="科室名称" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getTree, create, update, remove } from '@/api/organization'

const treeData = ref<any[]>([]), loading = ref(false), modalVisible = ref(false), modalLoading = ref(false), isEdit = ref(false)
const form = ref<any>({ orgName: '', parentId: undefined, department: '', section: '' })

const columns = [
  { title: '名称', dataIndex: 'orgName', key: 'orgName' },
  { title: '部门', dataIndex: 'department', key: 'department', width: 150 },
  { title: '科室', dataIndex: 'section', key: 'section', width: 150 },
  { title: '操作', key: 'action', width: 150 },
]

onMounted(() => fetchData())
const fetchData = async () => { loading.value = true; try { const res = await getTree(); if (res.code === 200) treeData.value = res.data || [] } catch {} finally { loading.value = false } }

const showAdd = () => { isEdit.value = false; form.value = { orgName: '', parentId: undefined, department: '', section: '' }; modalVisible.value = true }
const showEdit = (record: any) => { isEdit.value = true; form.value = { ...record }; modalVisible.value = true }

const handleOk = async () => {
  modalLoading.value = true
  try {
    if (isEdit.value) { await update(form.value.id, form.value); message.success('更新成功') }
    else { await create(form.value); message.success('新增成功') }
    modalVisible.value = false; await fetchData()
  } catch (err: any) { message.error(err.message || '操作失败') }
  finally { modalLoading.value = false }
}
const handleDelete = async (id: number) => { try { await remove(id); message.success('删除成功'); await fetchData() } catch { message.error('删除失败') } }
</script>
