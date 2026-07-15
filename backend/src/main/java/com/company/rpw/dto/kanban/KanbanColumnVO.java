package com.company.rpw.dto.kanban;

import lombok.Data;

import java.util.List;

/**
 * 看板列视图对象
 */
@Data
public class KanbanColumnVO {

    /** 状态标识（如：DRAFT, SUBMITTED, IN_PROGRESS, COMPLETED） */
    private String statusKey;

    /** 状态名称（如：草稿、已提交、进行中、已完成） */
    private String statusName;

    /** 列排序 */
    private Integer columnOrder;

    /** 该列下的卡片列表 */
    private List<KanbanCardVO> cards;
}
