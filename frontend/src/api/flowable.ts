import request from '@/utils/request'

/**
 * 流程定义管理
 */

// 部署流程定义
export function deployProcessDefinition(data: FormData) {
  return request({
    url: '/v1/flowable/process-definition/deploy',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 获取流程定义XML
export function getProcessDefinitionXml(processDefinitionId: string) {
  return request({
    url: `/v1/flowable/process-definition/${processDefinitionId}/xml`,
    method: 'get'
  })
}

// 保存流程定义到localStorage（前端本地保存）
export function saveProcessDefinition(key: string, xml: string) {
  localStorage.setItem('bpmn_xml_' + key, xml)
  return Promise.resolve({ code: 200, message: 'saved', data: true })
}

// 查询流程定义列表
export function listProcessDefinitions() {
  return request({
    url: '/v1/flowable/process-definition/list',
    method: 'get'
  })
}

// 删除流程定义
export function deleteProcessDefinition(deploymentId: string) {
  return request({
    url: `/v1/flowable/process-definition/${deploymentId}`,
    method: 'delete'
  })
}

/**
 * 流程实例管理
 */

// 启动流程实例
export function startProcessInstance(data: any) {
  return request({
    url: '/v1/flowable/process-instance/start',
    method: 'post',
    data
  })
}

// 查询流程实例
export function getProcessInstance(processInstanceId: string) {
  return request({
    url: `/v1/flowable/process-instance/${processInstanceId}`,
    method: 'get'
  })
}

// 终止流程实例
export function deleteProcessInstance(processInstanceId: string) {
  return request({
    url: `/v1/flowable/process-instance/${processInstanceId}`,
    method: 'delete'
  })
}

// 挂起流程实例
export function suspendProcessInstance(processInstanceId: string) {
  return request({
    url: `/v1/flowable/process-instance/suspend/${processInstanceId}`,
    method: 'post'
  })
}

// 激活流程实例
export function activateProcessInstance(processInstanceId: string) {
  return request({
    url: `/v1/flowable/process-instance/activate/${processInstanceId}`,
    method: 'post'
  })
}

/**
 * 任务管理
 */

// 查询待办任务
export function getTaskList(params: any) {
  return request({
    url: '/v1/flowable/task/list',
    method: 'get',
    params
  })
}

// 查询候选任务
export function getCandidateTaskList(params: any) {
  return request({
    url: '/v1/flowable/task/candidate-list',
    method: 'get',
    params
  })
}

// 查询历史任务
export function getHistoryTasks(params: any) {
  return request({
    url: '/v1/flowable/task/history',
    method: 'get',
    params
  })
}

// 完成任务
export function completeTask(taskId: string, data?: any) {
  return request({
    url: `/v1/flowable/task/${taskId}/complete`,
    method: 'post',
    data
  })
}

// 认领任务
export function claimTask(taskId: string, userId: string) {
  return request({
    url: `/v1/flowable/task/${taskId}/claim`,
    method: 'post',
    params: { userId }
  })
}

// 转派任务
export function assignTask(taskId: string, userId: string) {
  return request({
    url: `/v1/flowable/task/${taskId}/assign`,
    method: 'post',
    params: { userId }
  })
}

// 查询任务详情
export function getTask(taskId: string) {
  return request({
    url: `/v1/flowable/task/${taskId}`,
    method: 'get'
  })
}

/**
 * 后台干预
 */

// 任务跳转
export function jumpTask(taskId: string, activityId: string) {
  return request({
    url: `/v1/flowable/admin/jump/${taskId}`,
    method: 'post',
    params: { activityId }
  })
}

// 查询所有流程实例（后台管理用）
export function getAllProcessInstances() {
  return request({
    url: '/v1/flowable/admin/process-instances',
    method: 'get'
  })
}

// 查询流程实例历史
export function getProcessInstanceHistory(processInstanceId: string) {
  return request({
    url: `/v1/flowable/admin/history/${processInstanceId}`,
    method: 'get'
  })
}

/**
 * 物资计划审批
 */

// 提交审批
export function submitApproval(planId: number) {
  return request({
    url: '/v1/resource-plan/material/submit-approval',
    method: 'post',
    params: { planId }
  })
}

// 审批通过
export function approve(planId: number, comment?: string) {
  return request({
    url: '/v1/resource-plan/material/approve',
    method: 'post',
    params: { planId, comment }
  })
}

// 审批驳回
export function reject(planId: number, comment: string) {
  return request({
    url: '/v1/resource-plan/material/reject',
    method: 'post',
    params: { planId, comment }
  })
}

// 查询审批历史
export function getApprovalHistory(planId: number) {
  return request({
    url: `/v1/resource-plan/material/approval-history/${planId}`,
    method: 'get'
  })
}

// 获取当前待办任务
export function getCurrentTask(planId: number) {
  return request({
    url: `/v1/resource-plan/material/current-task/${planId}`,
    method: 'get'
  })
}
