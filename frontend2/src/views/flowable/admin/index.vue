<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { getProcessInstanceList, suspendProcessInstance, activateProcessInstance, deleteProcessInstance, getProcessInstanceDiagram } from '@/api/flowable'
import type { ProcessInstance } from '@/api/flowable'
import type { PageParams } from '@/types/api'
import dayjs from 'dayjs'
import { Modal, message } from 'ant-design-vue'
import {
  SearchOutlined,
  ReloadOutlined,
  EyeOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  StopOutlined,
  ThunderboltOutlined,
  PauseCircleFilled,
  CheckCircleFilled,
  ExclamationCircleFilled
} from '@ant-design/icons-vue'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'

/** 响应式 */
const isMobile = ref(false)
const checkMobile = () => { isMobile.value = window.innerWidth < 768 }

/** 表格数据 */
const dataSource = ref<ProcessInstance[]>([])
const total = ref(0)
const loading = ref(false)
const queryParams = ref<PageParams & { status?: string; processDefinitionKey?: string }>({
  pageNum: 1, pageSize: 10
})

/** 搜索 */
const searchKey = ref('')
const searchStatus = ref<string | undefined>(undefined)

/** 终止确认 */
const terminateVisible = ref(false)
const terminateForm = ref({ reason: '', instanceId: '' })
const terminateLoading = ref(false)

/** 流程图 */
const diagramVisible = ref(false)
const diagramLoading = ref(false)
const diagramBpmnXml = ref('')
const diagramCompletedIds = ref<string[]>([])
const diagramActiveIds = ref<string[]>([])
const diagramFlowIds = ref<string[]>([])

/** 流程Key翻译 */
const keyLabelMap: Record<string, string> = {
  'resource-plan': '资源计划审批',
  'leave-approval': '请假审批',
  'expense-approval': '费用审批',
  'purchase-approval': '采购审批',
  'contract-approval': '合同审批'
}

const translateKey = (key?: string) => {
  if (!key) return '-'
  return keyLabelMap[key] || key
}

/** 统计数据 */
const stats = computed(() => {
  const running = dataSource.value.filter(d => d.status === 'RUNNING').length
  const suspended = dataSource.value.filter(d => d.status === 'SUSPENDED').length
  const completed = dataSource.value.filter(d => d.status === 'COMPLETED').length
  return { running, suspended, completed, total: dataSource.value.length }
})

/** 表格列 */
const pcColumns = [
  { title: '流程实例ID', dataIndex: 'id', key: 'id', width: 200, ellipsis: true },
  { title: '流程类型', dataIndex: 'processDefinitionKey', key: 'processDefinitionKey', width: 150 },
  { title: '业务Key', dataIndex: 'businessKey', key: 'businessKey', width: 130, ellipsis: true },
  { title: '开始时间', dataIndex: 'startTime', key: 'startTime', width: 170 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 220, fixed: 'right' as const }
]

const mobileColumns = [
  { title: '流程信息', key: 'info', width: 'auto' },
  { title: '操作', key: 'action', width: 120, fixed: 'right' as const }
]

const columns = computed(() => isMobile.value ? mobileColumns : pcColumns)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getProcessInstanceList(queryParams.value)
    const list = Array.isArray(res.data) ? res.data : []
    dataSource.value = list
    total.value = list.length
  } catch (e) {
    console.error('加载流程实例失败', e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.value.processDefinitionKey = searchKey.value || undefined
  queryParams.value.status = searchStatus.value || undefined
  queryParams.value.pageNum = 1
  loadData()
}

const handleReset = () => {
  searchKey.value = ''
  searchStatus.value = undefined
  queryParams.value.processDefinitionKey = undefined
  queryParams.value.status = undefined
  queryParams.value.pageNum = 1
  loadData()
}

const onPageChange = (page: number, pageSize: number) => {
  queryParams.value.pageNum = page
  queryParams.value.pageSize = pageSize
  loadData()
}

/** 挂起 */
const handleSuspend = (instanceId: string) => {
  Modal.confirm({
    title: '确认挂起',
    content: '确定挂起此流程实例吗？挂起后流程将暂停执行。',
    okType: 'danger',
    onOk: async () => {
      try {
        await suspendProcessInstance(instanceId)
        message.success('挂起成功')
        loadData()
      } catch {
        message.error('挂起失败')
      }
    }
  })
}

/** 激活 */
const handleActivate = (instanceId: string) => {
  Modal.confirm({
    title: '确认激活',
    content: '确定激活此流程实例吗？',
    onOk: async () => {
      try {
        await activateProcessInstance(instanceId)
        message.success('激活成功')
        loadData()
      } catch {
        message.error('激活失败')
      }
    }
  })
}

/** 打开终止弹窗 */
const openTerminateModal = (instanceId: string) => {
  terminateForm.value = { reason: '', instanceId }
  terminateVisible.value = true
}

/** 提交终止 */
const handleTerminate = async () => {
  terminateLoading.value = true
  try {
    await deleteProcessInstance(terminateForm.value.instanceId, terminateForm.value.reason)
    terminateVisible.value = false
    message.success('终止成功')
    loadData()
  } catch {
    message.error('终止失败')
  } finally {
    terminateLoading.value = false
  }
}

const statusText = (status?: string) => {
  if (!status) return '-'
  switch (status) {
    case 'SUSPENDED': return '已挂起'
    case 'RUNNING': return '运行中'
    case 'COMPLETED': return '已完成'
    default: return status
  }
}

const statusColor = (status?: string) => {
  if (!status) return 'default'
  switch (status) {
    case 'SUSPENDED': return 'warning'
    case 'RUNNING': return 'processing'
    case 'COMPLETED': return 'success'
    default: return 'default'
  }
}

const statusTagColor = (status?: string) => {
  if (!status) return 'default'
  switch (status) {
    case 'SUSPENDED': return 'orange'
    case 'RUNNING': return 'blue'
    case 'COMPLETED': return 'green'
    default: return 'default'
  }
}

const isSuspended = (status?: string) => status === 'SUSPENDED'
const isFinished = (status?: string) => status === 'COMPLETED'

/** 时间格式化 */
const formatTime = (time?: string) => {
  if (!time) return '-'
  const d = dayjs(time)
  const now = dayjs()
  if (d.isSame(now, 'day')) return `今天 ${d.format('HH:mm')}`
  if (d.isSame(now.subtract(1, 'day'), 'day')) return `昨天 ${d.format('HH:mm')}`
  return d.format('YYYY-MM-DD HH:mm')
}

/** 打开流程图 */
async function openDiagram(instanceId: string) {
  diagramLoading.value = true
  diagramBpmnXml.value = ''
  diagramCompletedIds.value = []
  diagramActiveIds.value = []
  diagramFlowIds.value = []
  try {
    const res = await getProcessInstanceDiagram(instanceId)
    if (res.data) {
      diagramBpmnXml.value = res.data.bpmnXml || ''
      diagramCompletedIds.value = res.data.completedActivityIds || []
      diagramActiveIds.value = res.data.activeActivityIds || []
      diagramFlowIds.value = res.data.completedFlowIds || []
      diagramVisible.value = true
      await nextTick()
      await new Promise(resolve => setTimeout(resolve, 100))
      renderDiagram()
    }
  } catch (e) {
    console.error('获取流程图失败', e)
    message.error('获取流程图失败')
  } finally {
    diagramLoading.value = false
  }
}

let bpmnViewer: any = null

async function renderDiagram() {
  if (!diagramBpmnXml.value) return
  const container = document.getElementById('bpmn-diagram-canvas')
  if (!container) {
    console.error('bpmn-diagram-canvas 容器未找到，延迟重试')
    setTimeout(renderDiagram, 200)
    return
  }
  try {
    const { default: BpmnViewer } = await import('bpmn-js/lib/NavigatedViewer')
    if (bpmnViewer) { bpmnViewer.destroy() }
    bpmnViewer = new BpmnViewer({ container })
    await bpmnViewer.importXML(diagramBpmnXml.value)
    
    const canvas = bpmnViewer.get('canvas')
    diagramCompletedIds.value.forEach((id: string) => {
      try { canvas.addMarker(id, 'highlight-completed') } catch(e) {}
    })
    diagramActiveIds.value.forEach((id: string) => {
      try { canvas.addMarker(id, 'highlight-active') } catch(e) {}
    })
    diagramFlowIds.value.forEach((id: string) => {
      try { canvas.addMarker(id, 'highlight-flow') } catch(e) {}
    })
    canvas.zoom('fit-viewport')
  } catch (err) {
    console.error('渲染流程图失败', err)
  }
}

function onDiagramClose() {
  if (bpmnViewer) { bpmnViewer.destroy(); bpmnViewer = null }
  diagramVisible.value = false
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  loadData()
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
  if (bpmnViewer) { bpmnViewer.destroy(); bpmnViewer = null }
})
</script>

<template>
  <div class="admin-page">
    <!-- 统计栏 -->
    <div class="stats-row">
      <div class="stat-card" style="--accent: #1890ff;">
        <div class="stat-icon"><ThunderboltOutlined /></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.running }}</div>
          <div class="stat-label">运行中</div>
        </div>
      </div>
      <div class="stat-card" style="--accent: #fa8c16;">
        <div class="stat-icon"><PauseCircleFilled /></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.suspended }}</div>
          <div class="stat-label">已挂起</div>
        </div>
      </div>
      <div class="stat-card" style="--accent: #52c41a;">
        <div class="stat-icon"><CheckCircleFilled /></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.completed }}</div>
          <div class="stat-label">已完成</div>
        </div>
      </div>
      <div class="stat-card" style="--accent: #722ed1;">
        <div class="stat-icon"><ExclamationCircleFilled /></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.total }}</div>
          <div class="stat-label">总实例</div>
        </div>
      </div>
    </div>

    <!-- 搜索表单 -->
    <a-card class="search-card">
      <div class="search-form" :class="{ 'search-form--mobile': isMobile }">
        <a-input
          v-model:value="searchKey"
          placeholder="流程定义Key"
          allow-clear
          style="width: 200px;"
          @pressEnter="handleSearch"
        >
          <template #prefix><SearchOutlined style="color: #bfbfbf;" /></template>
        </a-input>
        <a-select
          v-model:value="searchStatus"
          placeholder="状态筛选"
          allow-clear
          style="width: 140px;"
        >
          <a-select-option value="RUNNING">运行中</a-select-option>
          <a-select-option value="SUSPENDED">已挂起</a-select-option>
          <a-select-option value="COMPLETED">已完成</a-select-option>
        </a-select>
        <a-button type="primary" @click="handleSearch">
          <template #icon><SearchOutlined /></template>
          查询
        </a-button>
        <a-button @click="handleReset">
          <template #icon><ReloadOutlined /></template>
          重置
        </a-button>
      </div>
    </a-card>

    <!-- 表格 -->
    <a-card class="table-card">
      <template #title>
        <span class="card-title">流程管理</span>
        <a-tag color="blue" style="margin-left: 8px;">共 {{ total }} 条</a-tag>
      </template>
      <template #extra>
        <a-button type="link" size="small" @click="loadData">
          <ReloadOutlined :spin="loading" /> 刷新
        </a-button>
      </template>

      <a-table
        :dataSource="dataSource"
        :columns="columns"
        :loading="loading"
        :pagination="{
          current: queryParams.pageNum,
          pageSize: queryParams.pageSize,
          total,
          showSizeChanger: true,
          showTotal: (t: number) => `共 ${t} 条`
        }"
        :scroll="isMobile ? { x: 400 } : undefined"
        rowKey="id"
        @change="(pag: any) => onPageChange(pag.current, pag.pageSize)"
        bordered
        size="middle"
      >
        <template #bodyCell="{ column, record }: { column: any; record: ProcessInstance }">
          <!-- PC 端列 -->
          <template v-if="!isMobile">
            <template v-if="column.dataIndex === 'processDefinitionKey'">
              <a-tag color="geekblue">{{ translateKey(record.processDefinitionKey) }}</a-tag>
            </template>
            <template v-if="column.dataIndex === 'startTime'">
              {{ formatTime(record.startTime) }}
            </template>
            <template v-if="column.dataIndex === 'status'">
              <a-badge :status="statusColor(record.status)" :text="statusText(record.status)" />
            </template>
            <template v-if="column.key === 'action'">
              <a-space :size="4">
                <a-button type="link" size="small" @click="openDiagram(record.id)">
                  <EyeOutlined /> 流程图
                </a-button>
                <a-button
                  v-if="!isSuspended(record.status) && !isFinished(record.status)"
                  type="link" size="small"
                  @click="handleSuspend(record.id)"
                >
                  <PauseCircleOutlined /> 挂起
                </a-button>
                <a-button
                  v-if="isSuspended(record.status)"
                  type="link" size="small"
                  @click="handleActivate(record.id)"
                >
                  <PlayCircleOutlined /> 激活
                </a-button>
                <a-button
                  v-if="!isFinished(record.status)"
                  type="link" size="small" danger
                  @click="openTerminateModal(record.id)"
                >
                  <StopOutlined /> 终止
                </a-button>
              </a-space>
            </template>
          </template>

          <!-- 移动端合并列 -->
          <template v-if="isMobile && column.key === 'info'">
            <div class="mobile-cell">
              <div class="mobile-cell__title">{{ translateKey(record.processDefinitionKey) }}</div>
              <div class="mobile-cell__meta">
                <a-tag :color="statusTagColor(record.status)" size="small">{{ statusText(record.status) }}</a-tag>
                <span class="mobile-cell__time">{{ formatTime(record.startTime) }}</span>
              </div>
              <div class="mobile-cell__id">ID: {{ record.id }}</div>
            </div>
          </template>
          <template v-if="isMobile && column.key === 'action'">
            <a-dropdown>
              <a-button type="link" size="small">操作</a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="openDiagram(record.id)">
                    <EyeOutlined /> 查看流程图
                  </a-menu-item>
                  <a-menu-item
                    v-if="!isSuspended(record.status) && !isFinished(record.status)"
                    @click="handleSuspend(record.id)"
                  >
                    <PauseCircleOutlined /> 挂起
                  </a-menu-item>
                  <a-menu-item
                    v-if="isSuspended(record.status)"
                    @click="handleActivate(record.id)"
                  >
                    <PlayCircleOutlined /> 激活
                  </a-menu-item>
                  <a-menu-item
                    v-if="!isFinished(record.status)"
                    danger
                    @click="openTerminateModal(record.id)"
                  >
                    <StopOutlined /> 终止
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 流程图弹窗 -->
    <a-modal
      v-model:open="diagramVisible"
      title="流程图"
      :width="isMobile ? '95vw' : 900"
      :footer="null"
      @cancel="onDiagramClose"
      destroyOnClose
    >
      <a-spin :spinning="diagramLoading">
        <div class="diagram-container">
          <div class="diagram-legend">
            <span class="legend-item"><span class="legend-dot completed"></span>已完成节点</span>
            <span class="legend-item"><span class="legend-dot active"></span>当前节点</span>
            <span class="legend-item"><span class="legend-line completed-flow"></span>已流转路径</span>
          </div>
          <div id="bpmn-diagram-canvas" class="diagram-canvas"></div>
        </div>
      </a-spin>
    </a-modal>

    <!-- 终止弹窗 -->
    <a-modal
      v-model:open="terminateVisible"
      title="终止流程实例"
      :confirmLoading="terminateLoading"
      :width="isMobile ? '95vw' : 480"
      @ok="handleTerminate"
    >
      <a-form layout="vertical">
        <a-form-item label="终止原因">
          <a-textarea
            v-model:value="terminateForm.reason"
            :rows="4"
            placeholder="请输入终止原因（可选）"
            :show-count="true"
            :maxlength="200"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.admin-page {
  padding: 16px;
}
@media (min-width: 768px) {
  .admin-page { padding: 24px; }
}

/* 统计栏 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}
@media (max-width: 767px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
}
.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.06);
  transition: transform 0.2s, box-shadow 0.2s;
  cursor: default;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: var(--accent, #1890ff);
  background: color-mix(in srgb, var(--accent, #1890ff) 10%, transparent);
}
.stat-value {
  font-size: 22px;
  font-weight: 600;
  line-height: 1.2;
  color: #1f1f1f;
}
.stat-label {
  font-size: 13px;
  color: #8c8c8c;
  margin-top: 2px;
}

/* 搜索 */
.search-card {
  margin-bottom: 16px;
}
.search-card :deep(.ant-card-body) {
  padding: 16px;
}
.search-form {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.search-form--mobile {
  flex-direction: column;
  align-items: stretch;
}
.search-form--mobile .ant-input,
.search-form--mobile .ant-select {
  width: 100% !important;
}

/* 表格 */
.table-card :deep(.ant-card-body) {
  padding: 0;
}
.card-title {
  font-size: 16px;
  font-weight: 500;
}

/* 移动端单元格 */
.mobile-cell {
  padding: 4px 0;
}
.mobile-cell__title {
  font-weight: 500;
  font-size: 14px;
  color: #1f1f1f;
  margin-bottom: 4px;
}
.mobile-cell__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.mobile-cell__time {
  font-size: 12px;
  color: #8c8c8c;
}
.mobile-cell__id {
  font-size: 11px;
  color: #bfbfbf;
  font-family: monospace;
}

/* 流程图弹窗 */
.diagram-container {
  position: relative;
}
.diagram-legend {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #666;
  flex-wrap: wrap;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.legend-dot {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid;
}
.legend-dot.completed {
  background: #d4edda;
  border-color: #28a745;
}
.legend-dot.active {
  background: #fff3cd;
  border-color: #ffc107;
}
.legend-line.completed-flow {
  display: inline-block;
  width: 24px;
  height: 3px;
  background: #28a745;
  border-radius: 2px;
}
.diagram-canvas {
  width: 100%;
  height: 500px;
  background: #f8f9fa;
  border-radius: 6px;
  border: 1px solid #e8e8e8;
}
@media (max-width: 767px) {
  .diagram-canvas { height: 350px; }
}

/* bpmn-js highlight markers */
:deep(.highlight-completed .djs-visual > :nth-child(1)) {
  fill: #d4edda !important;
  stroke: #28a745 !important;
}
:deep(.highlight-completed > .djs-visual > rect) {
  fill: #d4edda !important;
  stroke: #28a745 !important;
}
:deep(.highlight-completed > .djs-visual > circle) {
  fill: #d4edda !important;
  stroke: #28a745 !important;
}
:deep(.highlight-completed > .djs-visual > polygon) {
  fill: #d4edda !important;
  stroke: #28a745 !important;
}
:deep(.highlight-active .djs-visual > rect) {
  fill: #fff3cd !important;
  stroke: #ffc107 !important;
  stroke-width: 2.5px !important;
}
:deep(.highlight-active .djs-visual > circle) {
  fill: #fff3cd !important;
  stroke: #ffc107 !important;
  stroke-width: 2.5px !important;
}
:deep(.highlight-active .djs-visual > polygon) {
  fill: #fff3cd !important;
  stroke: #ffc107 !important;
  stroke-width: 2.5px !important;
}
:deep(.highlight-flow .djs-visual > path) {
  stroke: #28a745 !important;
  stroke-width: 2.5px !important;
  marker-end: none !important;
}
:deep(.highlight-flow .djs-visual > :nth-child(2)) {
  stroke: #28a745 !important;
  fill: #28a745 !important;
}
</style>
