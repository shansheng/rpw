import request from './request'
import type { R } from '@/types/api'

export interface WecomConfig {
  id?: number
  corpId: string
  corpName: string
  agentId: string
  agentSecret: string
  token?: string
  encodingAesKey?: string
  webhookUrl?: string
  enableNotify: boolean
  notifyTypes: string[]
  createdAt?: string
  updatedAt?: string
}

export interface WecomTestResult {
  success: boolean
  message: string
}

/** 获取企业微信配置 */
export const getWecomConfig = (): Promise<R<WecomConfig>> =>
  request.get('/v1/wecom/config')

/** 保存企业微信配置 */
export const saveWecomConfig = (data: Partial<WecomConfig>): Promise<R<void>> =>
  request.post('/v1/wecom/config', data)

/** 测试企业微信连接 */
export const testWecomConnection = (): Promise<R<WecomTestResult>> =>
  request.post('/v1/wecom/test')

/** 获取回调 URL */
export const getCallbackUrl = (): Promise<R<{ url: string }>> =>
  request.get('/v1/wecom/callback-url')
