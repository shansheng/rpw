<template>
  <el-dialog
      v-model="dialogVisible"
      title="发起变更申请"
      width="500px"
      @close="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
      <el-form-item label="变更类型" prop="changeType">
        <el-select v-model="form.changeType" placeholder="请选择变更类型">
          <el-option label="延期（延期）" value="DELAY" />
          <el-option label="修改（修改信息）" value="MODIFY" />
          <el-option label="终止（终止计划）" value="TERMINATE" />
        </el-select>
      </el-form-item>

      <el-form-item label="变更原因" prop="changeReason">
        <el-input v-model="form.changeReason" type="textarea" placeholder="请输入变更原因" />
      </el-form-item>

      <el-form-item v-if="form.changeType === 'DELAY'" label="新计划开始日期">
        <el-date-picker
            v-model="form.newPlanStartDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
        />
      </el-form-item>

      <el-form-item v-if="form.changeType === 'DELAY'" label="新计划结束日期">
        <el-date-picker
            v-model="form.newPlanEndDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
        />
      </el-form-item>

      <el-form-item v-if="form.changeType === 'MODIFY'" label="新劳务类别编码">
        <el-input v-model="form.newLaborCategoryCode" placeholder="请输入新劳务类别编码" />
      </el-form-item>

      <el-form-item v-if="form.changeType === 'MODIFY'" label="新劳务类别名称">
        <el-input v-model="form.newLaborCategoryName" placeholder="请输入新劳务类别名称" />
      </el-form-item>

      <el-form-item label="变更详情（JSON）">
        <el-input v-model="form.changeDetails" type="textarea" placeholder="请输入变更详情JSON" />
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
import { createChange } from '@/api/resourcePlanLabor'
import type { ResourcePlanLabor, LaborChangeDTO } from '@/api/resourcePlanLabor'

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
const form = reactive<LaborChangeDTO>({
  planId: undefined,
  changeType: '',
  changeReason: '',
  changeDetails: '',
  newPlanStartDate: '',
  newPlanEndDate: '',
  newLaborCategoryCode: '',
  newLaborCategoryName: ''
})

const rules: FormRules = {
  changeType: [{ required: true, message: '请选择变更类型', trigger: 'change' }],
  changeReason: [{ required: true, message: '请输入变更原因', trigger: 'blur' }]
}

// 监听data变化，填充表单
watch(() => props.data, (newData) => {
  if (newData && newData.id) {
    form.planId = newData.id
  }
}, { immediate: true })

// 提交
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const res = await createChange({ ...form })
        if (res.data) {
          ElMessage.success('变更申请已提交')
          emit('update:visible', false)
          emit('success')
        }
      } catch (error) {
        ElMessage.error('提交失败')
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
