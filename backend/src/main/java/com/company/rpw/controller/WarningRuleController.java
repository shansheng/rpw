package com.company.rpw.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.rpw.common.R;
import com.company.rpw.entity.WarningRule;
import com.company.rpw.service.WarningAttributeRegistry;
import com.company.rpw.service.WarningExpressionEngine;
import com.company.rpw.service.WarningRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 预警规则管理控制器（表达式驱动）
 */
@RestController
@RequestMapping("/api/v1/warning/rule")
@RequiredArgsConstructor
public class WarningRuleController {

    private final WarningRuleService warningRuleService;
    private final WarningAttributeRegistry attributeRegistry;
    private final WarningExpressionEngine expressionEngine;

    /** 分页列表 */
    @GetMapping("/list")
    public R<IPage<WarningRule>> list(
            @RequestParam(required = false) String objectType,
            @RequestParam(required = false) Integer enabled,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        LambdaQueryWrapper<WarningRule> wrapper = new LambdaQueryWrapper<>();
        if (objectType != null && !objectType.isEmpty()) {
            wrapper.eq(WarningRule::getObjectType, objectType);
        }
        if (enabled != null) {
            wrapper.eq(WarningRule::getEnabled, enabled);
        }
        wrapper.orderByAsc(WarningRule::getPriority).orderByDesc(WarningRule::getCreateTime);
        IPage<WarningRule> page = new Page<>(pageNum, pageSize);
        return R.ok(warningRuleService.page(page, wrapper));
    }

    @GetMapping("/{id}")
    public R<WarningRule> getById(@PathVariable Long id) {
        return R.ok(warningRuleService.getById(id));
    }

    @PostMapping
    public R<Boolean> create(@RequestBody WarningRule entity) {
        if (entity.getEnabled() == null) entity.setEnabled(1);
        if (entity.getPriority() == null) entity.setPriority(100);
        boolean result = warningRuleService.save(entity);
        return result ? R.ok(true) : R.fail(500, "新增失败");
    }

    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody WarningRule entity) {
        entity.setId(id);
        boolean result = warningRuleService.updateById(entity);
        return result ? R.ok(true) : R.fail(500, "修改失败");
    }

    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        boolean result = warningRuleService.removeById(id);
        return result ? R.ok(true) : R.fail(500, "删除失败");
    }

    @PutMapping("/{id}/toggle")
    public R<Boolean> toggle(@PathVariable Long id, @RequestParam Integer enabled) {
        WarningRule entity = new WarningRule();
        entity.setId(id);
        entity.setEnabled(enabled);
        boolean result = warningRuleService.updateById(entity);
        return result ? R.ok(true) : R.fail(500, "操作失败");
    }

    /**
     * 属性元数据：供规则编辑器展示可选对象、属性与系统属性。
     */
    @GetMapping("/attributes")
    public R<Map<String, Object>> attributes(@RequestParam(required = false) String objectType) {
        Map<String, Object> data = new LinkedHashMap<>();
        List<Map<String, String>> objectTypes = WarningAttributeRegistry.OBJECT_TYPE_LABELS.entrySet().stream()
                .map(e -> {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("value", e.getKey());
                    m.put("label", e.getValue());
                    return m;
                }).collect(Collectors.toList());
        data.put("objectTypes", objectTypes);

        List<Map<String, String>> system = WarningAttributeRegistry.SYSTEM_ATTRIBUTES.stream()
                .map(a -> {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("label", a.label);
                    m.put("type", a.type.name());
                    return m;
                }).collect(Collectors.toList());
        data.put("systemAttributes", system);

        List<Map<String, String>> attrs = new ArrayList<>();
        if (objectType != null && attributeRegistry.isSupported(objectType)) {
            attrs = attributeRegistry.getAttributes(objectType).stream()
                    .map(a -> {
                        Map<String, String> m = new LinkedHashMap<>();
                        m.put("label", a.label);
                        m.put("field", a.field);
                        m.put("type", a.type.name());
                        return m;
                    }).collect(Collectors.toList());
        }
        data.put("attributes", attrs);
        return R.ok(data);
    }

    /** 校验表达式语法 */
    @PostMapping("/validate")
    public R<Map<String, Object>> validate(@RequestBody Map<String, String> body) {
        String objectType = body.get("objectType");
        String conditionExpr = body.get("conditionExpr");
        String err = expressionEngine.validate(conditionExpr, objectType, attributeRegistry);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("valid", err == null);
        result.put("message", err);
        return R.ok(result);
    }

    /** 手动立即检查 */
    @PostMapping("/check")
    public R<String> check(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long ruleId) {
        int count = warningRuleService.checkAll(projectId, ruleId);
        return R.ok(String.format("预警检查完成，共产生 %d 条预警", count));
    }
}
