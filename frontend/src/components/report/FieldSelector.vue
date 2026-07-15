<template>
  <div class="field-selector">
    <div class="section-header">
      <span>字段选择</span>
      <el-button type="primary" link @click="handleSelectAll">全选</el-button>
      <el-button type="primary" link @click="handleDeselectAll">取消全选</el-button>
    </div>

    <el-table :data="modelValue" border size="small" row-key="field">
      <el-table-column label="显示" width="60" align="center">
        <template #default="{ row }">
          <el-checkbox v-model="row.visible" />
        </template>
      </el-table-column>
      <el-table-column prop="label" label="字段名称" min-width="120" />
      <el-table-column prop="field" label="字段标识" min-width="120" />
      <el-table-column label="列宽" width="100">
        <template #default="{ row }">
          <el-input-number v-model="row.width" :min="80" :max="500" :step="20" size="small" controls-position="right" />
        </template>
      </el-table-column>
      <el-table-column label="排序" width="70" align="center">
        <template #default="{ $index }">
          <el-button type="primary" link :disabled="$index === 0" @click="moveUp($index)">
            <el-icon><ArrowUp /></el-icon>
          </el-button>
          <el-button type="primary" link :disabled="$index === modelValue.length - 1" @click="moveDown($index)">
            <el-icon><ArrowDown /></el-icon>
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ArrowUp, ArrowDown } from '@element-plus/icons-vue'
import type { FieldItem } from '@/api/customReport'

const props = defineProps<{
  modelValue: FieldItem[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: FieldItem[]): void
}>()

const handleSelectAll = () => {
  const updated = props.modelValue.map(f => ({ ...f, visible: true }))
  emit('update:modelValue', updated)
}

const handleDeselectAll = () => {
  const updated = props.modelValue.map(f => ({ ...f, visible: false }))
  emit('update:modelValue', updated)
}

const moveUp = (index: number) => {
  const list = [...props.modelValue]
  const temp = list[index]
  list[index] = list[index - 1]
  list[index - 1] = temp
  emit('update:modelValue', list)
}

const moveDown = (index: number) => {
  const list = [...props.modelValue]
  const temp = list[index]
  list[index] = list[index + 1]
  list[index + 1] = temp
  emit('update:modelValue', list)
}
</script>

<style scoped>
.field-selector {
  margin-bottom: 20px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-weight: bold;
}
</style>
