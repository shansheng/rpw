<script setup lang="ts">
import { computed } from 'vue'
import type { TableColumnType } from 'ant-design-vue'

interface Props {
  columns: TableColumnType[]
  dataSource: Record<string, any>[]
  loading?: boolean
  pagination?: Record<string, any> | false
  rowKey?: string
  bordered?: boolean
  size?: 'large' | 'middle' | 'small'
  scroll?: Record<string, any> | false
  rowClassName?: (record: Record<string, any>, index: number) => string
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  pagination: () => ({
    pageNum: 1,
    pageSize: 10,
    total: 0,
    showSizeChanger: true,
    showTotal: (total: number) => `共 ${total} 条`
  }),
  rowKey: 'id',
  bordered: true,
  size: 'middle',
  scroll: false,
})

const emit = defineEmits<{
  (e: 'change', pagination: any): void
}>()

/** 计算 rowClassName：优先使用传入的函数，否则使用斑马纹 */
const rowClassName = (record: Record<string, any>, index: number) => {
  if (props.rowClassName) {
    return props.rowClassName(record, index)
  }
  return index % 2 === 1 ? 'row-odd' : ''
}

const handleTableChange = (pag: any) => {
  emit('change', pag)
}
</script>

<template>
  <a-table
    :columns="columns"
    :data-source="dataSource"
    :loading="loading"
    :pagination="pagination"
    :row-key="rowKey"
    :bordered="bordered"
    :size="size"
    :row-class-name="rowClassName"
    :scroll="scroll"
    @change="handleTableChange"
  >
    <template #bodyCell="slotProps">
      <slot name="bodyCell" v-bind="slotProps" />
    </template>
  </a-table>
</template>

<style scoped>
:deep(.row-odd) {
  background-color: #fafafa;
}
</style>
