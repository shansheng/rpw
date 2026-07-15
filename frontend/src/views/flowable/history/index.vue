<template>
  <div class="flowable-history-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>审批历史</span>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :inline="true" class="search-form">
        <el-form-item label="流程类型">
          <el-select v-model="searchForm.processType" placeholder="请选择" clearable>
            <el-option label="材料计划审批" value="material_approval" />
            <el-option label="设备计划审批" value="equipment_approval" />
            <el-option label="其他审批" value="other" />
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
        <el-table-column prop="processDefinitionName" label="流程名称" min-width="150" />
        <el-table-column prop="initiator" label="发起人" width="120" />
        <el-table-column prop="startTime" label="开始时间" width="180" />
        <el-table-column prop="endTime" label="结束时间" width="180" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="result" label="结果" width="100">
          <template #default="{ row }">
            <el-tag :type="getResultType(row.result)">
              {{ getResultText(row.result) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleViewHistory(row)">
              查看详情
            </el-button>
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

    <!-- 审批历史详情对话框 -->
    <el-dialog v-model="historyVisible" title="审批历史详情" width="700px">
      <el-timeline>
        <el-timeline-item
          v-for="(item, index) in historyDetail"
          :key="index"
          :timestamp="item.createTime"
          :type="item.type || 'primary'"
        >
          <div class="history-item">
            <p class="history-action">{{ item.action }}</p>
            <p class="history-operator" v-if="item.operator">操作人: {{ item.operator }}</p>
            <p class="history-comment" v-if="item.comment">意见: {{ item.comment }}</p>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-if="historyDetail.length === 0" description="暂无审批历史" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAllProcessInstances, getProcessInstanceHistory } from '@/api/flowable'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

// 搜索表单
const searchForm = reactive({
  processType: ''
})

const dateRange = ref<[string, string] | null>(null)

// 审批历史详情
const historyVisible = ref(false)
const historyDetail = ref<any[]>([])

// 获取列表数据
async function loadData() {
  loading.value = true
  try {
    const res = await getAllProcessInstances()
    if (res.code === 200) {
      const data = Array.isArray(res.data) ? res.data : []
      // 仅显示已完成的流程实例
      const historyData = data.filter(
        (item: any) => item.status === 'COMPLETED' || item.status === 'TERMINATED'
      )
      tableData.value = historyData
      total.value = historyData.length
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
  searchForm.processType = ''
  dateRange.value = null
  currentPage.value = 1
  loadData()
}

// 查看审批历史详情
async function handleViewHistory(row: any) {
  try {
    const res = await getProcessInstanceHistory(row.processInstanceId)
    if (res.code === 200) {
      const data = Array.isArray(res.data) ? res.data : []
      historyDetail.value = data.map((item: any) => ({
        action: item.action || item.taskName || '审批',
        operator: item.operator || item.assignee || '-',
        comment: item.comment || '',
        createTime: item.createTime || item.endTime || '-',
        type: item.type || 'primary'
      }))
      historyVisible.value = true
    }
  } catch (error) {
    console.error('获取审批历史失败', error)
    ElMessage.error('获取审批历史失败')
  }
}

// 获取状态标签类型
function getStatusType(status: string) {
  const map: Record<string, string> = {
    COMPLETED: 'success',
    TERMINATED: 'danger',
    RUNNING: 'warning',
    SUSPENDED: 'info'
  }
  return map[status] || 'info'
}

// 获取状态文本
function getStatusText(status: string) {
  const map: Record<string, string> = {
    COMPLETED: '已完成',
    TERMINATED: '已终止',
    RUNNING: '运行中',
    SUSPENDED: '已挂起'
  }
  return map[status] || status
}

// 获取结果标签类型
function getResultType(result: string) {
  const map: Record<string, string> = {
    APPROVED: 'success',
    REJECTED: 'danger',
    TERMINATED: 'info'
  }
  return map[result] || 'info'
}

// 获取结果文本
function getResultText(result: string) {
  const map: Record<string, string> = {
    APPROVED: '已通过',
    REJECTED: '已驳回',
    TERMINATED: '已终止'
  }
  return map[result] || result
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.flowable-history-container {
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

.history-item {
  padding: 4px 0;
}

.history-action {
  margin: 0;
  font-weight: 500;
}

.history-operator,
.history-comment {
  margin: 4px 0 0;
  font-size: 13px;
  color: #909399;
}
</style>
