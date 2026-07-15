package com.company.rpw.controller;

import com.company.rpw.common.R;
import com.company.rpw.dto.kanban.KanbanBoardVO;
import com.company.rpw.dto.kanban.KanbanCardDTO;
import com.company.rpw.dto.kanban.KanbanColumnVO;
import com.company.rpw.service.KanbanBoardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 看板控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/kanban")
@RequiredArgsConstructor
public class KanbanBoardController {

    private final KanbanBoardService kanbanBoardService;

    /**
     * 获取看板数据
     * GET /api/v1/kanban/board
     * @param projectId 项目ID（可选）
     * @param resourceType 资源类型（可选：SUBTRACT, LABOR, EQUIPMENT, etc.）
     * @return 看板数据（按状态分组的卡片列表）
     */
    @GetMapping("/board")
    public R<KanbanBoardVO> getBoardData(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String resourceType) {
        KanbanBoardVO boardData = kanbanBoardService.getBoardData(projectId, resourceType);
        return R.ok(boardData);
    }

    /**
     * 拖动卡片更新状态
     * PUT /api/v1/kanban/card/status
     * @param dto 卡片状态更新DTO（id, resourceType, newStatus）
     * @return 是否成功
     */
    @PutMapping("/card/status")
    public R<Boolean> updateCardStatus(@RequestBody KanbanCardDTO dto) {
        boolean result = kanbanBoardService.updateCardStatus(dto);
        return result ? R.ok(true) : R.fail(500, "更新状态失败");
    }

    /**
     * 获取看板列配置
     * GET /api/v1/kanban/columns
     * @return 看板列配置列表
     */
    @GetMapping("/columns")
    public R<List<KanbanColumnVO>> getColumnConfig() {
        return R.ok(kanbanBoardService.getColumnConfig());
    }
}
