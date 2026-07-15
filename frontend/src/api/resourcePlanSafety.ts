import request from '@/utils/request'
import type { R } from '@/types/api'

/**
 * 安全物资计划管理 API 接口
 */

export interface ResourcePlanSafety {
  id?: number
  projectId?: number
  wbsCode: string
  safetyName: string
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
 * 查询安全物资计划列表
 * GET /api/v1/resource-plan/safety/list
 * @param params 查询参数（可选projectId）
 * @returns 安全物资计划列表
 */
export function getResourcePlanSafetyList(params?: { projectId?: number }): Promise<R<ResourcePlanSafety[]>> {
  return request({
    url: '/v1/resource-plan/safety/list',
    method: 'get',
    params
  })
}

/**
 * 根据ID查询安全物资计划
 * GET /api/v1/resource-plan/safety/{id}
 * @param id 安全物资计划ID
 * @returns 安全物资计划详情
 */
export function getResourcePlanSafetyById(id: number): Promise<R<ResourcePlanSafety>> {
  return request({
    url: `/v1/resource-plan/safety/${id}`,
    method: 'get'
  })
}

/**
 * 新增安全物资计划
 * POST /api/v1/resource-plan/safety
 * @param data 安全物资计划信息
 * @returns 是否成功
 */
export function createResourcePlanSafety(data: ResourcePlanSafety): Promise<R<boolean>> {
  return request({
    url: '/v1/resource-plan/safety',
    method: 'post',
    data
  })
}

/**
 * 修改安全物资计划
 * PUT /api/v1/resource-plan/safety/{id}
 * @param id 安全物资计划ID
 * @param data 安全物资计划信息
 * @returns 是否成功
 */
export function updateResourcePlanSafety(id: number, data: ResourcePlanSafety): Promise<R<boolean>> {
  return request({
    url: `/v1/resource-plan/safety/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除安全物资计划
 * DELETE /api/v1/resource-plan/safety/{id}
 * @param id 安全物资计划ID
 * @returns 是否成功
 */
export function deleteResourcePlanSafety(id: number): Promise<R<boolean>> {
  return request({
    url: `/v1/resource-plan/safety/${id}`,
    method: 'delete'
  })
}
