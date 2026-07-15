import request from './request'
import type { R } from '@/types/api'

const API_MAP: Record<string, string> = {
  material: '/v1/resource-plan/material',
  equipment: '/v1/resource-plan/equipment',
  safety: '/v1/resource-plan/safety',
  office: '/v1/resource-plan/office',
  hardware: '/v1/resource-plan/hardware',
  circulation: '/v1/resource-plan/circulation',
  subcontract: '/v1/resource-plan/subcontract',
  labor: '/v1/resource-plan/labor',
}

function ep(type: string): string { return API_MAP[type] || '/v1/resource-plan/material' }

export function getList(type: string, params?: any): Promise<R<any[]>> {
  return request.get(ep(type) + '/list', { params })
}
export function createItem(type: string, data: any): Promise<R<any>> {
  return request.post(ep(type), data)
}
export function updateItem(type: string, id: number, data: any): Promise<R<any>> {
  return request.put(ep(type) + '/' + id, data)
}
export function deleteItem(type: string, id: number): Promise<R<any>> {
  return request.delete(ep(type) + '/' + id)
}
export function submitApproval(type: string, id: number): Promise<R<any>> {
  return request.post(ep(type) + '/' + id + '/submit')
}

// ---- 劳动力计划变更 ----
export function createLaborChange(data: {
  laborPlanId: number
  changeType: string
  changeReason: string
  newPlanStartDate?: string
  newPlanEndDate?: string
  newPlanQuantity?: number
}): Promise<R<any>> {
  return request.post('/v1/resource-plan/labor-change', data)
}

export function getLaborChangeList(params?: { laborPlanId?: number }): Promise<R<any[]>> {
  return request.get('/v1/resource-plan/labor-change/list', { params })
}

export function approveLaborChange(id: number): Promise<R<any>> {
  return request.post(`/v1/resource-plan/labor-change/${id}/approve`)
}

export function rejectLaborChange(id: number): Promise<R<any>> {
  return request.post(`/v1/resource-plan/labor-change/${id}/reject`)
}

// ---- 预警状态查询 ----
export function getWarningStatus(resourceType: string, resourceId: number): Promise<R<any>> {
  return request.get('/v1/warning/status', { params: { resourceType, resourceId } })
}
