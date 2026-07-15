import request from './request'
import type { R } from '@/types/api'

export interface Company {
  id: number
  companyName: string
  orgId?: number
  deleted?: number
  createTime?: string
  updateTime?: string
}

export function getList(params?: any): Promise<R<Company[]>> {
  return request.get('/v1/company/list', { params })
}

export function create(data: Partial<Company>): Promise<R<Company>> {
  return request.post('/v1/company', data)
}

export function update(id: number, data: Partial<Company>): Promise<R<Company>> {
  return request.put(`/v1/company/${id}`, data)
}

export function remove(id: number): Promise<R<any>> {
  return request.delete(`/v1/company/${id}`)
}
