import { requestClient } from '#/api/request';

export namespace SysRoleApi {
  /** 角色精简信息（下拉选择用） */
  export interface Role {
    id?: number;
    name?: string;
    code?: string;
    status?: number;
  }
}

/** 角色精简列表（用户绑定角色时下拉选择用） */
export function getRoleSimpleList() {
  return requestClient.get<SysRoleApi.Role[]>('/system/role/simple-list');
}
