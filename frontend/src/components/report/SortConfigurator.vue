<template>
  <div class="sort-configurator">
    <div class="section-header">
      <span>排序规则</span>
      <el-button type="primary" @click="handleAdd">添加排序</el-button>
    </div>

    <div v-if="modelValue.length === 0" class="empty-tip">
      暂无排序规则，点击"添加排序"开始配置
    </div>

    <div v-for="(sort, index) in modelValue" :key="index" class="sort-row">
      <el-select v-model="sort.field" placeholder="选择排序字段" style="flex: 1">
        <el-option
          v-for="item in availableFields"
          :key="item.field"
          :label="item.label"
          :value="item.field"
        />
      </el-select>

      <el-select v-model="sort.order" style="width: 120px">
        <el-option label="升序" value="ASC" />
        <el-option label="降序" value="DESC" />
      </el-select>

      <el-button type="danger" link @click="handleRemove(index)">删除</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { FieldItem, SortItem } from '@/api/customReport'

const props = defineProps<{
  modelValue: SortItem[]
  availableFields: FieldItem[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: SortItem[]): void
}>()

const handleAdd = () => {
  const updated = [...props.modelValue, { field: '', order: 'ASC' as const }]
  emit('update:modelValue', updated)
}

const handleRemove = (index: number) => {
  const updated = [...props.modelValue]
  updated.splice(index, 1)
  emit('update:modelValue', updated)
}
</script>

<style scoped>
.sort-configurator {
  margin-bottom: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-weight: bold;
}

.sort-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.empty-tip {
  text-align: center;
  color: #909399;
  padding: 20px;
}
</style>
