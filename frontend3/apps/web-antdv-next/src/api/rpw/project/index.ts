import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace ProjectApi {
  /** 项目 */
  export interface Project {
    id?: number;
    projectName?: string;
    companyId?: number;
    status?: number;
    planStartDate?: string;
    planEndDate?: string;
    createTime?: Date;
  }
}

/** 查询项目列表 */
export function getProjectList(params?: PageParam) {
  return requestClient.get<ProjectApi.Project[]>('/project/list', { params });
}

/** 查询项目详情 */
export function getProject(id: number) {
  return requestClient.get<ProjectApi.Project>(`/project/${id}`);
}

/** 新增项目 */
export function createProject(data: ProjectApi.Project) {
  return requestClient.post('/project', data);
}

/** 修改项目 */
export function updateProject(data: ProjectApi.Project) {
  return requestClient.put(`/project/${data.id}`, data);
}

/** 删除项目 */
export function deleteProject(id: number) {
  return requestClient.delete(`/project/${id}`);
}

/** 更新项目状态 */
export function updateProjectStatus(id: number, status: number) {
  return requestClient.put(`/project/${id}/status`, {}, { params: { status } });
}
