import type { PageParam } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace SubcontractorTeamApi {
  /** 分包队伍统计 */
  export interface SubcontractorTeam {
    id?: number;
    projectId?: number;
    professionalEngineering?: string;
    subcontractName?: string;
    subcontractMode?: string;
    teamSource?: string;
    latestEntryDate?: string;
    actualEntryDate?: string;
    tenderDocStartDate?: string;
    onlineTenderDate?: string;
    bidAwardDate?: string;
    mobilizationPeriodDays?: number;
    remarks?: string;
    createTime?: Date;
  }
}

/** 查询分包队伍统计列表 */
export function getSubcontractorTeamList(params?: PageParam) {
  return requestClient.get<SubcontractorTeamApi.SubcontractorTeam[]>('/subcontractor-team/list', { params });
}

/** 查询分包队伍统计详情 */
export function getSubcontractorTeam(id: number) {
  return requestClient.get<SubcontractorTeamApi.SubcontractorTeam>(`/subcontractor-team/${id}`);
}

/** 新增分包队伍统计 */
export function createSubcontractorTeam(data: SubcontractorTeamApi.SubcontractorTeam) {
  return requestClient.post('/subcontractor-team', data);
}

/** 修改分包队伍统计 */
export function updateSubcontractorTeam(data: SubcontractorTeamApi.SubcontractorTeam) {
  return requestClient.put(`/subcontractor-team/${data.id}`, data);
}

/** 删除分包队伍统计 */
export function deleteSubcontractorTeam(id: number) {
  return requestClient.delete(`/subcontractor-team/${id}`);
}
