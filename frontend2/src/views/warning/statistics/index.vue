<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import * as echarts from 'echarts'
import StatCard from '@/components/StatCard.vue'
import { getStatistics, type WarningStatistics } from '@/api/warning'
import request from '@/api/request'

/** 响应式 */
const isMobile = ref(false)
const checkMobile = () => { isMobile.value = window.innerWidth < 768 }
onMounted(() => { checkMobile(); window.addEventListener('resize', handleResize) })
onUnmounted(() => { window.removeEventListener('resize', handleResize) })

/** 统计数据 */
const stats = ref<WarningStatistics>({
  total: 0,
  pending: 0,
  handled: 0
})

const loading = ref(false)

/** 筛选 */
const filters = ref({
  startDate: undefined as string | undefined,
  endDate: undefined as string | undefined
})

/** ECharts 实例 */
let pieChart: echarts.ECharts | null = null
let lineChart: echarts.ECharts | null = null
const pieChartRef = ref<HTMLDivElement | null>(null)
const lineChartRef = ref<HTMLDivElement | null>(null)

/** 资源类型颜色映射 */
const typeColors: Record<string, string> = {
  material: '#1890ff',
  equipment: '#52c41a',
  labor: '#faad14',
  budget: '#ff4d4f',
  schedule: '#722ed1',
  other: '#13c2c2'
}

/** 处理率 */
const handleRate = computed(() => {
  if (stats.value.total === 0) return 0
  return Math.round((stats.value.handled / stats.value.total) * 100)
})

/** 获取数据 */
async function fetchData() {
  loading.value = true
  try {
    const params: any = {}
    if (filters.value.startDate) params.startDate = filters.value.startDate
    if (filters.value.endDate) params.endDate = filters.value.endDate
    const res = await getStatistics(params)
    stats.value = res.data
    await nextTick()
    renderCharts(res.data)
  } catch (err: any) {
    message.error(err.message || '获取统计数据失败')
  } finally {
    loading.value = false
  }
}

/** 渲染图表 */
function renderCharts(data: WarningStatistics) {
  // 饼图：按级别分布
  const byLevel = data.byLevel || {}
  const levelNames: Record<string, string> = { info: '提示', warning: '警告', critical: '严重' }
  const levelColors: Record<string, string> = { info: '#1890ff', warning: '#faad14', critical: '#ff4d4f' }
  const pieData = Object.entries(byLevel).map(([key, value]) => ({
    name: levelNames[key] || key,
    value,
    itemStyle: { color: levelColors[key] || '#ccc' }
  }))

  if (pieChartRef.value) {
    if (!pieChart) {
      pieChart = echarts.init(pieChartRef.value)
    }
    pieChart.setOption({
      title: { text: '按预警级别分布', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      series: [{
        type: 'pie',
        radius: ['35%', '60%'],
        center: ['50%', '55%'],
        data: pieData.length > 0 ? pieData : [{ name: '暂无数据', value: 1, itemStyle: { color: '#e8e8e8' } }],
        label: { formatter: '{b}\n{d}%' },
        emphasis: {
          label: { show: true, fontSize: 14, fontWeight: 'bold' }
        }
      }]
    })
  }

  // 折线图：按时间趋势
  const trend = data.trend || []
  if (lineChartRef.value) {
    if (!lineChart) {
      lineChart = echarts.init(lineChartRef.value)
    }
    lineChart.setOption({
      title: { text: '预警数量趋势', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: trend.map(t => t.date),
        axisLabel: { rotate: 30 }
      },
      yAxis: { type: 'value', minInterval: 1 },
      grid: { left: 50, right: 20, bottom: 50, top: 40 },
      series: [{
        type: 'line',
        data: trend.map(t => t.count),
        smooth: true,
        lineStyle: { color: '#1890ff', width: 2 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(24,144,255,0.3)' },
            { offset: 1, color: 'rgba(24,144,255,0.05)' }
          ])
        },
        itemStyle: { color: '#1890ff' }
      }]
    })
  }
}

/** 窗口 resize 时调整图表 */
function handleResize() {
  checkMobile()
  pieChart?.resize()
  lineChart?.resize()
}

/** 搜索 */
function handleSearch() {
  fetchData()
}

/** 重置 */
function handleReset() {
  filters.value = { startDate: undefined, endDate: undefined }
  fetchData()
}

/** 定时任务调试 */
const debugLoading = ref<Record<string, boolean>>({})
const debugLogs = ref<Record<string, Array<{ time: string; level: string; message: string }>>>({})
const debugExpanded = ref<Record<string, boolean>>({})

interface SchedulerLog {
  time: string
  level: string
  message: string
}

async function triggerScheduler(type: 'rule-check' | 'labor-scan') {
  debugLoading.value[type] = true
  debugLogs.value[type] = []
  try {
    const url = type === 'rule-check'
      ? '/v1/scheduler/trigger-rule-check'
      : '/v1/scheduler/trigger-labor-scan'
    const res = await request.post(url)
    const data = res.data as {
      success: boolean
      scheduler: string
      logs: SchedulerLog[]
    }
    debugLogs.value[type] = data.logs || []
    debugExpanded.value[type] = true
    if (data.success) {
      message.success('定时任务执行完成')
      fetchData()
    } else {
      message.error('定时任务执行失败，请查看日志')
    }
  } catch (err: any) {
    debugLogs.value[type] = [{ time: new Date().toISOString(), level: 'ERROR', message: '请求失败: ' + (err.message || '未知错误') }]
    debugExpanded.value[type] = true
    message.error('请求失败')
  } finally {
    debugLoading.value[type] = false
  }
}

function getLogColor(level: string): string {
  switch (level) {
    case 'ERROR': return '#ff4d4f'
    case 'WARN': return '#faad14'
    default: return '#52c41a'
  }
}

onMounted(() => {
  fetchData()
})

onUnmounted(() => {
  pieChart?.dispose()
  lineChart?.dispose()
})
</script>

<template>
  <div class="warning-statistics-page">
    <a-card :bordered="false" class="main-card">
      <!-- 筛选 -->
      <div class="filter-area">
        <a-form :model="filters" :layout="isMobile ? 'vertical' : 'inline'" class="filter-form">
          <a-form-item label="开始日期">
            <a-date-picker
              v-model:value="filters.startDate"
              placeholder="开始日期"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item label="结束日期">
            <a-date-picker
              v-model:value="filters.endDate"
              placeholder="结束日期"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" @click="handleSearch">查询</a-button>
              <a-button @click="handleReset">重置</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <!-- 统计卡片 -->
      <a-row :gutter="[16, 16]" class="stats-row">
        <a-col :xs="12" :sm="6">
          <StatCard title="总预警数" :value="stats.total" color="#1890ff" />
        </a-col>
        <a-col :xs="12" :sm="6">
          <StatCard title="已处理" :value="stats.handled" color="#52c41a" />
        </a-col>
        <a-col :xs="12" :sm="6">
          <StatCard title="未处理" :value="stats.pending" color="#ff4d4f" />
        </a-col>
        <a-col :xs="12" :sm="6">
          <div class="rate-card">
            <div class="rate-label">处理率</div>
            <a-progress
              type="circle"
              :percent="handleRate"
              :size="isMobile ? 60 : 80"
              :stroke-color="handleRate >= 80 ? '#52c41a' : handleRate >= 50 ? '#faad14' : '#ff4d4f'"
            />
          </div>
        </a-col>
      </a-row>

      <!-- 定时任务调试面板 -->
      <a-card title="定时任务调试" :bordered="false" style="margin-top: 24px" class="debug-card">
        <template #extra>
          <a-tag color="orange">仅用于调试</a-tag>
        </template>

        <a-row :gutter="[16, 16]">
          <a-col :xs="24" :sm="12">
            <a-card size="small" title="通用预警规则检查" class="debug-item-card">
              <p class="debug-desc">基于数据库中启用的预警规则，检查所有资源类型的预警条件。</p>
              <a-button
                type="primary"
                :loading="debugLoading['rule-check']"
                @click="triggerScheduler('rule-check')"
                block
              >
                {{ debugLoading['rule-check'] ? '执行中...' : '手动触发' }}
              </a-button>
              <a-collapse v-if="debugLogs['rule-check']?.length" :activeKey="debugExpanded['rule-check'] ? ['1'] : []" style="margin-top: 12px">
                <a-collapse-panel key="1" header="执行日志">
                  <div class="debug-log-container">
                    <div
                      v-for="(log, idx) in debugLogs['rule-check']"
                      :key="idx"
                      class="debug-log-line"
                    >
                      <span class="log-dot" :style="{ background: getLogColor(log.level) }"></span>
                      <span class="log-level">[{{ log.level }}]</span>
                      <span class="log-message">{{ log.message }}</span>
                    </div>
                  </div>
                </a-collapse-panel>
              </a-collapse>
            </a-card>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-card size="small" title="劳动力计划专项预警扫描" class="debug-item-card">
              <p class="debug-desc">检查所有活跃状态的劳动力计划，规则：计划开始日已过未登记实际开始 / 实际开始晚于计划开始。</p>
              <a-button
                type="primary"
                :loading="debugLoading['labor-scan']"
                @click="triggerScheduler('labor-scan')"
                block
              >
                {{ debugLoading['labor-scan'] ? '执行中...' : '手动触发' }}
              </a-button>
              <a-collapse v-if="debugLogs['labor-scan']?.length" :activeKey="debugExpanded['labor-scan'] ? ['1'] : []" style="margin-top: 12px">
                <a-collapse-panel key="1" header="执行日志">
                  <div class="debug-log-container">
                    <div
                      v-for="(log, idx) in debugLogs['labor-scan']"
                      :key="idx"
                      class="debug-log-line"
                    >
                      <span class="log-dot" :style="{ background: getLogColor(log.level) }"></span>
                      <span class="log-level">[{{ log.level }}]</span>
                      <span class="log-message">{{ log.message }}</span>
                    </div>
                  </div>
                </a-collapse-panel>
              </a-collapse>
            </a-card>
          </a-col>
        </a-row>
      </a-card>

      <!-- 图表区域 -->
      <a-row :gutter="[16, 16]" class="charts-row">
        <a-col :xs="24" :sm="12">
          <a-card :bordered="false" class="chart-card">
            <div ref="pieChartRef" style="height: 320px; width: 100%"></div>
          </a-card>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-card :bordered="false" class="chart-card">
            <div ref="lineChartRef" style="height: 320px; width: 100%"></div>
          </a-card>
        </a-col>
      </a-row>
    </a-card>
  </div>
</template>

<style scoped>
.warning-statistics-page {
  padding: 0;
}

.main-card {
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.filter-area {
  margin-bottom: 16px;
}

.filter-form {
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}

.stats-row {
  margin-bottom: 24px;
}

.rate-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100px;
}

.rate-label {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
  margin-bottom: 8px;
}

.charts-row {
  margin-top: 0;
}

.chart-card {
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.debug-card {
  border-radius: 8px;
  border-color: #faad14;
}

.debug-item-card {
  background: #fafafa;
}

.debug-desc {
  color: #666;
  font-size: 13px;
  margin-bottom: 12px;
}

.debug-log-container {
  max-height: 400px;
  overflow-y: auto;
  background: #1e1e1e;
  border-radius: 6px;
  padding: 12px;
  font-family: 'Menlo', 'Monaco', 'Courier New', monospace;
  font-size: 12px;
}

.debug-log-line {
  display: flex;
  align-items: flex-start;
  padding: 3px 0;
  line-height: 1.5;
}

.log-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 8px;
  margin-top: 5px;
  flex-shrink: 0;
}

.log-level {
  color: #aaa;
  margin-right: 8px;
  flex-shrink: 0;
  min-width: 52px;
}

.log-message {
  color: #d4d4d4;
  word-break: break-all;
}

.debug-log-container .debug-log-line:hover {
  background: rgba(255, 255, 255, 0.05);
}

@media (max-width: 767px) {
  .filter-form {
    padding: 12px;
  }

  .rate-card {
    min-height: 80px;
    padding: 12px;
  }
}
</style>
