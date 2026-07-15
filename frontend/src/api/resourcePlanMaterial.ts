import request from '@/utils/request'
import type { R } from '@/types/api'

/**
 * 材料计划管理 API 接口
 */

export interface ResourcePlanMaterial {
  id?: number
  projectId?: number
  wbsCode: string
  resourceName: string
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
 * 查询材料计划列表
 * GET /api/v1/resource-plan/material/list
 * @param params 查询参数（可选projectId）
 * @returns 材料计划列表
 */
export function getResourcePlanMaterialList(params?: { projectId?: number }): Promise<R<ResourcePlanMaterial[]>> {
  return request({
    url: '/v1/resource-plan/material/list',
    method: 'get',
    params
  })
}

/**
 * 根据ID查询材料计划
 * GET /api/v1/resource-plan/material/{id}
 * @param id 材料计划ID
 * @returns 材料计划详情
 */
export function getResourcePlanMaterialById(id: number): Promise<R<ResourcePlanMaterial>> {
  return request({
    url: `/v1/resource-plan/material/${id}`,
    method: 'get'
  })
}

/**
 * 新增材料计划
 * POST /api/v1/resource-plan/material
 * @param data 材料计划信息
 * @returns 是否成功
 */
export function createResourcePlanMaterial(data: ResourcePlanMaterial): Promise<R<boolean>> {
  return request({
    url: '/v1/resource-plan/material',
    method: 'post',
    data
  })
}

/**
 * 修改材料计划
 * PUT /api/v1/resource-plan/material/{id}
 * @param id 材料计划ID
 * @param data 材料计划信息
 * @returns 是否成功
 */
export function updateResourcePlanMaterial(id: number, data: ResourcePlanMaterial): Promise<R<boolean>> {
  return request({
    url: `/v1/resource-plan/material/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除材料计划
 * DELETE /api/v1/resource-plan/material/{id}
 * @param id 材料计划ID
 * @returns 是否成功
 */
export function deleteResourcePlanMaterial(id: number): Promise<R<boolean>> {
  return request({
    url: `/v1/resource-plan/material/${id}`,
    method: 'delete'
  })
}
