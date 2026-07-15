package com.company.rpw.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.rpw.common.R;
import com.company.rpw.entity.WarningRule;
import com.company.rpw.service.WarningRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import java.util.List;

/**
 * 预警规则管理控制器
 */
@RestController
@RequestMapping("/api/v1/warning/rule")
@RequiredArgsConstructor
public class WarningRuleController {

    private final WarningRuleService warningRuleService;

    /**
     * 查询预警规则列表（支持分页和筛选）
     * GET /api/v1/warning/rule/list
     * @param resourceType 资源类型（可选）
     * @param projectId 项目ID（可选）
     * @param enabled 是否启用（可选）
     * @param pageNum 页码（默认1）
     * @param pageSize 每页数量（默认10）
     * @return 预警规则列表
     */
    @GetMapping("/list")
    public R<IPage<WarningRule>> list(
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Integer enabled,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        LambdaQueryWrapper<WarningRule> wrapper = new LambdaQueryWrapper<>();
        if (resourceType != null && !resourceType.isEmpty()) {
            wrapper.eq(WarningRule::getResourceType, resourceType);
        }
        if (projectId != null) {
            wrapper.eq(WarningRule::getProjectId, projectId);
        }
        if (enabled != null) {
            wrapper.eq(WarningRule::getEnabled, enabled);
        }
        wrapper.orderByDesc(WarningRule::getCreateTime);
        
        IPage<WarningRule> page = new Page<>(pageNum, pageSize);
        IPage<WarningRule> result = warningRuleService.page(page, wrapper);
        return R.ok(result);
    }

    /**
     * 根据ID查询预警规则
     * GET /api/v1/warning/rule/{id}
     * @param id 预警规则ID
     * @return 预警规则详情
     */
    @GetMapping("/{id}")
    public R<WarningRule> getById(@PathVariable Long id) {
        WarningRule entity = warningRuleService.getById(id);
        return R.ok(entity);
    }

    /**
     * 新增预警规则
     * POST /api/v1/warning/rule
     * @param entity 预警规则信息
     * @return 是否成功
     */
    @PostMapping
    public R<Boolean> create(@RequestBody WarningRule entity) {
        boolean result = warningRuleService.save(entity);
        return result ? R.ok(true) : R.fail(500, "新增失败");
    }

    /**
     * 修改预警规则
     * PUT /api/v1/warning/rule/{id}
     * @param id 预警规则ID
     * @param entity 预警规则信息
     * @return 是否成功
     */
    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody WarningRule entity) {
        entity.setId(id);
        boolean result = warningRuleService.updateById(entity);
        return result ? R.ok(true) : R.fail(500, "修改失败");
    }

    /**
     * 删除预警规则
     * DELETE /api/v1/warning/rule/{id}
     * @param id 预警规则ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        boolean result = warningRuleService.removeById(id);
        return result ? R.ok(true) : R.fail(500, "删除失败");
    }

    /**
     * 启用/禁用预警规则
     * PUT /api/v1/warning/rule/{id}/toggle
     * @param id 预警规则ID
     * @param enabled 是否启用（0-禁用，1-启用）
     * @return 是否成功
     */
    @PutMapping("/{id}/toggle")
    public R<Boolean> toggle(@PathVariable Long id, @RequestParam Integer enabled) {
        WarningRule entity = new WarningRule();
        entity.setId(id);
        entity.setEnabled(enabled);
        boolean result = warningRuleService.updateById(entity);
        return result ? R.ok(true) : R.fail(500, "操作失败");
    }

    /**
     * 手动触发预警检查
     * POST /api/v1/warning/rule/check
     * @param projectId 项目ID（可选）
     * @param ruleId 规则ID（可选）
     * @return 检查结果
     */
    @PostMapping("/check")
    public R<String> check(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long ruleId) {
        String result = warningRuleService.checkWarning(projectId, ruleId);
        return R.ok(result);
    }
}
