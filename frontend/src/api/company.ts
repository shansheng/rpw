import request from '@/utils/request'
import type { R } from '@/types/api'

/**
 * 公司管理 API 接口
 */

export interface Company {
  id?: number
  companyName: string
  orgId?: number
  createTime?: string
  updateTime?: string
  deleted?: number
}

/**
 * 查询公司列表
 * GET /api/v1/company/list
 * @param params 查询参数（可选orgId）
 * @returns 公司列表
 */
export function getCompanyList(params?: { orgId?: number }): Promise<R<Company[]>> {
  return request({
    url: '/v1/company/list',
    method: 'get',
    params
  })
}

/**
 * 根据ID查询公司
 * GET /api/v1/company/{id}
 * @param id 公司ID
 * @returns 公司详情
 */
export function getCompanyById(id: number): Promise<R<Company>> {
  return request({
    url: `/v1/company/${id}`,
    method: 'get'
  })
}

/**
 * 新增公司
 * POST /api/v1/company
 * @param data 公司信息
 * @returns 是否成功
 */
export function createCompany(data: Company): Promise<R<boolean>> {
  return request({
    url: '/v1/company',
    method: 'post',
    data
  })
}

/**
 * 修改公司
 * PUT /api/v1/company/{id}
 * @param id 公司ID
 * @param data 公司信息
 * @returns 是否成功
 */
export function updateCompany(id: number, data: Company): Promise<R<boolean>> {
  return request({
    url: `/v1/company/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除公司
 * DELETE /api/v1/company/{id}
 * @param id 公司ID
 * @returns 是否成功
 */
export function deleteCompany(id: number): Promise<R<boolean>> {
  return request({
    url: `/v1/company/${id}`,
    method: 'delete'
  })
}
