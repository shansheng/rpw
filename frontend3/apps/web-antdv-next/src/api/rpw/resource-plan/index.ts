import { requestClient } from '#/api/request';

// ========== 资源计划实体类型 ==========

/** 劳动力资源计划 */
export interface ResourcePlanLabor {
  id?: number;
  projectId?: number;
  wbsName?: string;
  workTypeCode?: string;
  workTypeName?: string;
  laborCategoryCode?: string;
  laborCategoryName?: string;
  planQuantity?: number;
  planStartDate?: string;
  planEndDate?: string;
  actualQuantity?: number;
  actualStartDate?: string;
  actualEndDate?: string;
  approvalStatus?: string;
  remark?: string;
  createTime?: string;
}

/** 材料资源计划 */
export interface ResourcePlanMaterial {
  id?: number;
  projectId?: number;
  wbsCode?: string;
  resourceName?: string;
  specification?: string;
  unit?: string;
  budgetQuantity?: number;
  supplierCode?: string;
  purchaseSourceCode?: string;
  purchaseProgressCode?: string;
  shippingProgressCode?: string;
  planArrivalDate?: string;
  actualArrivalDate?: string;
  approvalStatus?: string;
  remark?: string;
  createTime?: string;
}

/** 机械设备资源计划 */
export interface ResourcePlanEquipment {
  id?: number;
  projectId?: number;
  wbsCode?: string;
  equipmentName?: string;
  specification?: string;
  unit?: string;
  budgetQuantity?: number;
  supplierCode?: string;
  purchaseProgressCode?: string;
  planArrivalDate?: string;
  actualArrivalDate?: string;
  remark?: string;
  createTime?: string;
}

/** 五金资源计划 */
export interface ResourcePlanHardware {
  id?: number;
  projectId?: number;
  wbsCode?: string;
  hardwareName?: string;
  specification?: string;
  unit?: string;
  budgetQuantity?: number;
  supplierCode?: string;
  purchaseProgressCode?: string;
  planArrivalDate?: string;
  actualArrivalDate?: string;
  remark?: string;
  createTime?: string;
}

/** 办公资源计划 */
export interface ResourcePlanOffice {
  id?: number;
  projectId?: number;
  wbsCode?: string;
  officeName?: string;
  specification?: string;
  unit?: string;
  budgetQuantity?: number;
  supplierCode?: string;
  purchaseProgressCode?: string;
  planArrivalDate?: string;
  actualArrivalDate?: string;
  remark?: string;
  createTime?: string;
}

/** 安全资源计划 */
export interface ResourcePlanSafety {
  id?: number;
  projectId?: number;
  wbsCode?: string;
  safetyName?: string;
  specification?: string;
  unit?: string;
  budgetQuantity?: number;
  supplierCode?: string;
  purchaseProgressCode?: string;
  planArrivalDate?: string;
  actualArrivalDate?: string;
  remark?: string;
  createTime?: string;
}

/** 周转材料资源计划 */
export interface ResourcePlanCirculation {
  id?: number;
  projectId?: number;
  wbsCode?: string;
  circulationName?: string;
  specification?: string;
  unit?: string;
  budgetQuantity?: number;
  supplierCode?: string;
  purchaseProgressCode?: string;
  planArrivalDate?: string;
  actualArrivalDate?: string;
  remark?: string;
  createTime?: string;
}

/** 分包资源计划 */
export interface ResourcePlanSubcontract {
  id?: number;
  projectId?: number;
  wbsName?: string;
  subcontractName?: string;
  workContent?: string;
  supplierCode?: string;
  supplierName?: string;
  planStartDate?: string;
  planEndDate?: string;
  actualStartDate?: string;
  actualEndDate?: string;
  approvalStatus?: string;
  remark?: string;
  createTime?: string;
}

/** 进度上报 DTO */
export interface ResourcePlanProgressDTO {
  progress?: number;
  progressDesc?: string;
  progressDate?: string;
}

/** 变更 DTO */
export interface ResourcePlanChangeDTO {
  id?: number;
  projectId?: number;
  changeType?: string;
  changeReason?: string;
  changeContent?: string;
  remark?: string;
}

/** 预警状态 */
export interface ResourcePlanWarningStatus {
  warning?: boolean;
  level?: string;
  message?: string;
}

/** 资源计划 API 契约 */
export interface ResourcePlanApi<T = any> {
  getList: (params?: Record<string, any>) => Promise<T[]>;
  get: (id: number) => Promise<T>;
  create: (data: T) => Promise<void>;
  update: (data: T) => Promise<void>;
  remove: (id: number) => Promise<void>;
  submit: (id: number) => Promise<void>;
  progress: (id: number, dto: ResourcePlanProgressDTO) => Promise<void>;
  getWarningStatus: (id: number) => Promise<ResourcePlanWarningStatus>;
  getChangeHistory?: (id: number) => Promise<any[]>;
  change?: (dto: ResourcePlanChangeDTO) => Promise<void>;
}

/**
 * 创建资源计划 API 对象（8 种类型共享同一 CRUD 契约）
 * @param type 资源计划类型：labor | material | equipment | hardware | office | safety | circulation | subcontract
 */
export function createResourcePlanApi<T = any>(type: string): ResourcePlanApi<T> {
  const api: ResourcePlanApi<T> = {
    getList: (params?: Record<string, any>) =>
      requestClient.get<T[]>(`/resource-plan/${type}/list`, { params }),
    get: (id: number) => requestClient.get<T>(`/resource-plan/${type}/${id}`),
    create: (data: T) =>
      requestClient.post(`/resource-plan/${type}`, data),
    update: (data: T) =>
      requestClient.put(`/resource-plan/${type}/${data.id}`, data),
    remove: (id: number) =>
      requestClient.delete(`/resource-plan/${type}/${id}`),
    submit: (id: number) =>
      requestClient.post(`/resource-plan/${type}/${id}/submit`),
    progress: (id: number, dto: ResourcePlanProgressDTO) =>
      requestClient.post(`/resource-plan/${type}/${id}/progress`, dto),
    getWarningStatus: (id: number) =>
      requestClient.get<ResourcePlanWarningStatus>(
        `/resource-plan/${type}/${id}/warning-status`,
      ),
  };

  // labor / material / subcontract 额外支持变更历史
  if (['labor', 'material', 'subcontract'].includes(type)) {
    api.getChangeHistory = (id: number) =>
      requestClient.get<any[]>(`/resource-plan/${type}/${id}/change-history`);
  }

  // material / subcontract 额外支持变更提交
  if (['material', 'subcontract'].includes(type)) {
    api.change = (dto: ResourcePlanChangeDTO) =>
      requestClient.post(`/resource-plan/${type}/change`, dto);
  }

  return api;
}

// ========== 8 种类型 API 实例 ==========

export const laborApi = createResourcePlanApi<ResourcePlanLabor>('labor');
export const materialApi = createResourcePlanApi<ResourcePlanMaterial>('material');
export const equipmentApi =
  createResourcePlanApi<ResourcePlanEquipment>('equipment');
export const hardwareApi = createResourcePlanApi<ResourcePlanHardware>('hardware');
export const officeApi = createResourcePlanApi<ResourcePlanOffice>('office');
export const safetyApi = createResourcePlanApi<ResourcePlanSafety>('safety');
export const circulationApi =
  createResourcePlanApi<ResourcePlanCirculation>('circulation');
export const subcontractApi =
  createResourcePlanApi<ResourcePlanSubcontract>('subcontract');
