import request from '@/utils/request'
import type { R } from '@/types/api'

export interface ResourcePlanLabor {
  id?: number
  projectId: number
  wbsCode: string
  wbsName?: string
  workTypeCode: string
  workTypeName?: string
  laborCategoryCode?: string
  laborCategoryName?: string
  planQuantity?: number
  planStartDate?: string
  planEndDate?: string
  actualQuantity?: number
  actualStartDate?: string
  actualEndDate?: string
  status?: string
  approvalStatus?: string
  warningStatus?: string
  remark?: string
  deleted?: number
}

export interface ProgressDTO {
  actualQuantity?: number
  actualStartDate?: string
  actualEndDate?: string
  remark?: string
  attendanceRecords?: string
}

export interface LaborChangeDTO {
  planId?: number
  changeType?: string
  changeReason?: string
  changeDetails?: string
  newPlanStartDate?: string
  newPlanEndDate?: string
  newLaborCategoryCode?: string
  newLaborCategoryName?: string
}

export interface WarningStatusVO {
  warningLevel?: string
  warningMessage?: string
  suggestion?: string
  checkTime?: string
}

/**
 * 查询劳动力需求列表
 */
export function getLaborList(params: { 
  projectId?: number, 
  status?: string,
  warningStatus?: string,
  workTypeCode?: string,
  laborCategoryCode?: string 
}): Promise<R<ResourcePlanLabor[]>> {
  return request({
    url: '/v1/resource-plan/labor/list',
    method: 'get',
    params
  })
}

/**
 * 根据ID查询劳动力需求
 */
export function getLaborById(id: number): Promise<R<ResourcePlanLabor>> {
  return request({
    url: `/v1/resource-plan/labor/${id}`,
    method: 'get'
  })
}

/**
 * 新增劳动力需求
 */
export function createLabor(data: ResourcePlanLabor): Promise<R<boolean>> {
  return request({
    url: '/v1/resource-plan/labor',
    method: 'post',
    data
  })
}

/**
 * 修改劳动力需求
 */
export function updateLabor(id: number, data: ResourcePlanLabor): Promise<R<boolean>> {
  return request({
    url: `/v1/resource-plan/labor/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除劳动力需求
 */
export function deleteLabor(id: number): Promise<R<boolean>> {
  return request({
    url: `/v1/resource-plan/labor/${id}`,
    method: 'delete'
  })
}

/**
 * 提交审批
 */
export function submitLabor(id: number): Promise<R<boolean>> {
  return request({
    url: `/v1/resource-plan/labor/${id}/submit`,
    method: 'post'
  })
}

/**
 * 登记进展
 */
export function registerProgress(id: number, data: ProgressDTO): Promise<R<boolean>> {
  return request({
    url: `/v1/resource-plan/labor/${id}/progress`,
    method: 'post',
    data
  })
}

/**
 * 查询预警状态
 */
export function getWarningStatus(id: number): Promise<R<WarningStatusVO>> {
  return request({
    url: `/v1/resource-plan/labor/${id}/warning-status`,
    method: 'get'
  })
}

/**
 * 查询变更历史
 */
export function getChangeHistory(id: number): Promise<R<any[]>> {
  return request({
    url: `/v1/resource-plan/labor/${id}/change-history`,
    method: 'get'
  })
}

/**
 * 发起变更申请
 */
export function createChange(data: LaborChangeDTO): Promise<R<number>> {
  return request({
    url: '/v1/resource-plan/labor/change',
    method: 'post',
    data
  })
}
