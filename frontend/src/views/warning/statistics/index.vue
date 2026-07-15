<template>
  <div class="warning-statistics-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>预警统计</span>
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
          <el-card class="stats-card warning" shadow="hover">
            <div class="stats-content">
              <div class="stats-number">{{ stats.yellow || 0 }}</div>
              <div class="stats-label">黄色预警</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stats-card danger" shadow="hover">
            <div class="stats-content">
              <div class="stats-number">{{ stats.red || 0 }}</div>
              <div class="stats-label">红色预警</div>
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

      <!-- 日期筛选 -->
      <el-form :inline="true" class="search-form">
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            clearable
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 项目维度统计 -->
      <h3 class="section-title">项目预警统计</h3>
      <el-table :data="projectStats" v-loading="loading" border style="width: 100%">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="projectName" label="项目名称" min-width="160" />
        <el-table-column prop="total" label="预警总数" width="100" />
        <el-table-column prop="infoCount" label="提醒" width="80">
          <template #default="{ row }">
            <el-tag type="info">{{ row.infoCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="warningCount" label="警告" width="80">
          <template #default="{ row }">
            <el-tag type="warning">{{ row.warningCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dangerCount" label="严重" width="80">
          <template #default="{ row }">
            <el-tag type="danger">{{ row.dangerCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="resolvedCount" label="已解决" width="80">
          <template #default="{ row }">
            <el-tag type="success">{{ row.resolvedCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pendingCount" label="待处理" width="80">
          <template #default="{ row }">
            <el-tag type="danger">{{ row.pendingCount || 0 }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getWarningStats, getWarningRecordList } from '@/api/warning'

// 统计数据
const stats = reactive({
  total: 0,
  yellow: 0,
  red: 0,
  resolved: 0
})

const loading = ref(false)
const dateRange = ref<[string, string] | null>(null)

// 项目维度统计
const projectStats = ref<any[]>([])

// 获取统计数据
async function loadStats() {
  try {
    const params: any = {}
    if (dateRange.value) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }

    const res = await getWarningStats(params)
    if (res.code === 200 && res.data) {
      Object.assign(stats, res.data)
    }
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
}

// 获取项目维度统计
async function loadProjectStats() {
  loading.value = true
  try {
    const params: any = { pageSize: 1000 }
    if (dateRange.value) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }

    const res = await getWarningRecordList(params)
    if (res.code === 200 && res.data) {
      const records = Array.isArray(res.data) ? res.data : (res.data as any).records || []

      // 按项目分组统计
      const projectMap = new Map<string, any>()
      for (const record of records) {
        const key = record.projectName || '未知项目'
        if (!projectMap.has(key)) {
          projectMap.set(key, {
            projectName: key,
            total: 0,
            infoCount: 0,
            warningCount: 0,
            dangerCount: 0,
            resolvedCount: 0,
            pendingCount: 0
          })
        }

        const item = projectMap.get(key)!
        item.total++
        const level = record.warningLevel
        if (level === 'info') item.infoCount++
        else if (level === 'warning') item.warningCount++
        else if (level === 'danger') item.dangerCount++

        const status = record.status
        if (status === 'RESOLVED') item.resolvedCount++
        else if (status === 'PENDING') item.pendingCount++
      }

      projectStats.value = Array.from(projectMap.values())
    }
  } catch (error) {
    console.error('加载项目统计失败', error)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadStats()
  loadProjectStats()
}

function handleReset() {
  dateRange.value = null
  loadStats()
  loadProjectStats()
}

onMounted(() => {
  loadStats()
  loadProjectStats()
})
</script>

<style scoped>
.warning-statistics-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stats-row {
  margin-bottom: 24px;
}

.stats-card {
  text-align: center;
  cursor: default;
  transition: all 0.3s;
}

.stats-card.warning .stats-number {
  color: #e6a23c;
}

.stats-card.danger .stats-number {
  color: #f56c6c;
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

.section-title {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
</style>
