import { requestClient } from '#/api/request';

export namespace LaborPlanChangeApi {
  /** 劳动力计划变更 */
  export interface LaborPlanChange {
    id?: number;
    laborPlanId?: number;
    changeType?: string;
    changeReason?: string;
    oldPlanStartDate?: string;
    newPlanStartDate?: string;
    oldPlanEndDate?: string;
    newPlanEndDate?: string;
    oldPlanQuantity?: number;
    newPlanQuantity?: number;
    approvalStatus?: string;
    processInstanceId?: string;
    applicant?: string;
  }
}

/** 创建变更申请 */
export function createLaborPlanChange(data: LaborPlanChangeApi.LaborPlanChange) {
  return requestClient.post<LaborPlanChangeApi.LaborPlanChange>(
    '/resource-plan/labor-change',
    data,
  );
}

/** 查询变更列表（laborPlanId 必填） */
export function getLaborPlanChangeList(laborPlanId: number) {
  return requestClient.get<LaborPlanChangeApi.LaborPlanChange[]>(
    '/resource-plan/labor-change/list',
    { params: { laborPlanId } },
  );
}

/** 重新提交（被驳回后再次发起审批，由工作流「待办任务」审批） */
export function resubmitLaborPlanChange(id: number) {
  return requestClient.post(`/resource-plan/labor-change/${id}/resubmit`);
}
