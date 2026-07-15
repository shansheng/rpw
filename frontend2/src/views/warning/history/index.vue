<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import { getRecordList, type WarningRecord } from '@/api/warning'

/** 响应式 */
const isMobile = ref(false)
const checkMobile = () => { isMobile.value = window.innerWidth < 768 }
onMounted(() => { checkMobile(); window.addEventListener('resize', checkMobile) })
onUnmounted(() => { window.removeEventListener('resize', checkMobile) })

/** 筛选条件 */
const filters = ref({
  startDate: undefined as string | undefined,
  endDate: undefined as string | undefined,
  resourceType: undefined as string | undefined,
  warningLevel: undefined as string | undefined
})

/** 列表数据 */
const records = ref<WarningRecord[]>([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

/** 预警级别选项 */
const levelOptions = [
  { value: 'GENERAL', label: '提示', color: '#1890ff', tagColor: 'blue' },
  { value: 'IMPORTANT', label: '警告', color: '#faad14', tagColor: 'orange' },
  { value: 'URGENT', label: '严重', color: '#ff4d4f', tagColor: 'red' }
]

/** 资源类型选项 */
const resourceTypeOptions = [
  { value: 'material', label: '材料' },
  { value: 'equipment', label: '设备' },
  { value: 'labor', label: '劳动力' },
  { value: 'safety', label: '安全物资' },
  { value: 'office', label: '办公用品' }
]

/** 级别标签 */
function getLevelTag(warningLevel?: string) {
  const opt = levelOptions.find(l => l.value === warningLevel)
  if (!opt) return { color: 'default', tagColor: 'default', label: warningLevel || '-' }
  return { color: opt.color, tagColor: opt.tagColor, label: opt.label }
}

/** 统计 */
const stats = computed(() => {
  const byLevel: Record<string, number> = {}
  levelOptions.forEach(l => { byLevel[l.value] = 0 })
  records.value.forEach(r => {
    if (r.warningLevel && byLevel[r.warningLevel] !== undefined) {
      byLevel[r.warningLevel]++
    }
  })
  return { total: total.value, byLevel }
})

/** 获取已处理的预警列表 */
async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: 'RESOLVED',
      ...filters.value
    }
    Object.keys(params).forEach(k => {
      if (params[k] === undefined || params[k] === '') delete params[k]
    })
    const res = await getRecordList(params)
    records.value = res.data.records
    total.value = res.data.total
  } catch (err: any) {
    message.error(err.message || '获取历史记录失败')
  } finally {
    loading.value = false
  }
}

/** 搜索 */
function handleSearch() {
  pageNum.value = 1
  fetchData()
}

/** 重置 */
function handleReset() {
  filters.value = {
    startDate: undefined,
    endDate: undefined,
    resourceType: undefined,
    warningLevel: undefined
  }
  pageNum.value = 1
  fetchData()
}

/** 查看详情 */
const detailVisible = ref(false)
const detailRecord = ref<WarningRecord | null>(null)
function viewDetail(record: WarningRecord) {
  detailRecord.value = record
  detailVisible.value = true
}

/** 格式化时间 */
function formatTime(time?: string) {
  if (!time) return '-'
  const d = new Date(time)
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  const dateStr = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  const timeStr = `${pad(d.getHours())}:${pad(d.getMinutes())}`

  const today = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
  const yesterday = new Date(now.getTime() - 86400000)
  const yesterdayStr = `${yesterday.getFullYear()}-${pad(yesterday.getMonth() + 1)}-${pad(yesterday.getDate())}`

  if (dateStr === today) return `今天 ${timeStr}`
  if (dateStr === yesterdayStr) return `昨天 ${timeStr}`
  return `${dateStr} ${timeStr}`
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="warning-history-page">
    <!-- 统计栏 -->
    <a-row :gutter="16" class="stats-row">
      <a-col :xs="24" :sm="8">
        <div class="stat-item stat-total">
          <div class="stat-value">{{ stats.total }}</div>
          <div class="stat-label">历史预警总数</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="8" v-for="level in levelOptions" :key="level.value">
        <div class="stat-item" :style="{ borderLeft: `3px solid ${level.color}` }">
          <div class="stat-value" :style="{ color: level.color }">{{ stats.byLevel[level.value] || 0 }}</div>
          <div class="stat-label">{{ level.label }}</div>
        </div>
      </a-col>
    </a-row>

    <a-card :bordered="false" class="history-card-main">
      <!-- 筛选区 -->
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
          <a-form-item label="预警级别">
            <a-select
              v-model:value="filters.warningLevel"
              placeholder="选择级别"
              allow-clear
              style="width: 100%"
            >
              <a-select-option v-for="opt in levelOptions" :key="opt.value" :value="opt.value">
                <span :style="{ color: opt.color }">●</span> {{ opt.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="资源类型">
            <a-select
              v-model:value="filters.resourceType"
              placeholder="选择资源类型"
              allow-clear
              style="width: 100%"
            >
              <a-select-option v-for="opt in resourceTypeOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" @click="handleSearch">查询</a-button>
              <a-button @click="handleReset">重置</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <!-- 卡片列表 -->
      <a-spin :spinning="loading">
        <div class="card-list" v-if="records.length > 0">
          <div
            v-for="record in records"
            :key="record.id"
            class="history-card"
            @click="viewDetail(record)"
          >
            <div class="card-body">
              <div class="card-main">
                <div class="card-header">
                  <span class="rule-name">{{ record.ruleName || '未知规则' }}</span>
                  <a-tag :color="getLevelTag(record.warningLevel).tagColor">
                    {{ getLevelTag(record.warningLevel).label }}
                  </a-tag>
                </div>
                <div class="card-content">{{ record.warningMessage || '无预警信息' }}</div>
                <div class="card-meta">
                  <span class="meta-item">
                    <ClockCircleOutlined style="margin-right: 4px" />
                    {{ formatTime(record.triggeredTime) }}
                  </span>
                  <span class="meta-item" v-if="record.handleTime">
                    <CheckCircleOutlined style="margin-right: 4px" />
                    {{ formatTime(record.handleTime) }}
                  </span>
                </div>
                <div v-if="record.handleRemark" class="card-remark">
                  处理意见：{{ record.handleRemark }}
                </div>
              </div>
            </div>
          </div>
        </div>
        <a-empty v-else-if="!loading" description="暂无历史记录" />
      </a-spin>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="total > pageSize">
        <a-pagination
          v-model:current="pageNum"
          :page-size="pageSize"
          :total="total"
          :show-total="(t: number) => `共 ${t} 条`"
          :size="isMobile ? 'small' : 'default'"
          @change="fetchData"
        />
      </div>
    </a-card>

    <!-- 详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="预警详情"
      :footer="null"
      :width="isMobile ? '95vw' : 520"
    >
      <template v-if="detailRecord">
        <a-descriptions :column="isMobile ? 1 : 2" bordered size="small">
          <a-descriptions-item label="规则名称" :span="2">{{ detailRecord.ruleName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="预警级别">
            <a-tag :color="getLevelTag(detailRecord.warningLevel).tagColor">
              {{ getLevelTag(detailRecord.warningLevel).label }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="资源类型">{{ detailRecord.resourceType || '-' }}</a-descriptions-item>
          <a-descriptions-item label="触发时间" :span="2">{{ detailRecord.triggeredTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="处理时间" :span="2">{{ detailRecord.handleTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="处理人" :span="2">{{ detailRecord.handledBy || '-' }}</a-descriptions-item>
          <a-descriptions-item label="预警信息" :span="2">{{ detailRecord.warningMessage || '-' }}</a-descriptions-item>
          <a-descriptions-item label="处理意见" :span="2">{{ detailRecord.handleRemark || '-' }}</a-descriptions-item>
        </a-descriptions>
      </template>
    </a-modal>
  </div>
</template>

<script lang="ts">
import { ClockCircleOutlined, CheckCircleOutlined } from '@ant-design/icons-vue'
export default {
  components: { ClockCircleOutlined, CheckCircleOutlined }
}
</script>

<style scoped>
.warning-history-page {
  padding: 0;
}

.stats-row {
  margin-bottom: 16px;
}

.stat-item {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
  text-align: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-total {
  border-left: 3px solid #1890ff;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1890ff;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
  margin-top: 4px;
}

.history-card-main {
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

.card-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.history-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
}

.history-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.rule-name {
  font-weight: 600;
  font-size: 14px;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-content {
  color: rgba(0, 0, 0, 0.65);
  font-size: 13px;
  margin-bottom: 8px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  flex-wrap: wrap;
}

.meta-item {
  display: inline-flex;
  align-items: center;
}

.card-remark {
  margin-top: 8px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  background: #f6ffed;
  padding: 6px 10px;
  border-radius: 4px;
  border-left: 3px solid #52c41a;
}

.pagination-wrapper {
  margin-top: 24px;
  text-align: right;
}

@media (max-width: 767px) {
  .card-list {
    grid-template-columns: 1fr;
  }

  .filter-form {
    padding: 12px;
  }

  .stat-item {
    padding: 12px 8px;
  }

  .stat-value {
    font-size: 22px;
  }

  .card-meta {
    flex-direction: column;
    gap: 4px;
    align-items: flex-start;
  }
}
</style>
