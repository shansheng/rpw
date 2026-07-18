import { requestClient } from '#/api/request';

export namespace WarningRuleApi {
  /** 预警规则 */
  export interface WarningRule {
    id?: number;
    /** 规则名称 */
    name?: string;
    /** 预警对象类型：SUBCONTRACT/MATERIAL/EQUIPMENT/LABOR/HARDWARE/CIRCULATION/OFFICE/SAFETY */
    objectType?: string;
    /** 规则条件表达式（IF 部分） */
    conditionExpr?: string;
    /** 预警等级：RED/ORANGE/YELLOW */
    warningLevel?: string;
    /** 优先级（越小越先检查） */
    priority?: number;
    /** 是否启用：1 启用 0 禁用 */
    enabled?: number;
    /** 备注 */
    remark?: string;
    createTime?: string;
    updateTime?: string;
  }

  /** 属性元数据 */
  export interface AttributeMeta {
    label: string;
    field: string;
    type: string;
  }

  /** 对象类型选项 */
  export interface ObjectTypeOption {
    value: string;
    label: string;
  }

  /** 系统属性 */
  export interface SystemAttribute {
    label: string;
    type: string;
  }

  /** /attributes 返回结构 */
  export interface AttributeMetaResult {
    objectTypes: ObjectTypeOption[];
    systemAttributes: SystemAttribute[];
    attributes: AttributeMeta[];
  }

  /** /validate 返回结构 */
  export interface ValidateResult {
    valid: boolean;
    message?: string;
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

/** 获取属性元数据（对象类型 / 系统属性 / 当前对象属性） */
export function getWarningAttributes(objectType?: string) {
  return requestClient.get<WarningRuleApi.AttributeMetaResult>(
    '/warning/rule/attributes',
    { params: objectType ? { objectType } : {} },
  );
}

/** 校验表达式语法 */
export function validateWarningRule(body: {
  objectType: string;
  conditionExpr: string;
}) {
  return requestClient.post<WarningRuleApi.ValidateResult>(
    '/warning/rule/validate',
    body,
  );
}

/** 立即检查（手动触发后台生成预警） */
export function checkWarningRule(params?: { projectId?: number; ruleId?: number }) {
  return requestClient.post<string>('/warning/rule/check', {}, { params });
}
