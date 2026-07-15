<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { getMyInitiated } from '@/api/flowable'
import type { ProcessInstance } from '@/api/flowable'
import { useUserStore } from '@/stores/user'
import dayjs from 'dayjs'

/** 响应式 */
const isMobile = ref(false)
const checkMobile = () => { isMobile.value = window.innerWidth < 768 }
onMounted(() => { checkMobile(); window.addEventListener('resize', checkMobile) })
onUnmounted(() => { window.removeEventListener('resize', checkMobile) })

/** 表格数据 */
const dataSource = ref<ProcessInstance[]>([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const userStore = useUserStore()

/** 流程名称映射 */
const processNameMap: Record<string, string> = {
  'resource-plan': '资源计划审批',
  'resource-plan-approval': '资源计划审批',
  'warning-handle': '预警处理',
  'labor-plan': '劳动力计划审批',
  'purchase': '采购审批',
  'expense': '费用报销',
}

function getProcessName(key?: string) {
  if (!key) return '-'
  return processNameMap[key] || key
}

/** PC 端列 */
const pcColumns = [
  { title: '流程名称', dataIndex: 'processDefinitionKey', key: 'processDefinitionKey', width: 160 },
  { title: '业务编号', dataIndex: 'businessKey', key: 'businessKey', width: 180, ellipsis: true },
  { title: '流程实例ID', dataIndex: 'processInstanceId', key: 'processInstanceId', width: 220, ellipsis: true },
  { title: '开始时间', dataIndex: 'startTime', key: 'startTime', width: 160 },
  { title: '结束时间', dataIndex: 'endTime', key: 'endTime', width: 160 },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' as const }
]

/** 移动端列 */
const mobileColumns = [
  { title: '流程名称', dataIndex: 'processDefinitionKey', key: 'processDefinitionKey', width: 140 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 60, fixed: 'right' as const }
]

const columns = computed(() => isMobile.value ? mobileColumns : pcColumns)

/** 状态统计 */
const stats = computed(() => {
  const running = dataSource.value.filter(r => !r.finished && !r.suspended).length
  const suspended = dataSource.value.filter(r => r.suspended).length
  const finished = dataSource.value.filter(r => r.finished).length
  return { running, suspended, finished, total: total.value }
})

const loadData = async () => {
  loading.value = true
  try {
    const username = userStore.username || 'admin'
    const res = await getMyInitiated({ initiator: username })
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

/** 状态 */
const statusText = (record: ProcessInstance) => {
  if (record.finished) return '已结束'
  if (record.suspended) return '已挂起'
  return '运行中'
}
const statusColor = (record: ProcessInstance) => {
  if (record.finished) return 'default'
  if (record.suspended) return 'warning'
  return 'processing'
}

/** 详情弹窗 */
const detailVisible = ref(false)
const detailRecord = ref<ProcessInstance | null>(null)
function viewDetail(record: ProcessInstance) {
  detailRecord.value = record
  detailVisible.value = true
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="my-initiated-page">
    <!-- 统计栏 -->
    <a-row :gutter="16" class="stats-row">
      <a-col :xs="8" :sm="6">
        <div class="stat-item stat-running">
          <div class="stat-value">{{ stats.running }}</div>
          <div class="stat-label">运行中</div>
        </div>
      </a-col>
      <a-col :xs="8" :sm="6">
        <div class="stat-item stat-suspended">
          <div class="stat-value">{{ stats.suspended }}</div>
          <div class="stat-label">已挂起</div>
        </div>
      </a-col>
      <a-col :xs="8" :sm="6">
        <div class="stat-item stat-finished">
          <div class="stat-value">{{ stats.finished }}</div>
          <div class="stat-label">已结束</div>
        </div>
      </a-col>
    </a-row>

    <a-card :bordered="false" class="main-card">
      <template #title>
        <div class="card-title-area">
          <span class="text-lg font-medium">我发起的</span>
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
        :scroll="{ x: isMobile ? 280 : 1200 }"
        rowKey="processInstanceId"
        size="middle"
        @change="(pag: any) => { pageNum = pag.current; pageSize = pag.pageSize }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'processDefinitionKey'">
            <div>
              <div class="process-name">{{ getProcessName(record.processDefinitionKey) }}</div>
              <div class="process-key" v-if="!isMobile && record.processDefinitionKey !== getProcessName(record.processDefinitionKey)">
                {{ record.processDefinitionKey }}
              </div>
            </div>
          </template>

          <template v-if="column.key === 'startTime'">
            {{ record.startTime ? dayjs(record.startTime).format('YYYY-MM-DD HH:mm') : '-' }}
          </template>

          <template v-if="column.key === 'endTime'">
            {{ record.endTime ? dayjs(record.endTime).format('YYYY-MM-DD HH:mm') : '-' }}
          </template>

          <template v-if="column.key === 'status'">
            <a-badge :status="statusColor(record)" :text="statusText(record)" />
          </template>

          <template v-if="column.key === 'action'">
            <a-button type="link" size="small" @click="viewDetail(record)">详情</a-button>
          </template>
        </template>

        <template #emptyText>
          <a-empty description="暂无发起的流程" />
        </template>
      </a-table>
    </a-card>

    <!-- 详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="流程详情"
      :footer="null"
      :width="isMobile ? '95vw' : 520"
    >
      <template v-if="detailRecord">
        <a-descriptions :column="isMobile ? 1 : 2" bordered size="small">
          <a-descriptions-item label="流程名称" :span="2">{{ getProcessName(detailRecord.processDefinitionKey) }}</a-descriptions-item>
          <a-descriptions-item label="流程Key">{{ detailRecord.processDefinitionKey || '-' }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-badge :status="statusColor(detailRecord)" :text="statusText(detailRecord)" />
          </a-descriptions-item>
          <a-descriptions-item label="业务编号" :span="2">{{ detailRecord.businessKey || '-' }}</a-descriptions-item>
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
.my-initiated-page {
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

.stat-running {
  border-left: 3px solid #1890ff;
}

.stat-running .stat-value {
  color: #1890ff;
}

.stat-suspended {
  border-left: 3px solid #faad14;
}

.stat-suspended .stat-value {
  color: #faad14;
}

.stat-finished {
  border-left: 3px solid #d9d9d9;
}

.stat-finished .stat-value {
  color: rgba(0, 0, 0, 0.45);
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

.process-name {
  font-weight: 500;
}

.process-key {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-top: 2px;
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
