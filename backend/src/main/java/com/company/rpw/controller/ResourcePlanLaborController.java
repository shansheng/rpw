package com.company.rpw.controller;

import com.company.rpw.common.R;
import com.company.rpw.entity.ResourcePlanLabor;
import com.company.rpw.service.ResourcePlanLaborService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 劳动力计划控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/resource-plan/labor")
@RequiredArgsConstructor
public class ResourcePlanLaborController {

    private final ResourcePlanLaborService service;

    /**
     * 查询劳动力计划列表
     * GET /api/v1/resource-plan/labor/list
     * @param projectId 项目ID（可选）
     * @param status 状态（可选）
     * @param wbsCode WBS编码（可选，模糊匹配）
     * @param workTypeCode 工种编码（可选）
     * @param laborCategoryCode 劳务类别编码（可选）
     * @return 劳动力计划列表
     */
    @GetMapping("/list")
    public R<List<ResourcePlanLabor>> list(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String wbsCode,
            @RequestParam(required = false) String workTypeCode,
            @RequestParam(required = false) String laborCategoryCode) {
        return R.ok(service.listByParams(projectId, status, wbsCode, workTypeCode, laborCategoryCode));
    }

    /**
     * 根据ID查询劳动力计划
     * GET /api/v1/resource-plan/labor/{id}
     */
    @GetMapping("/{id}")
    public R<ResourcePlanLabor> getById(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    /**
     * 新增劳动力计划（草稿状态）
     * POST /api/v1/resource-plan/labor
     */
    @PostMapping
    public R<Boolean> create(@RequestBody ResourcePlanLabor entity) {
        boolean result = service.create(entity);
        return result ? R.ok(true) : R.fail(500, "新增失败");
    }

    /**
     * 修改劳动力计划
     * PUT /api/v1/resource-plan/labor/{id}
     */
    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody ResourcePlanLabor entity) {
        boolean result = service.updatePlan(id, entity);
        return result ? R.ok(true) : R.fail(500, "修改失败");
    }

    /**
     * 删除劳动力计划（仅草稿状态可删）
     * DELETE /api/v1/resource-plan/labor/{id}
     */
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        boolean result = service.delete(id);
        return result ? R.ok(true) : R.fail(500, "删除失败（仅草稿状态可删除）");
    }

    /**
     * 提交审批
     * POST /api/v1/resource-plan/labor/{id}/submit
     */
    @PostMapping("/{id}/submit")
    public R<Boolean> submit(@PathVariable Long id) {
        boolean result = service.submit(id);
        return result ? R.ok(true) : R.fail(500, "提交失败");
    }

    /**
     * 登记进展
     * POST /api/v1/resource-plan/labor/{id}/progress
     */
    @PostMapping("/{id}/progress")
    public R<Boolean> registerProgress(@PathVariable Long id,
                                       @RequestBody @Valid com.company.rpw.dto.labor.LaborProgressDTO dto) {
        boolean result = service.registerProgress(id, dto.getActualQuantity(), dto.getActualStartDate(), dto.getActualEndDate(), dto.getAttendanceRecords());
        return result ? R.ok(true) : R.fail(500, "登记进展失败");
    }

    /**
     * 查询预警状态
     * GET /api/v1/resource-plan/labor/{id}/warning-status
     */
    @GetMapping("/{id}/warning-status")
    public R<com.company.rpw.dto.labor.WarningStatusVO> getWarningStatus(@PathVariable Long id) {
        return R.ok(service.getWarningStatus(id));
    }

    /**
     * 查询变更历史
     * GET /api/v1/resource-plan/labor/{id}/change-history
     */
    @GetMapping("/{id}/change-history")
    public R<List<com.company.rpw.dto.subcontract.ChangeRecordVO>> getChangeHistory(@PathVariable Long id) {
        return R.ok(service.getChangeHistory(id));
    }
}
