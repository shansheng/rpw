import request from './request'
import type { R, PageResult, PageParams } from '@/types/api'

export interface WarningRule {
  id: number
  ruleName: string
  resourceType?: string
  projectId?: number
  thresholdType?: string
  warningThreshold?: number
  compareField?: string
  actualField?: string
  warningLevel?: string
  checkFrequency?: string
  enabled?: number
  notifyWecom?: number
  notifyUsers?: string
  description?: string
  createTime?: string
  updateTime?: string
}

export interface WarningRecord {
  id: number
  ruleId?: number
  resourceType?: string
  projectId?: number
  resourceId?: number
  warningLevel?: string
  warningMessage?: string
  planValue?: number
  actualValue?: number
  deviationRate?: number
  status?: string
  handledBy?: number
  handleTime?: string
  handleRemark?: string
  notifyStatus?: string
  wecomMsgId?: string
  triggeredTime?: string
}

export interface WarningStatistics {
  total: number
  pending: number
  handled: number
  byLevel?: Record<string, number>
  trend?: Array<{ date: string; count: number }>
}

// ---- 规则 CRUD ----
export function getRuleList(params?: PageParams & { name?: string }): Promise<R<PageResult<WarningRule>>> {
  return request.get('/v1/warning/rule/list', { params })
}

export function createRule(data: Partial<WarningRule>): Promise<R<WarningRule>> {
  return request.post('/v1/warning/rule', data)
}

export function updateRule(id: number, data: Partial<WarningRule>): Promise<R<WarningRule>> {
  return request.put(`/v1/warning/rule/${id}`, data)
}

export function deleteRule(id: number): Promise<R<any>> {
  return request.delete(`/v1/warning/rule/${id}`)
}

// ---- 预警记录 ----
export function getRecordList(params?: PageParams & { status?: string; warningLevel?: string }): Promise<R<PageResult<WarningRecord>>> {
  return request.get('/v1/warning/record/list', { params })
}

export function handleRecord(id: number, status: string, handleRemark?: string): Promise<R<any>> {
  return request.put(`/v1/warning/record/${id}/handle`, null, {
    params: { status, handleRemark: handleRemark || undefined }
  })
}

// ---- 预警统计 ----
export function getStatistics(params?: { projectId?: number }): Promise<R<WarningStatistics>> {
  return request.get('/v1/warning/statistics', { params })
}
