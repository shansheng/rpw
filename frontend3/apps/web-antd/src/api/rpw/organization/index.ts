import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace OrganizationApi {
  /** 组织（统一 局/公司/项目 + 部门 树） */
  export interface Organization {
    id?: number;
    orgName?: string;
    /** 组织级别：1局 2公司 3项目（部门节点继承父级级别） */
    orgLevel?: number;
    /** 上级组织ID */
    parentId?: number;
    /** 节点类型：1组织节点(局/公司/项目) 2部门节点 */
    nodeType?: number;
    /** 同级排序（越小越靠前） */
    sort?: number;
    /** 项目编码（仅项目节点） */
    projectCode?: string;
    /** 状态（仅项目节点：1进行中 2已完工 3已暂停） */
    status?: number;
    /** 计划开始日期 */
    planStartDate?: string;
    /** 计划结束日期 */
    planEndDate?: string;
    createTime?: Date;
    /** 子节点（树形展示用） */
    children?: Organization[];
  }
}

/** 查询组织树（含部门，已排序：部门在上、组织在下） */
export function getOrganizationTree() {
  return requestClient.get<OrganizationApi.Organization[]>(
    '/organization/tree',
  );
}

/** 查询组织列表（支持按 orgLevel 筛选，如下拉取项目级 orgLevel=3） */
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

/** 新增组织（后端按 parentId + nodeType 推导层级） */
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
