import request from '@/utils/request'
import type { R } from '@/types/api'

export interface ResourcePlanSubcontract {
  id?: number
  projectId: number
  wbsCode: string
  wbsName?: string
  subcontractName: string
  workContent?: string
  supplierCode?: string
  supplierName?: string
  planStartDate?: string
  planEndDate?: string
  actualStartDate?: string
  actualEndDate?: string
  status?: string
  approvalStatus?: string
  processInstanceId?: string
  remark?: string
  deleted?: number
}

export interface ProgressDTO {
  actualStartDate?: string
  actualEndDate?: string
  remark?: string
}

export interface ChangeDTO {
  planId: number
  changeType: string
  changeReason: string
  changeDetails?: string
  newPlanStartDate?: string
  newPlanEndDate?: string
  newSupplierCode?: string
  newSupplierName?: string
}

export interface ChangeRecordVO {
  id?: number
  changeType?: string
  changeContent?: string
  applicantName?: string
  applyTime?: string
  approvalStatus?: string
}

/**
 * 查询分包计划列表
 */
export function getSubcontractList(params: { 
  projectId?: number, 
  status?: string,
  wbsCode?: string,
  subcontractName?: string 
}): Promise<R<ResourcePlanSubcontract[]>> {
  return request({
    url: '/v1/resource-plan/subcontract/list',
    method: 'get',
    params
  })
}

/**
 * 根据ID查询分包计划
 */
export function getSubcontractById(id: number): Promise<R<ResourcePlanSubcontract>> {
  return request({
    url: `/v1/resource-plan/subcontract/${id}`,
    method: 'get'
  })
}

/**
 * 新增分包计划
 */
export function createSubcontract(data: ResourcePlanSubcontract): Promise<R<boolean>> {
  return request({
    url: '/v1/resource-plan/subcontract',
    method: 'post',
    data
  })
}

/**
 * 修改分包计划
 */
export function updateSubcontract(id: number, data: ResourcePlanSubcontract): Promise<R<boolean>> {
  return request({
    url: `/v1/resource-plan/subcontract/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除分包计划
 */
export function deleteSubcontract(id: number): Promise<R<boolean>> {
  return request({
    url: `/v1/resource-plan/subcontract/${id}`,
    method: 'delete'
  })
}

/**
 * 提交审批
 */
export function submitSubcontract(id: number): Promise<R<boolean>> {
  return request({
    url: `/v1/resource-plan/subcontract/${id}/submit`,
    method: 'post'
  })
}

/**
 * 登记进展
 */
export function registerProgress(id: number, data: ProgressDTO): Promise<R<boolean>> {
  return request({
    url: `/v1/resource-plan/subcontract/${id}/progress`,
    method: 'post',
    data
  })
}

/**
 * 发起变更申请
 */
export function createChange(data: ChangeDTO): Promise<R<number>> {
  return request({
    url: '/v1/resource-plan/subcontract/change',
    method: 'post',
    data
  })
}

/**
 * 查询变更历史
 */
export function getChangeHistory(id: number): Promise<R<ChangeRecordVO[]>> {
  return request({
    url: `/v1/resource-plan/subcontract/${id}/change-history`,
    method: 'get'
  })
}
