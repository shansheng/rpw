import request from '@/utils/request'
import type { R } from '@/types/api'

/**
 * 获取字典表名列表
 * GET /api/v1/dict/tables
 * @returns 字典表名列表
 */
export function getDictTables(): Promise<R<string[]>> {
  return request({
    url: '/v1/dict/tables',
    method: 'get'
  })
}

/**
 * 查询字典列表
 * GET /api/v1/dict/{tableName}
 * @param tableName 字典表名
 * @returns 字典列表
 */
export function getDictList(tableName: string): Promise<R<Record<string, unknown>[]>> {
  return request({
    url: `/v1/dict/${tableName}`,
    method: 'get'
  })
}

/**
 * 根据ID查询字典
 * GET /api/v1/dict/{tableName}/{id}
 * @param tableName 字典表名
 * @param id 字典ID
 * @returns 字典详情
 */
export function getDictById(tableName: string, id: number): Promise<R<Record<string, unknown>>> {
  return request({
    url: `/v1/dict/${tableName}/${id}`,
    method: 'get'
  })
}

/**
 * 新增字典
 * POST /api/v1/dict/{tableName}
 * @param tableName 字典表名
 * @param data 字典数据
 * @returns 是否成功
 */
export function createDict(tableName: string, data: Record<string, unknown>): Promise<R<boolean>> {
  return request({
    url: `/v1/dict/${tableName}`,
    method: 'post',
    data
  })
}

/**
 * 修改字典
 * PUT /api/v1/dict/{tableName}/{id}
 * @param tableName 字典表名
 * @param id 字典ID
 * @param data 字典数据
 * @returns 是否成功
 */
export function updateDict(tableName: string, id: number, data: Record<string, unknown>): Promise<R<boolean>> {
  return request({
    url: `/v1/dict/${tableName}/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除字典
 * DELETE /api/v1/dict/{tableName}/{id}
 * @param tableName 字典表名
 * @param id 字典ID
 * @returns 是否成功
 */
export function deleteDict(tableName: string, id: number): Promise<R<boolean>> {
  return request({
    url: `/v1/dict/${tableName}/${id}`,
    method: 'delete'
  })
}
