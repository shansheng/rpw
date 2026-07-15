import { requestClient } from '#/api/request';

export namespace WorkflowApi {
  /** 业务类型中文映射 */
  export const PLAN_TYPE_LABELS: Record<string, string> = {
    material: '物资计划变更',
    labor: '劳动力计划',
    subcontract: '分包计划变更',
    laborChange: '劳动力计划变更',
  };

  /** 待办任务 */
  export interface PendingTaskDTO {
    taskId?: string;
    taskName?: string;
    assignee?: string;
    createTime?: string;
    planType?: string;
    planId?: number;
  }

  /** 已办任务 */
  export interface DoneTaskDTO {
    taskId?: string;
    taskName?: string;
    assignee?: string;
    startTime?: string;
    endTime?: string;
    planType?: string;
    planId?: number;
  }

  /** 我发起的流程实例 */
  export interface MyProcessDTO {
    processInstanceId?: string;
    processDefinitionKey?: string;
    businessKey?: string;
    planType?: string;
    planId?: number;
    status?: string;
    startTime?: string;
    endTime?: string;
  }

  /** 流程定义 */
  export interface ProcessDefinitionDTO {
    id?: string;
    key?: string;
    name?: string;
    version?: number;
    deploymentId?: string;
  }

  /** 审批历史 */
  export interface ApprovalHistoryDTO {
    taskId?: string;
    taskName?: string;
    assignee?: string;
    startTime?: string;
    endTime?: string;
  }

  /** 部署 XML */
  export interface XmlDeployDTO {
    xml: string;
    name?: string;
    deploymentName?: string;
  }
}

/** 待审批任务 */
export function getPendingTasks() {
  return requestClient.get<WorkflowApi.PendingTaskDTO[]>('/approval/pending');
}

/** 审批通过 */
export function approveTask(taskId: string, comment?: string) {
  return requestClient.post(`/approval/approve`, {}, { params: { taskId, comment } });
}

/** 审批驳回 */
export function rejectTask(taskId: string, comment: string) {
  return requestClient.post(`/approval/reject`, {}, { params: { taskId, comment } });
}

/** 审批历史（按业务） */
export function getApprovalHistory(planType: string, planId: number) {
  return requestClient.get<WorkflowApi.ApprovalHistoryDTO[]>(
    `/approval/history/${planType}/${planId}`,
  );
}

/** 已办任务 */
export function getDoneTasks() {
  return requestClient.get<WorkflowApi.DoneTaskDTO[]>('/approval/done');
}

/** 我发起的流程 */
export function getMyProcesses() {
  return requestClient.get<WorkflowApi.MyProcessDTO[]>('/approval/my');
}

/** 流程定义列表 */
export function getProcessDefinitions(params?: { key?: string }) {
  return requestClient.get<WorkflowApi.ProcessDefinitionDTO[]>(
    '/flowable/process-definition/list',
    { params },
  );
}

/** 删除流程定义 */
export function deleteProcessDefinition(deploymentId: string, cascade = false) {
  return requestClient.delete(`/flowable/process-definition/${deploymentId}`, {
    params: { cascade },
  });
}

/** 部署流程定义（XML） */
export function deployProcessDefinitionByXml(data: WorkflowApi.XmlDeployDTO) {
  return requestClient.post<string>('/flowable/process-definition/deploy/xml', data);
}

/** 获取流程定义 BPMN XML */
export function getProcessDefinitionXml(processDefinitionId: string) {
  return requestClient.get<string>(
    `/flowable/process-definition/${processDefinitionId}/xml`,
  );
}
