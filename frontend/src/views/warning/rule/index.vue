<template>
  <div class="warning-rule-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>预警规则配置</span>
          <el-button type="primary" @click="handleAdd">新增规则</el-button>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :inline="true" class="search-form">
        <el-form-item label="资源类型">
          <el-select v-model="searchForm.resourceType" placeholder="请选择" clearable>
            <el-option label="材料" value="material" />
            <el-option label="设备" value="equipment" />
            <el-option label="五金" value="hardware" />
            <el-option label="周转材" value="circulation" />
            <el-option label="办公用品" value="office" />
            <el-option label="安全物资" value="safety" />
            <el-option label="分包" value="subcontract" />
            <el-option label="劳动力" value="labor" />
          </el-select>
        </el-form-item>

        <el-form-item label="预警类型">
          <el-select v-model="searchForm.warningType" placeholder="请选择" clearable>
            <el-option label="预算超支" value="budget_overrun" />
            <el-option label="进度延迟" value="schedule_delay" />
            <el-option label="库存不足" value="inventory_shortage" />
            <el-option label="即将到期" value="expiring_soon" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table :data="tableData" v-loading="loading" border style="width: 100%">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="ruleName" label="规则名称" min-width="120" />
        <el-table-column prop="resourceType" label="资源类型" width="100">
          <template #default="{ row }">
            {{ getResourceTypeText(row.resourceType) }}
          </template>
        </el-table-column>
        <el-table-column prop="warningType" label="预警类型" width="120">
          <template #default="{ row }">
            {{ getWarningTypeText(row.warningType) }}
          </template>
        </el-table-column>
        <el-table-column prop="thresholdValue" label="阈值" width="100" />
        <el-table-column prop="comparisonOperator" label="比较符" width="80" />
        <el-table-column prop="advanceDays" label="提前天数" width="100" />
        <el-table-column prop="enabled" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'info'">
              {{ row.enabled === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="notifyWecom" label="企微通知" width="100">
          <template #default="{ row }">
            <el-tag :type="row.notifyWecom === 1 ? 'success' : 'info'">
              {{ row.notifyWecom === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
                :type="row.enabled === 1 ? 'warning' : 'success'"
                size="small"
                @click="handleToggleEnable(row)"
            >
              {{ row.enabled === 1 ? '禁用' : '启用' }}
            </el-button>
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
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="form.ruleName" placeholder="请输入规则名称" />
        </el-form-item>

        <el-form-item label="资源类型" prop="resourceType">
          <el-select v-model="form.resourceType" placeholder="请选择资源类型" style="width: 100%">
            <el-option label="材料" value="material" />
            <el-option label="设备" value="equipment" />
            <el-option label="五金" value="hardware" />
            <el-option label="周转材" value="circulation" />
            <el-option label="办公用品" value="office" />
            <el-option label="安全物资" value="safety" />
            <el-option label="分包" value="subcontract" />
            <el-option label="劳动力" value="labor" />
          </el-select>
        </el-form-item>

        <el-form-item label="预警类型" prop="warningType">
          <el-select v-model="form.warningType" placeholder="请选择预警类型" style="width: 100%">
            <el-option label="预算超支" value="budget_overrun" />
            <el-option label="进度延迟" value="schedule_delay" />
            <el-option label="库存不足" value="inventory_shortage" />
            <el-option label="即将到期" value="expiring_soon" />
          </el-select>
        </el-form-item>

        <el-form-item label="阈值" prop="thresholdValue">
          <el-input v-model="form.thresholdValue" placeholder="请输入阈值" type="number" />
        </el-form-item>

        <el-form-item label="比较运算符" prop="comparisonOperator">
          <el-select v-model="form.comparisonOperator" placeholder="请选择" style="width: 100%">
            <el-option label="大于 (>)" value=">" />
            <el-option label="小于 (<)" value="<" />
            <el-option label="等于 (=)" value="=" />
            <el-option label="大于等于 (>=)" value=">=" />
            <el-option label="小于等于 (<=)" value="<=" />
          </el-select>
        </el-form-item>

        <el-form-item label="提前天数">
          <el-input v-model="form.advanceDays" placeholder="提前提醒天数" type="number" />
        </el-form-item>

        <el-form-item label="启用">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>

        <el-form-item label="企微通知">
          <el-switch v-model="form.notifyWecom" :active-value="1" :inactive-value="0" />
        </el-form-item>

        <el-form-item label="通知用户">
          <el-input v-model="form.notifyUsers" placeholder="用户ID，多个用逗号分隔" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getWarningRuleList,
  getWarningRuleById,
  createWarningRule,
  updateWarningRule,
  deleteWarningRule
} from '@/api/warning'
import type { WarningRule } from '@/api/warning'

// 数据列表
const tableData = ref<WarningRule[]>([])
const loading = ref(false)

// 搜索表单
const searchForm = reactive({
  resourceType: '',
  warningType: ''
})

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('新增预警规则')
const isEdit = ref(false)
const currentId = ref<number>()

// 表单
const formRef = ref<FormInstance>()
const form = reactive<WarningRule>({
  ruleName: '',
  resourceType: '',
  warningType: '',
  thresholdValue: 0,
  comparisonOperator: '>',
  advanceDays: 0,
  enabled: 1,
  notifyWecom: 0,
  notifyUsers: '',
  remark: ''
})

// 表单验证规则
const rules: FormRules = {
  ruleName: [
    { required: true, message: '请输入规则名称', trigger: 'blur' }
  ],
  resourceType: [
    { required: true, message: '请选择资源类型', trigger: 'change' }
  ],
  warningType: [
    { required: true, message: '请选择预警类型', trigger: 'change' }
  ],
  thresholdValue: [
    { required: true, message: '请输入阈值', trigger: 'blur' }
  ],
  comparisonOperator: [
    { required: true, message: '请选择比较运算符', trigger: 'change' }
  ]
}

// 获取列表数据
async function loadData() {
  loading.value = true
  try {
    const params: any = {}
    if (searchForm.resourceType) params.resourceType = searchForm.resourceType
    if (searchForm.warningType) params.warningType = searchForm.warningType

    const res = await getWarningRuleList(params)
    if (res.code === 200 && res.data) {
      tableData.value = res.data
    }
  } catch (error) {
    console.error('加载数据失败', error)
  } finally {
    loading.value = false
  }
}

// 搜索
function handleSearch() {
  loadData()
}

// 重置
function handleReset() {
  searchForm.resourceType = ''
  searchForm.warningType = ''
  loadData()
}

// 新增
function handleAdd() {
  dialogTitle.value = '新增预警规则'
  isEdit.value = false
  currentId.value = undefined
  resetForm()
  dialogVisible.value = true
}

// 编辑
async function handleEdit(row: WarningRule) {
  dialogTitle.value = '编辑预警规则'
  isEdit.value = true
  currentId.value = row.id

  try {
    const res = await getWarningRuleById(row.id!)
    if (res.code === 200 && res.data) {
      Object.assign(form, res.data)
      dialogVisible.value = true
    }
  } catch (error) {
    console.error('获取详情失败', error)
  }
}

// 提交
async function handleSubmit() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch (error) {
    return
  }

  try {
    let res
    if (isEdit.value) {
      res = await updateWarningRule(currentId.value!, form)
    } else {
      res = await createWarningRule(form)
    }

    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
      dialogVisible.value = false
      loadData()
    }
  } catch (error) {
    console.error('提交失败', error)
  }
}

// 删除
async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除该规则吗？', '提示', {
      type: 'warning'
    })

    const res = await deleteWarningRule(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败', error)
    }
  }
}

// 启用/禁用
async function handleToggleEnable(row: WarningRule) {
  const newEnabled = row.enabled === 1 ? 0 : 1
  try {
    const res = await updateWarningRule(row.id!, { ...row, enabled: newEnabled })
    if (res.code === 200) {
      ElMessage.success(newEnabled === 1 ? '已启用' : '已禁用')
      loadData()
    }
  } catch (error) {
    console.error('操作失败', error)
  }
}

// 重置表单
function resetForm() {
  Object.assign(form, {
    ruleName: '',
    resourceType: '',
    warningType: '',
    thresholdValue: 0,
    comparisonOperator: '>',
    advanceDays: 0,
    enabled: 1,
    notifyWecom: 0,
    notifyUsers: '',
    remark: ''
  })
  formRef.value?.resetFields()
}

// 获取资源类型文本
function getResourceTypeText(type: string) {
  const map: Record<string, string> = {
    material: '材料',
    equipment: '设备',
    hardware: '五金',
    circulation: '周转材',
    office: '办公用品',
    safety: '安全物资',
    subcontract: '分包',
    labor: '劳动力'
  }
  return map[type] || type
}

// 获取预警类型文本
function getWarningTypeText(type: string) {
  const map: Record<string, string> = {
    budget_overrun: '预算超支',
    schedule_delay: '进度延迟',
    inventory_shortage: '库存不足',
    expiring_soon: '即将到期'
  }
  return map[type] || type
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.warning-rule-container {
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
