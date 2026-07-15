<template>
  <div class="filter-configurator">
    <div class="section-header">
      <span>筛选条件</span>
      <el-button type="primary" @click="handleAdd">添加条件</el-button>
    </div>

    <div v-if="modelValue.length === 0" class="empty-tip">
      暂无筛选条件，点击"添加条件"开始配置
    </div>

    <div v-for="(filter, index) in modelValue" :key="index" class="filter-row">
      <el-form-item label="字段">
        <el-select v-model="filter.field" placeholder="选择字段" @change="onFieldChange(filter)">
          <el-option
            v-for="item in availableFields"
            :key="item.field"
            :label="item.label"
            :value="item.field"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="操作符">
        <el-select v-model="filter.operator" placeholder="选择操作符">
          <el-option label="等于" value="EQ" />
          <el-option label="不等于" value="NE" />
          <el-option label="包含" value="LIKE" />
          <el-option label="大于" value="GT" />
          <el-option label="小于" value="LT" />
          <el-option label="大于等于" value="GTE" />
          <el-option label="小于等于" value="LTE" />
          <el-option label="范围" value="BETWEEN" />
          <el-option label="在...之中" value="IN" />
        </el-select>
      </el-form-item>

      <el-form-item label="值">
        <!-- BETWEEN: 日期范围 -->
        <template v-if="filter.operator === 'BETWEEN'">
          <el-date-picker
            v-model="filter.value"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </template>
        <!-- 其他操作符 -->
        <template v-else>
          <el-input v-model="filter.value" placeholder="请输入值" />
        </template>
      </el-form-item>

      <el-button type="danger" link @click="handleRemove(index)">删除</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { FieldItem, FilterItem } from '@/api/customReport'

const props = defineProps<{
  modelValue: FilterItem[]
  availableFields: FieldItem[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: FilterItem[]): void
}>()

const handleAdd = () => {
  const updated = [...props.modelValue, { field: '', operator: 'EQ', value: '' }]
  emit('update:modelValue', updated)
}

const handleRemove = (index: number) => {
  const updated = [...props.modelValue]
  updated.splice(index, 1)
  emit('update:modelValue', updated)
}

const onFieldChange = (filter: FilterItem) => {
  filter.value = ''
}
</script>

<style scoped>
.filter-configurator {
  margin-bottom: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-weight: bold;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.filter-row .el-form-item {
  margin-bottom: 0;
  flex: 1;
}

.empty-tip {
  text-align: center;
  color: #909399;
  padding: 20px;
}
</style>
