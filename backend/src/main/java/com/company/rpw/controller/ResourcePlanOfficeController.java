package com.company.rpw.controller;

import com.company.rpw.common.R;
import com.company.rpw.dto.common.ArrivalProgressDTO;
import com.company.rpw.dto.labor.WarningStatusVO;
import com.company.rpw.entity.ResourcePlanOffice;
import com.company.rpw.service.ResourcePlanOfficeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 办公用品资源计划管理控制器
 */
@RestController
@RequestMapping("/api/v1/resource-plan/office")
@RequiredArgsConstructor
public class ResourcePlanOfficeController {

    private final ResourcePlanOfficeService service;

    /**
     * 查询办公用品计划列表（支持按项目/WBS编码/办公用品名称筛选）
     * GET /api/v1/resource-plan/office/list
     * @param projectId 项目ID（可选）
     * @param wbsCode WBS编码（可选，模糊匹配）
     * @param officeName 办公用品名称（可选，模糊匹配）
     * @return 办公用品计划列表
     */
    @GetMapping("/list")
    public R<List<ResourcePlanOffice>> list(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String wbsCode,
            @RequestParam(required = false) String officeName) {
        return R.ok(service.listByParams(projectId, wbsCode, officeName));
    }

    /**
     * 根据ID查询办公用品计划
     * GET /api/v1/resource-plan/office/{id}
     * @param id 办公用品计划ID
     * @return 办公用品计划详情
     */
    @GetMapping("/{id}")
    public R<ResourcePlanOffice> getById(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    /**
     * 新增办公用品计划
     * POST /api/v1/resource-plan/office
     * @param entity 办公用品计划信息
     * @return 是否成功
     */
    @PostMapping
    public R<Boolean> create(@RequestBody ResourcePlanOffice entity) {
        boolean result = service.create(entity);
        return result ? R.ok(true) : R.fail(500, "新增失败");
    }

    /**
     * 修改办公用品计划
     * PUT /api/v1/resource-plan/office/{id}
     * @param id 办公用品计划ID
     * @param entity 办公用品计划信息
     * @return 是否成功
     */
    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody ResourcePlanOffice entity) {
        boolean result = service.updatePlan(id, entity);
        return result ? R.ok(true) : R.fail(500, "修改失败");
    }

    /**
     * 删除办公用品计划
     * DELETE /api/v1/resource-plan/office/{id}
     * @param id 办公用品计划ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        boolean result = service.delete(id);
        return result ? R.ok(true) : R.fail(500, "删除失败");
    }

    /**
     * 提交审批
     * POST /api/v1/resource-plan/office/{id}/submit
     */
    @PostMapping("/{id}/submit")
    public R<Boolean> submit(@PathVariable Long id) {
        ResourcePlanOffice entity = service.getById(id);
        if (entity == null) return R.fail("计划不存在");
        entity.setStatus("SUBMITTED");
        return R.ok(service.updateById(entity));
    }

    /**
     * 登记进展
     * POST /api/v1/resource-plan/office/{id}/progress
     */
    @PostMapping("/{id}/progress")
    public R<Boolean> registerProgress(@PathVariable Long id,
                                       @RequestBody @Valid ArrivalProgressDTO dto) {
        boolean result = service.registerProgress(id, dto.getActualArrivalDate());
        return result ? R.ok(true) : R.fail(500, "登记进展失败");
    }

    /**
     * 查询预警状态
     * GET /api/v1/resource-plan/office/{id}/warning-status
     */
    @GetMapping("/{id}/warning-status")
    public R<WarningStatusVO> getWarningStatus(@PathVariable Long id) {
        return R.ok(service.getWarningStatus(id));
    }
}
