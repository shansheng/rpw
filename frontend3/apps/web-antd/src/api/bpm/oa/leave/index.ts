import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace BpmOALeaveApi {
  export interface Leave {
    id: number;
    status: number;
    type: string;
    reason: string;
    processInstanceId: string;
    startTime: number;
    endTime: number;
    createTime: Date;
  }
}

/** 创建请假申请 */
export async function createLeave(data: BpmOALeaveApi.Leave) {
  return requestClient.post('/bpm/oa/leave/create', data);
}

/** 获得请假申请 */
export async function getLeave(id: number) {
  return requestClient.get<BpmOALeaveApi.Leave>(`/bpm/oa/leave/get?id=${id}`);
}

/** 根据流程实例ID获得请假申请（流程详情/流转记录查看业务表单用） */
export async function getLeaveByProcessInstanceId(processInstanceId: string) {
  return requestClient.get<BpmOALeaveApi.Leave>(
    `/bpm/oa/leave/get-by-process-instance-id?processInstanceId=${processInstanceId}`,
  );
}

/** 获得请假申请分页 */
export async function getLeavePage(params: PageParam) {
  return requestClient.get<PageResult<BpmOALeaveApi.Leave>>(
    '/bpm/oa/leave/page',
    { params },
  );
}

/** 取消请假申请（按请假ID取消其关联流程实例） */
export async function cancelLeave(id: number, reason: string) {
  return requestClient.delete<boolean>('/bpm/oa/leave/cancel', {
    params: { id, reason },
  });
}
