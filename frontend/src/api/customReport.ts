import request from '@/utils/request'
import type { R } from '@/types/api'

/** 字段配置项 */
export interface FieldItem {
  field: string
  label: string
  visible: boolean
  width?: number
}

/** 筛选条件项 */
export interface FilterItem {
  field: string
  operator: string
  value: any
}

/** 排序规则项 */
export interface SortItem {
  field: string
  order: 'ASC' | 'DESC'
}

/** 报表配置JSON结构 */
export interface ReportConfigJson {
  fields: FieldItem[]
  filters: FilterItem[]
  sorts: SortItem[]
}

/** 报表配置保存 DTO */
export interface ReportConfigDTO {
  id?: number
  userId: number
  reportName: string
  reportType: string
  configJson: string
  isDefault?: boolean
}

/** 报表配置 VO */
export interface ReportConfigVO {
  id: number
  userId: number
  reportName: string
  reportType: string
  configJson: string
  isDefault: number
  createTime: string
  updateTime: string
}

/** 报表查询 DTO */
export interface ReportQueryDTO {
  reportType: string
  configJson: string
  pageNum?: number
  pageSize?: number
}

/** 报表结果 VO */
export interface ReportResultVO {
  fields: FieldItem[]
  data: Record<string, any>[]
  total: number
}

/**
 * 保存报表配置
 */
export function saveReportConfig(data: ReportConfigDTO): Promise<R<number>> {
  return request({
    url: '/v1/report/config',
    method: 'post',
    data
  })
}

/**
 * 获取报表配置列表
 */
export function getReportConfigList(params: {
  userId: number
  reportType?: string
}): Promise<R<ReportConfigVO[]>> {
  return request({
    url: '/v1/report/config/list',
    method: 'get',
    params
  })
}

/**
 * 获取报表配置详情
 */
export function getReportConfigById(id: number): Promise<R<ReportConfigVO>> {
  return request({
    url: `/v1/report/config/${id}`,
    method: 'get'
  })
}

/**
 * 删除报表配置
 */
export function deleteReportConfig(id: number): Promise<R<boolean>> {
  return request({
    url: `/v1/report/config/${id}`,
    method: 'delete'
  })
}

/**
 * 预览报表
 */
export function previewReport(data: ReportQueryDTO): Promise<R<ReportResultVO>> {
  return request({
    url: '/v1/report/preview',
    method: 'post',
    data
  })
}

/**
 * 导出Excel
 */
export function exportExcel(data: ReportQueryDTO): Promise<Blob> {
  return request({
    url: '/v1/report/export/excel',
    method: 'post',
    data,
    responseType: 'blob'
  })
}

/**
 * 导出PDF（CSV格式）
 */
export function exportPdf(data: ReportQueryDTO): Promise<Blob> {
  return request({
    url: '/v1/report/export/pdf',
    method: 'post',
    data,
    responseType: 'blob'
  })
}
