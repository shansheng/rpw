import request from '@/utils/request'
import type { R } from '@/types/api'

/**
 * 办公用品计划管理 API 接口
 */

export interface ResourcePlanOffice {
  id?: number
  projectId?: number
  wbsCode: string
  officeName: string
  specification?: string
  unit?: string
  budgetQuantity?: number
  supplierCode?: string
  purchaseSourceCode?: string
  purchaseProgressCode?: string
  shippingProgressCode?: string
  planArrivalDate?: string
  actualArrivalDate?: string
  remark?: string
  createTime?: string
  updateTime?: string
  deleted?: number
}

/**
 * 查询办公用品计划列表
 * GET /api/v1/resource-plan/office/list
 * @param params 查询参数（可选projectId）
 * @returns 办公用品计划列表
 */
export function getResourcePlanOfficeList(params?: { projectId?: number }): Promise<R<ResourcePlanOffice[]>> {
  return request({
    url: '/v1/resource-plan/office/list',
    method: 'get',
    params
  })
}

/**
 * 根据ID查询办公用品计划
 * GET /api/v1/resource-plan/office/{id}
 * @param id 办公用品计划ID
 * @returns 办公用品计划详情
 */
export function getResourcePlanOfficeById(id: number): Promise<R<ResourcePlanOffice>> {
  return request({
    url: `/v1/resource-plan/office/${id}`,
    method: 'get'
  })
}

/**
 * 新增办公用品计划
 * POST /api/v1/resource-plan/office
 * @param data 办公用品计划信息
 * @returns 是否成功
 */
export function createResourcePlanOffice(data: ResourcePlanOffice): Promise<R<boolean>> {
  return request({
    url: '/v1/resource-plan/office',
    method: 'post',
    data
  })
}

/**
 * 修改办公用品计划
 * PUT /api/v1/resource-plan/office/{id}
 * @param id 办公用品计划ID
 * @param data 办公用品计划信息
 * @returns 是否成功
 */
export function updateResourcePlanOffice(id: number, data: ResourcePlanOffice): Promise<R<boolean>> {
  return request({
    url: `/v1/resource-plan/office/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除办公用品计划
 * DELETE /api/v1/resource-plan/office/{id}
 * @param id 办公用品计划ID
 * @returns 是否成功
 */
export function deleteResourcePlanOffice(id: number): Promise<R<boolean>> {
  return request({
    url: `/v1/resource-plan/office/${id}`,
    method: 'delete'
  })
}
