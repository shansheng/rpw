<script setup lang="ts">
import { ref, computed, onMounted, watch, h } from 'vue'
import { useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { WarningOutlined, ExclamationCircleOutlined } from '@ant-design/icons-vue'
import CommonTable from '@/components/CommonTable.vue'
import { getList as getProjectList } from '@/api/project'
import {
  getList,
  createItem,
  updateItem,
  deleteItem,
  submitApproval,
  createLaborChange,
  getLaborChangeList,
  approveLaborChange,
  rejectLaborChange,
  getWarningStatus,
} from '@/api/resourcePlan'

/** 表单字段配置类型 */
interface FormFieldConfig {
  key: string
  label: string
  type: 'input' | 'number' | 'date' | 'textarea' | 'select'
  required: boolean
  options?: { label: string; value: any }[]
  placeholder?: string
}

/** 表格列配置类型 */
interface ColumnConfig {
  title: string
  dataIndex: string
  key: string
  width?: number
  ellipsis?: boolean
  fixed?: 'left' | 'right'
  customRender?: (params: any) => string
}

/** 8种资源类型的完整配置 */
const typeConfig: Record<string, {
  title: string
  columns: ColumnConfig[]
  formFields: FormFieldConfig[]
}> = {
  material: {
    title: '材料计划',
    columns: [
      { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
      { title: '资源名称', dataIndex: 'resourceName', key: 'resourceName', width: 160, ellipsis: true },
      { title: '预算数量', dataIndex: 'budgetQuantity', key: 'budgetQuantity', width: 90 },
      { title: '单位', dataIndex: 'unit', key: 'unit', width: 60 },
      { title: '计划到货日期', dataIndex: 'planArrivalDate', key: 'planArrivalDate', width: 120 },
      { title: '状态', dataIndex: 'status', key: 'status', width: 90, customRender: ({ text }: any) => statusLabel(text) },
      { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
    ],
    formFields: [
      { key: 'projectId', label: '所属项目', type: 'select', required: false, placeholder: '请选择项目' },
      { key: 'resourceName', label: '资源名称', type: 'input', required: true },
      { key: 'budgetQuantity', label: '预算数量', type: 'number', required: false },
      { key: 'unit', label: '单位', type: 'input', required: false },
      { key: 'planArrivalDate', label: '计划到货日期', type: 'date', required: false },
      { key: 'planEndDate', label: '结束日期', type: 'date', required: false },
      { key: 'remark', label: '备注', type: 'textarea', required: false },
    ]
  },
  equipment: {
    title: '设备计划',
    columns: [
      { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
      { title: '设备名称', dataIndex: 'equipmentName', key: 'equipmentName', width: 160, ellipsis: true },
      { title: '预算数量', dataIndex: 'budgetQuantity', key: 'budgetQuantity', width: 90 },
      { title: '单位', dataIndex: 'unit', key: 'unit', width: 60 },
      { title: '计划到货日期', dataIndex: 'planArrivalDate', key: 'planArrivalDate', width: 120 },
      { title: '状态', dataIndex: 'status', key: 'status', width: 90, customRender: ({ text }: any) => statusLabel(text) },
      { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
    ],
    formFields: [
      { key: 'projectId', label: '所属项目', type: 'select', required: false, placeholder: '请选择项目' },
      { key: 'equipmentName', label: '设备名称', type: 'input', required: true },
      { key: 'budgetQuantity', label: '预算数量', type: 'number', required: false },
      { key: 'unit', label: '单位', type: 'input', required: false },
      { key: 'planArrivalDate', label: '计划到货日期', type: 'date', required: false },
      { key: 'planEndDate', label: '结束日期', type: 'date', required: false },
      { key: 'remark', label: '备注', type: 'textarea', required: false },
    ]
  },
  safety: {
    title: '安全物资计划',
    columns: [
      { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
      { title: '物资名称', dataIndex: 'safetyName', key: 'safetyName', width: 160, ellipsis: true },
      { title: '预算数量', dataIndex: 'budgetQuantity', key: 'budgetQuantity', width: 90 },
      { title: '单位', dataIndex: 'unit', key: 'unit', width: 60 },
      { title: '计划到货日期', dataIndex: 'planArrivalDate', key: 'planArrivalDate', width: 120 },
      { title: '状态', dataIndex: 'status', key: 'status', width: 90, customRender: ({ text }: any) => statusLabel(text) },
      { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
    ],
    formFields: [
      { key: 'projectId', label: '所属项目', type: 'select', required: false, placeholder: '请选择项目' },
      { key: 'safetyName', label: '物资名称', type: 'input', required: true },
      { key: 'budgetQuantity', label: '预算数量', type: 'number', required: false },
      { key: 'unit', label: '单位', type: 'input', required: false },
      { key: 'planArrivalDate', label: '计划到货日期', type: 'date', required: false },
      { key: 'planEndDate', label: '结束日期', type: 'date', required: false },
      { key: 'remark', label: '备注', type: 'textarea', required: false },
    ]
  },
  office: {
    title: '办公用品计划',
    columns: [
      { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
      { title: '物品名称', dataIndex: 'officeName', key: 'officeName', width: 160, ellipsis: true },
      { title: '预算数量', dataIndex: 'budgetQuantity', key: 'budgetQuantity', width: 90 },
      { title: '单位', dataIndex: 'unit', key: 'unit', width: 60 },
      { title: '计划到货日期', dataIndex: 'planArrivalDate', key: 'planArrivalDate', width: 120 },
      { title: '状态', dataIndex: 'status', key: 'status', width: 90, customRender: ({ text }: any) => statusLabel(text) },
      { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
    ],
    formFields: [
      { key: 'projectId', label: '所属项目', type: 'select', required: false, placeholder: '请选择项目' },
      { key: 'officeName', label: '物品名称', type: 'input', required: true },
      { key: 'budgetQuantity', label: '预算数量', type: 'number', required: false },
      { key: 'unit', label: '单位', type: 'input', required: false },
      { key: 'planArrivalDate', label: '计划到货日期', type: 'date', required: false },
      { key: 'planEndDate', label: '结束日期', type: 'date', required: false },
      { key: 'remark', label: '备注', type: 'textarea', required: false },
    ]
  },
  hardware: {
    title: '五金计划',
    columns: [
      { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
      { title: '五金名称', dataIndex: 'hardwareName', key: 'hardwareName', width: 160, ellipsis: true },
      { title: '预算数量', dataIndex: 'budgetQuantity', key: 'budgetQuantity', width: 90 },
      { title: '单位', dataIndex: 'unit', key: 'unit', width: 60 },
      { title: '计划到货日期', dataIndex: 'planArrivalDate', key: 'planArrivalDate', width: 120 },
      { title: '状态', dataIndex: 'status', key: 'status', width: 90, customRender: ({ text }: any) => statusLabel(text) },
      { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
    ],
    formFields: [
      { key: 'projectId', label: '所属项目', type: 'select', required: false, placeholder: '请选择项目' },
      { key: 'hardwareName', label: '五金名称', type: 'input', required: true },
      { key: 'budgetQuantity', label: '预算数量', type: 'number', required: false },
      { key: 'unit', label: '单位', type: 'input', required: false },
      { key: 'planArrivalDate', label: '计划到货日期', type: 'date', required: false },
      { key: 'planEndDate', label: '结束日期', type: 'date', required: false },
      { key: 'remark', label: '备注', type: 'textarea', required: false },
    ]
  },
  circulation: {
    title: '周转材计划',
    columns: [
      { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
      { title: '周转材名称', dataIndex: 'circulationName', key: 'circulationName', width: 160, ellipsis: true },
      { title: '预算数量', dataIndex: 'budgetQuantity', key: 'budgetQuantity', width: 90 },
      { title: '单位', dataIndex: 'unit', key: 'unit', width: 60 },
      { title: '计划到货日期', dataIndex: 'planArrivalDate', key: 'planArrivalDate', width: 120 },
      { title: '状态', dataIndex: 'status', key: 'status', width: 90, customRender: ({ text }: any) => statusLabel(text) },
      { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
    ],
    formFields: [
      { key: 'projectId', label: '所属项目', type: 'select', required: false, placeholder: '请选择项目' },
      { key: 'circulationName', label: '周转材名称', type: 'input', required: true },
      { key: 'budgetQuantity', label: '预算数量', type: 'number', required: false },
      { key: 'unit', label: '单位', type: 'input', required: false },
      { key: 'planArrivalDate', label: '计划到货日期', type: 'date', required: false },
      { key: 'planEndDate', label: '结束日期', type: 'date', required: false },
      { key: 'remark', label: '备注', type: 'textarea', required: false },
    ]
  },
  subcontract: {
    title: '分包计划',
    columns: [
      { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
      { title: '分包项目名称', dataIndex: 'subcontractName', key: 'subcontractName', width: 180, ellipsis: true },
      { title: '分包商名称', dataIndex: 'subcontractorName', key: 'subcontractorName', width: 140 },
      { title: '合同金额', dataIndex: 'contractAmount', key: 'contractAmount', width: 110 },
      { title: '计划开始日期', dataIndex: 'planStartDate', key: 'planStartDate', width: 120 },
      { title: '计划结束日期', dataIndex: 'planEndDate', key: 'planEndDate', width: 120 },
      { title: '状态', dataIndex: 'status', key: 'status', width: 90, customRender: ({ text }: any) => statusLabel(text) },
      { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
    ],
    formFields: [
      { key: 'projectId', label: '所属项目', type: 'select', required: false, placeholder: '请选择项目' },
      { key: 'subcontractName', label: '分包项目名称', type: 'input', required: true },
      { key: 'subcontractorName', label: '分包商名称', type: 'input', required: false },
      { key: 'contractAmount', label: '合同金额', type: 'number', required: false },
      { key: 'planStartDate', label: '计划开始日期', type: 'date', required: false },
      { key: 'planEndDate', label: '计划结束日期', type: 'date', required: false },
      { key: 'remark', label: '备注', type: 'textarea', required: false },
    ]
  },
  labor: {
    title: '劳动力计划',
    columns: [
      { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
      { title: '工种编码', dataIndex: 'workTypeCode', key: 'workTypeCode', width: 100 },
      { title: '工种名称', dataIndex: 'workTypeName', key: 'workTypeName', width: 120 },
      { title: '劳务类别', dataIndex: 'laborCategoryName', key: 'laborCategoryName', width: 120 },
      { title: 'WBS编码', dataIndex: 'wbsCode', key: 'wbsCode', width: 110, ellipsis: true },
      { title: '计划人数', dataIndex: 'planQuantity', key: 'planQuantity', width: 90 },
      { title: '实际人数', dataIndex: 'actualQuantity', key: 'actualQuantity', width: 90 },
      { title: '计划开始日期', dataIndex: 'planStartDate', key: 'planStartDate', width: 115 },
      { title: '计划结束日期', dataIndex: 'planEndDate', key: 'planEndDate', width: 115 },
      { title: '实际开始日期', dataIndex: 'actualStartDate', key: 'actualStartDate', width: 115 },
      { title: '实际结束日期', dataIndex: 'actualEndDate', key: 'actualEndDate', width: 115 },
      { title: '状态', dataIndex: 'status', key: 'status', width: 90, customRender: ({ text }: any) => statusLabel(text) },
      { title: '审批状态', dataIndex: 'approvalStatus', key: 'approvalStatus', width: 90, customRender: ({ text }: any) => approvalStatusLabel(text) },
      { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
    ],
    formFields: [
      { key: 'projectId', label: '所属项目', type: 'select', required: false, placeholder: '请选择项目' },
      { key: 'wbsCode', label: 'WBS编码', type: 'input', required: false, placeholder: '请输入WBS编码' },
      { key: 'workTypeCode', label: '工种编码', type: 'input', required: true, placeholder: '如: WT001' },
      { key: 'workTypeName', label: '工种名称', type: 'input', required: true, placeholder: '如: 电焊工' },
      { key: 'laborCategoryCode', label: '劳务类别编码', type: 'input', required: false, placeholder: '如: LC01' },
      { key: 'laborCategoryName', label: '劳务类别名称', type: 'input', required: false, placeholder: '如: 特种作业' },
      { key: 'planQuantity', label: '计划人数', type: 'number', required: false, placeholder: '请输入人数' },
      { key: 'planStartDate', label: '计划开始日期', type: 'date', required: false },
      { key: 'planEndDate', label: '计划结束日期', type: 'date', required: false },
      { key: 'remark', label: '备注', type: 'textarea', required: false },
    ]
  }
}

/** 状态选项 */
const statusOptions = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'SUBMITTED', label: '待审批' },
  { value: 'IN_PROGRESS', label: '进行中' },
  { value: 'APPROVED', label: '已审批' },
  { value: 'REJECTED', label: '已驳回' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'TERMINATED', label: '已终止' },
]

const approvalStatusOptions = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'SUBMITTED', label: '已提交' },
  { value: 'APPROVED', label: '已批准' },
  { value: 'REJECTED', label: '已驳回' },
]

/** 变更类型选项 */
const changeTypeOptions = [
  { value: 'DELAY', label: '延期' },
  { value: 'MODIFY_QUANTITY', label: '修改人数' },
  { value: 'MODIFY_DATE', label: '修改日期' },
]

/** 状态标签渲染 */
function statusLabel(text: string): string {
  return statusOptions.find(s => s.value === text)?.label || text || '-'
}

/** 审批状态标签渲染 */
function approvalStatusLabel(text: string): string {
  return approvalStatusOptions.find(s => s.value === text)?.label || text || '-'
}

/** 变更类型标签 */
function changeTypeLabel(type: string): string {
  return changeTypeOptions.find(c => c.value === type)?.label || type || '-'
}

const route = useRoute()
const resourceType = computed(() => (route.params.type as string) || 'material')
const config = computed(() => typeConfig[resourceType.value] || typeConfig.material)

/** 页面标题 */
const pageTitle = computed(() => config.value.title)

/** 项目列表 */
const projectList = ref<{ id: number; projectName: string }[]>([])
const projectOptions = computed(() =>
  projectList.value.map(p => ({ label: p.projectName, value: p.id }))
)

/** 搜索条件 */
const searchForm = ref({
  projectId: undefined as number | undefined,
  wbsCode: '',
  status: undefined as string | undefined,
  keyword: ''
})

/** 表格数据 */
const tableData = ref<any[]>([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

/** 预警状态缓存 */
const warningStatusMap = ref<Record<number, { warningLevel: string; warningMessage?: string; suggestion?: string; checkTime?: string }>>({})

/** 弹窗相关 */
const modalVisible = ref(false)
const modalTitle = ref('新增')
const editingId = ref<number | null>(null)
const confirmLoading = ref(false)
const formRef = ref()
const formState = ref<Record<string, any>>({})

/** 变更申请弹窗 */
const changeModalVisible = ref(false)
const changeModalLoading = ref(false)
const changeFormRef = ref()
const changeFormState = ref<{
  laborPlanId: number
  changeType: string
  changeReason: string
  newPlanStartDate?: string
  newPlanEndDate?: string
  newPlanQuantity?: number
}>({
  laborPlanId: 0,
  changeType: '',
  changeReason: '',
})
const currentPlan = ref<any>({})

/** 变更历史弹窗 */
const historyModalVisible = ref(false)
const historyList = ref<any[]>([])
const historyLoading = ref(false)

/** 预警详情弹窗 */
const warningModalVisible = ref(false)
const warningDetail = ref<{ warningLevel: string; warningMessage?: string; suggestion?: string; checkTime?: string }>({ warningLevel: '' })

/** 动态校验规则 */
const rules = computed(() => {
  const r: Record<string, any> = {}
  config.value.formFields.forEach(f => {
    if (f.required) {
      r[f.key] = [{ required: true, message: `请输入${f.label}`, trigger: 'blur' }]
    }
  })
  return r
})

/** 变更表单校验规则 */
const changeRules = {
  changeType: [{ required: true, message: '请选择变更类型', trigger: 'change' }],
  changeReason: [{ required: true, message: '请输入变更原因', trigger: 'blur' }],
}

/** 表格列定义 */
const columns = computed(() => {
  const baseCols = [...config.value.columns]
  if (resourceType.value === 'labor') {
    baseCols.push({
      title: '预警',
      key: 'warning',
      width: 80,
      customRender: ({ record }: any) => {
        const status = warningStatusMap.value[record.id]
        if (status?.warningLevel === 'CRITICAL') {
          return h(WarningOutlined, { style: { color: '#ff4d4f', fontSize: '18px' } })
        } else if (status?.warningLevel === 'WARNING') {
          return h(ExclamationCircleOutlined, { style: { color: '#faad14', fontSize: '18px' } })
        }
        return '-'
      }
    } as any)
  }
  baseCols.push({
    title: '操作', key: 'action', width: 400, fixed: 'right' as const,
  })
  return baseCols
})

/** 行样式类名 */
const rowClassName = (record: Record<string, any>, index: number) => {
  if (record.warningLevel === 'CRITICAL') {
    return 'row-critical'
  } else if (record.warningLevel === 'WARNING') {
    return 'row-warning'
  }
  return index % 2 === 1 ? 'row-odd' : ''
}

/** 格式化新旧值对比 */
function changeCompareText(record: any): string {
  const type = record.changeType
  if (type === 'MODIFY_DATE' || type === 'DELAY') {
    const oldStart = record.oldPlanStartDate || '-'
    const newStart = record.newPlanStartDate || '-'
    const oldEnd = record.oldPlanEndDate || '-'
    const newEnd = record.newPlanEndDate || '-'
    return `开始: ${oldStart} → ${newStart}; 结束: ${oldEnd} → ${newEnd}`
  } else if (type === 'MODIFY_QUANTITY') {
    return `人数: ${record.oldPlanQuantity ?? '-'} → ${record.newPlanQuantity ?? '-'}`
  }
  return '-'
}

/** 审批状态标签颜色 */
function approvalStatusColor(status: string): string {
  const map: Record<string, string> = {
    'PENDING': 'orange',
    'APPROVED': 'green',
    'REJECTED': 'red',
  }
  return map[status] || 'default'
}

/** 审批状态标签文本 */
function approvalStatusText(status: string): string {
  const map: Record<string, string> = {
    'PENDING': '待审批',
    'APPROVED': '已批准',
    'REJECTED': '已驳回',
  }
  return map[status] || status
}

/** 构建空表单 */
function buildEmptyForm(): Record<string, any> {
  const form: Record<string, any> = {}
  config.value.formFields.forEach(f => {
    if (f.type === 'select') {
      form[f.key] = undefined
    } else if (f.type === 'number') {
      form[f.key] = undefined
    } else {
      form[f.key] = ''
    }
  })
  return form
}

/** 加载项目列表 */
async function loadProjects() {
  try {
    const res = await getProjectList()
    if (res.data) {
      projectList.value = Array.isArray(res.data) ? res.data : (res.data.records || [])
    }
  } catch {
    // ignore
  }
}

/** 获取预警状态 */
async function loadWarningStatus(recordId: number) {
  try {
    const res = await getWarningStatus('labor', recordId)
    if (res.data) {
      warningStatusMap.value[recordId] = res.data
      const idx = tableData.value.findIndex(r => r.id === recordId)
      if (idx !== -1) {
        tableData.value[idx].warningLevel = res.data.warningLevel
      }
    }
  } catch {
    // ignore
  }
}

/** 获取列表数据 */
async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      ...searchForm.value
    }
    Object.keys(params).forEach(k => { if (!params[k] && params[k] !== 0) delete params[k] })
    const res = await getList(resourceType.value, params)
    if (res.data && typeof res.data === 'object' && 'records' in res.data) {
      tableData.value = res.data.records
      total.value = res.data.total
    } else {
      tableData.value = Array.isArray(res.data) ? res.data : []
      total.value = tableData.value.length
    }

    // 劳动力计划：批量查询预警状态
    if (resourceType.value === 'labor' && tableData.value.length > 0) {
      tableData.value.forEach(record => {
        loadWarningStatus(record.id)
      })
    }
  } catch (err: any) {
    message.error(err.message || '获取列表失败')
  } finally {
    loading.value = false
  }
}

/** 搜索 */
function handleSearch() {
  pageNum.value = 1
  fetchData()
}

/** 重置搜索 */
function handleReset() {
  searchForm.value = { projectId: undefined, wbsCode: '', status: undefined, keyword: '' }
  pageNum.value = 1
  fetchData()
}

/** 分页变化 */
function handlePageChange(pag: any) {
  pageNum.value = pag.current || pag.pageNum
  pageSize.value = pag.pageSize
  fetchData()
}

/** 打开新增弹窗 */
function openCreate() {
  modalTitle.value = '新增'
  editingId.value = null
  formState.value = buildEmptyForm()
  modalVisible.value = true
}

/** 打开编辑弹窗 */
function openEdit(record: any) {
  modalTitle.value = '编辑'
  editingId.value = record.id
  const form: Record<string, any> = buildEmptyForm()
  Object.keys(form).forEach(k => {
    if (record[k] !== undefined && record[k] !== null) {
      form[k] = record[k]
    }
  })
  formState.value = form
  modalVisible.value = true
}

/** 保存 */
async function handleSave() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  confirmLoading.value = true
  try {
    const payload = { ...formState.value }
    config.value.formFields.forEach(f => {
      if (f.type === 'date' && payload[f.key]) {
        const val = payload[f.key]
        if (val && typeof val === 'object' && val.format) {
          payload[f.key] = val.format('YYYY-MM-DD')
        }
      }
    })
    if (editingId.value) {
      await updateItem(resourceType.value, editingId.value, payload)
      message.success('更新成功')
    } else {
      await createItem(resourceType.value, payload)
      message.success('新增成功')
    }
    modalVisible.value = false
    fetchData()
  } catch (err: any) {
    message.error(err?.message || err?.response?.data?.message || '操作失败')
  } finally {
    confirmLoading.value = false
  }
}

/** 删除 */
function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除该${config.value.title}记录吗？删除后不可恢复。`,
    okType: 'danger',
    okText: '删除',
    cancelText: '取消',
    onOk: async () => {
      try {
        await deleteItem(resourceType.value, record.id)
        message.success('删除成功')
        fetchData()
      } catch (err: any) {
        message.error(err?.message || err?.response?.data?.message || '删除失败')
      }
    }
  })
}

/** 提交审批 */
function handleSubmitApproval(record: any) {
  Modal.confirm({
    title: '确认提交',
    content: '确定提交该记录进行审批吗？提交后将进入审批流程。',
    okText: '提交',
    cancelText: '取消',
    onOk: async () => {
      try {
        await submitApproval(resourceType.value, record.id)
        message.success('提交成功')
        fetchData()
      } catch (err: any) {
        message.error(err?.message || err?.response?.data?.message || '提交失败')
      }
    }
  })
}

/** 打开变更申请弹窗 */
function openChangeModal(record: any) {
  currentPlan.value = record
  changeFormState.value = {
    laborPlanId: record.id,
    changeType: '',
    changeReason: '',
    newPlanStartDate: undefined,
    newPlanEndDate: undefined,
    newPlanQuantity: undefined,
  }
  changeModalVisible.value = true
}

/** 提交变更申请 */
async function handleChangeSubmit() {
  try {
    await changeFormRef.value.validate()
  } catch {
    return
  }
  changeModalLoading.value = true
  try {
    const payload = { ...changeFormState.value }
    // 根据变更类型清理不需要的字段
    if (payload.changeType === 'MODIFY_QUANTITY') {
      delete payload.newPlanStartDate
      delete payload.newPlanEndDate
    } else {
      delete payload.newPlanQuantity
    }
    await createLaborChange(payload)
    message.success('变更申请已提交')
    changeModalVisible.value = false
    fetchData()
  } catch (err: any) {
    message.error(err?.message || err?.response?.data?.message || '提交变更失败')
  } finally {
    changeModalLoading.value = false
  }
}

/** 打开变更历史弹窗 */
async function openHistoryModal(record: any) {
  currentPlan.value = record
  historyLoading.value = true
  historyModalVisible.value = true
  try {
    const res = await getLaborChangeList({ laborPlanId: record.id })
    if (res.data) {
      historyList.value = Array.isArray(res.data) ? res.data : (res.data.records || [])
    }
  } catch (err: any) {
    message.error(err.message || '获取变更历史失败')
    historyList.value = []
  } finally {
    historyLoading.value = false
  }
}

/** 审批通过变更 */
async function handleApproveChange(id: number) {
  try {
    await approveLaborChange(id)
    message.success('审批通过')
    // 刷新变更历史
    if (currentPlan.value?.id) {
      openHistoryModal(currentPlan.value)
    }
  } catch (err: any) {
    message.error(err?.message || '审批失败')
  }
}

/** 驳回变更 */
async function handleRejectChange(id: number) {
  try {
    await rejectLaborChange(id)
    message.success('已驳回')
    if (currentPlan.value?.id) {
      openHistoryModal(currentPlan.value)
    }
  } catch (err: any) {
    message.error(err?.message || '操作失败')
  }
}

/** 打开预警详情弹窗 */
function openWarningModal(record: any) {
  const status = warningStatusMap.value[record.id]
  if (status) {
    warningDetail.value = status
    warningModalVisible.value = true
  } else {
    message.info('该记录暂无预警信息')
  }
}

onMounted(() => {
  loadProjects()
  fetchData()
})

watch(resourceType, () => {
  pageNum.value = 1
  fetchData()
})
</script>

<template>
  <div class="resource-plan-page">
    <a-card :title="pageTitle" :bordered="false">
      <!-- 搜索区 -->
      <div class="search-area">
        <a-form layout="inline" :model="searchForm" class="search-form">
          <a-form-item label="项目">
            <a-select
              v-model:value="searchForm.projectId"
              placeholder="选择项目"
              allow-clear
              show-search
              :filter-option="(input: string, opt: any) => opt.label.toLowerCase().includes(input.toLowerCase())"
              style="width: 200px"
            >
              <a-select-option v-for="p in projectOptions" :key="p.value" :value="p.value">
                {{ p.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="WBS编码">
            <a-input v-model:value="searchForm.wbsCode" placeholder="WBS编码" style="width: 160px" allow-clear />
          </a-form-item>
          <a-form-item label="状态">
            <a-select
              v-model:value="searchForm.status"
              placeholder="全部状态"
              allow-clear
              style="width: 120px"
            >
              <a-select-option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="关键词">
            <a-input v-model:value="searchForm.keyword" placeholder="资源名称/编码" style="width: 180px" allow-clear />
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" @click="handleSearch">
                <template #icon><SearchOutlined /></template>
                查询
              </a-button>
              <a-button @click="handleReset">
                <template #icon><ReloadOutlined /></template>
                重置
              </a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <!-- 操作栏 -->
      <div class="table-actions">
        <a-button type="primary" @click="openCreate">
          <template #icon><PlusOutlined /></template>
          新增
        </a-button>
      </div>

      <!-- 表格 -->
      <CommonTable
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="{ current: pageNum, pageSize, total, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` }"
        :scroll="{ x: 1600 }"
        :row-class-name="rowClassName"
        @change="handlePageChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="openEdit(record)">
                <template #icon><EditOutlined /></template>编辑
              </a-button>
              <a-popconfirm
                title="确定删除该记录吗？"
                ok-text="删除"
                cancel-text="取消"
                @confirm="handleDelete(record)"
              >
                <a-button type="link" size="small" danger>
                  <template #icon><DeleteOutlined /></template>删除
                </a-button>
              </a-popconfirm>
              <a-button
                type="link"
                size="small"
                v-if="record.status === 'DRAFT' || !record.status"
                @click="handleSubmitApproval(record)"
              >
                <template #icon><SendOutlined /></template>提交审批
              </a-button>
              <!-- 劳动力计划特有的操作 -->
              <template v-if="resourceType === 'labor'">
                <a-button
                  type="link"
                  size="small"
                  v-if="record.status !== 'DRAFT'"
                  @click="openChangeModal(record)"
                >
                  <template #icon><FormOutlined /></template>变更
                </a-button>
                <a-button
                  type="link"
                  size="small"
                  @click="openHistoryModal(record)"
                >
                  <template #icon><HistoryOutlined /></template>变更历史
                </a-button>
                <a-button
                  type="link"
                  size="small"
                  v-if="warningStatusMap[record.id]?.warningLevel"
                  @click="openWarningModal(record)"
                >
                  查看预警
                </a-button>
              </template>
            </a-space>
          </template>
        </template>
      </CommonTable>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="`${modalTitle}${pageTitle}`"
      @ok="handleSave"
      :confirm-loading="confirmLoading"
      :width="640"
      :destroy-on-close="true"
      ok-text="保存"
      cancel-text="取消"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="rules"
        :label-col="{ span: 7 }"
        :wrapper-col="{ span: 15 }"
      >
        <template v-for="field in config.formFields" :key="field.key">
          <a-form-item :label="field.label" :name="field.key">
            <a-select
              v-if="field.type === 'select'"
              v-model:value="formState[field.key]"
              :placeholder="field.placeholder || `请选择${field.label}`"
              show-search
              allow-clear
              :filter-option="(input: string, opt: any) => opt.label.toLowerCase().includes(input.toLowerCase())"
              style="width: 100%"
            >
              <a-select-option v-for="p in projectOptions" :key="p.value" :value="p.value">
                {{ p.label }}
              </a-select-option>
            </a-select>
            <a-input
              v-else-if="field.type === 'input'"
              v-model:value="formState[field.key]"
              :placeholder="field.placeholder || `请输入${field.label}`"
              allow-clear
            />
            <a-input-number
              v-else-if="field.type === 'number'"
              v-model:value="formState[field.key]"
              :placeholder="field.placeholder || `请输入${field.label}`"
              :min="0"
              style="width: 100%"
            />
            <a-date-picker
              v-else-if="field.type === 'date'"
              v-model:value="formState[field.key]"
              style="width: 100%"
              value-format="YYYY-MM-DD"
              :placeholder="`请选择${field.label}`"
            />
            <a-textarea
              v-else-if="field.type === 'textarea'"
              v-model:value="formState[field.key]"
              :placeholder="field.placeholder || `请输入${field.label}`"
              :rows="3"
            />
          </a-form-item>
        </template>
      </a-form>
    </a-modal>

    <!-- 变更申请弹窗 -->
    <a-modal
      v-model:open="changeModalVisible"
      title="劳动力变更申请"
      @ok="handleChangeSubmit"
      :confirm-loading="changeModalLoading"
      :width="640"
      :destroy-on-close="true"
      ok-text="提交"
      cancel-text="取消"
    >
      <a-form
        ref="changeFormRef"
        :model="changeFormState"
        :rules="changeRules"
        :label-col="{ span: 7 }"
        :wrapper-col="{ span: 15 }"
      >
        <a-form-item label="变更类型" name="changeType">
          <a-select
            v-model:value="changeFormState.changeType"
            placeholder="请选择变更类型"
            style="width: 100%"
          >
            <a-select-option v-for="ct in changeTypeOptions" :key="ct.value" :value="ct.value">
              {{ ct.label }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="变更原因" name="changeReason">
          <a-textarea
            v-model:value="changeFormState.changeReason"
            placeholder="请输入变更原因"
            :rows="3"
          />
        </a-form-item>

        <!-- 延期 / 修改日期：显示日期选择器 -->
        <template v-if="changeFormState.changeType === 'DELAY' || changeFormState.changeType === 'MODIFY_DATE'">
          <a-form-item label="新计划开始日期">
            <a-date-picker
              v-model:value="changeFormState.newPlanStartDate"
              style="width: 100%"
              value-format="YYYY-MM-DD"
              placeholder="请选择新开始日期"
            />
          </a-form-item>
          <a-form-item label="新计划结束日期">
            <a-date-picker
              v-model:value="changeFormState.newPlanEndDate"
              style="width: 100%"
              value-format="YYYY-MM-DD"
              placeholder="请选择新结束日期"
            />
          </a-form-item>
        </template>

        <!-- 修改人数：显示数字输入 -->
        <template v-if="changeFormState.changeType === 'MODIFY_QUANTITY'">
          <a-form-item label="新计划人数">
            <a-input-number
              v-model:value="changeFormState.newPlanQuantity"
              placeholder="请输入新计划人数"
              :min="0"
              style="width: 100%"
            />
          </a-form-item>
        </template>
      </a-form>
    </a-modal>

    <!-- 变更历史弹窗 -->
    <a-modal
      v-model:open="historyModalVisible"
      title="变更历史"
      :footer="null"
      :width="900"
      :destroy-on-close="true"
    >
      <a-table
        :dataSource="historyList"
        :loading="historyLoading"
        :pagination="false"
        :scroll="{ x: 900 }"
        rowKey="id"
        bordered
        size="middle"
      >
        <a-table-column title="变更类型" dataIndex="changeType" key="changeType" width="100">
          <template #default="{ text }">{{ changeTypeLabel(text) }}</template>
        </a-table-column>
        <a-table-column title="变更原因" dataIndex="changeReason" key="changeReason" ellipsis />
        <a-table-column title="新旧值对比" key="compare" width="280" ellipsis>
          <template #default="{ record }">{{ changeCompareText(record) }}</template>
        </a-table-column>
        <a-table-column title="审批状态" dataIndex="approvalStatus" key="approvalStatus" width="100">
          <template #default="{ text }">
            <a-tag :color="approvalStatusColor(text)">{{ approvalStatusText(text) }}</a-tag>
          </template>
        </a-table-column>
        <a-table-column title="申请人" dataIndex="applicantName" key="applicantName" width="100" />
        <a-table-column title="申请时间" dataIndex="createTime" key="createTime" width="160" />
        <a-table-column title="操作" key="action" width="150" fixed="right">
          <template #default="{ record: changeRecord }">
            <a-space v-if="changeRecord.approvalStatus === 'PENDING'">
              <a-button type="link" size="small" @click="handleApproveChange(changeRecord.id)">通过</a-button>
              <a-button type="link" size="small" danger @click="handleRejectChange(changeRecord.id)">驳回</a-button>
            </a-space>
            <span v-else>-</span>
          </template>
        </a-table-column>
      </a-table>
    </a-modal>

    <!-- 预警详情弹窗 -->
    <a-modal
      v-model:open="warningModalVisible"
      title="预警详情"
      :footer="null"
      :width="600"
      :destroy-on-close="true"
    >
      <a-descriptions :column="1" bordered size="small">
        <a-descriptions-item label="预警等级">
          <a-tag v-if="warningDetail.warningLevel === 'CRITICAL'" color="red">红色预警</a-tag>
          <a-tag v-else-if="warningDetail.warningLevel === 'WARNING'" color="orange">黄色预警</a-tag>
          <a-tag v-else-if="warningDetail.warningLevel === 'NORMAL'" color="green">绿色正常</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="预警消息">
          {{ warningDetail.warningMessage || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="建议">
          {{ warningDetail.suggestion || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="检查时间">
          {{ warningDetail.checkTime || '-' }}
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script lang="ts">
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  SendOutlined,
  FormOutlined,
  HistoryOutlined,
} from '@ant-design/icons-vue'
export default {
  components: {
    SearchOutlined,
    ReloadOutlined,
    PlusOutlined,
    EditOutlined,
    DeleteOutlined,
    SendOutlined,
    FormOutlined,
    HistoryOutlined,
  },
}
</script>

<style scoped>
.resource-plan-page {
  padding: 0;
}

.search-area {
  margin-bottom: 16px;
}

.search-form {
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

/* 响应式搜索表单：移动端改为垂直布局 */
@media (max-width: 767px) {
  .search-form :deep(.ant-form-item) {
    margin-bottom: 8px !important;
    flex: 0 0 100% !important;
    max-width: 100% !important;
  }
  .search-form :deep(.ant-select),
  .search-form :deep(.ant-input),
  .search-form :deep(.ant-picker) {
    width: 100% !important;
  }
}

.table-actions {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

/* 卡片样式优化 */
:deep(.ant-card) {
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

:deep(.ant-card-head) {
  padding: 0 20px;
  min-height: 52px;
}

:deep(.ant-card-body) {
  padding: 20px;
}

:deep(.row-critical) {
  background-color: #fff2f0 !important;
}

:deep(.row-warning) {
  background-color: #fffbe6 !important;
}

:deep(.row-odd) {
  background-color: #fafafa;
}

/* 表格响应式：确保横向滚动 */
:deep(.ant-table-wrapper) {
  overflow-x: auto;
}

/* 操作按钮优化 */
:deep(.ant-btn-link) {
  padding: 0 4px;
  height: auto;
}

</style>
