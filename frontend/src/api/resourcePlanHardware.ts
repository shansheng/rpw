import request from '@/utils/request'
import type { R } from '@/types/api'

/**
 * 五金计划管理 API 接口
 */

export interface ResourcePlanHardware {
  id?: number
  projectId?: number
  wbsCode: string
  hardwareName: string
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
 * 查询五金计划列表
 * GET /api/v1/resource-plan/hardware/list
 * @param params 查询参数（可选projectId）
 * @returns 五金计划列表
 */
export function getResourcePlanHardwareList(params?: { projectId?: number }): Promise<R<ResourcePlanHardware[]>> {
  return request({
    url: '/v1/resource-plan/hardware/list',
    method: 'get',
    params
  })
}

/**
 * 根据ID查询五金计划
 * GET /api/v1/resource-plan/hardware/{id}
 * @param id 五金计划ID
 * @returns 五金计划详情
 */
export function getResourcePlanHardwareById(id: number): Promise<R<ResourcePlanHardware>> {
  return request({
    url: `/v1/resource-plan/hardware/${id}`,
    method: 'get'
  })
}

/**
 * 新增五金计划
 * POST /api/v1/resource-plan/hardware
 * @param data 五金计划信息
 * @returns 是否成功
 */
export function createResourcePlanHardware(data: ResourcePlanHardware): Promise<R<boolean>> {
  return request({
    url: '/v1/resource-plan/hardware',
    method: 'post',
    data
  })
}

/**
 * 修改五金计划
 * PUT /api/v1/resource-plan/hardware/{id}
 * @param id 五金计划ID
 * @param data 五金计划信息
 * @returns 是否成功
 */
export function updateResourcePlanHardware(id: number, data: ResourcePlanHardware): Promise<R<boolean>> {
  return request({
    url: `/v1/resource-plan/hardware/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除五金计划
 * DELETE /api/v1/resource-plan/hardware/{id}
 * @param id 五金计划ID
 * @returns 是否成功
 */
export function deleteResourcePlanHardware(id: number): Promise<R<boolean>> {
  return request({
    url: `/v1/resource-plan/hardware/${id}`,
    method: 'delete'
  })
}
