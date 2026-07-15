<template>
  <div class="report-config-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>自定义报表</span>
          <div>
            <el-button type="primary" @click="handleNew">新建报表</el-button>
          </div>
        </div>
      </template>

      <!-- 报表类型筛选 -->
      <el-form :inline="true" class="filter-form">
        <el-form-item label="报表类型">
          <el-select v-model="filterReportType" placeholder="全部类型" clearable @change="loadConfigList">
            <el-option label="分包计划" value="SUBTRACT" />
            <el-option label="劳动力计划" value="LABOR" />
            <el-option label="设备计划" value="EQUIPMENT" />
            <el-option label="材料计划" value="MATERIAL" />
            <el-option label="安全物资计划" value="SAFETY" />
            <el-option label="办公用品计划" value="OFFICE" />
            <el-option label="五金计划" value="HARDWARE" />
            <el-option label="周转材计划" value="CIRCULATION" />
          </el-select>
        </el-form-item>
      </el-form>

      <!-- 已保存的报表配置列表 -->
      <el-table :data="configList" border v-loading="listLoading">
        <el-table-column prop="reportName" label="报表名称" min-width="150" />
        <el-table-column prop="reportType" label="报表类型" width="120">
          <template #default="{ row }">
            <el-tag>{{ getReportTypeName(row.reportType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isDefault" label="默认" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault === 1" type="success">是</el-tag>
            <span v-else>否</span>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="180" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handlePreview(row)">预览</el-button>
            <el-button type="warning" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建/编辑报表配置对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="800px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="configForm" :rules="formRules" label-width="100px">
        <el-form-item label="报表名称" prop="reportName">
          <el-input v-model="configForm.reportName" placeholder="请输入报表名称" />
        </el-form-item>

        <el-form-item label="报表类型" prop="reportType">
          <el-select v-model="configForm.reportType" placeholder="请选择报表类型" @change="onReportTypeChange" :disabled="isEdit">
            <el-option label="分包计划" value="SUBTRACT" />
            <el-option label="劳动力计划" value="LABOR" />
            <el-option label="设备计划" value="EQUIPMENT" />
            <el-option label="材料计划" value="MATERIAL" />
            <el-option label="安全物资计划" value="SAFETY" />
            <el-option label="办公用品计划" value="OFFICE" />
            <el-option label="五金计划" value="HARDWARE" />
            <el-option label="周转材计划" value="CIRCULATION" />
          </el-select>
        </el-form-item>

        <el-form-item label="设为默认">
          <el-switch v-model="configForm.isDefault" />
        </el-form-item>
      </el-form>

      <el-divider>字段选择</el-divider>
      <FieldSelector v-model="configForm.fields" />

      <el-divider>筛选条件</el-divider>
      <FilterConfigurator v-model="configForm.filters" :available-fields="configForm.fields" />

      <el-divider>排序规则</el-divider>
      <SortConfigurator v-model="configForm.sorts" :available-fields="configForm.fields" />

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存配置</el-button>
        <el-button type="success" @click="handleSaveAndPreview">保存并预览</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getReportConfigList,
  deleteReportConfig,
  saveReportConfig,
  type ReportConfigVO,
  type ReportConfigJson,
  type FieldItem
} from '@/api/customReport'
import FieldSelector from '@/components/report/FieldSelector.vue'
import FilterConfigurator from '@/components/report/FilterConfigurator.vue'
import SortConfigurator from '@/components/report/SortConfigurator.vue'

const router = useRouter()

// 报表类型对应的可选字段
const typeFieldMap: Record<string, FieldItem[]> = {
  SUBTRACT: [
    { field: 'wbsCode', label: 'WBS编码', visible: true, width: 120 },
    { field: 'wbsName', label: 'WBS名称', visible: true, width: 150 },
    { field: 'subcontractName', label: '分包项目名称', visible: true, width: 180 },
    { field: 'workContent', label: '工作内容', visible: true, width: 200 },
    { field: 'supplierName', label: '供应商', visible: true, width: 120 },
    { field: 'planStartDate', label: '计划开始日期', visible: true, width: 130 },
    { field: 'planEndDate', label: '计划结束日期', visible: true, width: 130 },
    { field: 'actualStartDate', label: '实际开始日期', visible: false, width: 130 },
    { field: 'actualEndDate', label: '实际结束日期', visible: false, width: 130 },
    { field: 'status', label: '状态', visible: true, width: 100 },
    { field: 'approvalStatus', label: '审批状态', visible: false, width: 100 },
    { field: 'remark', label: '备注', visible: false, width: 150 }
  ],
  LABOR: [
    { field: 'wbsCode', label: 'WBS编码', visible: true, width: 120 },
    { field: 'wbsName', label: 'WBS名称', visible: true, width: 150 },
    { field: 'workType', label: '工种', visible: true, width: 120 },
    { field: 'planQuantity', label: '计划数量', visible: true, width: 100 },
    { field: 'actualQuantity', label: '实际数量', visible: false, width: 100 },
    { field: 'unit', label: '单位', visible: true, width: 80 },
    { field: 'planStartDate', label: '计划开始日期', visible: true, width: 130 },
    { field: 'planEndDate', label: '计划结束日期', visible: true, width: 130 },
    { field: 'status', label: '状态', visible: true, width: 100 },
    { field: 'remark', label: '备注', visible: false, width: 150 }
  ],
  EQUIPMENT: [
    { field: 'wbsCode', label: 'WBS编码', visible: true, width: 120 },
    { field: 'equipmentName', label: '设备名称', visible: true, width: 150 },
    { field: 'equipmentModel', label: '设备型号', visible: true, width: 120 },
    { field: 'planQuantity', label: '计划数量', visible: true, width: 100 },
    { field: 'actualQuantity', label: '实际数量', visible: false, width: 100 },
    { field: 'planStartDate', label: '计划开始日期', visible: true, width: 130 },
    { field: 'planEndDate', label: '计划结束日期', visible: true, width: 130 },
    { field: 'status', label: '状态', visible: true, width: 100 },
    { field: 'remark', label: '备注', visible: false, width: 150 }
  ],
  MATERIAL: [
    { field: 'wbsCode', label: 'WBS编码', visible: true, width: 120 },
    { field: 'materialName', label: '材料名称', visible: true, width: 150 },
    { field: 'specification', label: '规格型号', visible: true, width: 120 },
    { field: 'planQuantity', label: '计划数量', visible: true, width: 100 },
    { field: 'unit', label: '单位', visible: true, width: 80 },
    { field: 'planStartDate', label: '计划开始日期', visible: true, width: 130 },
    { field: 'planEndDate', label: '计划结束日期', visible: true, width: 130 },
    { field: 'status', label: '状态', visible: true, width: 100 },
    { field: 'remark', label: '备注', visible: false, width: 150 }
  ],
  SAFETY: [
    { field: 'wbsCode', label: 'WBS编码', visible: true, width: 120 },
    { field: 'safetyType', label: '安全物资类型', visible: true, width: 150 },
    { field: 'safetyName', label: '安全物资名称', visible: true, width: 150 },
    { field: 'planQuantity', label: '计划数量', visible: true, width: 100 },
    { field: 'unit', label: '单位', visible: true, width: 80 },
    { field: 'planStartDate', label: '计划开始日期', visible: true, width: 130 },
    { field: 'planEndDate', label: '计划结束日期', visible: true, width: 130 },
    { field: 'status', label: '状态', visible: true, width: 100 },
    { field: 'remark', label: '备注', visible: false, width: 150 }
  ],
  OFFICE: [
    { field: 'wbsCode', label: 'WBS编码', visible: true, width: 120 },
    { field: 'officeType', label: '办公类型', visible: true, width: 120 },
    { field: 'officeName', label: '办公物品名称', visible: true, width: 150 },
    { field: 'planQuantity', label: '计划数量', visible: true, width: 100 },
    { field: 'unit', label: '单位', visible: true, width: 80 },
    { field: 'planStartDate', label: '计划开始日期', visible: true, width: 130 },
    { field: 'planEndDate', label: '计划结束日期', visible: true, width: 130 },
    { field: 'status', label: '状态', visible: true, width: 100 },
    { field: 'remark', label: '备注', visible: false, width: 150 }
  ],
  HARDWARE: [
    { field: 'wbsCode', label: 'WBS编码', visible: true, width: 120 },
    { field: 'hardwareName', label: '五金名称', visible: true, width: 150 },
    { field: 'specification', label: '规格型号', visible: true, width: 120 },
    { field: 'planQuantity', label: '计划数量', visible: true, width: 100 },
    { field: 'unit', label: '单位', visible: true, width: 80 },
    { field: 'planStartDate', label: '计划开始日期', visible: true, width: 130 },
    { field: 'planEndDate', label: '计划结束日期', visible: true, width: 130 },
    { field: 'status', label: '状态', visible: true, width: 100 },
    { field: 'remark', label: '备注', visible: false, width: 150 }
  ],
  CIRCULATION: [
    { field: 'wbsCode', label: 'WBS编码', visible: true, width: 120 },
    { field: 'circulationType', label: '周转材类型', visible: true, width: 150 },
    { field: 'circulationName', label: '周转材名称', visible: true, width: 150 },
    { field: 'planQuantity', label: '计划数量', visible: true, width: 100 },
    { field: 'unit', label: '单位', visible: true, width: 80 },
    { field: 'planStartDate', label: '计划开始日期', visible: true, width: 130 },
    { field: 'planEndDate', label: '计划结束日期', visible: true, width: 130 },
    { field: 'status', label: '状态', visible: true, width: 100 },
    { field: 'remark', label: '备注', visible: false, width: 150 }
  ]
}

const reportTypeNames: Record<string, string> = {
  SUBTRACT: '分包计划',
  LABOR: '劳动力计划',
  EQUIPMENT: '设备计划',
  MATERIAL: '材料计划',
  SAFETY: '安全物资计划',
  OFFICE: '办公用品计划',
  HARDWARE: '五金计划',
  CIRCULATION: '周转材计划'
}

// 配置列表
const configList = ref<ReportConfigVO[]>([])
const listLoading = ref(false)
const filterReportType = ref('')

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('新建报表')
const isEdit = ref(false)
const editingId = ref<number>()

// 表单
const formRef = ref<FormInstance>()
const configForm = reactive<{
  reportName: string
  reportType: string
  isDefault: boolean
  fields: FieldItem[]
  filters: any[]
  sorts: any[]
}>({
  reportName: '',
  reportType: '',
  isDefault: false,
  fields: [],
  filters: [],
  sorts: []
})

const formRules: FormRules = {
  reportName: [{ required: true, message: '请输入报表名称', trigger: 'blur' }],
  reportType: [{ required: true, message: '请选择报表类型', trigger: 'change' }]
}

const mockUserId = 1 // TODO: 从登录信息中获取

const getReportTypeName = (type: string) => reportTypeNames[type] || type

const loadConfigList = async () => {
  listLoading.value = true
  try {
    const res = await getReportConfigList({ userId: mockUserId, reportType: filterReportType.value || undefined })
    if (res.data) {
      configList.value = res.data
    }
  } catch (error) {
    ElMessage.error('加载报表配置失败')
  } finally {
    listLoading.value = false
  }
}

const onReportTypeChange = () => {
  const fields = typeFieldMap[configForm.reportType]
  if (fields) {
    configForm.fields = JSON.parse(JSON.stringify(fields))
  }
  configForm.filters = []
  configForm.sorts = []
}

const resetForm = () => {
  configForm.reportName = ''
  configForm.reportType = ''
  configForm.isDefault = false
  configForm.fields = []
  configForm.filters = []
  configForm.sorts = []
  formRef.value?.resetFields()
}

const handleNew = () => {
  resetForm()
  dialogTitle.value = '新建报表'
  isEdit.value = false
  editingId.value = undefined
  dialogVisible.value = true
}

const handleEdit = (row: ReportConfigVO) => {
  resetForm()
  dialogTitle.value = '编辑报表'
  isEdit.value = true
  editingId.value = row.id

  configForm.reportName = row.reportName
  configForm.reportType = row.reportType
  configForm.isDefault = row.isDefault === 1

  try {
    const config: ReportConfigJson = JSON.parse(row.configJson)
    configForm.fields = config.fields || []
    configForm.filters = config.filters || []
    configForm.sorts = config.sorts || []
  } catch (e) {
    ElMessage.error('配置JSON解析失败')
  }

  dialogVisible.value = true
}

const handleDelete = (id: number) => {
  ElMessageBox.confirm('确定删除该报表配置吗？', '提示', { type: 'warning' })
    .then(async () => {
      try {
        const res = await deleteReportConfig(id)
        if (res.data) {
          ElMessage.success('删除成功')
          await loadConfigList()
        }
      } catch (error) {
        ElMessage.error('删除失败')
      }
    })
    .catch(() => {})
}

const buildConfigJson = (): string => {
  const config: ReportConfigJson = {
    fields: configForm.fields,
    filters: configForm.filters,
    sorts: configForm.sorts
  }
  return JSON.stringify(config)
}

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await saveReportConfig({
          id: isEdit.value ? editingId.value : undefined,
          userId: mockUserId,
          reportName: configForm.reportName,
          reportType: configForm.reportType,
          configJson: buildConfigJson(),
          isDefault: configForm.isDefault
        })
        ElMessage.success('保存成功')
        dialogVisible.value = false
        await loadConfigList()
      } catch (error) {
        ElMessage.error('保存失败')
      }
    }
  })
}

const handleSaveAndPreview = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const res = await saveReportConfig({
          id: isEdit.value ? editingId.value : undefined,
          userId: mockUserId,
          reportName: configForm.reportName,
          reportType: configForm.reportType,
          configJson: buildConfigJson(),
          isDefault: configForm.isDefault
        })
        dialogVisible.value = false
        await loadConfigList()
        // 跳转到预览页面
        router.push({
          name: 'ReportPreview',
          query: {
            configId: String(res.data),
            reportType: configForm.reportType,
            reportName: configForm.reportName,
            configJson: buildConfigJson()
          }
        })
      } catch (error) {
        ElMessage.error('保存失败')
      }
    }
  })
}

const handlePreview = (row: ReportConfigVO) => {
  router.push({
    name: 'ReportPreview',
    query: {
      configId: String(row.id),
      reportType: row.reportType,
      reportName: row.reportName,
      configJson: row.configJson
    }
  })
}

onMounted(() => {
  loadConfigList()
})
</script>

<style scoped>
.report-config-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-form {
  margin-bottom: 16px;
}
</style>
