import { requestClient } from '#/api/request';

export namespace DictApi {
  /** 字典行（动态字段） */
  export type DictRow = Record<string, any>;
}

/** 获取所有字典表名 */
export function getTables() {
  return requestClient.get<string[]>('/dict/tables');
}

/** 查询字典列表 */
export function listDict(tableName: string) {
  return requestClient.get<DictApi.DictRow[]>(`/dict/${tableName}`);
}

/** 查询字典详情 */
export function getDict(tableName: string, id: number) {
  return requestClient.get<DictApi.DictRow>(`/dict/${tableName}/${id}`);
}

/** 新增字典 */
export function createDict(tableName: string, data: DictApi.DictRow) {
  return requestClient.post<boolean>(`/dict/${tableName}`, data);
}

/** 修改字典 */
export function updateDict(tableName: string, id: number, data: DictApi.DictRow) {
  return requestClient.put<boolean>(`/dict/${tableName}/${id}`, data);
}

/** 删除字典 */
export function removeDict(tableName: string, id: number) {
  return requestClient.delete<boolean>(`/dict/${tableName}/${id}`);
}
