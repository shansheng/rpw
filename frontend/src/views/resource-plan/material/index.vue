<template>
  <div class="material-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>材料计划管理</span>
          <el-button type="primary" @click="handleAdd">新增材料计划</el-button>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :inline="true" class="search-form">
        <el-form-item label="WBS编码">
          <el-input
              v-model="searchForm.wbsCode"
              placeholder="请输入WBS编码"
              clearable
              @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="材料名称">
          <el-input
              v-model="searchForm.resourceName"
              placeholder="请输入材料名称"
              clearable
              @keyup.enter="handleSearch"
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
        <el-table-column prop="resourceName" label="材料名称" min-width="150" />
        <el-table-column prop="specification" label="规格型号" min-width="150" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column prop="budgetQuantity" label="预算数量" width="100" />
        <el-table-column prop="supplierCode" label="供应商编码" width="120" />
        <el-table-column prop="purchaseProgressCode" label="采购进度" width="120" />
        <el-table-column prop="planArrivalDate" label="计划到场日期" width="130" />
        <el-table-column prop="actualArrivalDate" label="实际到场日期" width="130" />
        <el-table-column prop="actualArrivalDate" label="实际到场日期" width="130" />
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
        <el-table-column prop="approvalStatus" label="审批状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getApprovalStatusType(row.approvalStatus)">
              {{ getApprovalStatusText(row.approvalStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
            <el-button 
              v-if="!row.approvalStatus || row.approvalStatus === 'DRAFT' || row.approvalStatus === 'REJECTED'" 
              type="warning" 
              size="small" 
              @click="handleSubmitApproval(row)">
              提交审批
            </el-button>
            <el-button 
              v-if="row.approvalStatus === 'SUBMITTED'" 
              type="success" 
              size="small" 
              @click="handleViewProgress(row)">
              查看进度
            </el-button>
            <el-button 
              size="small" 
              @click="handleViewHistory(row)">
              审批历史
            </el-button>
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

        <el-form-item label="材料名称" prop="resourceName">
          <el-input v-model="form.resourceName" placeholder="请输入材料名称" />
        </el-form-item>

        <el-form-item label="规格型号" prop="specification">
          <el-input v-model="form.specification" placeholder="请输入规格型号" />
        </el-form-item>

        <el-form-item label="单位" prop="unit">
          <el-input v-model="form.unit" placeholder="请输入单位" />
        </el-form-item>

        <el-form-item label="预算数量" prop="budgetQuantity">
          <el-input v-model="form.budgetQuantity" placeholder="请输入预算数量" type="number" />
        </el-form-item>

        <el-form-item label="供应商编码" prop="supplierCode">
          <el-input v-model="form.supplierCode" placeholder="请输入供应商编码" />
        </el-form-item>

        <el-form-item label="采购来源编码" prop="purchaseSourceCode">
          <el-input v-model="form.purchaseSourceCode" placeholder="请输入采购来源编码" />
        </el-form-item>

        <el-form-item label="采购进度编码" prop="purchaseProgressCode">
          <el-input v-model="form.purchaseProgressCode" placeholder="请输入采购进度编码" />
        </el-form-item>

        <el-form-item label="发货进度编码" prop="shippingProgressCode">
          <el-input v-model="form.shippingProgressCode" placeholder="请输入发货进度编码" />
        </el-form-item>

        <el-form-item label="计划到场日期" prop="planArrivalDate">
          <el-date-picker
              v-model="form.planArrivalDate"
              type="date"
              placeholder="选择计划到场日期"
              style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="实际到场日期" prop="actualArrivalDate">
          <el-date-picker
              v-model="form.actualArrivalDate"
              type="date"
              placeholder="选择实际到场日期"
              style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 审批历史对话框 -->
    <el-dialog v-model="historyDialogVisible" title="审批历史" width="800px">
      <el-timeline>
        <el-timeline-item 
          v-for="item in historyList" 
          :key="item.id"
          :timestamp="item.endTime || item.startTime"
          placement="top"
        >
          <el-card>
            <h4>{{ item.name }}</h4>
            <p>负责人: {{ item.assignee }}</p>
            <p>开始时间: {{ item.startTime }}</p>
            <p v-if="item.endTime">结束时间: {{ item.endTime }}</p>
            <p>耗时: {{ item.durationInMillis ? (item.durationInMillis / 1000).toFixed(2) + '秒' : '进行中' }}</p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-if="historyList.length === 0" description="暂无审批历史" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getResourcePlanMaterialList,
  getResourcePlanMaterialById,
  createResourcePlanMaterial,
  updateResourcePlanMaterial,
  deleteResourcePlanMaterial
} from '@/api/resourcePlanMaterial'
import {
  submitApproval,
  getApprovalHistory
} from '@/api/flowable'

interface ResourcePlanMaterial {
  id?: number
  projectId?: number
  wbsCode: string
  resourceName: string
  specification?: string
  unit?: string
  budgetQuantity?: number
  supplierCode?: string
  purchaseSourceCode?: string
  purchaseProgressCode?: string
  shippingProgressCode?: string
  planArrivalDate?: string
  actualArrivalDate?: string
  remark?: string
  approvalStatus?: string
  processInstanceId?: string
  approvalComment?: string
}

// 数据列表
const tableData = ref<ResourcePlanMaterial[]>([])
const loading = ref(false)

// 搜索表单
const searchForm = reactive({
  wbsCode: '',
  resourceName: ''
})

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('新增材料计划')
const isEdit = ref(false)
const currentId = ref<number>()

// 表单
const formRef = ref<FormInstance>()
const form = reactive<ResourcePlanMaterial>({
  projectId: undefined,
  wbsCode: '',
  resourceName: '',
  specification: '',
  unit: '',
  budgetQuantity: undefined,
  supplierCode: '',
  purchaseSourceCode: '',
  purchaseProgressCode: '',
  shippingProgressCode: '',
  planArrivalDate: '',
  actualArrivalDate: '',
  remark: ''
})

// 表单验证规则
const rules: FormRules = {
  wbsCode: [{ required: true, message: '请输入WBS编码', trigger: 'blur' }],
  resourceName: [{ required: true, message: '请输入材料名称', trigger: 'blur' }]
}

// 加载数据列表
const loadData = async () => {
  loading.value = true
  try {
    const res = await getResourcePlanMaterialList()
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
  searchForm.wbsCode = ''
  searchForm.resourceName = ''
  loadData()
}

// 新增
const handleAdd = () => {
  resetForm()
  dialogTitle.value = '新增材料计划'
  isEdit.value = false
  dialogVisible.value = true
}

// 编辑
const handleEdit = async (row: ResourcePlanMaterial) => {
  resetForm()
  dialogTitle.value = '编辑材料计划'
  isEdit.value = true
  currentId.value = row.id

  try {
    const res = await getResourcePlanMaterialById(row.id!)
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
  ElMessageBox.confirm('确定删除该材料计划吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      const res = await deleteResourcePlanMaterial(id)
      if (res.data) {
        ElMessage.success('删除成功')
        await loadData()
      }
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        let res
        if (isEdit.value && currentId.value) {
          res = await updateResourcePlanMaterial(currentId.value, { ...form })
        } else {
          res = await createResourcePlanMaterial({ ...form })
        }

        if (res.data) {
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

// 重置表单
const resetForm = () => {
  form.projectId = undefined
  form.wbsCode = ''
  form.resourceName = ''
  form.specification = ''
  form.unit = ''
  form.budgetQuantity = undefined
  form.supplierCode = ''
  form.purchaseSourceCode = ''
  form.purchaseProgressCode = ''
  form.shippingProgressCode = ''
  form.planArrivalDate = ''
  form.actualArrivalDate = ''
  form.remark = ''
  formRef.value?.resetFields()
}

// 获取审批状态标签类型
const getApprovalStatusType = (status: string | undefined) => {
  if (!status) return 'info'
  switch (status) {
    case 'DRAFT': return 'info'
    case 'SUBMITTED': return 'warning'
    case 'APPROVED': return 'success'
    case 'REJECTED': return 'danger'
    default: return 'info'
  }
}

// 获取审批状态文本
const getApprovalStatusText = (status: string | undefined) => {
  if (!status) return '草稿'
  switch (status) {
    case 'DRAFT': return '草稿'
    case 'SUBMITTED': return '审批中'
    case 'APPROVED': return '已通过'
    case 'REJECTED': return '已驳回'
    default: return '未知'
  }
}

// 提交审批
const handleSubmitApproval = async (row: ResourcePlanMaterial) => {
  try {
    await ElMessageBox.confirm('确认提交审批吗？', '提示', {
      type: 'warning'
    })
    const res = await submitApproval(row.id!)
    if (res.data) {
      ElMessage.success('提交审批成功')
      await loadData()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('提交审批失败')
    }
  }
}

// 查看进度
const handleViewProgress = (row: ResourcePlanMaterial) => {
  // TODO: 跳转到流程进度页面或打开进度对话框
  ElMessage.info('查看进度功能开发中')
}

// 审批历史相关
const historyDialogVisible = ref(false)
const historyList = ref([])

// 查看审批历史
const handleViewHistory = async (row: ResourcePlanMaterial) => {
  try {
    const res = await getApprovalHistory(row.id!)
    if (res.data) {
      historyList.value = res.data
      historyDialogVisible.value = true
    }
  } catch (error) {
    ElMessage.error('查询审批历史失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.material-container {
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
