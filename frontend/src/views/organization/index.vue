<template>
  <div class="organization-container">
    <el-row :gutter="20">
      <!-- 左侧：树形结构 -->
      <el-col :span="10">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>组织架构</span>
              <el-button type="primary" size="small" @click="handleAdd">新增</el-button>
            </div>
          </template>

          <el-tree
              v-loading="treeLoading"
              :data="treeData"
              :props="treeProps"
              node-key="id"
              default-expand-all
              highlight-current
              @node-click="handleNodeClick"
          >
            <template #default="{ data }">
              <span class="tree-node">
                <span>{{ data.orgName }}</span>
                <span class="tree-node-level">({{ getLevelLabel(data.orgLevel) }})</span>
              </span>
            </template>
          </el-tree>
        </el-card>
      </el-col>

      <!-- 右侧：详细信息/表单 -->
      <el-col :span="14">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>{{ dialogTitle }}</span>
            </div>
          </template>

          <el-form
              ref="formRef"
              :model="form"
              :rules="rules"
              label-width="100px"
          >
            <el-form-item label="组织名称" prop="orgName">
              <el-input v-model="form.orgName" placeholder="请输入组织名称" />
            </el-form-item>

            <el-form-item label="组织级别" prop="orgLevel">
              <el-select v-model="form.orgLevel" placeholder="请选择组织级别" @change="handleLevelChange">
                <el-option label="局" :value="1" />
                <el-option label="公司" :value="2" />
                <el-option label="项目" :value="3" />
              </el-select>
            </el-form-item>

            <el-form-item label="上级组织" prop="parentId">
              <el-cascader
                  v-model="form.parentId"
                  :options="treeData"
                  :props="cascaderProps"
                  placeholder="请选择上级组织（可选）"
                  clearable
                  :disabled="!form.orgLevel"
              />
            </el-form-item>

            <el-form-item label="部门" prop="department">
              <el-input v-model="form.department" placeholder="请输入部门" />
            </el-form-item>

            <el-form-item label="处室" prop="section">
              <el-input v-model="form.section" placeholder="请输入处室" />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="handleSubmit">保存</el-button>
              <el-button v-if="isEdit" type="danger" @click="handleDelete">删除</el-button>
              <el-button @click="handleCancel">取消</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getOrganizationTree,
  getOrganizationById,
  createOrganization,
  updateOrganization,
  deleteOrganization
} from '@/api/organization'

/**
 * 组织架构管理页面
 * - 左侧树形展示组织结构
 * - 右侧显示选中节点的详细信息
 * - 支持新增、编辑、删除操作
 */

interface Organization {
  id?: number
  orgName: string
  orgLevel?: number
  parentId?: number
  department?: string
  section?: string
  children?: Organization[]
}

// 树形数据
const treeData = ref<Organization[]>([])
const treeLoading = ref(false)

// 树形配置
const treeProps = {
  children: 'children',
  label: 'orgName'
}

// 级联选择器配置
const cascaderProps = {
  value: 'id',
  label: 'orgName',
  children: 'children',
  checkStrictly: true,
  emitPath: false
}

// 表单
const formRef = ref<FormInstance>()
const isEdit = ref(false)
const dialogTitle = ref('组织架构详情')
const currentId = ref<number>()

// 表单数据
const form = reactive<Organization>({
  orgName: '',
  orgLevel: undefined,
  parentId: undefined,
  department: '',
  section: ''
})

// 表单验证规则
const rules: FormRules = {
  orgName: [{ required: true, message: '请输入组织名称', trigger: 'blur' }],
  orgLevel: [{ required: true, message: '请选择组织级别', trigger: 'change' }]
}

// 获取组织级别标签
const getLevelLabel = (level: number) => {
  const map: Record<number, string> = { 1: '局', 2: '公司', 3: '项目' }
  return map[level] || '未知'
}

// 加载树形数据
const loadTree = async () => {
  treeLoading.value = true
  try {
    const res = await getOrganizationTree()
    if (res.data) {
      treeData.value = res.data
    }
  } catch (error) {
    ElMessage.error('加载组织架构失败')
  } finally {
    treeLoading.value = false
  }
}

// 点击树节点
const handleNodeClick = async (data: Organization) => {
  if (!data.id) return

  try {
    const res = await getOrganizationById(data.id)
    if (res.data) {
      isEdit.value = true
      dialogTitle.value = '编辑组织架构'
      currentId.value = data.id
      Object.assign(form, {
        orgName: res.data.orgName,
        orgLevel: res.data.orgLevel,
        parentId: res.data.parentId,
        department: res.data.department || '',
        section: res.data.section || ''
      })
    }
  } catch (error) {
    ElMessage.error('加载详情失败')
  }
}

// 组织级别变更
const handleLevelChange = () => {
  // 清空上级组织（避免级别冲突）
  form.parentId = undefined
}

// 新增
const handleAdd = () => {
  resetForm()
  isEdit.value = false
  dialogTitle.value = '新增组织架构'
  currentId.value = undefined
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        let success = false
        if (isEdit.value && currentId.value) {
          success = await updateOrganization(currentId.value, { ...form })
        } else {
          success = await createOrganization({ ...form })
        }

        if (success) {
          ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
          await loadTree()
          resetForm()
        }
      } catch (error: any) {
        ElMessage.error(error.message || '操作失败')
      }
    }
  })
}

// 删除
const handleDelete = async () => {
  if (!currentId.value) return

  try {
    await ElMessageBox.confirm('确定删除该组织吗？删除前请确保没有子节点。', '提示', {
      type: 'warning'
    })

    const success = await deleteOrganization(currentId.value)
    if (success) {
      ElMessage.success('删除成功')
      await loadTree()
      resetForm()
    }
  } catch (error) {
    // 用户取消或未删除成功
  }
}

// 取消
const handleCancel = () => {
  resetForm()
  isEdit.value = false
  dialogTitle.value = '组织架构详情'
  currentId.value = undefined
}

// 重置表单
const resetForm = () => {
  form.orgName = ''
  form.orgLevel = undefined
  form.parentId = undefined
  form.department = ''
  form.section = ''
  formRef.value?.resetFields()
}

onMounted(() => {
  loadTree()
})
</script>

<style scoped>
.organization-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tree-node-level {
  font-size: 12px;
  color: #999;
}
</style>
