package com.company.rpw.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.rpw.entity.*;
import com.company.rpw.mapper.WarningRuleMapper;
import com.company.rpw.mapper.WarningRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 预警规则服务：根据规则定义与计划数据解析应产生的预警。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarningRuleService extends ServiceImpl<WarningRuleMapper, WarningRule> {

    private final WarningRecordMapper warningRecordMapper;
    private final WarningRecordService warningRecordService;
    private final WarningAttributeRegistry attributeRegistry;
    private final WarningExpressionEngine expressionEngine;

    private final ResourcePlanSubcontractService subcontractService;
    private final ResourcePlanMaterialService materialService;
    private final ResourcePlanEquipmentService equipmentService;
    private final ResourcePlanLaborService laborService;
    private final ResourcePlanHardwareService hardwareService;
    private final ResourcePlanCirculationService circulationService;
    private final ResourcePlanOfficeService officeService;
    private final ResourcePlanSafetyService safetyService;

    /** 预警等级中文 */
    public static String levelLabel(String level) {
        return switch (level == null ? "" : level) {
            case "RED" -> "红色预警";
            case "ORANGE" -> "橙色预警";
            case "YELLOW" -> "黄色预警";
            default -> level;
        };
    }

    // ===================== 入口 =====================

    /**
     * 执行全部启用规则的预警检查（供定时任务/手动触发调用）。
     *
     * @param projectId 限制项目（可选）
     * @param ruleId    限制规则（可选）
     * @return 检查结果描述
     */
    /**
     * 全量/指定规则检查，返回本次新生成的预警记录数。
     * 已存在 PENDING 状态的同规则同计划预警不会重复生成。
     */
    public int checkAll(Long projectId, Long ruleId) {
        LambdaQueryWrapper<WarningRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WarningRule::getEnabled, 1);
        if (ruleId != null) {
            wrapper.eq(WarningRule::getId, ruleId);
        }
        wrapper.orderByAsc(WarningRule::getPriority).orderByDesc(WarningRule::getCreateTime);

        List<WarningRule> rules = list(wrapper);
        int total = 0;
        for (WarningRule rule : rules) {
            try {
                int n = checkRule(rule, projectId);
                total += n;
                log.info("预警规则检查完成: ruleId={}, name={}, type={}, 命中={}",
                        rule.getId(), rule.getName(), rule.getObjectType(), n);
            } catch (Exception e) {
                log.error("预警规则检查失败: ruleId={}, error={}", rule.getId(), e.getMessage(), e);
            }
        }
        return total;
    }

    /** 单条规则检查 */
    private int checkRule(WarningRule rule, Long projectId) {
        List<?> plans = getPlans(rule.getObjectType(), projectId);
        int triggered = 0;
        for (Object plan : plans) {
            Map<String, Object> vars = buildVariables(rule.getObjectType(), plan);
            boolean match;
            try {
                match = expressionEngine.evaluate(rule.getConditionExpr(), vars);
            } catch (Exception e) {
                log.warn("规则表达式评估异常: ruleId={}, planId={}, error={}",
                        rule.getId(), attributeRegistry.getFieldValue(plan, "id"), e.getMessage());
                continue;
            }
            if (!match) {
                continue;
            }
            Long planId = toLong(attributeRegistry.getFieldValue(plan, "id"));
            if (planId == null) {
                continue;
            }
            if (existsActive(rule.getId(), planId)) {
                continue; // 已存在该计划的同规则预警，不重复产生
            }
            createRecord(rule, plan, vars);
            triggered++;
        }
        return triggered;
    }

    // ===================== 计划获取 =====================

    @SuppressWarnings("unchecked")
    private List<?> getPlans(String objectType, Long projectId) {
        List<?> all = switch (objectType) {
            case "SUBCONTRACT" -> subcontractService.list();
            case "MATERIAL" -> materialService.list();
            case "EQUIPMENT" -> equipmentService.list();
            case "LABOR" -> laborService.list();
            case "HARDWARE" -> hardwareService.list();
            case "CIRCULATION" -> circulationService.list();
            case "OFFICE" -> officeService.list();
            case "SAFETY" -> safetyService.list();
            default -> {
                log.warn("未知的预警对象类型: {}", objectType);
                yield List.of();
            }
        };
        if (projectId == null || all.isEmpty()) {
            return all;
        }
        final Long pid = projectId;
        return ((List<Object>) all).stream()
                .filter(p -> {
                    Object v = attributeRegistry.getFieldValue(p, "projectId");
                    return v != null && v.equals(pid);
                })
                .toList();
    }

    // ===================== 变量 & 记录 =====================

    /** 为某条计划构建表达式变量（属性 + 系统属性） */
    private Map<String, Object> buildVariables(String objectType, Object plan) {
        Map<String, Object> vars = new HashMap<>();
        for (WarningAttributeRegistry.AttributeMeta a : attributeRegistry.getAttributes(objectType)) {
            vars.put(a.label, attributeRegistry.getFieldValue(plan, a.field));
        }
        LocalDate today = LocalDate.now();
        vars.put("当前日期", today);
        vars.put("当前时间", today);
        vars.put("今天", today);
        return vars;
    }

    private boolean existsActive(Long ruleId, Long planId) {
        LambdaQueryWrapper<WarningRecord> w = new LambdaQueryWrapper<>();
        w.eq(WarningRecord::getRuleId, ruleId)
                .eq(WarningRecord::getPlanId, planId)
                .eq(WarningRecord::getStatus, "PENDING")
                .eq(WarningRecord::getDeleted, 0);
        return warningRecordMapper.selectCount(w) > 0;
    }

    private void createRecord(WarningRule rule, Object plan, Map<String, Object> vars) {
        String rendered = expressionEngine.renderWithValues(rule.getConditionExpr(), vars);
        String reason = String.format("规则「%s」触发%s。\n判断条件：%s\n代入实际数据：%s",
                rule.getName(), levelLabel(rule.getWarningLevel()),
                rule.getConditionExpr(), rendered);

        WarningRecord record = new WarningRecord();
        record.setRuleId(rule.getId());
        record.setObjectType(rule.getObjectType());
        record.setPlanId(toLong(attributeRegistry.getFieldValue(plan, "id")));
        record.setPlanName(attributeRegistry.getPlanName(rule.getObjectType(), plan));
        record.setWarningLevel(rule.getWarningLevel());
        record.setReason(reason);
        record.setConditionExpr(rule.getConditionExpr());
        record.setStatus("PENDING");
        record.setTriggeredTime(LocalDateTime.now());
        warningRecordService.save(record);
        log.info("生成预警: ruleId={}, objectType={}, planId={}, level={}",
                rule.getId(), rule.getObjectType(), record.getPlanId(), rule.getWarningLevel());
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
