import request from './request'
import type { R } from '@/types/api'

export function getDictList(tableName: string): Promise<R<any[]>> {
  return request.get('/v1/dict/' + tableName)
}
export function getDictById(tableName: string, id: number): Promise<R<any>> {
  return request.get('/v1/dict/' + tableName + '/' + id)
}
export function createDictItem(tableName: string, data: any): Promise<R<any>> {
  return request.post('/v1/dict/' + tableName, data)
}
export function updateDictItem(tableName: string, id: number, data: any): Promise<R<any>> {
  return request.put('/v1/dict/' + tableName + '/' + id, data)
}
export function deleteDictItem(tableName: string, id: number): Promise<R<any>> {
  return request.delete('/v1/dict/' + tableName + '/' + id)
}
