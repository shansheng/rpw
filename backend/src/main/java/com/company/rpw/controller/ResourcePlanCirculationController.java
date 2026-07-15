package com.company.rpw.controller;

import com.company.rpw.common.R;
import com.company.rpw.dto.common.ArrivalProgressDTO;
import com.company.rpw.dto.labor.WarningStatusVO;
import com.company.rpw.entity.ResourcePlanCirculation;
import com.company.rpw.service.ResourcePlanCirculationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 周转材计划管理控制器
 */
@RestController
@RequestMapping("/api/v1/resource-plan/circulation")
@RequiredArgsConstructor
public class ResourcePlanCirculationController {

    private final ResourcePlanCirculationService service;

    /**
     * 查询周转材计划列表（支持按项目/WBS编码/周转材名称筛选）
     * GET /api/v1/resource-plan/circulation/list
     * @param projectId 项目ID（可选）
     * @param wbsCode WBS编码（可选，模糊匹配）
     * @param circulationName 周转材名称（可选，模糊匹配）
     * @return 周转材计划列表
     */
    @GetMapping("/list")
    public R<List<ResourcePlanCirculation>> list(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String wbsCode,
            @RequestParam(required = false) String circulationName) {
        return R.ok(service.listByParams(projectId, wbsCode, circulationName));
    }

    /**
     * 根据ID查询周转材计划
     * GET /api/v1/resource-plan/circulation/{id}
     * @param id 周转材计划ID
     * @return 周转材计划详情
     */
    @GetMapping("/{id}")
    public R<ResourcePlanCirculation> getById(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    /**
     * 新增周转材计划
     * POST /api/v1/resource-plan/circulation
     * @param entity 周转材计划信息
     * @return 是否成功
     */
    @PostMapping
    public R<Boolean> create(@RequestBody ResourcePlanCirculation entity) {
        boolean result = service.create(entity);
        return result ? R.ok(true) : R.fail(500, "新增失败");
    }

    /**
     * 修改周转材计划
     * PUT /api/v1/resource-plan/circulation/{id}
     * @param id 周转材计划ID
     * @param entity 周转材计划信息
     * @return 是否成功
     */
    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody ResourcePlanCirculation entity) {
        boolean result = service.updatePlan(id, entity);
        return result ? R.ok(true) : R.fail(500, "修改失败");
    }

    /**
     * 删除周转材计划
     * DELETE /api/v1/resource-plan/circulation/{id}
     * @param id 周转材计划ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        boolean result = service.delete(id);
        return result ? R.ok(true) : R.fail(500, "删除失败");
    }

    /**
     * 提交审批
     * POST /api/v1/resource-plan/circulation/{id}/submit
     */
    @PostMapping("/{id}/submit")
    public R<Boolean> submit(@PathVariable Long id) {
        ResourcePlanCirculation entity = service.getById(id);
        if (entity == null) return R.fail("计划不存在");
        entity.setStatus("SUBMITTED");
        return R.ok(service.updateById(entity));
    }

    /**
     * 登记进展
     * POST /api/v1/resource-plan/circulation/{id}/progress
     */
    @PostMapping("/{id}/progress")
    public R<Boolean> registerProgress(@PathVariable Long id,
                                       @RequestBody @Valid ArrivalProgressDTO dto) {
        boolean result = service.registerProgress(id, dto.getActualArrivalDate());
        return result ? R.ok(true) : R.fail(500, "登记进展失败");
    }

    /**
     * 查询预警状态
     * GET /api/v1/resource-plan/circulation/{id}/warning-status
     */
    @GetMapping("/{id}/warning-status")
    public R<WarningStatusVO> getWarningStatus(@PathVariable Long id) {
        return R.ok(service.getWarningStatus(id));
    }
}
