import request from './request'
import type { R } from '@/types/api'

export interface KanbanCard {
  id: number
  title: string
  description?: string
  status: string
  priority?: string
  assignee?: string
  dueDate?: string
  sort?: number
  createTime?: string
}

export interface KanbanColumn {
  key: string
  title: string
  cards: KanbanCard[]
}

export function getBoardData(): Promise<R<KanbanColumn[]>> {
  return request.get('/v1/kanban/board')
}

export function getColumns(): Promise<R<KanbanColumn[]>> {
  return request.get('/v1/kanban/columns')
}

export function createCard(data: Partial<KanbanCard>): Promise<R<KanbanCard>> {
  return request.post('/v1/kanban/card', data)
}

export function updateCard(id: number, data: Partial<KanbanCard>): Promise<R<KanbanCard>> {
  return request.put(`/v1/kanban/card/${id}`, data)
}

export function updateCardStatus(id: number, status: string, sort?: number): Promise<R<any>> {
  return request.put(`/v1/kanban/card/${id}/status`, { status, sort })
}

export function deleteCard(id: number): Promise<R<any>> {
  return request.delete(`/v1/kanban/card/${id}`)
}
