import { requestClient } from '#/api/request';

export namespace SysUserApi {
  /** 系统用户 */
  export interface SysUser {
    id?: number;
    /** 用户名（登录账号） */
    username?: string;
    /** 密码（仅创建/重置时传输，列表/详情不返回明文） */
    password?: string;
    /** 真实姓名 */
    realName?: string;
    /** 邮箱 */
    email?: string;
    /** 手机号 */
    phone?: string;
    /** 所属组织ID（对应 organization 表节点） */
    orgId?: number;
    /** 状态：1启用 0禁用 */
    status?: number;
    /** 角色ID列表（新建/编辑时提交；详情/列表时返回） */
    roleIds?: number[];
    createTime?: Date;
    updateTime?: Date;
  }

  /** 分页查询参数 */
  export interface PageParam {
    pageNum?: number;
    pageSize?: number;
    username?: string;
    realName?: string;
    status?: number;
    orgId?: number;
  }
}

/** 分页查询用户 */
export function getSysUserPage(params: SysUserApi.PageParam) {
  return requestClient.get<{
    records: SysUserApi.SysUser[];
    total: number;
    size: number;
    current: number;
  }>('/system/user/list', { params });
}

/** 用户详情 */
export function getSysUser(id: number) {
  return requestClient.get<SysUserApi.SysUser>(`/system/user/${id}`);
}

/** 新增用户 */
export function createSysUser(data: SysUserApi.SysUser) {
  return requestClient.post<number>('/system/user/create', data);
}

/** 修改用户 */
export function updateSysUser(data: SysUserApi.SysUser) {
  return requestClient.put<boolean>('/system/user/update', data);
}

/** 删除用户 */
export function deleteSysUser(id: number) {
  return requestClient.delete<boolean>(`/system/user/${id}`);
}

/** 启用/禁用用户 */
export function updateSysUserStatus(id: number, status: number) {
  return requestClient.put<boolean>(
    `/system/user/${id}/status`,
    {},
    { params: { status } },
  );
}

/** 重置密码（不传 password 则重置为默认口令 123456） */
export function resetSysUserPassword(id: number, password?: string) {
  return requestClient.put<boolean>(
    `/system/user/${id}/reset-password`,
    {},
    { params: password ? { password } : {} },
  );
}
