<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  AlertOutlined, ExclamationCircleOutlined, CheckCircleOutlined,
  SearchOutlined, ReloadOutlined
} from '@ant-design/icons-vue'
import { getRecordList, handleRecord, type WarningRecord } from '@/api/warning'

/** 筛选条件 */
const filters = ref({
  projectId: undefined as number | undefined,
  warningLevel: undefined as string | undefined,
  resourceType: undefined as string | undefined
})

/** 预警记录列表 */
const records = ref<WarningRecord[]>([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(12)

/** 处理弹窗 */
const handleModalVisible = ref(false)
const currentRecord = ref<WarningRecord | null>(null)
const handleRemark = ref('')
const handleLoading = ref(false)

/** 预警级别 */
const levelOptions = [
  { value: 'GENERAL', label: '一般', color: '#1890ff', bg: '#e6f4ff' },
  { value: 'IMPORTANT', label: '重要', color: '#faad14', bg: '#fffbe6' },
  { value: 'URGENT', label: '紧急', color: '#ff4d4f', bg: '#fff2f0' },
]

/** 资源类型 */
const resourceTypeOptions = [
  { value: 'material', label: '材料' },
  { value: 'equipment', label: '设备' },
  { value: 'safety', label: '安全物资' },
  { value: 'office', label: '办公用品' },
  { value: 'hardware', label: '五金' },
  { value: 'labor', label: '劳动力' },
  { value: 'subcontract', label: '分包' },
]

function getLevelInfo(warningLevel?: string) {
  return levelOptions.find(l => l.value === warningLevel) || { color: '#999', bg: '#f5f5f5', label: warningLevel || '-' }
}

function getStatusTag(status?: string) {
  if (status === 'PENDING') return { color: 'orange', label: '待处理' }
  if (status === 'PROCESSING') return { color: 'processing', label: '处理中' }
  if (status === 'RESOLVED') return { color: 'success', label: '已解决' }
  if (status === 'IGNORED') return { color: 'default', label: '已忽略' }
  return { color: 'default', label: '-' }
}

// 各级别统计
const levelCounts = computed(() => {
  return levelOptions.map(l => ({
    ...l,
    count: records.value.filter(r => r.warningLevel === l.value).length
  }))
})

async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: 'PENDING',
      ...filters.value
    }
    Object.keys(params).forEach(k => { if (params[k] === undefined || params[k] === '') delete params[k] })
    const res = await getRecordList(params)
    records.value = res.data?.records ?? []
    total.value = res.data?.total ?? 0
  } catch (err: any) {
    message.error(err.message || '获取预警列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() { pageNum.value = 1; fetchData() }
function handleReset() {
  filters.value = { projectId: undefined, warningLevel: undefined, resourceType: undefined }
  pageNum.value = 1
  fetchData()
}

function openHandle(record: WarningRecord) {
  currentRecord.value = record
  handleRemark.value = ''
  handleModalVisible.value = true
}

async function confirmHandle() {
  if (!currentRecord.value) return
  handleLoading.value = true
  try {
    await handleRecord(currentRecord.value.id, 'RESOLVED', handleRemark.value)
    message.success('处理成功')
    handleModalVisible.value = false
    fetchData()
  } catch (err: any) {
    message.error(err.message || '处理失败')
  } finally {
    handleLoading.value = false
  }
}

function filterByLevel(level: string) {
  filters.value.warningLevel = filters.value.warningLevel === level ? undefined : level
  pageNum.value = 1
  fetchData()
}

onMounted(() => { fetchData() })
</script>

<template>
  <div class="warning-board-page">
    <!-- 统计概览 -->
    <div class="level-summary">
      <div
        v-for="lc in levelCounts"
        :key="lc.value"
        class="level-card"
        :class="{ active: filters.warningLevel === lc.value }"
        :style="{ borderLeftColor: lc.color }"
        @click="filterByLevel(lc.value)"
      >
        <div class="level-count" :style="{ color: lc.color }">{{ lc.count }}</div>
        <div class="level-label">{{ lc.label }}预警</div>
      </div>
    </div>

    <a-card :bordered="false" class="board-card">
      <template #title>
        <span><AlertOutlined style="margin-right:6px;color:#ff4d4f" />预警看板</span>
      </template>
      <template #extra>
        <span class="total-info">共 <b>{{ total }}</b> 条待处理</span>
      </template>

      <!-- 筛选区 -->
      <div class="filter-bar">
        <a-select
          v-model:value="filters.warningLevel"
          placeholder="全部级别"
          allow-clear
          style="width: 120px"
          @change="handleSearch"
        >
          <a-select-option v-for="opt in levelOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </a-select-option>
        </a-select>
        <a-select
          v-model:value="filters.resourceType"
          placeholder="全部资源类型"
          allow-clear
          style="width: 150px"
          @change="handleSearch"
        >
          <a-select-option v-for="opt in resourceTypeOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </a-select-option>
        </a-select>
        <a-button :icon="h(ReloadOutlined)" @click="handleReset">重置</a-button>
        <a-button type="primary" :icon="h(SearchOutlined)" :loading="loading" @click="handleSearch">
          刷新
        </a-button>
      </div>

      <!-- 预警卡片列表 -->
      <a-spin :spinning="loading">
        <div class="card-grid" v-if="records.length > 0">
          <div
            v-for="record in records"
            :key="record.id"
            class="warning-card"
            :style="{ borderLeftColor: getLevelInfo(record.warningLevel).color }"
          >
            <div class="wc-header">
              <div class="wc-level-dot" :style="{ background: getLevelInfo(record.warningLevel).color }" />
              <span class="wc-rule-name">{{ record.ruleName || '预警消息' }}</span>
              <a-tag
                :color="getLevelInfo(record.warningLevel).color"
                style="margin-left:auto;flex-shrink:0"
              >
                {{ getLevelInfo(record.warningLevel).label }}
              </a-tag>
            </div>
            <div class="wc-message">{{ record.warningMessage || '暂无详细信息' }}</div>
            <div class="wc-footer">
              <span class="wc-time">{{ record.triggeredTime || record.createTime || '-' }}</span>
              <a-tag :color="getStatusTag(record.status).color" style="margin-right:0">
                {{ getStatusTag(record.status).label }}
              </a-tag>
              <a-button
                type="primary"
                size="small"
                danger
                @click="openHandle(record)"
                style="margin-left:8px"
              >
                处理
              </a-button>
            </div>
          </div>
        </div>
        <a-empty
          v-else-if="!loading"
          description="暂无待处理的预警"
          :image="Empty.PRESENTED_IMAGE_SIMPLE"
          style="padding: 40px 0"
        >
          <template #extra>
            <a-tag color="success"><CheckCircleOutlined /> 系统运行正常</a-tag>
          </template>
        </a-empty>
      </a-spin>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="total > pageSize">
        <a-pagination
          v-model:current="pageNum"
          :page-size="pageSize"
          :total="total"
          :show-total="(t: number) => `共 ${t} 条`"
          @change="fetchData"
          size="small"
        />
      </div>
    </a-card>

    <!-- 处理弹窗 -->
    <a-modal
      v-model:open="handleModalVisible"
      title="处理预警"
      @ok="confirmHandle"
      ok-text="确认处理"
      :confirm-loading="handleLoading"
      :width="520"
    >
      <div class="handle-modal-content">
        <a-descriptions :column="1" size="small" bordered class="mb-16">
          <a-descriptions-item label="规则名称">
            {{ currentRecord?.ruleName || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="预警级别">
            <a-tag :color="getLevelInfo(currentRecord?.warningLevel).color">
              {{ getLevelInfo(currentRecord?.warningLevel).label }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="预警内容">
            {{ currentRecord?.warningMessage || '-' }}
          </a-descriptions-item>
        </a-descriptions>

        <a-form-item label="处理意见" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
          <a-textarea
            v-model:value="handleRemark"
            placeholder="请输入处理意见（可选）"
            :rows="4"
            :maxlength="500"
            show-count
          />
        </a-form-item>
      </div>
    </a-modal>
  </div>
</template>

<script lang="ts">
import { h } from 'vue'
import { Empty } from 'ant-design-vue'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
export default { components: { ReloadOutlined, SearchOutlined } }
</script>

<style scoped>
.warning-board-page { padding: 0; }

/* ===== 级别概览 ===== */
.level-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

@media (max-width: 576px) {
  .level-summary { grid-template-columns: repeat(3, 1fr); }
}

.level-card {
  background: #fff;
  border-radius: 10px;
  border-left: 4px solid #d9d9d9;
  padding: 16px 20px;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  transition: all 0.2s;
  text-align: center;
}

.level-card:hover, .level-card.active {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-1px);
}

.level-count {
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 4px;
}

.level-label { font-size: 12px; color: rgba(0, 0, 0, 0.45); }

/* ===== 看板卡片 ===== */
.board-card {
  border-radius: 12px !important;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06) !important;
  border: none !important;
}

.total-info { font-size: 13px; color: rgba(0, 0, 0, 0.45); }
.total-info b { color: #ff4d4f; }

/* ===== 筛选栏 ===== */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #fafafa;
  border-radius: 8px;
}

/* ===== 卡片网格 ===== */
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 12px;
}

@media (max-width: 576px) {
  .card-grid { grid-template-columns: 1fr; }
}

.warning-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-left: 4px solid #d9d9d9;
  border-radius: 10px;
  padding: 14px 16px;
  transition: box-shadow 0.2s, transform 0.2s;
}

.warning-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.wc-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.wc-level-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.wc-rule-name {
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.wc-message {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
  line-height: 1.5;
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.wc-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  border-top: 1px solid #f5f5f5;
  padding-top: 8px;
}

.wc-time {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.35);
  flex: 1;
}

/* ===== 分页 ===== */
.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* ===== 处理弹窗 ===== */
.handle-modal-content { padding: 4px 0; }
.mb-16 { margin-bottom: 16px; }
</style>
