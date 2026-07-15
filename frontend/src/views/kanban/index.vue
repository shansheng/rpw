<template>
  <div class="kanban-container">
    <!-- 筛选栏 -->
    <el-card class="filter-card">
      <el-form :inline="true">
        <el-form-item label="项目">
          <el-select v-model="filter.projectId" placeholder="全部项目" clearable>
            <el-option v-for="item in projects" :key="item.id" :label="item.projectName" :value="item.id" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="资源类型">
          <el-select v-model="filter.resourceType" placeholder="全部类型" clearable>
            <el-option label="分包计划" value="SUBTRACT" />
            <el-option label="劳动力计划" value="LABOR" />
            <el-option label="设备计划" value="EQUIPMENT" />
            <el-option label="材料计划" value="MATERIAL" />
            <el-option label="安全资源" value="SAFETY" />
            <el-option label="办公资源" value="OFFICE" />
            <el-option label="硬件计划" value="HARDWARE" />
            <el-option label="流通资源" value="CIRCULATION" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="loadBoardData">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 看板列 -->
    <div class="kanban-columns">
      <div v-for="column in columns" :key="column.statusKey" class="kanban-column">
        <div class="column-header">
          <span class="column-title">{{ column.statusName }}</span>
          <el-tag size="small">{{ column.cards ? column.cards.length : 0 }}</el-tag>
        </div>
        
        <!-- 可拖拽区域 -->
        <draggable
          v-model="column.cards"
          :group="{ name: 'kanban', pull: true, put: true }"
          :animation="200"
          @end="onDragEnd($event, column)"
          class="card-list"
          item-key="id"
        >
          <template #item="{ element }">
            <KanbanCard :card="element" />
          </template>
        </draggable>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { VueDraggable as draggable } from 'vue-draggable-plus'
import { ElMessage } from 'element-plus'
import { getKanbanBoardData, updateCardStatus, getKanbanColumns } from '@/api/kanban'
import KanbanCard from '@/components/kanban/KanbanCard.vue'

interface Project {
  id: number
  projectName: string
}

interface KanbanCard {
  id: number
  resourceType: string
  wbsCode: string
  resourceName: string
  status: string
  responsiblePerson?: string
  planStartDate?: string
  planEndDate?: string
  priority?: string
}

interface KanbanColumn {
  statusKey: string
  statusName: string
  order: number
  cards: KanbanCard[]
}

// 筛选条件
const filter = ref({
  projectId: undefined as number | undefined,
  resourceType: ''
})

// 项目列表
const projects = ref<Project[]>([])

// 看板列数据
const columns = ref<KanbanColumn[]>([])

// 加载看板数据
const loadBoardData = async () => {
  try {
    const params: any = {}
    if (filter.value.projectId) {
      params.projectId = filter.value.projectId
    }
    if (filter.value.resourceType) {
      params.resourceType = filter.value.resourceType
    }
    
    const res = await getKanbanBoardData(params)
    if (res.data) {
      columns.value = res.data.columns || []
    }
  } catch (error) {
    ElMessage.error('加载看板数据失败')
  }
}

// 加载项目列表
const loadProjects = async () => {
  try {
    // TODO: 调用获取项目列表的API
    // 临时使用空数组，待项目API完成后替换
    projects.value = []
  } catch (error) {
    ElMessage.error('加载项目列表失败')
  }
}

// 加载列配置
const loadColumnConfig = async () => {
  try {
    const res = await getKanbanColumns()
    if (res.data) {
      // 初始化列数据
      columns.value = res.data
    }
  } catch (error) {
    ElMessage.error('加载列配置失败')
  }
}

// 拖拽结束事件
const onDragEnd = async (event: any, targetColumn: KanbanColumn) => {
  // 获取拖拽的卡片数据
  const element = event.item._underlying_vm_
  if (!element || !element.id) {
    return
  }
  
  try {
    await updateCardStatus({
      id: element.id,
      resourceType: element.resourceType,
      newStatus: targetColumn.statusKey
    })
    
    ElMessage.success('状态更新成功')
  } catch (error) {
    ElMessage.error('状态更新失败')
    await loadBoardData()  // 失败时重新加载数据，恢复原状
  }
}

// 重置筛选条件
const handleReset = () => {
  filter.value.projectId = undefined
  filter.value.resourceType = ''
  loadBoardData()
}

onMounted(() => {
  loadBoardData()
  loadProjects()
})
</script>

<style scoped>
.kanban-container {
  padding: 20px;
}

.filter-card {
  margin-bottom: 20px;
}

.kanban-columns {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  min-height: 500px;
}

.kanban-column {
  flex: 1;
  min-width: 280px;
  max-width: 350px;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 12px;
  display: flex;
  flex-direction: column;
}

.column-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 2px solid #dcdfe6;
}

.column-title {
  font-weight: bold;
  font-size: 14px;
  color: #303133;
}

.card-list {
  flex: 1;
  min-height: 400px;
  overflow-y: auto;
}

.card-list::-webkit-scrollbar {
  width: 4px;
}

.card-list::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 2px;
}
</style>
