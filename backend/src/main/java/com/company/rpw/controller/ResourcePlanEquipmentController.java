package com.company.rpw.controller;

import com.company.rpw.common.R;
import com.company.rpw.dto.common.ArrivalProgressDTO;
import com.company.rpw.dto.labor.WarningStatusVO;
import com.company.rpw.entity.ResourcePlanEquipment;
import com.company.rpw.service.ResourcePlanEquipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 设备资源计划管理控制器
 */
@RestController
@RequestMapping("/api/v1/resource-plan/equipment")
@RequiredArgsConstructor
public class ResourcePlanEquipmentController {

    private final ResourcePlanEquipmentService service;

    /**
     * 查询设备计划列表（支持按项目/WBS编码/设备名称筛选）
     * GET /api/v1/resource-plan/equipment/list
     * @param projectId 项目ID（可选）
     * @param wbsCode WBS编码（可选，模糊匹配）
     * @param equipmentName 设备名称（可选，模糊匹配）
     * @return 设备计划列表
     */
    @GetMapping("/list")
    public R<List<ResourcePlanEquipment>> list(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String wbsCode,
            @RequestParam(required = false) String equipmentName) {
        return R.ok(service.listByParams(projectId, wbsCode, equipmentName));
    }

    /**
     * 根据ID查询设备计划
     * GET /api/v1/resource-plan/equipment/{id}
     * @param id 设备计划ID
     * @return 设备计划详情
     */
    @GetMapping("/{id}")
    public R<ResourcePlanEquipment> getById(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    /**
     * 新增设备计划
     * POST /api/v1/resource-plan/equipment
     * @param entity 设备计划信息
     * @return 是否成功
     */
    @PostMapping
    public R<Boolean> create(@RequestBody ResourcePlanEquipment entity) {
        boolean result = service.create(entity);
        return result ? R.ok(true) : R.fail(500, "新增失败");
    }

    /**
     * 修改设备计划
     * PUT /api/v1/resource-plan/equipment/{id}
     * @param id 设备计划ID
     * @param entity 设备计划信息
     * @return 是否成功
     */
    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody ResourcePlanEquipment entity) {
        boolean result = service.updatePlan(id, entity);
        return result ? R.ok(true) : R.fail(500, "修改失败");
    }

    /**
     * 删除设备计划
     * DELETE /api/v1/resource-plan/equipment/{id}
     * @param id 设备计划ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        boolean result = service.delete(id);
        return result ? R.ok(true) : R.fail(500, "删除失败");
    }

    /**
     * 提交审批
     * POST /api/v1/resource-plan/equipment/{id}/submit
     */
    @PostMapping("/{id}/submit")
    public R<Boolean> submit(@PathVariable Long id) {
        ResourcePlanEquipment entity = service.getById(id);
        if (entity == null) return R.fail("计划不存在");
        entity.setStatus("SUBMITTED");
        return R.ok(service.updateById(entity));
    }

    /**
     * 登记进展
     * POST /api/v1/resource-plan/equipment/{id}/progress
     */
    @PostMapping("/{id}/progress")
    public R<Boolean> registerProgress(@PathVariable Long id,
                                       @RequestBody @Valid ArrivalProgressDTO dto) {
        boolean result = service.registerProgress(id, dto.getActualArrivalDate());
        return result ? R.ok(true) : R.fail(500, "登记进展失败");
    }

    /**
     * 查询预警状态
     * GET /api/v1/resource-plan/equipment/{id}/warning-status
     */
    @GetMapping("/{id}/warning-status")
    public R<WarningStatusVO> getWarningStatus(@PathVariable Long id) {
        return R.ok(service.getWarningStatus(id));
    }
}
