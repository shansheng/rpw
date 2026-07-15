package com.company.rpw.dto.kanban;

import lombok.Data;

import java.util.List;

/**
 * 看板数据视图对象
 */
@Data
public class KanbanBoardVO {

    /** 看板列数据 */
    private List<KanbanColumnVO> columns;

    /** 卡片总数 */
    private Integer totalCards;
}
