import request from './request'
import type { R } from '@/types/api'

export interface Project {
  id: number
  projectName: string
  companyId?: number
  status?: number
  planStartDate?: string
  planEndDate?: string
  createTime?: string
  updateTime?: string
}

export function getList(params?: any): Promise<R<Project[]>> {
  return request.get('/v1/project/list', { params })
}

export function create(data: Partial<Project>): Promise<R<Project>> {
  return request.post('/v1/project', data)
}

export function update(id: number, data: Partial<Project>): Promise<R<Project>> {
  return request.put(`/v1/project/${id}`, data)
}

export function remove(id: number): Promise<R<any>> {
  return request.delete(`/v1/project/${id}`)
}
