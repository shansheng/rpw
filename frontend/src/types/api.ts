/**
 * 全局类型定义
 */

/**
 * 统一响应格式（对应后端 R 类）
 */
export interface R<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: number
}

/**
 * 分页请求参数
 */
export interface PageParams {
  pageNum: number
  pageSize: number
}

/**
 * 分页响应数据
 */
export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
}
