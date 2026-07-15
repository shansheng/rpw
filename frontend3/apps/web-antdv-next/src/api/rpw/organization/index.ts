import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace OrganizationApi {
  /** 组织 */
  export interface Organization {
    id?: number;
    orgName?: string;
    orgLevel?: number;
    parentId?: number;
    department?: string;
    section?: string;
    createTime?: Date;
  }
}

/** 查询组织列表 */
export function getOrganizationList(params?: PageParam) {
  return requestClient.get<OrganizationApi.Organization[]>(
    '/organization/list',
    { params },
  );
}

/** 查询组织详情 */
export function getOrganization(id: number) {
  return requestClient.get<OrganizationApi.Organization>(
    `/organization/${id}`,
  );
}

/** 新增组织 */
export function createOrganization(data: OrganizationApi.Organization) {
  return requestClient.post('/organization', data);
}

/** 修改组织 */
export function updateOrganization(data: OrganizationApi.Organization) {
  return requestClient.put(`/organization/${data.id}`, data);
}

/** 删除组织 */
export function deleteOrganization(id: number) {
  return requestClient.delete(`/organization/${id}`);
}
