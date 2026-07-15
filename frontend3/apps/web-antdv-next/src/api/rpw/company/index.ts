import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace CompanyApi {
  /** 公司 */
  export interface Company {
    id?: number;
    companyName?: string;
    orgId?: number;
    createTime?: Date;
  }
}

/** 查询公司列表 */
export function getCompanyList(params?: PageParam) {
  return requestClient.get<CompanyApi.Company[]>('/company/list', { params });
}

/** 查询公司详情 */
export function getCompany(id: number) {
  return requestClient.get<CompanyApi.Company>(`/company/${id}`);
}

/** 新增公司 */
export function createCompany(data: CompanyApi.Company) {
  return requestClient.post('/company', data);
}

/** 修改公司 */
export function updateCompany(data: CompanyApi.Company) {
  return requestClient.put(`/company/${data.id}`, data);
}

/** 删除公司 */
export function deleteCompany(id: number) {
  return requestClient.delete(`/company/${id}`);
}
