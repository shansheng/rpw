<template>
  <div class="admin-container">
    <h2>流程实例后台管理</h2>
    
    <!-- 流程实例列表 -->
    <el-table :data="processInstanceList" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="流程实例ID" width="200" />
      <el-table-column prop="processDefinitionName" label="流程名称" width="200" />
      <el-table-column prop="processDefinitionKey" label="流程Key" width="150" />
      <el-table-column prop="startTime" label="启动时间" width="180" />
      <el-table-column prop="isSuspended" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.suspended ? 'danger' : 'success'">
            {{ row.suspended ? '已挂起' : '运行中' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="500">
        <template #default="{ row }">
          <el-button 
            v-if="!row.suspended" 
            type="warning" 
            size="small" 
            @click="handleSuspend(row)">
            挂起
          </el-button>
          <el-button 
            v-if="row.suspended" 
            type="success" 
            size="small" 
            @click="handleActivate(row)">
            激活
          </el-button>
          <el-button type="primary" size="small" @click="handleViewTasks(row)">
            查看任务
          </el-button>
          <el-button type="info" size="small" @click="handleJump(row)">
            跳转
          </el-button>
          <el-button type="danger" size="small" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 任务跳转对话框 -->
    <el-dialog v-model="jumpDialogVisible" title="任务跳转" width="450px">
      <el-form label-width="100px">
        <el-form-item label="流程实例ID">
          <el-input v-model="currentProcessInstanceId" disabled />
        </el-form-item>
        <el-form-item label="任务ID">
          <el-input v-model="jumpTaskId" placeholder="请输入任务ID" />
        </el-form-item>
        <el-form-item label="目标活动ID">
          <el-input v-model="jumpActivityId" placeholder="请输入目标活动ID" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="jumpDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitJump">确定跳转</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAllProcessInstances, suspendProcessInstance, activateProcessInstance, deleteProcessInstance, jumpTask } from '@/api/flowable'

const loading = ref(false)
const processInstanceList = ref([])
const jumpDialogVisible = ref(false)
const jumpActivityId = ref('')
const jumpTaskId = ref('')
const currentProcessInstanceId = ref('')

const getList = async () => {
  loading.value = true
  try {
    const res = await getAllProcessInstances()
    if (res.code === 200) {
      processInstanceList.value = res.data || []
    }
  } catch (error) {
    ElMessage.error('查询失败')
  } finally {
    loading.value = false
  }
}

const handleSuspend = async (row: any) => {
  try {
    await ElMessageBox.confirm('确认挂起该流程实例吗？', '提示', {
      type: 'warning'
    })
    await suspendProcessInstance(row.id)
    ElMessage.success('挂起成功')
    getList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('挂起失败')
    }
  }
}

const handleActivate = async (row: any) => {
  try {
    await activateProcessInstance(row.id)
    ElMessage.success('激活成功')
    getList()
  } catch (error) {
    ElMessage.error('激活失败')
  }
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确认删除该流程实例吗？', '提示', {
      type: 'warning'
    })
    await deleteProcessInstance(row.id)
    ElMessage.success('删除成功')
    getList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleJump = (row: any) => {
  currentProcessInstanceId.value = row.id
  jumpActivityId.value = ''
  jumpDialogVisible.value = true
}

const submitJump = async () => {
  if (!jumpTaskId.value) {
    ElMessage.warning('请输入任务ID')
    return
  }
  if (!jumpActivityId.value) {
    ElMessage.warning('请输入目标活动ID')
    return
  }
  
  try {
    // 执行跳转
    await jumpTask(jumpTaskId.value, jumpActivityId.value)
    ElMessage.success('跳转成功')
    jumpDialogVisible.value = false
    getList()
  } catch (error) {
    ElMessage.error('跳转失败: ' + (error instanceof Error ? error.message : error))
  }
}

const handleViewTasks = async (row: any) => {
  try {
    const res = await getProcessInstanceTasks(row.id)
    if (res.code === 200 && res.data && res.data.length > 0) {
      const taskInfo = res.data.map((t: any) => `任务ID: ${t.id}, 名称: ${t.name}, 负责人: ${t.assignee || '未认领'}`).join('\n')
      ElMessage.info(`当前任务:\n${taskInfo}`)
    } else {
      ElMessage.info('当前流程实例没有待处理任务')
    }
  } catch (error) {
    ElMessage.error('查询任务失败')
  }
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.admin-container {
  padding: 20px;
}
</style>
