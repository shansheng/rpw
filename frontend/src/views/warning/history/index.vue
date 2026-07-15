<template>
  <div class="warning-history-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>预警历史</span>
        </div>
      </template>

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
            <el-option label="已解决" value="RESOLVED" />
            <el-option label="已忽略" value="IGNORED" />
          </el-select>
        </el-form-item>

        <el-form-item label="项目">
          <el-select v-model="searchForm.projectId" placeholder="请选择项目" clearable filterable>
            <el-option
              v-for="item in projectList"
              :key="item.id"
              :label="item.projectName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

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
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table :data="tableData" v-loading="loading" border style="width: 100%">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="warningType" label="预警类型" width="120">
          <template #default="{ row }">
            {{ getWarningTypeText(row.warningType) }}
          </template>
        </el-table-column>
        <el-table-column prop="warningLevel" label="预警级别" width="100">
          <template #default="{ row }">
            <el-tag :type="getWarningLevelType(row.warningLevel)">
              {{ getWarningLevelText(row.warningLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="projectName" label="项目名称" min-width="140" />
        <el-table-column prop="ruleName" label="资源名称" min-width="120" />
        <el-table-column prop="actualValue" label="实际值" width="90" />
        <el-table-column prop="thresholdValue" label="阈值" width="90" />
        <el-table-column prop="createTime" label="触发时间" width="180" />
        <el-table-column prop="updateTime" label="处理时间" width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getWarningRecordList } from '@/api/warning'
import type { WarningRecord } from '@/api/warning'

// 数据列表
const tableData = ref<WarningRecord[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

// 搜索表单
const searchForm = reactive({
  warningLevel: '',
  status: '',
  projectId: undefined as number | undefined,
  startDate: '',
  endDate: ''
})

const dateRange = ref<[string, string] | null>(null)

// 项目列表（可扩展为从API获取）
const projectList = ref<{ id: number; projectName: string }[]>([])

// 获取列表数据
async function loadData() {
  loading.value = true
  try {
    const params: any = {
      page: currentPage.value,
      pageSize: pageSize.value
    }
    if (searchForm.warningLevel) params.warningLevel = searchForm.warningLevel
    if (searchForm.status) params.status = searchForm.status
    if (searchForm.projectId) params.projectId = searchForm.projectId
    if (dateRange.value) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }

    const res = await getWarningRecordList(params)
    if (res.code === 200 && res.data) {
      // 处理分页响应（兼容数组或分页对象）
      if (Array.isArray(res.data)) {
        tableData.value = res.data
        total.value = res.data.length
      } else {
        tableData.value = (res.data as any).records || []
        total.value = (res.data as any).total || 0
      }
    }
  } catch (error) {
    console.error('加载数据失败', error)
  } finally {
    loading.value = false
  }
}

// 搜索
function handleSearch() {
  currentPage.value = 1
  loadData()
}

// 重置
function handleReset() {
  searchForm.warningLevel = ''
  searchForm.status = ''
  searchForm.projectId = undefined
  dateRange.value = null
  currentPage.value = 1
  loadData()
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
    RESOLVED: 'success',
    IGNORED: 'info',
    PENDING: 'danger',
    PROCESSING: 'warning'
  }
  return map[status] || 'info'
}

// 获取状态文本
function getStatusText(status: string) {
  const map: Record<string, string> = {
    RESOLVED: '已解决',
    IGNORED: '已忽略',
    PENDING: '待处理',
    PROCESSING: '处理中'
  }
  return map[status] || status
}

// 获取预警类型文本
function getWarningTypeText(type: string) {
  const map: Record<string, string> = {
    budget_overrun: '预算超支',
    schedule_delay: '进度延迟',
    inventory_shortage: '库存不足',
    expiring_soon: '即将到期'
  }
  return map[type] || type
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.warning-history-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
