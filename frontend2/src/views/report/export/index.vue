<template>
  <div class="p-6">
    <a-card class="mb-6">
      <template #title>报表导出</template>
      <a-form layout="inline">
        <a-form-item label="选择报表">
          <a-select
            v-model:value="selectedConfigId"
            style="width:280px"
            placeholder="请选择报表配置"
            :options="configOptions"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="导出格式">
          <a-select v-model:value="exportFormat" style="width:120px">
            <a-select-option value="xlsx">Excel</a-select-option>
            <a-select-option value="pdf">PDF</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" :loading="exporting" @click="handleExport" :disabled="!selectedConfigId">
            {{ exporting ? '导出中...' : '导出' }}
          </a-button>
        </a-form-item>
      </a-form>
    </a-card>
    <a-card title="导出历史">
      <a-table
        :columns="historyColumns"
        :data-source="historyList"
        row-key="id"
        :pagination="false"
        bordered
        :locale="{ emptyText: '暂无导出记录' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a @click="downloadAgain(record)">重新下载</a>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { getConfigList, exportReport } from '@/api/report'
import type { ReportConfig } from '@/api/report'

const configOptions = ref<{ label: string; value: number }[]>([])
const selectedConfigId = ref<number | undefined>(undefined)
const exportFormat = ref<string>('xlsx')
const exporting = ref(false)

const historyColumns = [
  { title: '报表名称', dataIndex: 'name', key: 'name' },
  { title: '格式', dataIndex: 'format', key: 'format', width: 80 },
  { title: '导出时间', dataIndex: 'time', key: 'time', width: 180 },
  { title: '操作', key: 'action', width: 100 },
]

interface ExportRecord {
  id: number
  configId: number
  name: string
  format: string
  time: string
}

const historyList = ref<ExportRecord[]>([])
let historySeq = 0

async function loadConfigs() {
  try {
    const res = await getConfigList({ pageSize: 999 })
    configOptions.value = (res.data.records ?? []).map((c: ReportConfig) => ({
      label: c.name,
      value: c.id,
    }))
  } catch {
    message.error('加载报表配置失败')
  }
}

async function handleExport() {
  if (!selectedConfigId.value) return
  exporting.value = true
  try {
    const blob = await exportReport(selectedConfigId.value, exportFormat.value)
    const selectedConfig = configOptions.value.find((c) => c.value === selectedConfigId.value)
    const fileName = `${selectedConfig?.label ?? 'report'}.${exportFormat.value}`
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = fileName
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
    historySeq++
    historyList.value.unshift({
      id: historySeq,
      configId: selectedConfigId.value,
      name: selectedConfig?.label ?? '未知报表',
      format: exportFormat.value === 'xlsx' ? 'Excel' : 'PDF',
      time: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    })
  } catch {
    message.error('导出失败')
  } finally {
    exporting.value = false
  }
}

function downloadAgain(record: ExportRecord) {
  selectedConfigId.value = record.configId
  exportFormat.value = record.format === 'Excel' ? 'xlsx' : 'pdf'
  handleExport()
}

onMounted(loadConfigs)
</script>
