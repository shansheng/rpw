<template>
  <div class="project-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>项目管理</span>
          <el-button type="primary" @click="handleAdd">新增项目</el-button>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :inline="true" class="search-form">
        <el-form-item label="项目名称">
          <el-input
              v-model="searchForm.projectName"
              placeholder="请输入项目名称"
              clearable
              @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="所属公司">
          <el-select
              v-model="searchForm.companyId"
              placeholder="请选择公司"
              clearable
              filterable
          >
            <el-option
                v-for="company in companyList"
                :key="company.id"
                :label="company.companyName"
                :value="company.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="进行中" :value="1" />
            <el-option label="已完工" :value="2" />
            <el-option label="已暂停" :value="3" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table :data="tableData" v-loading="loading" border style="width: 100%">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="projectName" label="项目名称" min-width="150" />
        <el-table-column prop="companyId" label="所属公司" min-width="150">
          <template #default="{ row }">
            {{ getCompanyName(row.companyId) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="planStartDate" label="计划开始日期" width="130" />
        <el-table-column prop="planEndDate" label="计划结束日期" width="130" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
                v-if="row.status !== 2"
                type="warning"
                size="small"
                @click="handleStatusChange(row)"
            >状态变更</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        width="600px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="项目名称" prop="projectName">
          <el-input v-model="form.projectName" placeholder="请输入项目名称" />
        </el-form-item>

        <el-form-item label="所属公司" prop="companyId">
          <el-select
              v-model="form.companyId"
              placeholder="请选择公司"
              filterable
          >
            <el-option
                v-for="company in companyList"
                :key="company.id"
                :label="company.companyName"
                :value="company.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="计划开始日期" prop="planStartDate">
          <el-date-picker
              v-model="form.planStartDate"
              type="date"
              placeholder="请选择计划开始日期"
              value-format="YYYY-MM-DD"
          />
        </el-form-item>

        <el-form-item label="计划结束日期" prop="planEndDate">
          <el-date-picker
              v-model="form.planEndDate"
              type="date"
              placeholder="请选择计划结束日期"
              value-format="YYYY-MM-DD"
          />
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态">
            <el-option label="进行中" :value="1" />
            <el-option label="已完工" :value="2" />
            <el-option label="已暂停" :value="3" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getProjectList,
  getProjectById,
  createProject,
  updateProject,
  deleteProject,
  updateProjectStatus
} from '@/api/project'
import { getCompanyList } from '@/api/company'

/**
 * 项目管理页面
 * - 支持按项目名称、所属公司、状态搜索
 * - 提供项目的增删改查及状态变更功能
 */

interface Project {
  id?: number
  projectName: string
  companyId?: number
  status?: number
  planStartDate?: string
  planEndDate?: string
}

// 数据列表
const tableData = ref<Project[]>([])
const loading = ref(false)

// 公司列表（用于下拉选择）
const companyList = ref<any[]>([])

// 公司名称映射
const companyNameMap = ref<Map<number, string>>(new Map())

// 搜索表单
const searchForm = reactive({
  projectName: '',
  companyId: undefined as number | undefined,
  status: undefined as number | undefined
})

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('新增项目')
const isEdit = ref(false)
const currentId = ref<number>()

// 表单
const formRef = ref<FormInstance>()
const form = reactive<Project>({
  projectName: '',
  companyId: undefined,
  status: 1,
  planStartDate: '',
  planEndDate: ''
})

// 表单验证规则
const rules: FormRules = {
  projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  companyId: [{ required: true, message: '请选择所属公司', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

// 状态标签类型
const getStatusType = (status: number) => {
  const map: Record<number, string> = { 1: 'success', 2: 'info', 3: 'warning' }
  return map[status] || 'info'
}

// 状态标签文本
const getStatusLabel = (status: number) => {
  const map: Record<number, string> = { 1: '进行中', 2: '已完工', 3: '已暂停' }
  return map[status] || '未知'
}

// 获取公司名称
const getCompanyName = (companyId: number) => {
  return companyNameMap.value.get(companyId) || '-'
}

// 加载项目列表
const loadData = async () => {
  loading.value = true
  try {
    const res = await getProjectList()
    if (res.data) {
      // 前端过滤
      let data = res.data
      if (searchForm.projectName) {
        data = data.filter((item: Project) =>
            item.projectName?.includes(searchForm.projectName)
        )
      }
      if (searchForm.companyId) {
        data = data.filter((item: Project) => item.companyId === searchForm.companyId)
      }
      if (searchForm.status) {
        data = data.filter((item: Project) => item.status === searchForm.status)
      }
      tableData.value = data
    }
  } catch (error) {
    ElMessage.error('加载项目列表失败')
  } finally {
    loading.value = false
  }
}

// 加载公司列表
const loadCompanyList = async () => {
  try {
    const res = await getCompanyList()
    if (res.data) {
      companyList.value = res.data
      res.data.forEach((company: any) => {
        companyNameMap.value.set(company.id, company.companyName)
      })
    }
  } catch (error) {
    ElMessage.error('加载公司列表失败')
  }
}

// 搜索
const handleSearch = () => {
  loadData()
}

// 重置搜索
const handleReset = () => {
  searchForm.projectName = ''
  searchForm.companyId = undefined
  searchForm.status = undefined
  loadData()
}

// 新增
const handleAdd = () => {
  resetForm()
  dialogTitle.value = '新增项目'
  isEdit.value = false
  dialogVisible.value = true
}

// 编辑
const handleEdit = async (row: Project) => {
  resetForm()
  dialogTitle.value = '编辑项目'
  isEdit.value = true
  currentId.value = row.id

  try {
    const res = await getProjectById(row.id!)
    if (res.data) {
      form.projectName = res.data.projectName
      form.companyId = res.data.companyId
      form.status = res.data.status
      form.planStartDate = res.data.planStartDate
      form.planEndDate = res.data.planEndDate
    }
  } catch (error) {
    ElMessage.error('加载详情失败')
  }

  dialogVisible.value = true
}

// 状态变更
const handleStatusChange = async (row: Project) => {
  const statusOptions = [
    { label: '进行中', value: 1 },
    { label: '已完工', value: 2 },
    { label: '已暂停', value: 3 }
  ]

  try {
    const { value } = await ElMessageBox.prompt(
        '请选择新状态',
        '状态变更',
        {
          inputType: 'select',
          inputOptions: statusOptions,
          inputValue: row.status
        }
    )

    const success = await updateProjectStatus(row.id!, value)
    if (success) {
      ElMessage.success('状态更新成功')
      await loadData()
    }
  } catch (error) {
    // 用户取消
  }
}

// 删除
const handleDelete = (id: number) => {
  ElMessageBox.confirm('确定删除该项目吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      const success = await deleteProject(id)
      if (success) {
        ElMessage.success('删除成功')
        await loadData()
      }
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        let success = false
        if (isEdit.value && currentId.value) {
          success = await updateProject(currentId.value, { ...form })
        } else {
          success = await createProject({ ...form })
        }

        if (success) {
          ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
          dialogVisible.value = false
          await loadData()
        }
      } catch (error) {
        ElMessage.error('操作失败')
      }
    }
  })
}

// 重置表单
const resetForm = () => {
  form.projectName = ''
  form.companyId = undefined
  form.status = 1
  form.planStartDate = ''
  form.planEndDate = ''
  formRef.value?.resetFields()
}

onMounted(() => {
  loadData()
  loadCompanyList()
})
</script>

<style scoped>
.project-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}
</style>
