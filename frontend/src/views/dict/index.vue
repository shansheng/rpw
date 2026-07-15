<template>
  <div class="dict-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>字典管理</span>
          <el-button type="primary" @click="handleAdd">新增</el-button>
        </div>
      </template>

      <!-- 字典表选择 -->
      <el-form :inline="true" class="search-form">
        <el-form-item label="字典表">
          <el-select v-model="selectedTable" placeholder="请选择字典表" @change="handleTableChange">
            <el-option
                v-for="table in dictTables"
                :key="table"
                :label="table"
                :value="table"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <!-- 字典数据表格 -->
      <el-table :data="dictList" v-loading="loading" border style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="code" label="编码" width="150" />
        <el-table-column prop="name" label="名称" width="200" />
        <el-table-column prop="sortOrder" label="排序号" width="100" />
        <el-table-column prop="enabled" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'">
              {{ row.enabled ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        width="500px"
    >
      <el-form ref="dictFormRef" :model="dictForm" :rules="dictRules" label-width="100px">
        <el-form-item label="编码" prop="code">
          <el-input v-model="dictForm.code" placeholder="请输入编码" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="dictForm.name" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="排序号" prop="sortOrder">
          <el-input-number v-model="dictForm.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="状态" prop="enabled">
          <el-switch v-model="dictForm.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useDict } from '@/composables/useDict'

/**
 * 字典管理页面
 * - 支持切换不同字典表
 * - 提供字典的增删改查功能
 */
const {
  loading,
  dictList,
  dictTables,
  fetchDictTables,
  fetchDictList,
  addDict,
  editDict,
  removeDict
} = useDict('')

const selectedTable = ref('')
const dialogVisible = ref(false)
const dialogTitle = ref('新增字典')
const isEdit = ref(false)
const currentId = ref<number>()

// 表单数据
const dictFormRef = ref()
const dictForm = reactive({
  code: '',
  name: '',
  sortOrder: 0,
  enabled: 1
})

// 表单验证规则
const dictRules = {
  code: [{ required: true, message: '请输入编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

// 切换字典表
const handleTableChange = (tableName: string) => {
  selectedTable.value = tableName
  fetchDictList()
}

// 新增字典
const handleAdd = () => {
  if (!selectedTable.value) {
    ElMessage.warning('请先选择字典表')
    return
  }
  resetForm()
  dialogTitle.value = '新增字典'
  isEdit.value = false
  dialogVisible.value = true
}

// 编辑字典
const handleEdit = (row: Record<string, unknown>) => {
  resetForm()
  dialogTitle.value = '编辑字典'
  isEdit.value = true
  currentId.value = row.id as number
  Object.assign(dictForm, {
    code: row.code,
    name: row.name,
    sortOrder: row.sortOrder,
    enabled: row.enabled
  })
  dialogVisible.value = true
}

// 删除字典
const handleDelete = (id: number) => {
  ElMessageBox.confirm('确定删除该字典吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    const success = await removeDict(id)
    if (success) {
      ElMessage.success('删除成功')
    }
  }).catch(() => {})
}

// 提交表单
const handleSubmit = () => {
  dictFormRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      let success = false
      if (isEdit.value && currentId.value) {
        success = await editDict(currentId.value, { ...dictForm })
      } else {
        success = await addDict({ ...dictForm })
      }

      if (success) {
        ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
        dialogVisible.value = false
      }
    }
  })
}

// 重置表单
const resetForm = () => {
  dictForm.code = ''
  dictForm.name = ''
  dictForm.sortOrder = 0
  dictForm.enabled = 1
  currentId.value = undefined
}

onMounted(() => {
  fetchDictTables()
})
</script>

<style scoped>
.dict-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}
</style>
