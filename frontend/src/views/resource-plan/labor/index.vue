<template>
  <div class="resource-plan-labor-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>劳动力需求管理</span>
          <div>
            <el-button type="primary" @click="handleAdd">新增</el-button>
            <el-button type="success" @click="handleImport">导入</el-button>
            <el-button type="warning" @click="handleExport">导出</el-button>
          </div>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :inline="true" class="search-form">
        <el-form-item label="工种编码">
          <el-input
              v-model="searchForm.workTypeCode"
              placeholder="请输入工种编码"
              clearable
              @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="劳务类别">
          <el-input
              v-model="searchForm.laborCategoryCode"
              placeholder="请输入劳务类别编码"
              clearable
              @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="全部" value="" />
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已提交" value="SUBMITTED" />
            <el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="已完成" value="COMPLETED" />
          </el-select>
        </el-form-item>

        <el-form-item label="项目ID">
          <el-input
              v-model="searchForm.projectId"
              placeholder="请输入项目ID"
              clearable
              type="number"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table :data="tableData" v-loading="loading" border style="width: 100%">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="wbsCode" label="WBS编码" width="120" />
        <el-table-column prop="wbsName" label="WBS名称" min-width="120" />
        <el-table-column prop="workTypeCode" label="工种编码" width="120" />
        <el-table-column prop="workTypeName" label="工种名称" width="120" />
        <el-table-column prop="laborCategoryName" label="劳务类别" width="120" />
        <el-table-column prop="planQuantity" label="计划人数" width="100" />
        <el-table-column label="人力状态" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.actualQuantity != null && row.planQuantity != null && row.actualQuantity < row.planQuantity * 0.8" type="danger">
              人力不足
            </el-tag>
            <el-tag v-else-if="row.actualQuantity != null && row.planQuantity != null && row.actualQuantity < row.planQuantity" type="warning">
              人力即将不足
            </el-tag>
            <el-tag v-else type="success">
              人力充足
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="actualQuantity" label="实际人数" width="100" />
        <el-table-column prop="planStartDate" label="计划开始日期" width="130" />
        <el-table-column prop="planEndDate" label="计划结束日期" width="130" />
        <el-table-column prop="actualStartDate" label="实际开始日期" width="130" />
        <el-table-column prop="actualEndDate" label="实际结束日期" width="130" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="success" size="small" @click="handleProgress(row)">进展登记</el-button>
            <el-button type="warning" size="small" @click="handleChange(row)">发起变更</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        width="600px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="项目ID" prop="projectId">
          <el-input v-model="form.projectId" placeholder="请输入项目ID" type="number" />
        </el-form-item>

        <el-form-item label="WBS编码" prop="wbsCode">
          <el-input v-model="form.wbsCode" placeholder="请输入WBS编码" />
        </el-form-item>

        <el-form-item label="WBS名称">
          <el-input v-model="form.wbsName" placeholder="请输入WBS名称" />
        </el-form-item>

        <el-form-item label="工种编码" prop="workTypeCode">
          <el-input v-model="form.workTypeCode" placeholder="请输入工种编码" />
        </el-form-item>

        <el-form-item label="工种名称">
          <el-input v-model="form.workTypeName" placeholder="请输入工种名称" />
        </el-form-item>

        <el-form-item label="劳务类别编码" prop="laborCategoryCode">
          <el-input v-model="form.laborCategoryCode" placeholder="请输入劳务类别编码" />
        </el-form-item>

        <el-form-item label="劳务类别名称">
          <el-input v-model="form.laborCategoryName" placeholder="请输入劳务类别名称" />
        </el-form-item>

        <el-form-item label="计划人数" prop="planQuantity">
          <el-input v-model="form.planQuantity" placeholder="请输入计划人数" type="number" />
        </el-form-item>

        <el-form-item label="实际人数">
          <el-input v-model="form.actualQuantity" placeholder="请输入实际人数" type="number" />
        </el-form-item>

        <el-form-item label="计划开始日期" prop="planStartDate">
          <el-date-picker
              v-model="form.planStartDate"
              type="date"
              placeholder="选择日期"
              value-format="YYYY-MM-DD"
          />
        </el-form-item>

        <el-form-item label="计划结束日期" prop="planEndDate">
          <el-date-picker
              v-model="form.planEndDate"
              type="date"
              placeholder="选择日期"
              value-format="YYYY-MM-DD"
          />
        </el-form-item>

        <el-form-item label="实际开始日期">
          <el-date-picker
              v-model="form.actualStartDate"
              type="date"
              placeholder="选择日期"
              value-format="YYYY-MM-DD"
          />
        </el-form-item>

        <el-form-item label="实际结束日期">
          <el-date-picker
              v-model="form.actualEndDate"
              type="date"
              placeholder="选择日期"
              value-format="YYYY-MM-DD"
          />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 进展登记对话框 -->
    <ProgressDialog
        v-model:visible="progressVisible"
        :data="currentRow"
        @success="loadData"
    />

    <!-- 变更申请对话框 -->
    <ChangeDialog
        v-model:visible="changeVisible"
        :data="currentRow"
        @success="loadData"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getLaborList,
  getLaborById,
  createLabor,
  updateLabor,
  deleteLabor,
  submitLabor,
  registerProgress
} from '@/api/resourcePlanLabor'
import ProgressDialog from './components/ProgressDialog.vue'
import ChangeDialog from './components/ChangeDialog.vue'

interface ResourcePlanLabor {
  id?: number;
  projectId?: number;
  wbsCode: string;
  wbsName?: string;
  workTypeCode: string;
  workTypeName?: string;
  laborCategoryCode?: string;
  laborCategoryName?: string;
  planQuantity?: number;
  actualQuantity?: number;
  planStartDate?: string;
  planEndDate?: string;
  actualStartDate?: string;
  actualEndDate?: string;
  status?: string;
  remark?: string;
}

// 数据列表
const tableData = ref<ResourcePlanLabor[]>([])
const loading = ref(false)

// 搜索表单
const searchForm = reactive({
  workTypeCode: '',
  laborCategoryCode: '',
  status: '',
  projectId: undefined as number | undefined
})

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('新增劳动力需求')
const isEdit = ref(false)
const currentId = ref<number>()

// 进展登记
const progressVisible = ref(false)
const currentRow = ref<ResourcePlanLabor>()

// 变更申请
const changeVisible = ref(false)

// 表单
const formRef = ref<FormInstance>()
const form = reactive<ResourcePlanLabor>({
  projectId: undefined,
  wbsCode: '',
  wbsName: '',
  workTypeCode: '',
  workTypeName: '',
  laborCategoryCode: '',
  laborCategoryName: '',
  planQuantity: undefined,
  actualQuantity: undefined,
  planStartDate: '',
  planEndDate: '',
  actualStartDate: '',
  actualEndDate: '',
  status: '',
  remark: ''
})

// 表单验证规则
const rules: FormRules = {
  projectId: [{ required: true, message: '请输入项目ID', trigger: 'blur' }],
  wbsCode: [{ required: true, message: '请输入WBS编码', trigger: 'blur' }],
  workTypeCode: [{ required: true, message: '请输入工种编码', trigger: 'blur' }],
  laborCategoryCode: [{ required: true, message: '请输入劳务类别编码', trigger: 'blur' }],
  planQuantity: [{ required: true, message: '请输入计划人数', trigger: 'blur' }]
}

// 加载数据列表
const loadData = async () => {
  loading.value = true
  try {
    const params: any = {}
    if (searchForm.projectId) params.projectId = searchForm.projectId
    if (searchForm.status) params.status = searchForm.status
    if (searchForm.workTypeCode) params.workTypeCode = searchForm.workTypeCode
    if (searchForm.laborCategoryCode) params.laborCategoryCode = searchForm.laborCategoryCode
    const res = await getLaborList(params)
    if (res.data) {
      tableData.value = res.data
    }
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  loadData()
}

// 重置搜索
const handleReset = () => {
  searchForm.workTypeCode = ''
  searchForm.laborCategoryCode = ''
  searchForm.status = ''
  searchForm.projectId = undefined
  loadData()
}

// 新增
const handleAdd = () => {
  resetForm()
  dialogTitle.value = '新增劳动力需求'
  isEdit.value = false
  dialogVisible.value = true
}

// 编辑
const handleEdit = async (row: ResourcePlanLabor) => {
  resetForm()
  dialogTitle.value = '编辑劳动力需求'
  isEdit.value = true
  currentId.value = row.id

  try {
    const res = await getLaborById(row.id!)
    if (res.data) {
      Object.assign(form, res.data)
    }
  } catch (error) {
    ElMessage.error('加载详情失败')
  }

  dialogVisible.value = true
}

// 删除
const handleDelete = (id: number) => {
  ElMessageBox.confirm('确定删除该劳动力计划吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      const res = await deleteLabor(id)
      if (res.data) {
        ElMessage.success('删除成功')
        await loadData()
      }
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

// 提交审批
const handleSubmitApprove = async (id: number) => {
  try {
    const res = await submitLabor(id)
    if (res.data) {
      ElMessage.success('提交成功')
      await loadData()
    }
  } catch (error) {
    ElMessage.error('提交失败')
  }
}

// 进展登记
const handleProgress = (row: ResourcePlanLabor) => {
  currentRow.value = row
  progressVisible.value = true
}

// 发起变更
const handleChange = (row: ResourcePlanLabor) => {
  currentRow.value = row
  changeVisible.value = true
}

// 导入
const handleImport = () => {
  ElMessage.info('导入功能开发中')
}

// 导出
const handleExport = () => {
  ElMessage.info('导出功能开发中')
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        let success = false
        if (isEdit.value && currentId.value) {
          const res = await updateLabor(currentId.value, { ...form })
          success = res.data || false
        } else {
          const res = await createLabor({ ...form })
          success = res.data || false
        }

        if (success) {
          ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
          dialogVisible.value = false
          await loadData()
        }
      } catch (error) {
        ElMessage.error('操作失败')
      }
    }
  })
}

// 获取状态标签类型
const getStatusType = (status: string | undefined) => {
  if (!status) return 'info'
  switch (status) {
    case 'DRAFT': return 'info'
    case 'SUBMITTED': return 'warning'
    case 'IN_PROGRESS': return 'primary'
    case 'COMPLETED': return 'success'
    case 'TERMINATED': return 'danger'
    default: return 'info'
  }
}

// 获取状态文本
const getStatusText = (status: string | undefined) => {
  if (!status) return '未知'
  switch (status) {
    case 'DRAFT': return '草稿'
    case 'SUBMITTED': return '已提交'
    case 'IN_PROGRESS': return '进行中'
    case 'COMPLETED': return '已完成'
    case 'TERMINATED': return '已终止'
    default: return status
  }
}

// 重置表单
const resetForm = () => {
  form.projectId = undefined
  form.wbsCode = ''
  form.wbsName = ''
  form.workTypeCode = ''
  form.workTypeName = ''
  form.laborCategoryCode = ''
  form.laborCategoryName = ''
  form.planQuantity = undefined
  form.actualQuantity = undefined
  form.planStartDate = ''
  form.planEndDate = ''
  form.actualStartDate = ''
  form.actualEndDate = ''
  form.status = ''
  form.remark = ''
  formRef.value?.resetFields()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.resource-plan-labor-container {
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
