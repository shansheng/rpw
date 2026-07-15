import request from '@/utils/request'
import type { R } from '@/types/api'

// ==================== 类型定义 ====================

export interface WarningRule {
  id?: number
  ruleName: string
  resourceType: string
  warningType: string
  thresholdValue: number
  comparisonOperator: string
  advanceDays?: number
  enabled: number
  notifyWecom?: number
  notifyUsers?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface WarningRecord {
  id?: number
  ruleId?: number
  ruleName?: string
  projectId?: number
  projectName?: string
  resourceType?: string
  warningType?: string
  actualValue?: number
  thresholdValue?: number
  warningLevel?: string
  status?: string
  notifyStatus?: string
  notifyTime?: string
  createTime?: string
}

export interface WarningStats {
  total: number
  pending: number
  processing: number
  resolved: number
  ignored: number
}

// ==================== 预警规则 API ====================

/**
 * 查询预警规则列表
 * GET /api/v1/warning/rule/list
 */
export function getWarningRuleList(params?: {
  resourceType?: string
  warningType?: string
}): Promise<R<WarningRule[]>> {
  return request({
    url: '/v1/warning/rule/list',
    method: 'get',
    params
  })
}

/**
 * 根据ID查询预警规则
 * GET /api/v1/warning/rule/{id}
 */
export function getWarningRuleById(id: number): Promise<R<WarningRule>> {
  return request({
    url: `/v1/warning/rule/${id}`,
    method: 'get'
  })
}

/**
 * 新增预警规则
 * POST /api/v1/warning/rule
 */
export function createWarningRule(data: WarningRule): Promise<R<boolean>> {
  return request({
    url: '/v1/warning/rule',
    method: 'post',
    data
  })
}

/**
 * 修改预警规则
 * PUT /api/v1/warning/rule/{id}
 */
export function updateWarningRule(id: number, data: WarningRule): Promise<R<boolean>> {
  return request({
    url: `/v1/warning/rule/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除预警规则
 * DELETE /api/v1/warning/rule/{id}
 */
export function deleteWarningRule(id: number): Promise<R<boolean>> {
  return request({
    url: `/v1/warning/rule/${id}`,
    method: 'delete'
  })
}

// ==================== 预警记录 API ====================

/**
 * 查询预警记录列表
 * GET /api/v1/warning/record/list
 */
export function getWarningRecordList(params?: {
  projectId?: number
  ruleId?: number
  status?: string
  warningLevel?: string
  startDate?: string
  endDate?: string
}): Promise<R<WarningRecord[]>> {
  return request({
    url: '/v1/warning/record/list',
    method: 'get',
    params
  })
}

/**
 * 获取预警统计
 * GET /api/v1/warning/record/statistics
 */
export function getWarningStats(params?: {
  projectId?: number
  startDate?: string
  endDate?: string
}): Promise<R<WarningStats>> {
  return request({
    url: '/v1/warning/record/statistics',
    method: 'get',
    params
  })
}

/**
 * 更新预警记录状态
 * PUT /api/v1/warning/record/{id}/status
 */
export function updateWarningRecordStatus(id: number, status: string): Promise<R<boolean>> {
  return request({
    url: `/v1/warning/record/${id}/status`,
    method: 'put',
    params: { status }
  })
}

// ==================== 预警检查 API ====================

/**
 * 手动触发预警检查
 * POST /api/v1/warning/check
 */
export function triggerWarningCheck(params?: {
  projectId?: number
  ruleId?: number
}): Promise<R<string>> {
  return request({
    url: '/v1/warning/check',
    method: 'post',
    params
  })
}
