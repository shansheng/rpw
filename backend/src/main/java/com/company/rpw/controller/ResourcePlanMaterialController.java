package com.company.rpw.controller;

import com.company.rpw.common.R;
import com.company.rpw.dto.common.ArrivalProgressDTO;
import com.company.rpw.dto.labor.WarningStatusVO;
import com.company.rpw.dto.subcontract.ChangeDTO;
import com.company.rpw.dto.subcontract.ChangeRecordVO;
import com.company.rpw.entity.ResourcePlanMaterial;
import com.company.rpw.service.ResourcePlanMaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 材料资源计划管理控制器
 */
@RestController
@RequestMapping("/api/v1/resource-plan/material")
@RequiredArgsConstructor
public class ResourcePlanMaterialController {

    private final ResourcePlanMaterialService service;

    /**
     * 查询材料计划列表（支持按项目/状态/WBS编码/材料名称筛选）
     * GET /api/v1/resource-plan/material/list
     * @param projectId 项目ID（可选）
     * @param approvalStatus 审批状态（可选）
     * @param wbsCode WBS编码（可选，模糊匹配）
     * @param resourceName 材料名称（可选，模糊匹配）
     * @return 材料计划列表
     */
    @GetMapping("/list")
    public R<List<ResourcePlanMaterial>> list(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String approvalStatus,
            @RequestParam(required = false) String wbsCode,
            @RequestParam(required = false) String resourceName) {
        return R.ok(service.listByParams(projectId, approvalStatus, wbsCode, resourceName));
    }

    /**
     * 根据ID查询材料计划
     * GET /api/v1/resource-plan/material/{id}
     * @param id 材料计划ID
     * @return 材料计划详情
     */
    @GetMapping("/{id}")
    public R<ResourcePlanMaterial> getById(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    /**
     * 新增材料计划（草稿状态）
     * POST /api/v1/resource-plan/material
     * @param entity 材料计划信息
     * @return 是否成功
     */
    @PostMapping
    public R<Boolean> create(@RequestBody ResourcePlanMaterial entity) {
        boolean result = service.create(entity);
        return result ? R.ok(true) : R.fail(500, "新增失败");
    }

    /**
     * 修改材料计划
     * PUT /api/v1/resource-plan/material/{id}
     * @param id 材料计划ID
     * @param entity 材料计划信息
     * @return 是否成功
     */
    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody ResourcePlanMaterial entity) {
        boolean result = service.updatePlan(id, entity);
        return result ? R.ok(true) : R.fail(500, "修改失败");
    }

    /**
     * 删除材料计划（仅草稿状态可删）
     * DELETE /api/v1/resource-plan/material/{id}
     * @param id 材料计划ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        boolean result = service.delete(id);
        return result ? R.ok(true) : R.fail(500, "删除失败（仅草稿状态可删除）");
    }

    /**
     * 提交审批
     * POST /api/v1/resource-plan/material/{id}/submit
     */
    @PostMapping("/{id}/submit")
    public R<Boolean> submit(@PathVariable Long id) {
        boolean result = service.submit(id);
        return result ? R.ok(true) : R.fail(500, "提交失败");
    }

    /**
     * 登记进展
     * POST /api/v1/resource-plan/material/{id}/progress
     */
    @PostMapping("/{id}/progress")
    public R<Boolean> registerProgress(@PathVariable Long id,
                                       @RequestBody @Valid ArrivalProgressDTO dto) {
        boolean result = service.registerProgress(id, dto.getActualArrivalDate());
        return result ? R.ok(true) : R.fail(500, "登记进展失败");
    }

    /**
     * 查询预警状态
     * GET /api/v1/resource-plan/material/{id}/warning-status
     */
    @GetMapping("/{id}/warning-status")
    public R<WarningStatusVO> getWarningStatus(@PathVariable Long id) {
        return R.ok(service.getWarningStatus(id));
    }

    /**
     * 查询变更历史
     * GET /api/v1/resource-plan/material/{id}/change-history
     */
    @GetMapping("/{id}/change-history")
    public R<List<ChangeRecordVO>> getChangeHistory(@PathVariable Long id) {
        return R.ok(service.getChangeHistory(id));
    }

    /**
     * 发起变更申请
     * POST /api/v1/resource-plan/material/change
     */
    @PostMapping("/change")
    public R<Boolean> createChange(@RequestBody ChangeDTO changeDTO) {
        return R.ok(service.createChange(changeDTO));
    }
}
