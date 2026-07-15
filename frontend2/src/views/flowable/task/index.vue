<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message, Empty } from 'ant-design-vue'
import {
  ClockCircleOutlined, CheckCircleOutlined, UserOutlined,
  SwapOutlined, ReloadOutlined
} from '@ant-design/icons-vue'
import { getTodoList, claimTask, completeTask } from '@/api/flowable'
import type { FlowTask } from '@/api/flowable'
import dayjs from 'dayjs'
import { useUserStore } from '@/stores/user'

const dataSource = ref<FlowTask[]>([])
const loading = ref(false)
const approveVisible = ref(false)
const approveForm = ref({ taskId: '', comment: '' })
const approveLoading = ref(false)
const currentRecord = ref<FlowTask | null>(null)
const transferVisible = ref(false)
const transferForm = ref({ assignee: '', taskId: '' })
const transferLoading = ref(false)
const userStore = useUserStore()

const isMobile = ref(window.innerWidth < 768)

const columns = computed(() => isMobile.value ? [
  { title: '任务', dataIndex: 'name', key: 'name' },
  { title: '操作', key: 'action', width: 120, fixed: 'right' as const }
] : [
  { title: '任务名称', dataIndex: 'name', key: 'name', width: 160 },
  { title: '流程定义', dataIndex: 'processDefinitionKey', key: 'processDefinitionKey', width: 180, ellipsis: true },
  { title: '处理人', dataIndex: 'assignee', key: 'assignee', width: 110 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' as const }
])

// 待认领 / 待我处理 统计
const unclaimedCount = computed(() => dataSource.value.filter(t => !t.assignee).length)
const myTaskCount = computed(() => dataSource.value.filter(t => t.assignee === userStore.username).length)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getTodoList({ userId: userStore.username || 'admin' })
    dataSource.value = Array.isArray(res.data) ? res.data : []
  } catch {
    dataSource.value = []
  } finally {
    loading.value = false
  }
}

const handleClaim = async (record: FlowTask) => {
  try {
    await claimTask(record.taskId || record.id, userStore.username || 'admin')
    message.success('认领成功')
    loadData()
  } catch { /* handled */ }
}

const openApproveModal = (record: FlowTask) => {
  currentRecord.value = record
  approveForm.value = { taskId: record.taskId || record.id, comment: '' }
  approveVisible.value = true
}

const handleApprove = async () => {
  approveLoading.value = true
  try {
    await completeTask(approveForm.value.taskId, { comment: approveForm.value.comment, approved: true })
    message.success('审批通过 ✓')
    approveVisible.value = false
    loadData()
  } finally { approveLoading.value = false }
}

const handleReject = async () => {
  if (!approveForm.value.comment.trim()) {
    message.warning('驳回时必须填写审批意见')
    return
  }
  approveLoading.value = true
  try {
    await completeTask(approveForm.value.taskId, { comment: approveForm.value.comment, approved: false })
    message.success('已驳回')
    approveVisible.value = false
    loadData()
  } finally { approveLoading.value = false }
}

const openTransferModal = (record: FlowTask) => {
  transferForm.value = { assignee: '', taskId: record.taskId || record.id }
  transferVisible.value = true
}

const handleTransfer = async () => {
  if (!transferForm.value.assignee.trim()) { message.warning('请输入转派人'); return }
  transferLoading.value = true
  try {
    const { default: request } = await import('@/api/request')
    await request.post(`/v1/flowable/task/${transferForm.value.taskId}/assign`, null, {
      params: { userId: transferForm.value.assignee }
    })
    message.success('转派成功')
    transferVisible.value = false
    loadData()
  } finally { transferLoading.value = false }
}

const formatTime = (t: string) => t ? dayjs(t).format('MM-DD HH:mm') : '-'

onMounted(() => { loadData() })
</script>

<template>
  <div class="task-page">
    <!-- 统计栏 -->
    <div class="task-stats">
      <div class="stat-chip">
        <ClockCircleOutlined style="color:#faad14;margin-right:4px" />
        待认领 <b>{{ unclaimedCount }}</b>
      </div>
      <div class="stat-chip">
        <UserOutlined style="color:#1890ff;margin-right:4px" />
        我的待办 <b>{{ myTaskCount }}</b>
      </div>
      <div style="flex:1" />
      <a-button :icon="h(ReloadOutlined)" :loading="loading" @click="loadData">刷新</a-button>
    </div>

    <a-card class="task-card" :bordered="false">
      <template #title>
        <span><ClockCircleOutlined style="margin-right:6px;color:#faad14" />待办任务</span>
      </template>
      <template #extra>
        <span class="total-hint">共 {{ dataSource.length }} 条</span>
      </template>

      <a-table
        :dataSource="dataSource"
        :columns="columns"
        :loading="loading"
        :pagination="{ pageSize: 10, showTotal: (t: number) => `共 ${t} 条`, size: 'small' }"
        :scroll="{ x: isMobile ? 400 : 900 }"
        rowKey="taskId"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'name'">
            <div class="task-name-cell">
              <span class="task-name-text">{{ record.name }}</span>
              <div v-if="record.businessInfo" class="task-biz-info">
                {{ record.businessInfo.businessName || record.businessInfo.businessType || '' }}
              </div>
            </div>
          </template>

          <template v-if="column.dataIndex === 'assignee'">
            <a-tag v-if="record.assignee" :color="record.assignee === userStore.username ? 'blue' : 'default'">
              {{ record.assignee }}
            </a-tag>
            <a-tag v-else color="warning">待认领</a-tag>
          </template>

          <template v-if="column.dataIndex === 'createTime'">
            <span class="time-text">{{ formatTime(record.createTime) }}</span>
          </template>

          <template v-if="column.key === 'action'">
            <a-space :size="4" wrap>
              <a-button v-if="!record.assignee" type="link" size="small" @click="handleClaim(record)">
                认领
              </a-button>
              <a-button type="link" size="small" @click="openApproveModal(record)">
                审批
              </a-button>
              <a-button type="link" size="small" @click="openTransferModal(record)">
                转派
              </a-button>
            </a-space>
          </template>
        </template>

        <template #emptyText>
          <a-empty description="暂无待办任务，太棒了！" :image="Empty.PRESENTED_IMAGE_SIMPLE" style="padding:40px 0">
            <template #extra>
              <a-tag color="success"><CheckCircleOutlined /> 全部处理完毕</a-tag>
            </template>
          </a-empty>
        </template>
      </a-table>
    </a-card>

    <!-- 审批弹窗 -->
    <a-modal
      v-model:open="approveVisible"
      title="审批任务"
      :width="isMobile ? '95vw' : 560"
      :destroy-on-close="true"
      :footer="null"
    >
      <div class="approve-modal">
        <template v-if="currentRecord?.businessInfo">
          <a-descriptions
            :column="1"
            bordered
            size="small"
            style="margin-bottom:16px"
          >
            <a-descriptions-item
              v-for="(val, key) in currentRecord.businessInfo"
              :key="String(key)"
              :label="String(key)"
            >{{ val }}</a-descriptions-item>
          </a-descriptions>
        </template>

        <a-form layout="vertical">
          <a-form-item label="任务名称">
            <a-input :value="currentRecord?.name" disabled />
          </a-form-item>
          <a-form-item label="审批意见">
            <a-textarea
              v-model:value="approveForm.comment"
              :rows="4"
              placeholder="请输入审批意见（驳回时必填）"
              :maxlength="500"
              show-count
            />
          </a-form-item>
        </a-form>

        <div class="approve-footer">
          <a-button @click="approveVisible = false">取消</a-button>
          <a-button danger :loading="approveLoading" @click="handleReject">
            <template #icon><span style="font-size:14px">✕</span></template>
            驳回
          </a-button>
          <a-button type="primary" :loading="approveLoading" @click="handleApprove">
            <template #icon><CheckCircleOutlined /></template>
            通过
          </a-button>
        </div>
      </div>
    </a-modal>

    <!-- 转派弹窗 -->
    <a-modal
      v-model:open="transferVisible"
      title="转派任务"
      :confirm-loading="transferLoading"
      @ok="handleTransfer"
      :width="isMobile ? '95vw' : 400"
    >
      <a-form layout="vertical" style="margin-top:8px">
        <a-form-item label="转派给（用户名）">
          <a-input
            v-model:value="transferForm.assignee"
            placeholder="请输入处理人用户名"
            :prefix="h(UserOutlined)"
            allow-clear
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts">
import { h } from 'vue'
import { ReloadOutlined, UserOutlined } from '@ant-design/icons-vue'
export default { components: { ReloadOutlined, UserOutlined } }
</script>

<style scoped>
.task-page { max-width: 1200px; margin: 0 auto; }

.task-stats {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.stat-chip {
  padding: 6px 14px;
  background: #fff;
  border-radius: 20px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.stat-chip b { color: rgba(0, 0, 0, 0.85); margin-left: 2px; }

.task-card {
  border-radius: 12px !important;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06) !important;
}

.total-hint { font-size: 13px; color: rgba(0, 0, 0, 0.45); }

.task-name-cell { display: flex; flex-direction: column; gap: 2px; }
.task-name-text { font-weight: 500; color: rgba(0, 0, 0, 0.85); }
.task-biz-info { font-size: 12px; color: rgba(0, 0, 0, 0.45); }
.time-text { font-size: 12px; color: rgba(0, 0, 0, 0.45); }

.approve-modal { padding: 4px 0; }
.approve-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}
</style>
