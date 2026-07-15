<template>
  <div class="my-initiated-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我发起的审批</span>
        </div>
      </template>

      <!-- 表格 -->
      <el-table :data="tableData" v-loading="loading" border style="width: 100%">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="processDefinitionName" label="流程名称" min-width="150" />
        <el-table-column prop="processDefinitionKey" label="流程定义" min-width="150" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="180" />
        <el-table-column prop="currentTaskName" label="当前任务" min-width="140" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleViewDiagram(row)">
              查看流程图
            </el-button>
            <el-button type="info" size="small" @click="handleViewHistory(row)">
              审批历史
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

    <!-- 流程图对话框 -->
    <el-dialog v-model="diagramVisible" title="流程图预览" width="80%">
      <div class="diagram-wrapper">
        <img v-if="diagramUrl" :src="diagramUrl" alt="流程图" style="max-width: 100%" />
        <el-empty v-else description="暂无流程图" />
      </div>
    </el-dialog>

    <!-- 审批历史对话框 -->
    <el-dialog v-model="historyVisible" title="审批历史" width="700px">
      <el-timeline>
        <el-timeline-item
          v-for="(item, index) in historyList"
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
      <el-empty v-if="historyList.length === 0" description="暂无审批历史" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAllProcessInstances, getProcessInstanceHistory, getProcessDefinitionXml } from '@/api/flowable'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

// 流程图对话框
const diagramVisible = ref(false)
const diagramUrl = ref('')

// 审批历史对话框
const historyVisible = ref(false)
const historyList = ref<any[]>([])

// 获取列表数据
async function loadData() {
  loading.value = true
  try {
    const res = await getAllProcessInstances()
    if (res.code === 200) {
      const data = Array.isArray(res.data) ? res.data : []
      // 目前API没有按发起人过滤，这里展示全部并默认当前用户由后端控制
      tableData.value = data
      total.value = data.length
    }
  } catch (error) {
    console.error('加载数据失败', error)
  } finally {
    loading.value = false
  }
}

// 查看流程图
async function handleViewDiagram(row: any) {
  try {
    // 尝试获取流程定义XML（实际可展示流程图的URL需根据后端调整）
    if (row.processDefinitionId) {
      diagramUrl.value = `/api/v1/flowable/process-definition/${row.processDefinitionId}/xml`
      diagramVisible.value = true
    } else {
      ElMessage.warning('暂无流程图信息')
    }
  } catch (error) {
    console.error('获取流程图失败', error)
    ElMessage.error('获取流程图失败')
  }
}

// 查看审批历史
async function handleViewHistory(row: any) {
  try {
    const res = await getProcessInstanceHistory(row.processInstanceId)
    if (res.code === 200) {
      const data = Array.isArray(res.data) ? res.data : []
      historyList.value = data.map((item: any) => ({
        action: item.action || item.taskName || '审批',
        operator: item.operator || item.assignee || '-',
        comment: item.comment || '',
        createTime: item.createTime || item.endTime || '-',
        type: item.type || (item.status === 'COMPLETED' ? 'success' : 'primary')
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
    RUNNING: 'warning',
    COMPLETED: 'success',
    TERMINATED: 'danger',
    SUSPENDED: 'info'
  }
  return map[status] || 'info'
}

// 获取状态文本
function getStatusText(status: string) {
  const map: Record<string, string> = {
    RUNNING: '运行中',
    COMPLETED: '已完成',
    TERMINATED: '已终止',
    SUSPENDED: '已挂起'
  }
  return map[status] || status
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.my-initiated-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.diagram-wrapper {
  text-align: center;
  padding: 10px;
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
