<template>
  <el-dialog
      v-model="dialogVisible"
      title="进展登记"
      width="500px"
      @close="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
      <el-form-item label="实际人数" prop="actualQuantity">
        <el-input
            v-model="form.actualQuantity"
            type="number"
            placeholder="请输入实际人数"
        />
      </el-form-item>

      <el-form-item label="实际开始日期" prop="actualStartDate">
        <el-date-picker
            v-model="form.actualStartDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
        />
      </el-form-item>

      <el-form-item label="实际结束日期" prop="actualEndDate">
        <el-date-picker
            v-model="form.actualEndDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
        />
      </el-form-item>

      <el-form-item label="备注" prop="remark">
        <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { registerProgress } from '@/api/resourcePlanLabor'
import type { ResourcePlanLabor, ProgressDTO } from '@/api/resourcePlanLabor'

interface Props {
  visible: boolean
  data?: ResourcePlanLabor
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'success'): void
}>()

const dialogVisible = computed({
  get: () => props.visible,
  set: (val: boolean) => emit('update:visible', val)
})

const formRef = ref<FormInstance>()
const form = reactive<ProgressDTO>({
  actualQuantity: undefined,
  actualStartDate: '',
  actualEndDate: '',
  remark: ''
})

const rules: FormRules = {
  // 无必填项
}

// 监听data变化，填充表单
watch(() => props.data, (newData) => {
  if (newData) {
    form.actualQuantity = newData.actualQuantity
    form.actualStartDate = newData.actualStartDate || ''
    form.actualEndDate = newData.actualEndDate || ''
    form.remark = newData.remark || ''
  }
}, { immediate: true })

// 提交
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (!props.data.id) {
          ElMessage.error('数据ID不存在')
          return
        }
        const res = await registerProgress(props.data.id, { ...form })
        if (res.data) {
          ElMessage.success('登记成功')
          emit('update:visible', false)
          emit('success')
        }
      } catch (error) {
        ElMessage.error('登记失败')
      }
    }
  })
}

// 关闭
const handleClose = () => {
  formRef.value?.resetFields()
  emit('update:visible', false)
}
</script>
