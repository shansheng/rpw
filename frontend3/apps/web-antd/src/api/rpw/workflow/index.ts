import { requestClient } from '#/api/request';

export namespace WorkflowApi {
  /** 待办任务 */
  export interface TaskDTO {
    taskId?: string;
    name?: string;
    assignee?: string;
    createTime?: string;
    processInstanceId?: string;
    processDefinitionId?: string;
    businessKey?: string;
    businessInfo?: Record<string, any>;
  }

  /** 历史任务（已办） */
  export interface HistoricTaskDTO {
    taskId?: string;
    name?: string;
    assignee?: string;
    startTime?: string;
    endTime?: string;
    processInstanceId?: string;
  }

  /** 流程定义 */
  export interface ProcessDefinitionDTO {
    id?: string;
    key?: string;
    name?: string;
    version?: number;
    deploymentId?: string;
  }

  /** 流程实例 */
  export interface ProcessInstanceDTO {
    id?: string;
    processInstanceId?: string;
    processDefinitionId?: string;
    processDefinitionKey?: string;
    businessKey?: string;
    status?: string;
    suspended?: boolean;
    finished?: boolean;
    startTime?: string;
    endTime?: string;
  }

  /** 流程实例详情 */
  export interface ProcessInstanceDetailVO {
    processInstanceId?: string;
    processDefinitionKey?: string;
    businessKey?: string;
    suspended?: boolean;
    finished?: boolean;
    startTime?: string;
    endTime?: string;
    historicTasks?: Array<Record<string, any>>;
    bpmnXml?: string;
  }

  /** 待审批任务 */
  export interface PendingTaskDTO {
    taskId?: string;
    taskName?: string;
    assignee?: string;
    createTime?: string;
    planType?: string;
    planId?: number;
  }

  /** 审批历史 */
  export interface ApprovalHistoryDTO {
    taskId?: string;
    taskName?: string;
    assignee?: string;
    startTime?: string;
    endTime?: string;
  }

  /** 启动流程 */
  export interface StartProcessDTO {
    processDefinitionKey: string;
    businessKey?: string;
    variables?: Record<string, any>;
  }

  /** 部署 XML */
  export interface XmlDeployDTO {
    xml: string;
    name?: string;
    deploymentName?: string;
  }
}

/** 待办任务 */
export function getTodoTasks(params?: { userId?: string }) {
  return requestClient.get<WorkflowApi.TaskDTO[]>('/flowable/task/todo', { params });
}

/** 历史任务（已办） */
export function getHistoryTasks(params?: { assignee?: string }) {
  return requestClient.get<WorkflowApi.HistoricTaskDTO[]>(
    '/flowable/task/history',
    { params },
  );
}

/** 认领任务 */
export function claimTask(taskId: string, userId?: string) {
  return requestClient.post(`/flowable/task/${taskId}/claim`, {}, { params: { userId } });
}

/** 完成任务 */
export function completeTask(taskId: string, variables?: Record<string, any>) {
  return requestClient.post(`/flowable/task/${taskId}/complete`, variables ?? {});
}

/** 任务流程图数据 */
export function getTaskDiagram(taskId: string) {
  return requestClient.get<Record<string, any>>(`/flowable/task/${taskId}/diagram`);
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

/** 流程实例列表 */
export function getProcessInstances(params?: {
  processDefinitionKey?: string;
  finished?: boolean;
}) {
  return requestClient.get<WorkflowApi.ProcessInstanceDTO[]>(
    '/flowable/process-instance/list',
    { params },
  );
}

/** 我发起的流程 */
export function getMyProcessInstances(params?: { initiator?: string }) {
  return requestClient.get<WorkflowApi.ProcessInstanceDTO[]>(
    '/flowable/process-instance/my',
    { params },
  );
}

/** 流程实例详情 */
export function getProcessInstanceDetail(id: string) {
  return requestClient.get<WorkflowApi.ProcessInstanceDetailVO>(
    `/flowable/process-instance/${id}/detail`,
  );
}

/** 流程实例流程图 */
export function getProcessDiagram(instanceId: string) {
  return requestClient.get<Record<string, any>>(
    `/flowable/process-instance/${instanceId}/diagram`,
  );
}

/** 删除流程实例 */
export function deleteProcessInstance(instanceId: string, deleteReason?: string) {
  return requestClient.delete(`/flowable/process-instance/${instanceId}`, {
    params: { deleteReason },
  });
}

/** 启动流程 */
export function startProcess(data: WorkflowApi.StartProcessDTO) {
  return requestClient.post<string>('/flowable/process-instance/start', data);
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

/** 审批历史 */
export function getApprovalHistory(planType: string, planId: number) {
  return requestClient.get<WorkflowApi.ApprovalHistoryDTO[]>(
    `/approval/history/${planType}/${planId}`,
  );
}
