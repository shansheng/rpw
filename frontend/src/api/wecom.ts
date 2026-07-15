import request from '@/utils/request'
import type { R } from '@/types/api'

/**
 * 测试企业微信连接
 * GET /api/v1/wecom/test
 * @returns 测试结果
 */
export function testWeComConnection(): Promise<R<string>> {
  return request({
    url: '/v1/wecom/test',
    method: 'get'
  })
}

/**
 * 发送文本消息测试
 * POST /api/v1/wecom/send-text
 * @param data 消息参数
 * @returns 发送结果
 */
export function sendTextMessage(data: { touser: string; content: string }): Promise<R<boolean>> {
  return request({
    url: '/v1/wecom/send-text',
    method: 'post',
    data
  })
}

/**
 * 发送Markdown消息
 * POST /api/v1/wecom/send-markdown
 * @param data 消息参数
 * @returns 发送结果
 */
export function sendMarkdownMessage(data: { touser: string; content: string }): Promise<R<boolean>> {
  return request({
    url: '/v1/wecom/send-markdown',
    method: 'post',
    data
  })
}

/**
 * 获取企业微信配置
 * GET /api/v1/wecom/config
 * @returns 配置信息
 */
export function getWeComConfig(): Promise<R<Record<string, unknown>>> {
  return request({
    url: '/v1/wecom/config',
    method: 'get'
  })
}

/**
 * 保存企业微信配置
 * POST /api/v1/wecom/config
 * @param data 配置参数
 * @returns 保存结果
 */
export function saveWeComConfig(data: Record<string, unknown>): Promise<R<boolean>> {
  return request({
    url: '/v1/wecom/config',
    method: 'post',
    data
  })
}
