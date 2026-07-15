<template>
  <div class="export-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>导出中心</span>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="8" v-for="item in exportTypes" :key="item.key">
          <el-card class="export-card" shadow="hover">
            <div class="export-card-content">
              <div class="export-icon">
                <el-icon :size="48" :color="item.color">
                  <component :is="item.icon" />
                </el-icon>
              </div>
              <h3 class="export-title">{{ item.title }}</h3>
              <p class="export-desc">{{ item.description }}</p>
              <div class="export-actions">
                <el-button
                  v-if="item.formats.includes('excel')"
                  type="success"
                  size="default"
                  :icon="Download"
                  @click="handleExport(item.key, 'excel')"
                >
                  导出 Excel
                </el-button>
                <el-button
                  v-if="item.formats.includes('pdf')"
                  type="danger"
                  size="default"
                  :icon="Download"
                  @click="handleExport(item.key, 'pdf')"
                >
                  导出 PDF
                </el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { Download, Document, WarningFilled, Setting } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

interface ExportType {
  key: string
  title: string
  description: string
  icon: any
  color: string
  formats: string[]
}

const exportTypes: ExportType[] = [
  {
    key: 'resource-plan',
    title: '资源计划报表',
    description: '导出各类资源计划的详细报表，包括材料设备月度计划、采购计划等数据。',
    icon: Document,
    color: '#409eff',
    formats: ['excel']
  },
  {
    key: 'warning-report',
    title: '预警报表',
    description: '导出预警记录和分析报告，包含预警类型、级别、处理状态等完整信息。',
    icon: WarningFilled,
    color: '#e6a23c',
    formats: ['excel']
  },
  {
    key: 'custom-report',
    title: '自定义报表',
    description: '根据已配置的自定义报表规则，生成并导出 PDF 或 Excel 格式的报表。',
    icon: Setting,
    color: '#67c23a',
    formats: ['excel', 'pdf']
  }
]

async function handleExport(key: string, format: string) {
  ElMessage.success(`正在导出 ${format.toUpperCase()} 格式的${getExportTitle(key)}，请稍候...`)
  // 实际导出逻辑通过后端API下载文件
  try {
    const url = `/api/v1/report/export/${key}?format=${format}`
    // 创建隐藏的a标签触发下载
    const link = document.createElement('a')
    link.href = url
    link.download = `${key}.${format === 'excel' ? 'xlsx' : 'pdf'}`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  } catch (error) {
    ElMessage.error('导出失败，请重试')
  }
}

function getExportTitle(key: string) {
  const map: Record<string, string> = {
    'resource-plan': '资源计划报表',
    'warning-report': '预警报表',
    'custom-report': '自定义报表'
  }
  return map[key] || key
}
</script>

<style scoped>
.export-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.export-card {
  margin-bottom: 20px;
  transition: all 0.3s;
}

.export-card:hover {
  transform: translateY(-4px);
}

.export-card-content {
  text-align: center;
  padding: 20px 10px;
}

.export-icon {
  margin-bottom: 16px;
}

.export-title {
  margin: 0 0 12px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.export-desc {
  margin: 0 0 20px;
  font-size: 14px;
  color: #909399;
  line-height: 1.6;
  min-height: 42px;
}

.export-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}
</style>
