<template>
  <div class="equipment-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>设备计划管理</span>
          <el-button type="primary" @click="handleAdd">新增设备计划</el-button>
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

        <el-form-item label="设备名称">
          <el-input
              v-model="searchForm.equipmentName"
              placeholder="请输入设备名称"
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
        <el-table-column prop="equipmentName" label="设备名称" min-width="150" />
        <el-table-column prop="specification" label="规格型号" min-width="150" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column prop="budgetQuantity" label="预算数量" width="100" />
        <el-table-column prop="supplierCode" label="供应商编码" width="120" />
        <el-table-column prop="purchaseProgressCode" label="采购进度" width="120" />
        <el-table-column prop="planArrivalDate" label="计划到场日期" width="130" />
        <el-table-column prop="actualArrivalDate" label="实际到场日期" width="130" />
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" width="150" fixed="right">
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
        width="600px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="项目ID" prop="projectId">
          <el-input v-model="form.projectId" placeholder="请输入项目ID" type="number" />
        </el-form-item>

        <el-form-item label="WBS编码" prop="wbsCode">
          <el-input v-model="form.wbsCode" placeholder="请输入WBS编码" />
        </el-form-item>

        <el-form-item label="设备名称" prop="equipmentName">
          <el-input v-model="form.equipmentName" placeholder="请输入设备名称" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getResourcePlanEquipmentList,
  getResourcePlanEquipmentById,
  createResourcePlanEquipment,
  updateResourcePlanEquipment,
  deleteResourcePlanEquipment
} from '@/api/resourcePlanEquipment'

interface ResourcePlanEquipment {
  id?: number
  projectId?: number
  wbsCode: string
  equipmentName: string
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
}

// 数据列表
const tableData = ref<ResourcePlanEquipment[]>([])
const loading = ref(false)

// 搜索表单
const searchForm = reactive({
  wbsCode: '',
  equipmentName: ''
})

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('新增设备计划')
const isEdit = ref(false)
const currentId = ref<number>()

// 表单
const formRef = ref<FormInstance>()
const form = reactive<ResourcePlanEquipment>({
  projectId: undefined,
  wbsCode: '',
  equipmentName: '',
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
  equipmentName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }]
}

// 加载数据列表
const loadData = async () => {
  loading.value = true
  try {
    const res = await getResourcePlanEquipmentList()
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
  searchForm.equipmentName = ''
  loadData()
}

// 新增
const handleAdd = () => {
  resetForm()
  dialogTitle.value = '新增设备计划'
  isEdit.value = false
  dialogVisible.value = true
}

// 编辑
const handleEdit = async (row: ResourcePlanEquipment) => {
  resetForm()
  dialogTitle.value = '编辑设备计划'
  isEdit.value = true
  currentId.value = row.id

  try {
    const res = await getResourcePlanEquipmentById(row.id!)
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
  ElMessageBox.confirm('确定删除该设备计划吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      const res = await deleteResourcePlanEquipment(id)
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
          res = await updateResourcePlanEquipment(currentId.value, { ...form })
        } else {
          res = await createResourcePlanEquipment({ ...form })
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
  form.equipmentName = ''
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

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.equipment-container {
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
