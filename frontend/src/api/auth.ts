import request from '@/utils/request'
import type { R } from '@/types/api'

/**
 * 登录接口
 * POST /api/v1/auth/login
 * @param data 登录参数
 * @returns 包含Token的响应
 */
export function login(data: { username: string; password: string }): Promise<R<{ token: string; tokenType: string; expiresIn: number }>> {
  return request({
    url: '/v1/auth/login',
    method: 'post',
    data
  })
}

/**
 * 注册接口
 * POST /api/v1/auth/register
 * @param data 注册参数
 * @returns 是否成功
 */
export function register(data: { username: string; password: string; realName: string }): Promise<R<boolean>> {
  return request({
    url: '/v1/auth/register',
    method: 'post',
    data
  })
}

/**
 * 获取用户信息接口
 * GET /api/v1/auth/userinfo
 * @returns 用户信息
 */
export function getUserInfo(): Promise<R<{ username: string; realName: string }>> {
  return request({
    url: '/v1/auth/userinfo',
    method: 'get'
  })
}
