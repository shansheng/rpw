<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import CommonTable from '@/components/CommonTable.vue'
import { getRuleList, createRule, updateRule, deleteRule, type WarningRule } from '@/api/warning'

/** 响应式 */
const isMobile = ref(false)
const checkMobile = () => { isMobile.value = window.innerWidth < 768 }
onMounted(() => { checkMobile(); window.addEventListener('resize', checkMobile) })
onUnmounted(() => { window.removeEventListener('resize', checkMobile) })

/** 搜索条件 */
const searchForm = ref({
  name: ''
})

/** 表格数据 */
const rules = ref<WarningRule[]>([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

/** 弹窗 */
const modalOpen = ref(false)
const modalTitle = ref('新增规则')
const editingId = ref<number | null>(null)
const formRef = ref()
const formState = ref<Partial<WarningRule>>({
  ruleName: '',
  resourceType: 'material',
  thresholdType: 'RATE',
  warningThreshold: undefined,
  warningLevel: 'warning',
  enabled: 1,
  description: ''
})

/** 表单规则 */
const rulesConfig: Record<string, any> = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  warningThreshold: [{ required: true, message: '请输入阈值', trigger: 'blur' }]
}

/** 资源类型选项 */
const resourceTypeOptions = [
  { value: 'material', label: '材料', icon: '📦' },
  { value: 'equipment', label: '设备', icon: '🏗️' },
  { value: 'safety', label: '安全物资', icon: '🦺' },
  { value: 'office', label: '办公用品', icon: '📎' },
  { value: 'hardware', label: '五金', icon: '🔧' },
  { value: 'circulation', label: '周转材', icon: '🔄' },
  { value: 'subcontract', label: '分包', icon: '👥' },
  { value: 'labor', label: '劳动力', icon: '👷' }
]

/** 阈值类型选项 */
const thresholdTypeOptions = [
  { value: 'RATE', label: '比率' },
  { value: 'DATE', label: '日期' },
  { value: 'QUANTITY', label: '数量' }
]

/** 预警级别选项 */
const warningLevelOptions = [
  { value: 'GENERAL', label: '一般', color: '#1890ff' },
  { value: 'IMPORTANT', label: '重要', color: '#faad14' },
  { value: 'URGENT', label: '紧急', color: '#ff4d4f' }
]

/** 预警级别 Tag 颜色映射 */
const levelColorMap: Record<string, string> = {
  GENERAL: 'blue',
  IMPORTANT: 'orange',
  URGENT: 'red'
}

/** 资源类型标签映射 */
const resourceLabelMap = computed(() => {
  const map: Record<string, string> = {}
  resourceTypeOptions.forEach(o => { map[o.value] = o.label })
  return map
})

/** 统计 */
const stats = computed(() => {
  const all = rules.value
  const enabled = all.filter(r => r.enabled === 1).length
  const disabled = all.filter(r => r.enabled === 0).length
  return { total: total.value, enabled, disabled }
})

/** 表格列 - PC端 */
const pcColumns = [
  { title: '规则名称', dataIndex: 'ruleName', key: 'ruleName', width: 180 },
  { title: '资源类型', dataIndex: 'resourceType', key: 'resourceType', width: 120 },
  { title: '阈值类型', dataIndex: 'thresholdType', key: 'thresholdType', width: 100 },
  { title: '预警级别', dataIndex: 'warningLevel', key: 'warningLevel', width: 100 },
  { title: '阈值', dataIndex: 'warningThreshold', key: 'warningThreshold', width: 100 },
  {
    title: '状态', dataIndex: 'enabled', key: 'enabled', width: 100
  },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' as const }
]

/** 表格列 - 移动端 */
const mobileColumns = [
  { title: '规则名称', dataIndex: 'ruleName', key: 'ruleName', width: 160 },
  { title: '级别', dataIndex: 'warningLevel', key: 'warningLevel', width: 80 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' as const }
]

const columns = computed(() => isMobile.value ? mobileColumns : pcColumns)

/** 获取规则列表 */
async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    if (searchForm.value.name) params.name = searchForm.value.name
    const res = await getRuleList(params)
    rules.value = res.data.records
    total.value = res.data.total
  } catch (err: any) {
    message.error(err.message || '获取规则列表失败')
  } finally {
    loading.value = false
  }
}

/** 搜索 */
function handleSearch() {
  pageNum.value = 1
  fetchData()
}

/** 重置 */
function handleReset() {
  searchForm.value.name = ''
  pageNum.value = 1
  fetchData()
}

/** 打开新增弹窗 */
function openCreate() {
  modalTitle.value = '新增规则'
  editingId.value = null
  formState.value = {
    ruleName: '',
    resourceType: 'material',
    thresholdType: 'RATE',
    warningThreshold: undefined,
    warningLevel: 'GENERAL',
    enabled: 1,
    description: ''
  }
  modalOpen.value = true
}

/** 打开编辑弹窗 */
function openEdit(record: WarningRule) {
  modalTitle.value = '编辑规则'
  editingId.value = record.id
  formState.value = { ...record }
  modalOpen.value = true
}

/** 保存 */
async function handleSave() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  try {
    if (editingId.value) {
      await updateRule(editingId.value, formState.value)
      message.success('更新成功')
    } else {
      await createRule(formState.value)
      message.success('新增成功')
    }
    modalOpen.value = false
    fetchData()
  } catch (err: any) {
    message.error(err.message || '保存失败')
  }
}

/** 删除 */
function handleDelete(record: WarningRule) {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除规则「${record.ruleName}」吗？`,
    okType: 'danger',
    onOk: async () => {
      try {
        await deleteRule(record.id)
        message.success('删除成功')
        fetchData()
      } catch (err: any) {
        message.error(err.message || '删除失败')
      }
    }
  })
}

/** 切换启用/禁用 */
async function toggleEnabled(record: WarningRule) {
  const newEnabled = record.enabled === 1 ? 0 : 1
  try {
    await updateRule(record.id, { enabled: newEnabled })
    message.success(newEnabled === 1 ? '已启用' : '已禁用')
    fetchData()
  } catch (err: any) {
    message.error(err.message || '操作失败')
  }
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="warning-rule-page">
    <!-- 统计栏 -->
    <a-row :gutter="16" class="stats-row">
      <a-col :xs="8" :sm="8">
        <div class="stat-item">
          <div class="stat-value">{{ stats.total }}</div>
          <div class="stat-label">总规则</div>
        </div>
      </a-col>
      <a-col :xs="8" :sm="8">
        <div class="stat-item stat-enabled">
          <div class="stat-value">{{ stats.enabled }}</div>
          <div class="stat-label">已启用</div>
        </div>
      </a-col>
      <a-col :xs="8" :sm="8">
        <div class="stat-item stat-disabled">
          <div class="stat-value">{{ stats.disabled }}</div>
          <div class="stat-label">已禁用</div>
        </div>
      </a-col>
    </a-row>

    <a-card :bordered="false" class="rule-card">
      <!-- 搜索 -->
      <div class="search-area">
        <a-form :model="searchForm" :layout="isMobile ? 'vertical' : 'inline'" class="search-form">
          <a-form-item label="规则名称" class="search-item">
            <a-input
              v-model:value="searchForm.name"
              placeholder="搜索规则名称"
              allow-clear
              @pressEnter="handleSearch"
            >
              <template #prefix>
                <SearchOutlined style="color: #bfbfbf" />
              </template>
            </a-input>
          </a-form-item>
          <a-form-item class="search-item">
            <a-space>
              <a-button type="primary" @click="handleSearch">查询</a-button>
              <a-button @click="handleReset">重置</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <!-- 操作栏 -->
      <div class="table-actions">
        <a-button type="primary" @click="openCreate">
          <template #icon><PlusOutlined /></template>
          新增规则
        </a-button>
      </div>

      <!-- 表格 -->
      <CommonTable
        :columns="columns"
        :data-source="rules"
        :loading="loading"
        :scroll="{ x: isMobile ? 360 : 1200 }"
        :pagination="{ current: pageNum, pageSize, total, showSizeChanger: !isMobile, showTotal: (t: number) => `共 ${t} 条` }"
        @change="(pag: any) => { pageNum = pag.current; pageSize = pag.pageSize; fetchData() }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'resourceType'">
            <a-tag>{{ resourceLabelMap[record.resourceType] || record.resourceType || '-' }}</a-tag>
          </template>
          <template v-if="column.key === 'thresholdType'">
            {{ thresholdTypeOptions.find(o => o.value === record.thresholdType)?.label || record.thresholdType || '-' }}
          </template>
          <template v-if="column.key === 'warningLevel'">
            <a-tag :color="levelColorMap[record.warningLevel] || 'default'">
              {{ warningLevelOptions.find(o => o.value === record.warningLevel)?.label || record.warningLevel || '-' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'enabled'">
            <a-switch
              :checked="record.enabled === 1"
              checked-children="启用"
              un-checked-children="禁用"
              @change="toggleEnabled(record)"
              size="small"
            />
          </template>
          <template v-if="column.key === 'action'">
            <a-space :size="isMobile ? 4 : 8">
              <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
              <a-popconfirm title="确定删除吗？" @confirm="handleDelete(record)">
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </CommonTable>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="modalOpen"
      :title="modalTitle"
      @ok="handleSave"
      :width="isMobile ? '95vw' : 560"
      :destroy-on-close="true"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="rulesConfig"
        :label-col="isMobile ? { span: 24 } : { span: 6 }"
        :wrapper-col="isMobile ? { span: 24 } : { span: 16 }"
      >
        <a-form-item label="规则名称" name="ruleName">
          <a-input v-model:value="formState.ruleName" placeholder="请输入规则名称" />
        </a-form-item>
        <a-form-item label="资源类型" name="resourceType">
          <a-select v-model:value="formState.resourceType">
            <a-select-option v-for="opt in resourceTypeOptions" :key="opt.value" :value="opt.value">
              {{ opt.icon }} {{ opt.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="阈值类型" name="thresholdType">
          <a-select v-model:value="formState.thresholdType">
            <a-select-option v-for="opt in thresholdTypeOptions" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="阈值" name="warningThreshold">
          <a-input-number v-model:value="formState.warningThreshold" placeholder="请输入阈值" style="width: 100%" :min="0" />
        </a-form-item>
        <a-form-item label="预警级别" name="warningLevel">
          <a-select v-model:value="formState.warningLevel">
            <a-select-option v-for="opt in warningLevelOptions" :key="opt.value" :value="opt.value">
              <span :style="{ color: opt.color }">●</span> {{ opt.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态" name="enabled">
          <a-switch
            v-model:checked="formState.enabled"
            :checked-value="1"
            :un-checked-value="0"
            checked-children="启用"
            un-checked-children="禁用"
          />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="formState.description" placeholder="请输入描述" :rows="3" show-count :maxlength="200" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts">
import { SearchOutlined, PlusOutlined } from '@ant-design/icons-vue'
export default {
  components: { SearchOutlined, PlusOutlined }
}
</script>

<style scoped>
.warning-rule-page {
  padding: 0;
}

.stats-row {
  margin-bottom: 16px;
}

.stat-item {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
  text-align: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1890ff;
  line-height: 1.2;
}

.stat-enabled .stat-value {
  color: #52c41a;
}

.stat-disabled .stat-value {
  color: #ff4d4f;
}

.stat-label {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
  margin-top: 4px;
}

.rule-card {
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.search-area {
  margin-bottom: 16px;
}

.search-form {
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}

.search-item {
  margin-bottom: 0;
}

.table-actions {
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

@media (max-width: 767px) {
  .search-form {
    padding: 12px;
  }

  .stat-item {
    padding: 12px 8px;
  }

  .stat-value {
    font-size: 22px;
  }

  .search-item {
    margin-bottom: 8px;
    width: 100%;
  }
}
</style>
