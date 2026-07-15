/** 通用响应类型 */
export interface R<T = any> {
  code: number
  message: string
  data: T
  timestamp?: number
}

/** 分页参数 */
export interface PageParams {
  pageNum?: number
  pageSize?: number
}

/** 分页结果 */
export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
}
