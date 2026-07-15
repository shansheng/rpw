import request from '@/utils/request'
import type { R } from '@/types/api'

/**
 * 组织架构 API 接口
 */

export interface Organization {
  id?: number
  orgName: string
  orgLevel: number
  parentId?: number
  department?: string
  section?: string
  children?: Organization[]
  createTime?: string
  updateTime?: string
  deleted?: number
}

/**
 * 查询组织列表
 * GET /api/v1/organization/list
 * @param params 查询参数（可选orgLevel）
 * @returns 组织列表
 */
export function getOrganizationList(params?: { orgLevel?: number }): Promise<R<Organization[]>> {
  return request({
    url: '/v1/organization/list',
    method: 'get',
    params
  })
}

/**
 * 查询组织树形结构
 * GET /api/v1/organization/tree
 * @returns 树形结构
 */
export function getOrganizationTree(): Promise<R<Organization[]>> {
  return request({
    url: '/v1/organization/tree',
    method: 'get'
  })
}

/**
 * 根据ID查询组织
 * GET /api/v1/organization/{id}
 * @param id 组织ID
 * @returns 组织详情
 */
export function getOrganizationById(id: number): Promise<R<Organization>> {
  return request({
    url: `/v1/organization/${id}`,
    method: 'get'
  })
}

/**
 * 新增组织
 * POST /api/v1/organization
 * @param data 组织信息
 * @returns 是否成功
 */
export function createOrganization(data: Organization): Promise<R<boolean>> {
  return request({
    url: '/v1/organization',
    method: 'post',
    data
  })
}

/**
 * 修改组织
 * PUT /api/v1/organization/{id}
 * @param id 组织ID
 * @param data 组织信息
 * @returns 是否成功
 */
export function updateOrganization(id: number, data: Organization): Promise<R<boolean>> {
  return request({
    url: `/v1/organization/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除组织
 * DELETE /api/v1/organization/{id}
 * @param id 组织ID
 * @returns 是否成功
 */
export function deleteOrganization(id: number): Promise<R<boolean>> {
  return request({
    url: `/v1/organization/${id}`,
    method: 'delete'
  })
}
