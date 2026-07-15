<template>
  <div class="dashboard-page">
    <!-- 欢迎栏 -->
    <div class="welcome-bar">
      <div class="welcome-left">
        <div class="welcome-title">{{ greetText }}，{{ userStore.displayName || '用户' }} 👋</div>
        <div class="welcome-sub">{{ today }} — 今日有 <b>{{ stats.todoCount }}</b> 条待办任务</div>
      </div>
      <div class="welcome-right" v-if="!isMobile">
        <a-button type="primary" @click="$router.push('/flowable/task')" :icon="h(FileProtectOutlined)">
          去处理待办
        </a-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <a-row :gutter="[16, 16]" class="stat-row">
      <a-col :xs="12" :sm="12" :lg="6" v-for="(card, i) in statCards" :key="card.title">
        <div class="stat-card" :style="{ animationDelay: `${i * 0.08}s` }" @click="card.onClick && card.onClick()">
          <div class="stat-icon" :style="{ background: card.bg }">
            <component :is="card.icon" />
          </div>
          <div class="stat-info">
            <div class="stat-value">
              <a-skeleton-element v-if="loading" type="input" active style="width:60px;height:28px" />
              <count-up v-else :end-val="Number(card.value)" :duration="1.5" />
            </div>
            <div class="stat-label">{{ card.title }}</div>
          </div>
          <div class="stat-trend" :style="{ color: card.trendColor }">
            <component :is="card.trendIcon" v-if="card.trendIcon" style="margin-right:2px" />
            {{ card.trend }}
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 下方内容区 -->
    <a-row :gutter="[16, 16]" style="margin-top: 16px;">
      <!-- 待办任务 -->
      <a-col :xs="24" :md="14">
        <a-card class="section-card" :body-style="{ padding: '0' }">
          <template #title>
            <span class="card-title"><CheckSquareOutlined style="margin-right:6px;color:#1890ff" />待办任务</span>
          </template>
          <template #extra>
            <a @click="$router.push('/flowable/task')" class="card-link">查看全部 →</a>
          </template>
          <a-table
            :columns="taskColumns"
            :dataSource="tasks"
            :pagination="false"
            size="small"
            :loading="taskLoading"
            rowKey="id"
            :scroll="{ x: 500 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <span class="task-name" :title="record.name">{{ record.name }}</span>
              </template>
              <template v-if="column.key === 'status'">
                <a-tag color="orange" size="small">待处理</a-tag>
              </template>
              <template v-if="column.key === 'createTime'">
                <span class="time-text">{{ formatTime(record.createTime) }}</span>
              </template>
            </template>
            <template #emptyText>
              <a-empty description="暂无待办任务" :image="Empty.PRESENTED_IMAGE_SIMPLE" style="padding:24px 0" />
            </template>
          </a-table>
        </a-card>
      </a-col>

      <!-- 预警概览 -->
      <a-col :xs="24" :md="10">
        <a-card class="section-card">
          <template #title>
            <span class="card-title"><AlertOutlined style="margin-right:6px;color:#ff4d4f" />预警概览</span>
          </template>
          <template #extra>
            <a @click="$router.push('/warning/board')" class="card-link">查看全部 →</a>
          </template>

          <div v-if="warningLoading" class="warning-loading">
            <a-skeleton :paragraph="{ rows: 4 }" active />
          </div>
          <template v-else>
            <div v-for="w in warnings" :key="w.id" class="warning-item">
              <div class="warning-dot" :class="w.level"></div>
              <div class="warning-content">
                <div class="warning-title">{{ w.ruleName || '预警消息' }}</div>
                <div class="warning-time">{{ formatTime(w.createTime) }}</div>
              </div>
              <a-tag
                :color="w.level === 'CRITICAL' ? 'red' : w.level === 'WARNING' ? 'orange' : 'blue'"
                style="flex-shrink:0"
              >
                {{ w.level === 'CRITICAL' ? '严重' : w.level === 'WARNING' ? '警告' : '提示' }}
              </a-tag>
            </div>
            <a-empty v-if="warnings.length === 0" description="暂无预警" :image="Empty.PRESENTED_IMAGE_SIMPLE" style="padding:16px 0" />
          </template>
        </a-card>
      </a-col>
    </a-row>

    <!-- 快捷入口 -->
    <a-card class="section-card quick-entry-card" style="margin-top:16px">
      <template #title>
        <span class="card-title"><AppstoreOutlined style="margin-right:6px;color:#52c41a" />快捷入口</span>
      </template>
      <div class="quick-entry-grid">
        <div
          v-for="entry in quickEntries"
          :key="entry.label"
          class="quick-entry-item"
          @click="$router.push(entry.path)"
        >
          <div class="quick-entry-icon" :style="{ background: entry.bg }">
            <component :is="entry.icon" />
          </div>
          <span class="quick-entry-label">{{ entry.label }}</span>
        </div>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, h } from 'vue'
import { Empty } from 'ant-design-vue'
import {
  FileProtectOutlined, AlertOutlined, AuditOutlined, SmileOutlined,
  CheckSquareOutlined, AppstoreOutlined, ScheduleOutlined, ProjectOutlined,
  ApartmentOutlined, RiseOutlined, FallOutlined
} from '@ant-design/icons-vue'
import { getTodoList, getProcessInstanceList } from '@/api/flowable'
import { getRecordList } from '@/api/warning'
import { useUserStore } from '@/stores/user'
import dayjs from 'dayjs'

const userStore = useUserStore()
const isMobile = ref(window.innerWidth < 768)

const tasks = ref<any[]>([])
const warnings = ref<any[]>([])
const processInstances = ref<any[]>([])
const taskLoading = ref(false)
const warningLoading = ref(false)
const loading = ref(true)

const today = computed(() => dayjs().format('YYYY年MM月DD日 dddd'))
const greetText = computed(() => {
  const h = dayjs().hour()
  if (h < 6) return '夜深了'
  if (h < 12) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const stats = reactive({
  todoCount: 0,
  warningCount: 0,
  runningProcessCount: 0,
  finishedCount: 0,
})

const statCards = computed(() => [
  {
    title: '待办任务',
    value: String(stats.todoCount),
    icon: FileProtectOutlined,
    bg: 'linear-gradient(135deg,#1890ff,#096dd9)',
    trend: stats.todoCount > 0 ? `需要处理` : '暂无待办',
    trendColor: stats.todoCount > 0 ? '#ff4d4f' : '#999',
    trendIcon: stats.todoCount > 0 ? RiseOutlined : null,
    onClick: () => {},
  },
  {
    title: '活跃预警',
    value: String(stats.warningCount),
    icon: AlertOutlined,
    bg: 'linear-gradient(135deg,#ff7a45,#d4380d)',
    trend: stats.warningCount > 0 ? `条预警消息` : '系统正常',
    trendColor: stats.warningCount > 0 ? '#ff4d4f' : '#52c41a',
    trendIcon: null,
    onClick: () => {},
  },
  {
    title: '运行中流程',
    value: String(stats.runningProcessCount),
    icon: AuditOutlined,
    bg: 'linear-gradient(135deg,#36cfc9,#08979c)',
    trend: `个流程进行中`,
    trendColor: '#1890ff',
    trendIcon: null,
    onClick: () => {},
  },
  {
    title: '已完成流程',
    value: String(stats.finishedCount),
    icon: SmileOutlined,
    bg: 'linear-gradient(135deg,#73d13d,#389e0d)',
    trend: `历史累计`,
    trendColor: '#52c41a',
    trendIcon: null,
    onClick: () => {},
  },
])

const taskColumns = [
  { title: '任务名称', dataIndex: 'name', key: 'name', ellipsis: true, minWidth: 140 },
  { title: '流程', dataIndex: 'processDefinitionKey', key: 'processDefinitionKey', ellipsis: true, width: 120 },
  { title: '时间', dataIndex: 'createTime', key: 'createTime', width: 100 },
  { title: '状态', key: 'status', width: 70 },
]

const quickEntries = [
  { label: '材料计划', path: '/resource-plan/material', icon: ScheduleOutlined, bg: 'linear-gradient(135deg,#e6f7ff,#bae7ff)' },
  { label: '设备计划', path: '/resource-plan/equipment', icon: ScheduleOutlined, bg: 'linear-gradient(135deg,#f6ffed,#d9f7be)' },
  { label: '预警看板', path: '/warning/board', icon: AlertOutlined, bg: 'linear-gradient(135deg,#fff7e6,#ffe7ba)' },
  { label: '待办任务', path: '/flowable/task', icon: CheckSquareOutlined, bg: 'linear-gradient(135deg,#e6f4ff,#bfdbfe)' },
  { label: '项目管理', path: '/project', icon: ProjectOutlined, bg: 'linear-gradient(135deg,#f9f0ff,#efdbff)' },
  { label: '组织架构', path: '/organization', icon: ApartmentOutlined, bg: 'linear-gradient(135deg,#fff0f6,#ffd6e7)' },
]

const formatTime = (t: string) => {
  if (!t) return '-'
  const d = dayjs(t)
  const now = dayjs()
  if (d.isSame(now, 'day')) return d.format('HH:mm')
  if (d.isSame(now.subtract(1, 'day'), 'day')) return '昨天 ' + d.format('HH:mm')
  return d.format('MM-DD HH:mm')
}

onMounted(async () => {
  loading.value = true

  // 加载待办任务
  taskLoading.value = true
  try {
    const tr = await getTodoList()
    const todoList = Array.isArray(tr.data) ? tr.data : []
    tasks.value = todoList.slice(0, 5)
    stats.todoCount = todoList.length
  } catch (e) {
    console.error('加载待办任务失败', e)
  }
  taskLoading.value = false

  // 加载预警记录
  warningLoading.value = true
  try {
    const wr = await getRecordList({ pageNum: 1, pageSize: 5 })
    const records = wr.data?.records || wr.data || []
    warnings.value = Array.isArray(records) ? records : []
    stats.warningCount = wr.data?.total ?? warnings.value.length
  } catch (e) {
    console.error('加载预警记录失败', e)
  }
  warningLoading.value = false

  // 加载流程实例列表
  try {
    const pr = await getProcessInstanceList()
    const instances = Array.isArray(pr.data) ? pr.data : []
    processInstances.value = instances
    stats.runningProcessCount = instances.filter(p => !p.finished).length
    stats.finishedCount = instances.filter(p => p.finished).length
  } catch (e) {
    console.error('加载流程实例失败', e)
  }

  loading.value = false
})
</script>

<style scoped>
.dashboard-page {
  max-width: 1400px;
  margin: 0 auto;
}

/* ===== 欢迎栏 ===== */
.welcome-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  border-radius: 12px;
  margin-bottom: 16px;
  color: #fff;
}

.welcome-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 4px;
}

.welcome-sub {
  font-size: 14px;
  opacity: 0.85;
}

/* ===== 统计卡片 ===== */
.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  animation: fadeInUp 0.4s ease both;
  transition: transform 0.2s, box-shadow 0.2s;
  cursor: pointer;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  font-size: 22px;
  color: #fff;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.2;
  color: rgba(0, 0, 0, 0.85);
}

.stat-label {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-top: 2px;
}

.stat-trend {
  font-size: 12px;
  white-space: nowrap;
  text-align: right;
}

/* ===== Section Cards ===== */
.section-card {
  border-radius: 12px !important;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06) !important;
  border: none !important;
}

:deep(.section-card .ant-card-head) {
  padding: 0 20px;
  min-height: 48px;
  border-bottom: 1px solid #f0f0f0;
}

:deep(.section-card .ant-card-body) {
  padding: 16px 20px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
}

.card-link {
  font-size: 13px;
  color: #1890ff;
}

/* ===== 待办任务表格 ===== */
.task-name {
  font-weight: 500;
  color: rgba(0, 0, 0, 0.85);
}

.time-text {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

/* ===== 预警列表 ===== */
.warning-loading {
  padding: 8px 0;
}

.warning-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
  transition: background 0.15s;
}

.warning-item:last-child {
  border-bottom: none;
}

.warning-item:hover {
  background: #fafafa;
  margin: 0 -8px;
  padding: 10px 8px;
  border-radius: 6px;
}

.warning-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.warning-dot.CRITICAL { background: #ff4d4f; box-shadow: 0 0 4px rgba(255,77,79,0.4); }
.warning-dot.WARNING { background: #faad14; box-shadow: 0 0 4px rgba(250,173,20,0.4); }
.warning-dot.INFO { background: #1890ff; box-shadow: 0 0 4px rgba(24,144,255,0.4); }

.warning-content {
  flex: 1;
  min-width: 0;
}

.warning-title {
  font-size: 13px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.85);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.warning-time {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.35);
  margin-top: 2px;
}

/* ===== 快捷入口 ===== */
.quick-entry-card :deep(.ant-card-body) {
  padding: 16px 20px;
}

.quick-entry-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

@media (max-width: 1200px) {
  .quick-entry-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 768px) {
  .quick-entry-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .stat-value {
    font-size: 20px;
  }
  .stat-icon {
    width: 40px;
    height: 40px;
    font-size: 18px;
  }
  .welcome-bar {
    padding: 16px;
  }
  .welcome-title {
    font-size: 16px;
  }
}

.quick-entry-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px;
  border-radius: 10px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  border: 1px solid #f0f0f0;
}

.quick-entry-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.quick-entry-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 10px;
  font-size: 20px;
  color: rgba(0, 0, 0, 0.65);
}

.quick-entry-label {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.65);
  text-align: center;
  white-space: nowrap;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
