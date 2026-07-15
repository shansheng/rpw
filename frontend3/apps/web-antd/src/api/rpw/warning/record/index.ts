import { requestClient } from '#/api/request';

export namespace WarningRecordApi {
  /** 预警记录 */
  export interface WarningRecord {
    id?: number;
    ruleName?: string;
    resourceType?: string;
    projectId?: number;
    warningLevel?: string;
    status?: string;
    triggerTime?: Date;
    handleRemark?: string;
    handleTime?: Date;
    createTime?: Date;
  }

  /** 预警记录统计 */
  export interface WarningRecordStatistics {
    totalCount?: number;
    pendingCount?: number;
    processingCount?: number;
    resolvedCount?: number;
    ignoredCount?: number;
    urgentCount?: number;
    importantCount?: number;
    generalCount?: number;
    todayTriggeredCount?: number;
  }
}

/** 查询预警记录分页 */
export function getWarningRecordPage(params: any) {
  return requestClient.get<{
    records: WarningRecordApi.WarningRecord[];
    total: number;
    size: number;
    current: number;
  }>('/warning/record/list', { params });
}

/** 查询预警记录详情 */
export function getWarningRecord(id: number) {
  return requestClient.get<WarningRecordApi.WarningRecord>(
    `/warning/record/${id}`,
  );
}

/** 处理预警记录 */
export function handleWarningRecord(
  id: number,
  status: string,
  handleRemark?: string,
) {
  return requestClient.put(
    `/warning/record/${id}/handle`,
    {},
    { params: { status, handleRemark } },
  );
}

/** 批量处理预警记录 */
export function batchHandleWarningRecord(
  ids: number[],
  status: string,
  handleRemark: string,
) {
  return requestClient.put('/warning/record/batch-handle', {
    ids,
    status,
    handleRemark,
  });
}

/** 统计预警记录 */
export function getWarningRecordStatistics(params?: { projectId?: number }) {
  return requestClient.get<WarningRecordApi.WarningRecordStatistics>(
    '/warning/record/statistics',
    { params },
  );
}
