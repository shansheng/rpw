<template>
  <div class="company-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>公司管理</span>
          <el-button type="primary" @click="handleAdd">新增公司</el-button>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :inline="true" class="search-form">
        <el-form-item label="公司名称">
          <el-input
              v-model="searchForm.companyName"
              placeholder="请输入公司名称"
              clearable
              @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="所属组织">
          <el-cascader
              v-model="searchForm.orgId"
              :options="orgTree"
              :props="cascaderProps"
              placeholder="请选择组织"
              clearable
              :show-all-levels="false"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table :data="tableData" v-loading="loading" border style="width: 100%">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="companyName" label="公司名称" min-width="150" />
        <el-table-column prop="orgId" label="所属组织" min-width="150">
          <template #default="{ row }">
            {{ getOrgName(row.orgId) }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        width="500px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="公司名称" prop="companyName">
          <el-input v-model="form.companyName" placeholder="请输入公司名称" />
        </el-form-item>

        <el-form-item label="所属组织" prop="orgId">
          <el-cascader
              v-model="form.orgId"
              :options="orgTree"
              :props="cascaderProps"
              placeholder="请选择所属组织"
              clearable
              :show-all-levels="false"
          />
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
  getCompanyList,
  getCompanyById,
  createCompany,
  updateCompany,
  deleteCompany
} from '@/api/company'
import { getOrganizationTree } from '@/api/organization'

/**
 * 公司管理页面
 * - 支持按公司名称、所属组织搜索
 * - 提供公司的增删改查功能
 */

interface Company {
  id?: number
  companyName: string
  orgId?: number
}

// 数据列表
const tableData = ref<Company[]>([])
const loading = ref(false)

// 组织树数据（用于级联选择器）
const orgTree = ref<any[]>([])

// 级联选择器配置
const cascaderProps = {
  value: 'id',
  label: 'orgName',
  children: 'children',
  checkStrictly: true,
  emitPath: false
}

// 搜索表单
const searchForm = reactive({
  companyName: '',
  orgId: undefined as number | undefined
})

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('新增公司')
const isEdit = ref(false)
const currentId = ref<number>()

// 表单
const formRef = ref<FormInstance>()
const form = reactive<Company>({
  companyName: '',
  orgId: undefined
})

// 表单验证规则
const rules: FormRules = {
  companyName: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
  orgId: [{ required: true, message: '请选择所属组织', trigger: 'change' }]
}

// 组织名称映射
const orgNameMap = ref<Map<number, string>>(new Map())

// 加载公司列表
const loadData = async () => {
  loading.value = true
  try {
    const res = await getCompanyList()
    if (res.data) {
      tableData.value = res.data
    }
  } catch (error) {
    ElMessage.error('加载公司列表失败')
  } finally {
    loading.value = false
  }
}

// 加载组织树
const loadOrgTree = async () => {
  try {
    const res = await getOrganizationTree()
    if (res.data) {
      orgTree.value = res.data
      buildOrgNameMap(res.data)
    }
  } catch (error) {
    ElMessage.error('加载组织数据失败')
  }
}

// 构建组织名称映射
const buildOrgNameMap = (orgs: any[]) => {
  orgs.forEach(org => {
    orgNameMap.value.set(org.id, org.orgName)
    if (org.children && org.children.length > 0) {
      buildOrgNameMap(org.children)
    }
  })
}

// 获取组织名称
const getOrgName = (orgId: number) => {
  return orgNameMap.value.get(orgId) || '-'
}

// 搜索
const handleSearch = () => {
  // 前端过滤
  loadData()
  // TODO: 如果后端支持搜索，可以传参给后端
}

// 重置搜索
const handleReset = () => {
  searchForm.companyName = ''
  searchForm.orgId = undefined
  loadData()
}

// 新增
const handleAdd = () => {
  resetForm()
  dialogTitle.value = '新增公司'
  isEdit.value = false
  dialogVisible.value = true
}

// 编辑
const handleEdit = async (row: Company) => {
  resetForm()
  dialogTitle.value = '编辑公司'
  isEdit.value = true
  currentId.value = row.id

  try {
    const res = await getCompanyById(row.id!)
    if (res.data) {
      form.companyName = res.data.companyName
      form.orgId = res.data.orgId
    }
  } catch (error) {
    ElMessage.error('加载详情失败')
  }

  dialogVisible.value = true
}

// 删除
const handleDelete = (id: number) => {
  ElMessageBox.confirm('确定删除该公司吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      const success = await deleteCompany(id)
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
          success = await updateCompany(currentId.value, { ...form })
        } else {
          success = await createCompany({ ...form })
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
  form.companyName = ''
  form.orgId = undefined
  formRef.value?.resetFields()
}

onMounted(() => {
  loadData()
  loadOrgTree()
})
</script>

<style scoped>
.company-container {
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
