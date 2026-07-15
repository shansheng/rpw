import { requestClient } from '#/api/request';

export namespace KanbanApi {
  /** 看板卡片 */
  export interface KanbanCardVO {
    id?: number;
    resourceType?: string;
    wbsCode?: string;
    resourceName?: string;
    status?: string;
    responsiblePerson?: string;
    planStartDate?: string;
    planEndDate?: string;
    priority?: string;
  }

  /** 看板列 */
  export interface KanbanColumnVO {
    statusKey?: string;
    statusName?: string;
    columnOrder?: number;
    cards?: KanbanCardVO[];
  }

  /** 看板数据 */
  export interface KanbanBoardVO {
    columns?: KanbanColumnVO[];
    totalCards?: number;
  }

  /** 卡片状态更新 */
  export interface KanbanCardDTO {
    id: number;
    resourceType: string;
    newStatus: string;
  }
}

/** 获取看板数据 */
export function getBoard(params?: { projectId?: number; resourceType?: string }) {
  return requestClient.get<KanbanApi.KanbanBoardVO>('/kanban/board', { params });
}

/** 获取看板列配置 */
export function getColumns() {
  return requestClient.get<KanbanApi.KanbanColumnVO[]>('/kanban/columns');
}

/** 更新卡片状态 */
export function updateCardStatus(dto: KanbanApi.KanbanCardDTO) {
  return requestClient.put<boolean>('/kanban/card/status', dto);
}
