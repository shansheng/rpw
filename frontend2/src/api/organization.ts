import request from './request'
import type { R } from '@/types/api'

export interface Organization {
  id: number
  orgName: string
  parentId?: number
  orgLevel?: number
  department?: string
  section?: string
  children?: Organization[]
}

export function getTree(): Promise<R<Organization[]>> {
  return request.get('/v1/organization/tree')
}

export function create(data: Partial<Organization>): Promise<R<Organization>> {
  return request.post('/v1/organization', data)
}

export function update(id: number, data: Partial<Organization>): Promise<R<Organization>> {
  return request.put(`/v1/organization/${id}`, data)
}

export function remove(id: number): Promise<R<any>> {
  return request.delete(`/v1/organization/${id}`)
}
