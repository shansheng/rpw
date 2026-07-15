import request from '@/utils/request'
import type { R } from '@/types/api'

/**
 * 设备计划管理 API 接口
 */

export interface ResourcePlanEquipment {
  id?: number
  projectId?: number
  wbsCode: string
  equipmentName: string
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
 * 查询设备计划列表
 * GET /api/v1/resource-plan/equipment/list
 * @param params 查询参数（可选projectId）
 * @returns 设备计划列表
 */
export function getResourcePlanEquipmentList(params?: { projectId?: number }): Promise<R<ResourcePlanEquipment[]>> {
  return request({
    url: '/v1/resource-plan/equipment/list',
    method: 'get',
    params
  })
}

/**
 * 根据ID查询设备计划
 * GET /api/v1/resource-plan/equipment/{id}
 * @param id 设备计划ID
 * @returns 设备计划详情
 */
export function getResourcePlanEquipmentById(id: number): Promise<R<ResourcePlanEquipment>> {
  return request({
    url: `/v1/resource-plan/equipment/${id}`,
    method: 'get'
  })
}

/**
 * 新增设备计划
 * POST /api/v1/resource-plan/equipment
 * @param data 设备计划信息
 * @returns 是否成功
 */
export function createResourcePlanEquipment(data: ResourcePlanEquipment): Promise<R<boolean>> {
  return request({
    url: '/v1/resource-plan/equipment',
    method: 'post',
    data
  })
}

/**
 * 修改设备计划
 * PUT /api/v1/resource-plan/equipment/{id}
 * @param id 设备计划ID
 * @param data 设备计划信息
 * @returns 是否成功
 */
export function updateResourcePlanEquipment(id: number, data: ResourcePlanEquipment): Promise<R<boolean>> {
  return request({
    url: `/v1/resource-plan/equipment/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除设备计划
 * DELETE /api/v1/resource-plan/equipment/{id}
 * @param id 设备计划ID
 * @returns 是否成功
 */
export function deleteResourcePlanEquipment(id: number): Promise<R<boolean>> {
  return request({
    url: `/v1/resource-plan/equipment/${id}`,
    method: 'delete'
  })
}
