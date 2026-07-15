package com.company.rpw.controller;

import com.company.rpw.common.R;
import com.company.rpw.entity.ResourcePlanSubcontract;
import com.company.rpw.service.ResourcePlanSubcontractService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分包计划控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/resource-plan/subcontract")
@RequiredArgsConstructor
public class ResourcePlanSubcontractController {

    private final ResourcePlanSubcontractService service;

    /**
     * 查询分包计划列表
     * GET /api/v1/resource-plan/subcontract/list
     * @param projectId 项目ID（可选）
     * @param status 状态（可选）
     * @param wbsCode WBS编码（可选，模糊匹配）
     * @param subcontractName 分包名称（可选，模糊匹配）
     * @return 分包计划列表
     */
    @GetMapping("/list")
    public R<List<ResourcePlanSubcontract>> list(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String wbsCode,
            @RequestParam(required = false) String subcontractName) {
        var query = service.lambdaQuery()
                .eq(ResourcePlanSubcontract::getDeleted, 0);
        if (projectId != null) {
            query.eq(ResourcePlanSubcontract::getProjectId, projectId);
        }
        if (status != null && !status.isEmpty()) {
            query.eq(ResourcePlanSubcontract::getStatus, status);
        }
        if (wbsCode != null && !wbsCode.isEmpty()) {
            query.like(ResourcePlanSubcontract::getWbsCode, wbsCode);
        }
        if (subcontractName != null && !subcontractName.isEmpty()) {
            query.like(ResourcePlanSubcontract::getSubcontractName, subcontractName);
        }
        return R.ok(query.list());
    }

    /**
     * 根据ID查询分包计划
     * GET /api/v1/resource-plan/subcontract/{id}
     */
    @GetMapping("/{id}")
    public R<ResourcePlanSubcontract> getById(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    /**
     * 新增分包计划（草稿状态）
     * POST /api/v1/resource-plan/subcontract
     */
    @PostMapping
    public R<Boolean> create(@RequestBody ResourcePlanSubcontract entity) {
        boolean result = service.create(entity);
        return result ? R.ok(true) : R.fail(500, "新增失败");
    }

    /**
     * 修改分包计划
     * PUT /api/v1/resource-plan/subcontract/{id}
     */
    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody ResourcePlanSubcontract entity) {
        boolean result = service.updatePlan(id, entity);
        return result ? R.ok(true) : R.fail(500, "修改失败");
    }

    /**
     * 删除分包计划（仅草稿状态可删）
     * DELETE /api/v1/resource-plan/subcontract/{id}
     */
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        boolean result = service.delete(id);
        return result ? R.ok(true) : R.fail(500, "删除失败（仅草稿状态可删除）");
    }

    /**
     * 提交审批
     * POST /api/v1/resource-plan/subcontract/{id}/submit
     */
    @PostMapping("/{id}/submit")
    public R<Boolean> submit(@PathVariable Long id) {
        boolean result = service.submit(id);
        return result ? R.ok(true) : R.fail(500, "提交失败");
    }

    /**
     * 登记进展
     * POST /api/v1/resource-plan/subcontract/{id}/progress
     */
    @PostMapping("/{id}/progress")
    public R<Boolean> registerProgress(@PathVariable Long id,
                                       @RequestBody @Valid com.company.rpw.dto.subcontract.ProgressDTO dto) {
        boolean result = service.registerProgress(id, dto.getActualStartDate(), dto.getActualEndDate(), dto.getRemark());
        return result ? R.ok(true) : R.fail(500, "登记进展失败");
    }

    /**
     * 查询变更历史
     * GET /api/v1/resource-plan/subcontract/{id}/change-history
     */
    @GetMapping("/{id}/change-history")
    public R<List<com.company.rpw.dto.subcontract.ChangeRecordVO>> getChangeHistory(@PathVariable Long id) {
        return R.ok(service.getChangeHistory(id));
    }

    /**
     * 发起变更申请
     * POST /api/v1/resource-plan/subcontract/change
     */
    @PostMapping("/change")
    public R<Boolean> createChange(@RequestBody com.company.rpw.dto.subcontract.ChangeDTO changeDTO) {
        return R.ok(service.createChange(changeDTO));
    }
}
