import request from './request'
import type { R, PageResult, PageParams } from '@/types/api'

export interface ReportConfig {
  id: number
  name: string
  type?: string
  template?: string
  params?: Record<string, any>
  schedule?: string
  recipients?: string[]
  enabled?: boolean
  createTime?: string
}

// ---- 报表配置 ----
export function getConfigList(params?: PageParams & { name?: string }): Promise<R<PageResult<ReportConfig>>> {
  return request.get('/v1/report/config/list', { params })
}

export function createConfig(data: Partial<ReportConfig>): Promise<R<ReportConfig>> {
  return request.post('/v1/report/config', data)
}

export function updateConfig(id: number, data: Partial<ReportConfig>): Promise<R<ReportConfig>> {
  return request.put(`/v1/report/config/${id}`, data)
}

export function deleteConfig(id: number): Promise<R<any>> {
  return request.delete(`/v1/report/config/${id}`)
}

// ---- 报表预览 ----
export function fetchPreview(id?: number, extraParams?: Record<string, any>): Promise<R<any>> {
  return request.get('/v1/report/preview', { params: { id, ...extraParams } })
}

// ---- 报表导出 ----
export function exportReport(id: number, format?: string): Promise<Blob> {
  const url = format === 'pdf' ? '/v1/report/export/pdf' : '/v1/report/export/excel'
  return request.get(url, {
    params: { id },
    responseType: 'blob'
  })
}
