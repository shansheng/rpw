package com.company.rpw.controller;

import com.company.rpw.common.R;
import com.company.rpw.dto.common.ArrivalProgressDTO;
import com.company.rpw.dto.labor.WarningStatusVO;
import com.company.rpw.entity.ResourcePlanHardware;
import com.company.rpw.service.ResourcePlanHardwareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 五金计划管理控制器
 */
@RestController
@RequestMapping("/api/v1/resource-plan/hardware")
@RequiredArgsConstructor
public class ResourcePlanHardwareController {

    private final ResourcePlanHardwareService service;

    /**
     * 查询五金计划列表（支持按项目/WBS编码/五金名称筛选）
     * GET /api/v1/resource-plan/hardware/list
     * @param projectId 项目ID（可选）
     * @param wbsCode WBS编码（可选，模糊匹配）
     * @param hardwareName 五金名称（可选，模糊匹配）
     * @return 五金计划列表
     */
    @GetMapping("/list")
    public R<List<ResourcePlanHardware>> list(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String wbsCode,
            @RequestParam(required = false) String hardwareName) {
        return R.ok(service.listByParams(projectId, wbsCode, hardwareName));
    }

    /**
     * 根据ID查询五金计划
     * GET /api/v1/resource-plan/hardware/{id}
     * @param id 五金计划ID
     * @return 五金计划详情
     */
    @GetMapping("/{id}")
    public R<ResourcePlanHardware> getById(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    /**
     * 新增五金计划
     * POST /api/v1/resource-plan/hardware
     * @param entity 五金计划信息
     * @return 是否成功
     */
    @PostMapping
    public R<Boolean> create(@RequestBody ResourcePlanHardware entity) {
        boolean result = service.create(entity);
        return result ? R.ok(true) : R.fail(500, "新增失败");
    }

    /**
     * 修改五金计划
     * PUT /api/v1/resource-plan/hardware/{id}
     * @param id 五金计划ID
     * @param entity 五金计划信息
     * @return 是否成功
     */
    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody ResourcePlanHardware entity) {
        boolean result = service.updatePlan(id, entity);
        return result ? R.ok(true) : R.fail(500, "修改失败");
    }

    /**
     * 删除五金计划
     * DELETE /api/v1/resource-plan/hardware/{id}
     * @param id 五金计划ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        boolean result = service.delete(id);
        return result ? R.ok(true) : R.fail(500, "删除失败");
    }

    /**
     * 提交审批
     * POST /api/v1/resource-plan/hardware/{id}/submit
     */
    @PostMapping("/{id}/submit")
    public R<Boolean> submit(@PathVariable Long id) {
        ResourcePlanHardware entity = service.getById(id);
        if (entity == null) return R.fail("计划不存在");
        entity.setStatus("SUBMITTED");
        return R.ok(service.updateById(entity));
    }

    /**
     * 登记进展
     * POST /api/v1/resource-plan/hardware/{id}/progress
     */
    @PostMapping("/{id}/progress")
    public R<Boolean> registerProgress(@PathVariable Long id,
                                       @RequestBody @Valid ArrivalProgressDTO dto) {
        boolean result = service.registerProgress(id, dto.getActualArrivalDate());
        return result ? R.ok(true) : R.fail(500, "登记进展失败");
    }

    /**
     * 查询预警状态
     * GET /api/v1/resource-plan/hardware/{id}/warning-status
     */
    @GetMapping("/{id}/warning-status")
    public R<WarningStatusVO> getWarningStatus(@PathVariable Long id) {
        return R.ok(service.getWarningStatus(id));
    }
}
