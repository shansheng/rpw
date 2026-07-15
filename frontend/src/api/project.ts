import request from '@/utils/request'
import type { R } from '@/types/api'

/**
 * 项目管理 API 接口
 */

export interface Project {
  id?: number
  projectName: string
  companyId?: number
  status?: number
  planStartDate?: string
  planEndDate?: string
  createTime?: string
  updateTime?: string
  deleted?: number
}

/**
 * 查询项目列表
 * GET /api/v1/project/list
 * @param params 查询参数（可选companyId、status）
 * @returns 项目列表
 */
export function getProjectList(params?: { companyId?: number; status?: number }): Promise<R<Project[]>> {
  return request({
    url: '/v1/project/list',
    method: 'get',
    params
  })
}

/**
 * 根据ID查询项目
 * GET /api/v1/project/{id}
 * @param id 项目ID
 * @returns 项目详情
 */
export function getProjectById(id: number): Promise<R<Project>> {
  return request({
    url: `/v1/project/${id}`,
    method: 'get'
  })
}

/**
 * 新增项目
 * POST /api/v1/project
 * @param data 项目信息
 * @returns 是否成功
 */
export function createProject(data: Project): Promise<R<boolean>> {
  return request({
    url: '/v1/project',
    method: 'post',
    data
  })
}

/**
 * 修改项目
 * PUT /api/v1/project/{id}
 * @param id 项目ID
 * @param data 项目信息
 * @returns 是否成功
 */
export function updateProject(id: number, data: Project): Promise<R<boolean>> {
  return request({
    url: `/v1/project/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除项目
 * DELETE /api/v1/project/{id}
 * @param id 项目ID
 * @returns 是否成功
 */
export function deleteProject(id: number): Promise<R<boolean>> {
  return request({
    url: `/v1/project/${id}`,
    method: 'delete'
  })
}

/**
 * 更新项目状态
 * PUT /api/v1/project/{id}/status
 * @param id 项目ID
 * @param status 状态：1进行中 2已完工 3已暂停
 * @returns 是否成功
 */
export function updateProjectStatus(id: number, status: number): Promise<R<boolean>> {
  return request({
    url: `/v1/project/${id}/status`,
    method: 'put',
    params: { status }
  })
}
