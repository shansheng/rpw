import { requestClient } from '#/api/request';

export namespace WarningRuleApi {
  /** 预警规则 */
  export interface WarningRule {
    id?: number;
    ruleName?: string;
    resourceType?: string;
    projectId?: number;
    thresholdType?: string;
    warningThreshold?: number;
    compareField?: string;
    actualField?: string;
    warningLevel?: string;
    checkFrequency?: string;
    enabled?: number;
    notifyWecom?: number;
    notifyUsers?: string;
    description?: string;
    createTime?: Date;
  }
}

/** 查询预警规则分页 */
export function getWarningRulePage(params: any) {
  return requestClient.get<{
    records: WarningRuleApi.WarningRule[];
    total: number;
    size: number;
    current: number;
  }>('/warning/rule/list', { params });
}

/** 查询预警规则详情 */
export function getWarningRule(id: number) {
  return requestClient.get<WarningRuleApi.WarningRule>(`/warning/rule/${id}`);
}

/** 新增预警规则 */
export function createWarningRule(data: WarningRuleApi.WarningRule) {
  return requestClient.post('/warning/rule', data);
}

/** 修改预警规则 */
export function updateWarningRule(data: WarningRuleApi.WarningRule) {
  return requestClient.put(`/warning/rule/${data.id}`, data);
}

/** 删除预警规则 */
export function deleteWarningRule(id: number) {
  return requestClient.delete(`/warning/rule/${id}`);
}

/** 启用/禁用预警规则 */
export function toggleWarningRule(id: number, enabled: number) {
  return requestClient.put(`/warning/rule/${id}/toggle?enabled=${enabled}`);
}

/** 立即检查预警规则 */
export function checkWarningRule(params?: { projectId?: number; ruleId?: number }) {
  return requestClient.post('/warning/rule/check', {}, { params });
}
