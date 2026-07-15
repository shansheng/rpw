<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { getHistoryList } from '@/api/flowable'
import { useUserStore } from '@/stores/user'
import dayjs from 'dayjs'

/** 响应式 */
const isMobile = ref(false)
const checkMobile = () => { isMobile.value = window.innerWidth < 768 }
onMounted(() => { checkMobile(); window.addEventListener('resize', checkMobile) })
onUnmounted(() => { window.removeEventListener('resize', checkMobile) })

interface HistoryItem {
  taskId: string
  name: string
  assignee?: string
  startTime?: string
  endTime?: string
  processInstanceId?: string
  comment?: string
  outcome?: string
}

/** 表格数据 */
const dataSource = ref<HistoryItem[]>([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const userStore = useUserStore()

/** 审批结果映射 */
const outcomeMap: Record<string, { label: string; color: string }> = {
  'approved': { label: '通过', color: 'green' },
  'rejected': { label: '驳回', color: 'red' },
  'forwarded': { label: '转办', color: 'orange' },
}

/** PC 端列 */
const pcColumns = [
  { title: '任务名称', dataIndex: 'name', key: 'name', width: 140 },
  { title: '审批结果', dataIndex: 'outcome', key: 'outcome', width: 100 },
  { title: '处理人', dataIndex: 'assignee', key: 'assignee', width: 100 },
  { title: '审批意见', dataIndex: 'comment', key: 'comment', width: 200, ellipsis: true },
  { title: '流程实例ID', dataIndex: 'processInstanceId', key: 'processInstanceId', width: 220, ellipsis: true },
  { title: '开始时间', dataIndex: 'startTime', key: 'startTime', width: 150 },
  { title: '结束时间', dataIndex: 'endTime', key: 'endTime', width: 150 },
]

/** 移动端列 */
const mobileColumns = [
  { title: '任务名称', dataIndex: 'name', key: 'name', width: 120 },
  { title: '结果', dataIndex: 'outcome', key: 'outcome', width: 70 },
  { title: '时间', dataIndex: 'endTime', key: 'endTime', width: 120 },
]

const columns = computed(() => isMobile.value ? mobileColumns : pcColumns)

/** 统计 */
const stats = computed(() => {
  const approved = dataSource.value.filter(r => r.outcome === 'approved').length
  const rejected = dataSource.value.filter(r => r.outcome === 'rejected').length
  const other = dataSource.value.filter(r => r.outcome && r.outcome !== 'approved' && r.outcome !== 'rejected').length
  return { total: total.value, approved, rejected, other }
})

const loadData = async () => {
  loading.value = true
  try {
    const username = userStore.username || 'admin'
    const res = await getHistoryList({ assignee: username })
    if (res.code === 200) {
      dataSource.value = res.data || []
      total.value = dataSource.value.length
    }
  } catch (e) {
    dataSource.value = []
  } finally {
    loading.value = false
  }
}

/** 详情弹窗 */
const detailVisible = ref(false)
const detailRecord = ref<HistoryItem | null>(null)
function viewDetail(record: HistoryItem) {
  detailRecord.value = record
  detailVisible.value = true
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="history-page">
    <!-- 统计栏 -->
    <a-row :gutter="16" class="stats-row">
      <a-col :xs="8" :sm="6">
        <div class="stat-item stat-total">
          <div class="stat-value">{{ stats.total }}</div>
          <div class="stat-label">审批总数</div>
        </div>
      </a-col>
      <a-col :xs="8" :sm="6">
        <div class="stat-item stat-approved">
          <div class="stat-value">{{ stats.approved }}</div>
          <div class="stat-label">通过</div>
        </div>
      </a-col>
      <a-col :xs="8" :sm="6">
        <div class="stat-item stat-rejected">
          <div class="stat-value">{{ stats.rejected }}</div>
          <div class="stat-label">驳回</div>
        </div>
      </a-col>
    </a-row>

    <a-card :bordered="false" class="main-card">
      <template #title>
        <div class="card-title-area">
          <span class="text-lg font-medium">审批历史</span>
          <a-button type="link" size="small" @click="loadData">
            <ReloadOutlined /> 刷新
          </a-button>
        </div>
      </template>

      <a-table
        :dataSource="dataSource"
        :columns="columns"
        :loading="loading"
        :pagination="{ current: pageNum, pageSize, total, showSizeChanger: !isMobile, showTotal: (t: number) => `共 ${t} 条` }"
        :scroll="{ x: isMobile ? 310 : 1200 }"
        rowKey="taskId"
        size="middle"
        @change="(pag: any) => { pageNum = pag.current; pageSize = pag.pageSize }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a-button type="link" size="small" style="padding: 0" @click="viewDetail(record)">
              {{ record.name || '-' }}
            </a-button>
          </template>

          <template v-if="column.key === 'outcome'">
            <a-tag v-if="record.outcome" :color="(outcomeMap[record.outcome] || { color: 'default' }).color">
              {{ (outcomeMap[record.outcome] || { label: record.outcome }).label }}
            </a-tag>
            <span v-else style="color: rgba(0,0,0,0.25)">-</span>
          </template>

          <template v-if="column.key === 'assignee'">
            {{ record.assignee || '-' }}
          </template>

          <template v-if="column.key === 'comment'">
            <a-tooltip v-if="record.comment" :title="record.comment">
              <span class="comment-text">{{ record.comment }}</span>
            </a-tooltip>
            <span v-else style="color: rgba(0,0,0,0.25)">-</span>
          </template>

          <template v-if="column.key === 'startTime'">
            {{ record.startTime ? dayjs(record.startTime).format('YYYY-MM-DD HH:mm') : '-' }}
          </template>

          <template v-if="column.key === 'endTime'">
            {{ record.endTime ? dayjs(record.endTime).format('YYYY-MM-DD HH:mm') : '-' }}
          </template>
        </template>

        <template #emptyText>
          <a-empty description="暂无审批历史" />
        </template>
      </a-table>
    </a-card>

    <!-- 详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="审批详情"
      :footer="null"
      :width="isMobile ? '95vw' : 520"
    >
      <template v-if="detailRecord">
        <a-descriptions :column="isMobile ? 1 : 2" bordered size="small">
          <a-descriptions-item label="任务名称" :span="2">{{ detailRecord.name || '-' }}</a-descriptions-item>
          <a-descriptions-item label="审批结果">
            <a-tag v-if="detailRecord.outcome" :color="(outcomeMap[detailRecord.outcome] || { color: 'default' }).color">
              {{ (outcomeMap[detailRecord.outcome] || { label: detailRecord.outcome }).label }}
            </a-tag>
            <span v-else>-</span>
          </a-descriptions-item>
          <a-descriptions-item label="处理人">{{ detailRecord.assignee || '-' }}</a-descriptions-item>
          <a-descriptions-item label="审批意见" :span="2">{{ detailRecord.comment || '-' }}</a-descriptions-item>
          <a-descriptions-item label="流程实例ID" :span="2">
            <a-typography-paragraph :copyable="true" :ellipsis="false" style="margin: 0; font-size: 12px">
              {{ detailRecord.processInstanceId || '-' }}
            </a-typography-paragraph>
          </a-descriptions-item>
          <a-descriptions-item label="开始时间">{{ detailRecord.startTime ? dayjs(detailRecord.startTime).format('YYYY-MM-DD HH:mm:ss') : '-' }}</a-descriptions-item>
          <a-descriptions-item label="结束时间">{{ detailRecord.endTime ? dayjs(detailRecord.endTime).format('YYYY-MM-DD HH:mm:ss') : '-' }}</a-descriptions-item>
        </a-descriptions>
      </template>
    </a-modal>
  </div>
</template>

<script lang="ts">
import { ReloadOutlined } from '@ant-design/icons-vue'
export default {
  components: { ReloadOutlined }
}
</script>

<style scoped>
.history-page {
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

.stat-total .stat-value {
  color: #1890ff;
}

.stat-approved {
  border-left: 3px solid #52c41a;
}

.stat-approved .stat-value {
  color: #52c41a;
}

.stat-rejected {
  border-left: 3px solid #ff4d4f;
}

.stat-rejected .stat-value {
  color: #ff4d4f;
}

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

.main-card {
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.card-title-area {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.comment-text {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
}

@media (max-width: 767px) {
  .stat-item {
    padding: 12px 8px;
  }

  .stat-value {
    font-size: 22px;
  }

  .comment-text {
    max-width: 100px;
  }
}
</style>
