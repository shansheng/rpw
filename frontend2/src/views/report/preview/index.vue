<template>
  <div class="p-6">
    <a-card>
      <template #title>报表预览</template>
      <div class="mb-4">
        <a-space>
          <span>选择报表：</span>
          <a-select
            v-model:value="selectedConfigId"
            style="width:300px"
            placeholder="请选择报表配置"
            :options="configOptions"
            @change="onConfigChange"
            allow-clear
          />
        </a-space>
      </div>
      <a-table
        v-if="selectedConfigId"
        :columns="dynamicColumns"
        :data-source="dataList"
        :loading="loading"
        row-key="id"
        :pagination="pagination"
        @change="handleTableChange"
        bordered
        :scroll="{ x: 'max-content' }"
      >
        <template #empty>
          <a-empty description="暂无数据" />
        </template>
      </a-table>
      <div v-else style="text-align:center;padding:60px 0;color:#999">
        请先选择一个报表配置
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import { getConfigList, fetchPreview } from '@/api/report'
import type { ReportConfig } from '@/api/report'
import type { TablePaginationConfig } from 'ant-design-vue/es/table'

const loading = ref(false)
const configOptions = ref<{ label: string; value: number }[]>([])
const selectedConfigId = ref<number | undefined>(undefined)
const dataList = ref<any[]>([])
const rawData = ref<any[]>([])

const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const dynamicColumns = computed(() => {
  if (rawData.value.length === 0) return []
  const first = rawData.value[0]
  return Object.keys(first).map((key) => ({
    title: key,
    dataIndex: key,
    key,
    ellipsis: true,
  }))
})

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

async function onConfigChange(val: number | undefined) {
  if (!val) {
    dataList.value = []
    rawData.value = []
    return
  }
  loading.value = true
  try {
    const res = await fetchPreview(val)
    const data = res.data
    let records: any[] = []
    let total = 0
    if (Array.isArray(data)) {
      records = data
      total = data.length
    } else if (data && typeof data === 'object') {
      if ('records' in data && Array.isArray((data as any).records)) {
        records = (data as any).records
        total = (data as any).total ?? records.length
      } else {
        records = [data]
        total = 1
      }
    }
    rawData.value = records
    pagination.value.total = total
    pagination.value.current = 1
    updatePageData()
  } catch {
    message.error('获取预览数据失败')
    rawData.value = []
    dataList.value = []
  } finally {
    loading.value = false
  }
}

function updatePageData() {
  const { current, pageSize } = pagination.value
  const start = (current - 1) * pageSize
  const end = start + pageSize
  dataList.value = rawData.value.slice(start, end)
}

function handleTableChange(pag: TablePaginationConfig) {
  pagination.value.current = pag.current ?? 1
  pagination.value.pageSize = pag.pageSize ?? 10
  updatePageData()
}

onMounted(loadConfigs)
</script>
