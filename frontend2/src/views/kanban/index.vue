<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { getBoardData, updateCardStatus } from '@/api/kanban'
import type { KanbanCard } from '@/api/kanban'
import { message } from 'ant-design-vue'

/** 响应式 */
const isMobile = ref(false)
const checkMobile = () => { isMobile.value = window.innerWidth < 768 }
onMounted(() => { checkMobile(); window.addEventListener('resize', checkMobile) })
onUnmounted(() => { window.removeEventListener('resize', checkMobile) })

/** 列定义 */
const columnDefs = [
  { key: 'DRAFT', title: '草稿', color: '#d9d9d9', icon: '📝' },
  { key: 'SUBMITTED', title: '已提交', color: '#91caff', icon: '📤' },
  { key: 'IN_PROGRESS', title: '进行中', color: '#95de64', icon: '🔄' },
  { key: 'COMPLETED', title: '已完成', color: '#69b1ff', icon: '✅' },
  { key: 'TERMINATED', title: '已终止', color: '#ff7875', icon: '❌' }
]

const columns = ref<{ key: string; title: string; color: string; icon: string; cards: KanbanCard[] }[]>(
  columnDefs.map((c) => ({ ...c, cards: [] }))
)

const loading = ref(false)

/** 筛选 */
const projectFilter = ref('')
const resourceTypeFilter = ref('')

/** 筛选后的列数据 */
const filteredColumns = computed(() => {
  return columns.value.map(col => ({
    ...col,
    cards: col.cards.filter(card => {
      const matchProject = !projectFilter.value ||
        (card.title && card.title.includes(projectFilter.value)) ||
        ((card as any).wbsCode && (card as any).wbsCode.includes(projectFilter.value))
      const matchType = !resourceTypeFilter.value ||
        (card.priority && card.priority.includes(resourceTypeFilter.value))
      return matchProject && matchType
    })
  }))
})

/** 统计 */
const stats = computed(() => {
  const allCards = columns.value.flatMap(c => c.cards)
  return {
    total: allCards.length,
    inProgress: columns.value.find(c => c.key === 'IN_PROGRESS')?.cards.length || 0,
    completed: columns.value.find(c => c.key === 'COMPLETED')?.cards.length || 0
  }
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getBoardData()
    const columnMap = new Map<string, KanbanCard[]>()
    for (const def of columnDefs) {
      columnMap.set(def.key, [])
    }
    for (const card of res.data) {
      const list = columnMap.get(card.status)
      if (list) {
        list.push(card)
      } else {
        columnMap.get('DRAFT')!.push(card)
      }
    }
    columns.value = columnDefs.map((c) => ({
      ...c,
      cards: columnMap.get(c.key) || []
    }))
  } finally {
    loading.value = false
  }
}

/** 拖拽 */
let dragCard: KanbanCard | null = null
let dragSourceColumnKey: string | null = null

const onDragStart = (card: KanbanCard, columnKey: string) => {
  dragCard = card
  dragSourceColumnKey = columnKey
}

const onDragOver = (e: DragEvent) => {
  e.preventDefault()
  const target = e.currentTarget as HTMLElement
  target.classList.add('drag-over')
}

const onDragLeave = (e: DragEvent) => {
  const target = e.currentTarget as HTMLElement
  target.classList.remove('drag-over')
}

const onDrop = async (targetColumnKey: string) => {
  // 移除拖拽样式
  document.querySelectorAll('.drag-over').forEach(el => el.classList.remove('drag-over'))

  if (!dragCard || dragCard.status === targetColumnKey) {
    dragCard = null
    return
  }

  const oldStatus = dragCard.status
  const oldColumn = columns.value.find(c => c.key === oldStatus)
  const newColumn = columns.value.find(c => c.key === targetColumnKey)

  if (oldColumn && newColumn) {
    // 乐观更新
    oldColumn.cards = oldColumn.cards.filter(c => c.id !== dragCard!.id)
    dragCard.status = targetColumnKey
    newColumn.cards.push(dragCard)
  }

  try {
    await updateCardStatus(dragCard.id, targetColumnKey, dragCard.sort)
    message.success(`已移至「${columnDefs.find(d => d.key === targetColumnKey)?.title}」`)
  } catch {
    // 回滚
    if (oldColumn && newColumn && dragCard) {
      newColumn.cards = newColumn.cards.filter(c => c.id !== dragCard!.id)
      dragCard.status = oldStatus
      oldColumn.cards.push(dragCard)
    }
    message.error('状态更新失败')
  }
  dragCard = null
}

/** 详情弹窗 */
const detailVisible = ref(false)
const detailCard = ref<KanbanCard | null>(null)
function viewDetail(card: KanbanCard) {
  detailCard.value = card
  detailVisible.value = true
}

/** 截止日期样式 */
function getDueDateClass(dueDate?: string) {
  if (!dueDate) return ''
  const due = new Date(dueDate)
  const now = new Date()
  const diffDays = Math.ceil((due.getTime() - now.getTime()) / (1000 * 60 * 60 * 24))
  if (diffDays < 0) return 'overdue'
  if (diffDays <= 3) return 'urgent'
  return ''
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="kanban-page">
    <!-- 统计栏 -->
    <a-row :gutter="16" class="stats-row">
      <a-col :xs="8" :sm="8">
        <div class="stat-item stat-total">
          <div class="stat-value">{{ stats.total }}</div>
          <div class="stat-label">总卡片</div>
        </div>
      </a-col>
      <a-col :xs="8" :sm="8">
        <div class="stat-item stat-progress">
          <div class="stat-value">{{ stats.inProgress }}</div>
          <div class="stat-label">进行中</div>
        </div>
      </a-col>
      <a-col :xs="8" :sm="8">
        <div class="stat-item stat-done">
          <div class="stat-value">{{ stats.completed }}</div>
          <div class="stat-label">已完成</div>
        </div>
      </a-col>
    </a-row>

    <a-card :bordered="false" class="kanban-card-main">
      <template #title>
        <div class="card-title-area">
          <span class="text-lg font-medium">看板</span>
          <a-button type="link" size="small" @click="loadData">
            <ReloadOutlined /> 刷新
          </a-button>
        </div>
      </template>

      <!-- 筛选 -->
      <div class="filter-bar" :class="{ 'filter-bar-mobile': isMobile }">
        <a-input
          v-model:value="projectFilter"
          placeholder="项目 / WBS 筛选"
          allow-clear
          class="filter-input"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-input
          v-model:value="resourceTypeFilter"
          placeholder="资源类型"
          allow-clear
          class="filter-input"
        >
          <template #prefix><FilterOutlined /></template>
        </a-input>
      </div>

      <!-- 看板列 -->
      <a-spin :spinning="loading">
        <div class="kanban-board" :class="{ 'kanban-board-mobile': isMobile }" v-if="!loading">
          <div
            v-for="col in filteredColumns"
            :key="col.key"
            class="kanban-column"
            @dragover="onDragOver"
            @dragleave="onDragLeave"
            @drop="onDrop(col.key)"
          >
            <!-- 列标题 -->
            <div class="column-header" :style="{ borderLeft: `4px solid ${col.color}` }">
              <div class="column-title-area">
                <span class="column-icon">{{ col.icon }}</span>
                <span class="column-title">{{ col.title }}</span>
              </div>
              <a-tag :color="col.color" round>{{ col.cards.length }}</a-tag>
            </div>

            <!-- 卡片列表 -->
            <div class="column-body">
              <div
                v-for="card in col.cards"
                :key="card.id"
                class="kanban-card"
                draggable="true"
                @dragstart="onDragStart(card, col.key)"
                @click="viewDetail(card)"
              >
                <div class="card-title">{{ card.title }}</div>
                <div class="card-info">
                  <span v-if="(card as any).wbsCode" class="info-item">
                    <CodeOutlined /> {{ (card as any).wbsCode }}
                  </span>
                  <span v-if="(card as any).plannedDate" class="info-item">
                    <CalendarOutlined /> {{ (card as any).plannedDate }}
                  </span>
                  <span v-if="card.dueDate" class="info-item" :class="getDueDateClass(card.dueDate)">
                    <ClockCircleOutlined /> {{ card.dueDate }}
                  </span>
                </div>
                <div class="card-meta" v-if="card.assignee || card.priority">
                  <a-tag v-if="card.priority" color="blue" size="small">{{ card.priority }}</a-tag>
                  <span v-if="card.assignee" class="assignee">
                    <UserOutlined /> {{ card.assignee }}
                  </span>
                </div>
              </div>

              <!-- 空状态 -->
              <div v-if="col.cards.length === 0" class="empty-column">
                <a-empty description="暂无卡片" :image="1" />
              </div>
            </div>
          </div>
        </div>
      </a-spin>
    </a-card>

    <!-- 详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="卡片详情"
      :footer="null"
      :width="isMobile ? '95vw' : 480"
    >
      <template v-if="detailCard">
        <a-descriptions :column="isMobile ? 1 : 2" bordered size="small">
          <a-descriptions-item label="标题" :span="2">{{ detailCard.title || '-' }}</a-descriptions-item>
          <a-descriptions-item label="状态">{{ detailCard.status || '-' }}</a-descriptions-item>
          <a-descriptions-item label="优先级">
            <a-tag v-if="detailCard.priority" color="blue">{{ detailCard.priority }}</a-tag>
            <span v-else>-</span>
          </a-descriptions-item>
          <a-descriptions-item label="负责人">{{ detailCard.assignee || '-' }}</a-descriptions-item>
          <a-descriptions-item label="截止日期" :class="getDueDateClass(detailCard.dueDate)">
            {{ detailCard.dueDate || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="WBS" v-if="(detailCard as any).wbsCode">{{ (detailCard as any).wbsCode }}</a-descriptions-item>
          <a-descriptions-item label="计划日期" v-if="(detailCard as any).plannedDate">{{ (detailCard as any).plannedDate }}</a-descriptions-item>
        </a-descriptions>
      </template>
    </a-modal>
  </div>
</template>

<script lang="ts">
import {
  ReloadOutlined, SearchOutlined, FilterOutlined,
  CodeOutlined, CalendarOutlined, ClockCircleOutlined, UserOutlined
} from '@ant-design/icons-vue'
export default {
  components: { ReloadOutlined, SearchOutlined, FilterOutlined, CodeOutlined, CalendarOutlined, ClockCircleOutlined, UserOutlined }
}
</script>

<style scoped>
.kanban-page {
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

.stat-total { border-left: 3px solid #1890ff; }
.stat-total .stat-value { color: #1890ff; }
.stat-progress { border-left: 3px solid #52c41a; }
.stat-progress .stat-value { color: #52c41a; }
.stat-done { border-left: 3px solid #69b1ff; }
.stat-done .stat-value { color: #69b1ff; }

.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
  margin-top: 4px;
}

.kanban-card-main {
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.card-title-area {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.filter-bar-mobile {
  flex-direction: column;
}

.filter-input {
  width: 220px;
}

.filter-bar-mobile .filter-input {
  width: 100%;
}

.kanban-board {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  padding: 8px 0;
  min-height: 500px;
}

.kanban-board-mobile {
  flex-direction: column;
  min-height: auto;
}

.kanban-column {
  min-width: 260px;
  max-width: 300px;
  flex: 1;
  background: #f5f5f5;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  transition: background 0.2s;
}

.kanban-board-mobile .kanban-column {
  min-width: 100%;
  max-width: 100%;
}

.kanban-column.drag-over {
  background: #e6f7ff;
  border-color: #91d5ff;
}

.column-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fff;
  border-radius: 8px 8px 0 0;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
  border-bottom: 1px solid #f0f0f0;
}

.column-title-area {
  display: flex;
  align-items: center;
  gap: 6px;
}

.column-icon {
  font-size: 16px;
}

.column-title {
  font-size: 14px;
}

.column-body {
  flex: 1;
  padding: 8px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 600px;
}

.kanban-board-mobile .column-body {
  max-height: none;
  flex-direction: row;
  flex-wrap: wrap;
}

.kanban-board-mobile .kanban-card {
  min-width: 280px;
  flex: 1;
}

.kanban-card {
  background: #fff;
  border-radius: 8px;
  padding: 12px;
  border: 1px solid #e8e8e8;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.15s;
}

.kanban-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-1px);
}

.kanban-card:active {
  cursor: grabbing;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
  transform: scale(0.98);
}

.card-title {
  font-weight: 500;
  font-size: 14px;
  margin-bottom: 8px;
  color: rgba(0, 0, 0, 0.85);
}

.card-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-bottom: 8px;
}

.info-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.info-item.overdue {
  color: #ff4d4f;
  font-weight: 500;
}

.info-item.urgent {
  color: #faad14;
  font-weight: 500;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.assignee {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.empty-column {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 0;
  opacity: 0.5;
}

@media (max-width: 767px) {
  .stat-item {
    padding: 12px 8px;
  }

  .stat-value {
    font-size: 22px;
  }
}
</style>
