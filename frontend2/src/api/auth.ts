import request from './request'
import type { R } from '@/types/api'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
}

export interface UserInfo {
  id: number
  username: string
  realName: string
  avatar?: string
  email?: string
  phone?: string
}

export function login(data: LoginParams): Promise<R<LoginResult>> {
  return request.post('/v1/auth/login', data)
}

export function register(data: LoginParams): Promise<R<any>> {
  return request.post('/v1/auth/register', data)
}

export function getUserInfo(): Promise<R<UserInfo>> {
  return request.get('/v1/auth/userinfo')
}

export function logout(): Promise<R<any>> {
  return request.post('/v1/auth/logout')
}
