<template>
  <div class="task-container">
    <h2>待办任务</h2>
    
    <!-- 搜索栏 -->
    <el-form :inline="true" :model="queryParams" class="search-form">
      <el-form-item label="任务负责人">
        <el-input v-model="queryParams.assignee" placeholder="请输入负责人" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="getList">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 任务列表 -->
    <el-table :data="taskList" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="任务ID" width="200" />
      <el-table-column prop="name" label="任务名称" width="200" />
      <el-table-column prop="description" label="描述" width="200" />
      <el-table-column prop="assignee" label="负责人" width="120" />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="300">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="handleTask(row)">
            处理
          </el-button>
          <el-button type="warning" size="small" @click="claimTask(row)">
            认领
          </el-button>
          <el-button type="danger" size="small" @click="assignTask(row)">
            转派
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 处理任务对话框 -->
    <el-dialog v-model="dialogVisible" title="处理任务" width="500px">
      <el-form :model="taskForm" label-width="100px">
        <el-form-item label="审批结果">
          <el-radio-group v-model="taskForm.approvalResult">
            <el-radio label="APPROVED">通过</el-radio>
            <el-radio label="REJECTED">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="taskForm.comment" type="textarea" rows="4" placeholder="请输入审批意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTask">确定</el-button>
      </template>
    </el-dialog>

    <!-- 转派任务对话框 -->
    <el-dialog v-model="assignDialogVisible" title="转派任务" width="400px">
      <el-form label-width="100px">
        <el-form-item label="目标用户">
          <el-input v-model="assignUserId" placeholder="请输入用户ID" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssign">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTaskList, completeTask, claimTask as claimTaskApi, assignTask as assignTaskApi } from '@/api/flowable'

const loading = ref(false)
const taskList = ref([])
const dialogVisible = ref(false)
const assignDialogVisible = ref(false)
const currentTaskId = ref('')
const assignUserId = ref('')

const queryParams = ref({
  assignee: 'admin'  // 默认查询admin的任务
})

const taskForm = ref({
  approvalResult: 'APPROVED',
  comment: ''
})

const getList = async () => {
  loading.value = true
  try {
    const res = await getTaskList(queryParams.value)
    if (res.code === 200) {
      taskList.value = res.data || []
    }
  } catch (error) {
    ElMessage.error('查询失败')
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryParams.value.assignee = ''
  getList()
}

const handleTask = (row: any) => {
  currentTaskId.value = row.id
  dialogVisible.value = true
}

const submitTask = async () => {
  try {
    const variables = {
      approvalResult: taskForm.value.approvalResult,
      approvalComment: taskForm.value.comment
    }
    await completeTask(currentTaskId.value, { variables })
    ElMessage.success('处理成功')
    dialogVisible.value = false
    getList()
  } catch (error) {
    ElMessage.error('处理失败')
  }
}

const claimTask = async (row: any) => {
  try {
    await ElMessageBox.confirm('确认认领该任务吗？', '提示', {
      type: 'warning'
    })
    await claimTaskApi(row.id, queryParams.value.assignee)
    ElMessage.success('认领成功')
    getList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('认领失败')
    }
  }
}

const assignTask = (row: any) => {
  currentTaskId.value = row.id
  assignDialogVisible.value = true
}

const submitAssign = async () => {
  if (!assignUserId.value) {
    ElMessage.warning('请输入目标用户ID')
    return
  }
  try {
    await assignTaskApi(currentTaskId.value, assignUserId.value)
    ElMessage.success('转派成功')
    assignDialogVisible.value = false
    assignUserId.value = ''
    getList()
  } catch (error) {
    ElMessage.error('转派失败')
  }
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.task-container {
  padding: 20px;
}
.search-form {
  margin-bottom: 20px;
}
</style>
