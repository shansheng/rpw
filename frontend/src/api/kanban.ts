import request from '@/utils/request'
import type { R } from '@/types/api'

/**
 * 看板列配置接口
 */
export interface KanbanColumn {
  statusKey: string
  statusName: string
  order: number
  cards: KanbanCard[]
}

/**
 * 看板卡片接口
 */
export interface KanbanCard {
  id: number
  resourceType: string
  wbsCode: string
  resourceName: string
  status: string
  responsiblePerson?: string
  planStartDate?: string
  planEndDate?: string
  priority?: string
}

/**
 * 看板数据接口
 */
export interface KanbanBoard {
  columns: KanbanColumn[]
  totalCards: number
}

/**
 * 卡片状态更新 DTO
 */
export interface KanbanCardDTO {
  id: number
  resourceType: string
  newStatus: string
  remark?: string
}

/**
 * 获取看板数据
 * @param params 筛选参数
 */
export function getKanbanBoardData(params: {
  projectId?: number
  resourceType?: string
}): Promise<R<KanbanBoard>> {
  return request({
    url: '/v1/kanban/board',
    method: 'get',
    params
  })
}

/**
 * 更新卡片状态
 * @param data 状态更新数据
 */
export function updateCardStatus(data: KanbanCardDTO): Promise<R<boolean>> {
  return request({
    url: '/v1/kanban/card/status',
    method: 'put',
    data
  })
}

/**
 * 获取看板列配置
 */
export function getKanbanColumns(): Promise<R<KanbanColumn[]>> {
  return request({
    url: '/v1/kanban/columns',
    method: 'get'
  })
}
