import { requestClient } from '#/api/request';

export namespace WarningRecordApi {
  /** 预警记录 */
  export interface WarningRecord {
    id?: number;
    ruleId?: number;
    /** 预警对象类型 */
    objectType?: string;
    /** 计划 ID */
    planId?: number;
    /** 计划名称 */
    planName?: string;
    /** 预警等级：RED/ORANGE/YELLOW */
    warningLevel?: string;
    /** 预警原因（规则定义 + 具体计算值） */
    reason?: string;
    /** 触发规则的表达式 */
    conditionExpr?: string;
    /** 状态：PENDING 待处理 / RESOLVED 已解决 / IGNORED 已忽略 */
    status?: string;
    /** 触发时间 */
    triggeredTime?: string;
    /** 处理备注 */
    handleRemark?: string;
    /** 处理时间 */
    handleTime?: string;
    createTime?: string;
  }

  /** 统计信息 */
  export interface WarningRecordStatistics {
    totalCount?: number;
    pendingCount?: number;
    resolvedCount?: number;
    ignoredCount?: number;
    redCount?: number;
    orangeCount?: number;
    yellowCount?: number;
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

/** 处理单条预警记录 */
export function handleWarningRecord(
  id: number,
  status: string,
  handleRemark?: string,
) {
  return requestClient.put<boolean>(`/warning/record/${id}/handle`, null, {
    params: { status, ...(handleRemark ? { handleRemark } : {}) },
  });
}

/** 批量处理预警记录 */
export function batchHandleWarningRecord(
  ids: number[],
  status: string,
  handleRemark?: string,
) {
  return requestClient.put<boolean>('/warning/record/batch-handle', {
    ids,
    status,
    handleRemark,
  });
}

/** 获取预警记录统计 */
export function getWarningRecordStatistics() {
  return requestClient.get<WarningRecordApi.WarningRecordStatistics>(
    '/warning/record/statistics',
  );
}
