<template>
  <div class="report-preview-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <div>
            <el-button @click="goBack">返回</el-button>
            <span style="margin-left: 12px; font-size: 16px; font-weight: bold">{{ reportName }}</span>
            <el-tag style="margin-left: 12px">{{ getReportTypeName(reportType) }}</el-tag>
          </div>
          <div>
            <el-button type="warning" @click="handleExportExcel">导出 Excel</el-button>
            <el-button type="danger" @click="handleExportCsv">导出 CSV</el-button>
          </div>
        </div>
      </template>

      <!-- 动态报表表格 -->
      <el-table :data="tableData" border v-loading="loading" style="width: 100%">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column
          v-for="field in visibleFields"
          :key="field.field"
          :prop="field.field"
          :label="field.label"
          :width="field.width"
          :min-width="80"
          show-overflow-tooltip
        />
      </el-table>

      <div class="result-info">
        共 {{ total }} 条记录
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { previewReport, exportExcel, exportPdf, type ReportQueryDTO } from '@/api/customReport'

const route = useRoute()
const router = useRouter()

const reportName = ref((route.query.reportName as string) || '报表预览')
const reportType = ref((route.query.reportType as string) || '')
const configJson = ref((route.query.configJson as string) || '')

const visibleFields = ref<any[]>([])
const tableData = ref<Record<string, any>[]>([])
const total = ref(0)
const loading = ref(false)

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

const getReportTypeName = (type: string) => reportTypeNames[type] || type

const goBack = () => {
  router.push({ name: 'ReportConfig' })
}

const loadPreviewData = async () => {
  if (!configJson.value || !reportType.value) {
    ElMessage.warning('缺少报表配置信息')
    return
  }
  loading.value = true
  try {
    const queryDTO: ReportQueryDTO = {
      reportType: reportType.value,
      configJson: configJson.value,
      pageNum: 1,
      pageSize: 200
    }
    const res = await previewReport(queryDTO)
    if (res.data) {
      visibleFields.value = res.data.fields || []
      tableData.value = res.data.data || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    ElMessage.error('加载报表数据失败')
  } finally {
    loading.value = false
  }
}

const handleExportExcel = async () => {
  try {
    const queryDTO: ReportQueryDTO = {
      reportType: reportType.value,
      configJson: configJson.value
    }
    const blob = await exportExcel(queryDTO)
    downloadFile(blob, `${reportName.value}.csv`)
  } catch (error) {
    ElMessage.error('导出Excel失败')
  }
}

const handleExportCsv = async () => {
  try {
    const queryDTO: ReportQueryDTO = {
      reportType: reportType.value,
      configJson: configJson.value
    }
    const blob = await exportPdf(queryDTO)
    downloadFile(blob, `${reportName.value}.csv`)
  } catch (error) {
    ElMessage.error('导出CSV失败')
  }
}

const downloadFile = (blob: Blob, fileName: string) => {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.click()
  window.URL.revokeObjectURL(url)
}

onMounted(() => {
  loadPreviewData()
})
</script>

<style scoped>
.report-preview-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-info {
  margin-top: 16px;
  text-align: right;
  color: #909399;
  font-size: 14px;
}
</style>
