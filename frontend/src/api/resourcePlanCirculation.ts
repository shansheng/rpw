import request from '@/utils/request'
import type { R } from '@/types/api'

/**
 * 周转材计划管理 API 接口
 */

export interface ResourcePlanCirculation {
  id?: number
  projectId?: number
  wbsCode: string
  circulationName: string
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
 * 查询周转材计划列表
 * GET /api/v1/resource-plan/circulation/list
 * @param params 查询参数（可选projectId）
 * @returns 周转材计划列表
 */
export function getResourcePlanCirculationList(params?: { projectId?: number }): Promise<R<ResourcePlanCirculation[]>> {
  return request({
    url: '/v1/resource-plan/circulation/list',
    method: 'get',
    params
  })
}

/**
 * 根据ID查询周转材计划
 * GET /api/v1/resource-plan/circulation/{id}
 * @param id 周转材计划ID
 * @returns 周转材计划详情
 */
export function getResourcePlanCirculationById(id: number): Promise<R<ResourcePlanCirculation>> {
  return request({
    url: `/v1/resource-plan/circulation/${id}`,
    method: 'get'
  })
}

/**
 * 新增周转材计划
 * POST /api/v1/resource-plan/circulation
 * @param data 周转材计划信息
 * @returns 是否成功
 */
export function createResourcePlanCirculation(data: ResourcePlanCirculation): Promise<R<boolean>> {
  return request({
    url: '/v1/resource-plan/circulation',
    method: 'post',
    data
  })
}

/**
 * 修改周转材计划
 * PUT /api/v1/resource-plan/circulation/{id}
 * @param id 周转材计划ID
 * @param data 周转材计划信息
 * @returns 是否成功
 */
export function updateResourcePlanCirculation(id: number, data: ResourcePlanCirculation): Promise<R<boolean>> {
  return request({
    url: `/v1/resource-plan/circulation/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除周转材计划
 * DELETE /api/v1/resource-plan/circulation/{id}
 * @param id 周转材计划ID
 * @returns 是否成功
 */
export function deleteResourcePlanCirculation(id: number): Promise<R<boolean>> {
  return request({
    url: `/v1/resource-plan/circulation/${id}`,
    method: 'delete'
  })
}
