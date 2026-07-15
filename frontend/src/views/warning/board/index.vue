<template>
  <div class="warning-board-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>预警看板</span>
          <el-button type="primary" @click="handleCheck">手动检查</el-button>
        </div>
      </template>

      <!-- 统计卡片 -->
      <el-row :gutter="20" class="stats-row">
        <el-col :span="6">
          <el-card class="stats-card" shadow="hover">
            <div class="stats-content">
              <div class="stats-number">{{ stats.total || 0 }}</div>
              <div class="stats-label">总预警数</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stats-card pending" shadow="hover">
            <div class="stats-content">
              <div class="stats-number">{{ stats.pending || 0 }}</div>
              <div class="stats-label">待处理</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stats-card processing" shadow="hover">
            <div class="stats-content">
              <div class="stats-number">{{ stats.processing || 0 }}</div>
              <div class="stats-label">处理中</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stats-card resolved" shadow="hover">
            <div class="stats-content">
              <div class="stats-number">{{ stats.resolved || 0 }}</div>
              <div class="stats-label">已解决</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 搜索表单 -->
      <el-form :inline="true" class="search-form">
        <el-form-item label="预警级别">
          <el-select v-model="searchForm.warningLevel" placeholder="请选择" clearable>
            <el-option label="正常" value="normal" />
            <el-option label="提醒" value="info" />
            <el-option label="警告" value="warning" />
            <el-option label="严重" value="danger" />
          </el-select>
        </el-form-item>

        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable>
            <el-option label="待处理" value="pending" />
            <el-option label="处理中" value="processing" />
            <el-option label="已解决" value="resolved" />
            <el-option label="已忽略" value="ignored" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table :data="tableData" v-loading="loading" border style="width: 100%">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="ruleName" label="规则名称" min-width="120" />
        <el-table-column prop="projectName" label="项目名称" min-width="120" />
        <el-table-column prop="resourceType" label="资源类型" width="100" />
        <el-table-column prop="warningType" label="预警类型" width="100" />
        <el-table-column prop="actualValue" label="实际值" width="100" />
        <el-table-column prop="thresholdValue" label="阈值" width="100" />
        <el-table-column prop="warningLevel" label="预警级别" width="100">
          <template #default="{ row }">
            <el-tag :type="getWarningLevelType(row.warningLevel)">
              {{ getWarningLevelText(row.warningLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
                v-if="row.status === 'pending'"
                type="primary"
                size="small"
                @click="handleUpdateStatus(row, 'processing')"
            >处理中</el-button>
            <el-button
                v-if="row.status === 'processing'"
                type="success"
                size="small"
                @click="handleUpdateStatus(row, 'resolved')"
            >已解决</el-button>
            <el-button
                v-if="row.status === 'pending'"
                type="info"
                size="small"
                @click="handleUpdateStatus(row, 'ignored')"
            >忽略</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getWarningRecordList,
  getWarningStats,
  updateWarningRecordStatus,
  triggerWarningCheck
} from '@/api/warning'
import type { WarningRecord, WarningStats } from '@/api/warning'

// 统计数据
const stats = reactive<WarningStats>({
  total: 0,
  pending: 0,
  processing: 0,
  resolved: 0,
  ignored: 0
})

// 数据列表
const tableData = ref<WarningRecord[]>([])
const loading = ref(false)

// 搜索表单
const searchForm = reactive({
  warningLevel: '',
  status: ''
})

// 获取统计数据
async function loadStats() {
  try {
    const res = await getWarningStats()
    if (res.code === 200 && res.data) {
      Object.assign(stats, res.data)
    }
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
}

// 获取列表数据
async function loadData() {
  loading.value = true
  try {
    const params: any = {}
    if (searchForm.warningLevel) params.warningLevel = searchForm.warningLevel
    if (searchForm.status) params.status = searchForm.status

    const res = await getWarningRecordList(params)
    if (res.code === 200 && res.data) {
      tableData.value = res.data
    }
  } catch (error) {
    console.error('加载数据失败', error)
  } finally {
    loading.value = false
  }
}

// 搜索
function handleSearch() {
  loadData()
}

// 重置
function handleReset() {
  searchForm.warningLevel = ''
  searchForm.status = ''
  loadData()
}

// 手动检查
async function handleCheck() {
  try {
    const res = await triggerWarningCheck()
    if (res.code === 200) {
      ElMessage.success(res.data || '检查完成')
      loadStats()
      loadData()
    }
  } catch (error) {
    console.error('手动检查失败', error)
  }
}

// 更新状态
async function handleUpdateStatus(row: WarningRecord, status: string) {
  try {
    const res = await updateWarningRecordStatus(row.id!, status)
    if (res.code === 200) {
      ElMessage.success('状态更新成功')
      loadStats()
      loadData()
    }
  } catch (error) {
    console.error('更新状态失败', error)
  }
}

// 获取预警级别标签类型
function getWarningLevelType(level: string) {
  const map: Record<string, string> = {
    normal: 'info',
    info: 'info',
    warning: 'warning',
    danger: 'danger'
  }
  return map[level] || 'info'
}

// 获取预警级别文本
function getWarningLevelText(level: string) {
  const map: Record<string, string> = {
    normal: '正常',
    info: '提醒',
    warning: '警告',
    danger: '严重'
  }
  return map[level] || level
}

// 获取状态标签类型
function getStatusType(status: string) {
  const map: Record<string, string> = {
    PENDING: 'danger',
    PROCESSING: 'warning',
    RESOLVED: 'success',
    IGNORED: 'info'
  }
  return map[status] || 'info'
}

// 获取状态文本
function getStatusText(status: string) {
  const map: Record<string, string> = {
    PENDING: '待处理',
    PROCESSING: '处理中',
    RESOLVED: '已解决',
    IGNORED: '已忽略'
  }
  return map[status] || status
}

onMounted(() => {
  loadStats()
  loadData()
})
</script>

<style scoped>
.warning-board-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stats-row {
  margin-bottom: 20px;
}

.stats-card {
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}

.stats-card.pending .stats-number {
  color: #f56c6c;
}

.stats-card.processing .stats-number {
  color: #e6a23c;
}

.stats-card.resolved .stats-number {
  color: #67c23a;
}

.stats-content {
  padding: 10px;
}

.stats-number {
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 8px;
}

.stats-label {
  font-size: 14px;
  color: #909399;
}

.search-form {
  margin-bottom: 20px;
}
</style>
