import request from './request'
import type { R, PageResult, PageParams } from '@/types/api'

export interface FlowTask {
  id: string
  taskId?: string
  name: string
  processInstanceId?: string
  processDefinitionId?: string
  assignee?: string
  createTime?: string
  dueDate?: string
  status?: string
  businessKey?: string
  businessInfo?: {
    businessType: string
    businessId: number
    businessName: string
  }
  comment?: string
}

export interface ProcessDefinition {
  id: string
  name: string
  key: string
  version: number
  deploymentId?: string
  description?: string
  deployTime?: string
  suspended?: boolean
}

export interface ProcessInstance {
  id: string
  processInstanceId?: string
  processDefinitionId?: string
  processDefinitionKey?: string
  businessKey?: string
  startUserId?: string
  startTime?: string
  endTime?: string
  status?: string
  finished?: boolean
  suspended?: boolean
}

// ---- 待办任务 ----
export function getTodoList(params?: { userId?: string }): Promise<R<FlowTask[]>> {
  return request.get('/v1/flowable/task/todo', { params })
}

export function getTaskList(params?: { assignee?: string; candidateUser?: string; candidateGroup?: string }): Promise<R<FlowTask[]>> {
  return request.get('/v1/flowable/task/list', { params })
}

export function claimTask(taskId: string, userId?: string): Promise<R<any>> {
  return request.post(`/v1/flowable/task/${taskId}/claim`, null, { params: { userId } })
}

export function completeTask(taskId: string, variables?: Record<string, any>): Promise<R<any>> {
  return request.post(`/v1/flowable/task/${taskId}/complete`, variables)
}

// ---- 我发起的 ----
export function getMyInitiated(params?: { initiator?: string }): Promise<R<ProcessInstance[]>> {
  return request.get('/v1/flowable/process-instance/my', { params })
}

// ---- 历史（审批记录） ----
export function getHistoryList(params?: { assignee?: string }): Promise<R<any[]>> {
  return request.get('/v1/flowable/task/history', { params })
}

// ---- 流程定义 ----
export function getProcessDefinitionList(params?: { key?: string }): Promise<R<ProcessDefinition[]>> {
  return request.get('/v1/flowable/process-definition/list', { params })
}

export function getProcessDefinitionXml(processDefinitionId: string): Promise<R<string>> {
  return request.get(`/v1/flowable/process-definition/${processDefinitionId}/xml`)
}

export function deployProcessDefinition(formData: FormData): Promise<R<any>> {
  return request.post('/v1/flowable/process-definition/deploy', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function deployProcessByXml(xml: string, name?: string, deploymentName?: string): Promise<R<string>> {
  return request.post('/v1/flowable/process-definition/deploy/xml', { xml, name, deploymentName })
}

export function deleteProcessDefinition(deploymentId: string, cascade?: boolean): Promise<R<any>> {
  return request.delete(`/v1/flowable/process-definition/${deploymentId}`, { params: { cascade } })
}

// ---- 流程实例管理 ----
export function startProcessInstance(data: {
  processDefinitionKey: string
  businessKey?: string
  variables?: Record<string, any>
}): Promise<R<string>> {
  return request.post('/v1/flowable/process-instance/start', data)
}

export function getProcessInstance(processInstanceId: string): Promise<R<ProcessInstance>> {
  return request.get(`/v1/flowable/process-instance/${processInstanceId}`)
}

export function deleteProcessInstance(instanceId: string, reason?: string): Promise<R<any>> {
  return request.delete(`/v1/flowable/process-instance/${instanceId}`, { params: { deleteReason: reason } })
}

/** 获取流程实例列表 */
export function getProcessInstanceList(params?: { 
  processDefinitionKey?: string
  businessKey?: string
  finished?: boolean 
}): Promise<R<ProcessInstance[]>> {
  return request.get('/v1/flowable/process-instance/list', { params })
}

/** 挂起流程实例 */
export function suspendProcessInstance(instanceId: string): Promise<R<any>> {
  return request.put(`/v1/flowable/process-instance/${instanceId}/suspend`)
}

/** 激活流程实例 */
export function activateProcessInstance(instanceId: string): Promise<R<any>> {
  return request.put(`/v1/flowable/process-instance/${instanceId}/activate`)
}

// ---- 审批历史详情 ----
export function getApprovalHistory(taskId: string): Promise<R<any[]>> {
  return request.get(`/v1/flowable/task/${taskId}/approval-history`)
}

// ---- 流程实例详情 ----
export function getProcessInstanceDetail(id: string): Promise<R<any>> {
  return request.get(`/v1/flowable/process-instance/${id}/detail`)
}

/** 获取流程实例流程图（含高亮数据） */
export function getProcessInstanceDiagram(instanceId: string): Promise<R<{
  bpmnXml: string
  completedActivityIds: string[]
  activeActivityIds: string[]
  completedFlowIds: string[]
}>> {
  return request.get(`/v1/flowable/process-instance/${instanceId}/diagram`)
}
