<template>
  <div class="kanban-card">
    <div class="card-header">
      <span class="wbs-code">{{ card.wbsCode }}</span>
      <el-tag v-if="card.priority" :type="getPriorityType(card.priority)" size="small">
        {{ getPriorityText(card.priority) }}
      </el-tag>
    </div>
    
    <div class="card-body">
      <div class="resource-name">{{ card.resourceName }}</div>
      <div v-if="card.planStartDate || card.planEndDate" class="date-range">
        <span>{{ card.planStartDate || '' }}</span>
        <span v-if="card.planStartDate && card.planEndDate"> ~ </span>
        <span>{{ card.planEndDate || '' }}</span>
      </div>
    </div>
    
    <div class="card-footer">
      <span v-if="card.responsiblePerson" class="responsible-person">
        <el-icon><User /></el-icon>
        {{ card.responsiblePerson }}
      </span>
      <el-tag :type="getResourceTypeTag(card.resourceType)" size="small" class="resource-type-tag">
        {{ getResourceTypeName(card.resourceType) }}
      </el-tag>
    </div>
  </div>
</template>

<script setup lang="ts">
import { User } from '@element-plus/icons-vue'

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

defineProps<{
  card: KanbanCard
}>()

const getPriorityType = (priority: string | undefined) => {
  if (!priority) return 'info'
  switch (priority) {
    case 'HIGH': return 'danger'
    case 'MEDIUM': return 'warning'
    case 'LOW': return 'info'
    default: return 'info'
  }
}

const getPriorityText = (priority: string | undefined) => {
  if (!priority) return ''
  switch (priority) {
    case 'HIGH': return '高'
    case 'MEDIUM': return '中'
    case 'LOW': return '低'
    default: return priority
  }
}

const getResourceTypeTag = (resourceType: string) => {
  switch (resourceType) {
    case 'SUBTRACT': return ''
    case 'LABOR': return 'success'
    case 'EQUIPMENT': return 'warning'
    case 'MATERIAL': return 'danger'
    default: return 'info'
  }
}

const getResourceTypeName = (resourceType: string) => {
  switch (resourceType) {
    case 'SUBTRACT': return '分包'
    case 'LABOR': return '劳动力'
    case 'EQUIPMENT': return '设备'
    case 'MATERIAL': return '材料'
    case 'SAFETY': return '安全'
    case 'OFFICE': return '办公'
    case 'HARDWARE': return '硬件'
    case 'CIRCULATION': return '流通'
    default: return resourceType
  }
}
</script>

<style scoped>
.kanban-card {
  background: white;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 8px;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  transition: all 0.3s;
}

.kanban-card:hover {
  box-shadow: 0 3px 8px rgba(0,0,0,0.15);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.wbs-code {
  font-weight: bold;
  color: #409eff;
  font-size: 12px;
}

.card-body {
  margin-bottom: 8px;
}

.resource-name {
  font-size: 14px;
  margin-bottom: 4px;
  word-break: break-all;
}

.date-range {
  font-size: 12px;
  color: #909399;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #ebeef5;
  padding-top: 8px;
  font-size: 12px;
  color: #606266;
}

.responsible-person {
  display: flex;
  align-items: center;
  gap: 4px;
}

.resource-type-tag {
  font-size: 10px;
}
</style>
