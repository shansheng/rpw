import { requestClient } from '#/api/request';

export namespace ReportApi {
  /** 报表配置 */
  export interface ReportConfigVO {
    id?: number;
    userId?: number;
    reportName?: string;
    reportType?: string;
    configJson?: string;
    isDefault?: number;
    createTime?: string;
    updateTime?: string;
  }

  /** 报表字段配置 */
  export interface ReportFieldConfig {
    field?: string;
    label?: string;
    visible?: boolean;
    width?: number;
  }

  /** 报表查询结果 */
  export interface ReportResultVO {
    fields?: ReportFieldConfig[];
    data?: Array<Record<string, any>>;
    total?: number;
  }

  /** 报表查询 */
  export interface ReportQueryDTO {
    reportType: string;
    configJson: string;
    pageNum?: number;
    pageSize?: number;
  }
}

/** 报表配置列表（userId 必填） */
export function getReportConfigs(params: { userId: number; reportType?: string }) {
  return requestClient.get<ReportApi.ReportConfigVO[]>('/report/config/list', { params });
}

/** 报表配置详情 */
export function getReportConfig(id: number) {
  return requestClient.get<ReportApi.ReportConfigVO>(`/report/config/${id}`);
}

/** 删除报表配置 */
export function deleteReportConfig(id: number) {
  return requestClient.delete<boolean>(`/report/config/${id}`);
}

/** 预览报表数据 */
export function previewReport(dto: ReportApi.ReportQueryDTO) {
  return requestClient.post<ReportApi.ReportResultVO>('/report/preview', dto);
}
