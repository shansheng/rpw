import { requestClient } from '#/api/request';

// ========== 分包计划变更（主从结构 + p_fbchange 流程） ==========

/** 变更主表 */
export interface SubcontractChange {
  id?: number;
  /** 选中的分包计划ID（编辑界面主表"id"字段） */
  planId?: number;
  projectId?: number;
  projectName?: string;
  specialtyEngineering?: string;
  subcontractName?: string;
  subcontractMode?: string;
  teamSource?: string;
  remark?: string;
  /** 状态：RUNNING 审批中 / APPROVED 已通过 / CANCEL 已取消 */
  status?: string;
  processInstanceId?: string;
  creator?: string;
  createTime?: string;
}

/** 变更明细 */
export interface SubcontractChangeDetail {
  id?: number;
  changeId?: number;
  seq?: number;
  /** 日期类型：1最晚进场 2招标文件 3挂网 4定标 */
  dateType?: number;
  /** 原日期（取自所选分包计划当前值） */
  originalDate?: string;
  /** 调整后日期（用户填写） */
  adjustedDate?: string;
}

/** 创建变更请求 */
export interface SubcontractChangeCreate {
  planId?: number;
  projectId?: number;
  projectName?: string;
  specialtyEngineering?: string;
  subcontractName?: string;
  subcontractMode?: string;
  teamSource?: string;
  remark?: string;
  details?: SubcontractChangeDetail[];
  /** 发起人自选审批人：节点ID -> 用户ID列表 */
  startUserSelectAssignees?: Record<string, number[]>;
}

/** 日期类型选项：1最晚进场 2招标文件 3挂网 4定标 */
export const DATE_TYPE_OPTIONS = [
  { value: 1, label: '最晚进场日期', field: 'latestEntryDate' },
  { value: 2, label: '招标文件日期', field: 'startPrepareBidDate' },
  { value: 3, label: '挂网日期', field: 'plannedOnlineBidDate' },
  { value: 4, label: '定标日期', field: 'plannedAwardDate' },
] as const;

export function dateTypeLabel(dateType?: number): string {
  return DATE_TYPE_OPTIONS.find((o) => o.value === dateType)?.label ?? '未选择';
}

export const STATUS_LABEL: Record<string, string> = {
  RUNNING: '审批中',
  APPROVED: '已通过',
  CANCEL: '已取消',
};

export const subcontractChangeApi = {
  /** 分页查询 */
  page: (params: Record<string, any>) =>
    requestClient.get<{ list: SubcontractChange[]; total: number }>(
      '/resource-plan/subcontract-change/page',
      { params },
    ),
  /** 根据ID查询 */
  get: (id: number) =>
    requestClient.get<SubcontractChange>(
      `/resource-plan/subcontract-change/get?id=${id}`,
    ),
  /** 查询明细 */
  details: (changeId: number) =>
    requestClient.get<SubcontractChangeDetail[]>(
      `/resource-plan/subcontract-change/details?changeId=${changeId}`,
    ),
  /** 创建并启动流程 */
  create: (data: SubcontractChangeCreate) =>
    requestClient.post<number>('/resource-plan/subcontract-change/create', data),
  /** 取消 */
  cancel: (id: number, reason?: string) =>
    requestClient.delete(
      `/resource-plan/subcontract-change/cancel?id=${id}${
        reason ? `&reason=${encodeURIComponent(reason)}` : ''
      }`,
    ),
};
