package com.company.rpw.controller;

import com.company.rpw.common.R;
import com.company.rpw.entity.ResourcePlanLabor;
import com.company.rpw.entity.WarningRecord;
import com.company.rpw.service.LaborPlanChangeService;
import com.company.rpw.service.ResourcePlanLaborService;
import com.company.rpw.service.WarningRecordService;
import com.company.rpw.service.WarningRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 定时任务手动触发调试 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/scheduler")
@RequiredArgsConstructor
public class SchedulerDebugController {

    private final WarningRuleService warningRuleService;
    private final ResourcePlanLaborService laborService;
    private final WarningRecordService warningRecordService;

    /**
     * 手动触发通用预警规则检查
     * POST /api/v1/scheduler/trigger-rule-check
     * 返回详细的处理过程日志
     */
    @PostMapping("/trigger-rule-check")
    public R<Map<String, Object>> triggerRuleCheck(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long ruleId) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, String>> logs = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        logs.add(Map.of("time", now(), "level", "INFO", "message", "开始手动触发通用预警规则检查"));

        try {
            // 检查启用的规则
            var rules = warningRuleService.lambdaQuery()
                    .eq(ruleId != null, com.company.rpw.entity.WarningRule::getId, ruleId)
                    .eq(com.company.rpw.entity.WarningRule::getEnabled, 1)
                    .list();

            logs.add(Map.of("time", now(), "level", "INFO",
                    "message", String.format("查询到 %d 条启用的预警规则", rules.size())));

            for (var rule : rules) {
                logs.add(Map.of("time", now(), "level", "INFO",
                        "message", String.format("检查规则: id=%d, name=%s, type=%s, thresholdType=%s, threshold=%.2f",
                                rule.getId(), rule.getRuleName(), rule.getResourceType(),
                                rule.getThresholdType(), rule.getWarningThreshold())));

                String checkResult = warningRuleService.checkWarning(projectId, rule.getId());
                logs.add(Map.of("time", now(), "level", "INFO",
                        "message", "规则检查结果: " + checkResult));
            }

            // 查询本次新增的预警记录（最近30秒内）
            var recentRecords = warningRecordService.lambdaQuery()
                    .ge(WarningRecord::getTriggeredTime,
                            java.time.LocalDateTime.now().minusSeconds(30))
                    .orderByDesc(WarningRecord::getTriggeredTime)
                    .list();

            logs.add(Map.of("time", now(), "level", "INFO",
                    "message", String.format("本次检查共产生 %d 条新预警记录", recentRecords.size())));

            for (var record : recentRecords) {
                logs.add(Map.of("time", now(), "level", "WARN",
                        "message", String.format("  [预警] %s | %s | 级别=%s | %s",
                                record.getResourceType(),
                                record.getResourceId() != null ? "resourceId=" + record.getResourceId() : "聚合预警",
                                record.getWarningLevel(),
                                record.getWarningMessage())));
            }

            long elapsed = System.currentTimeMillis() - startTime;
            logs.add(Map.of("time", now(), "level", "INFO",
                    "message", String.format("通用预警规则检查完成，耗时 %d ms", elapsed)));

            result.put("success", true);
        } catch (Exception e) {
            logs.add(Map.of("time", now(), "level", "ERROR",
                    "message", "检查失败: " + e.getMessage()));
            result.put("success", false);
        }

        result.put("scheduler", "通用预警规则检查 (WarningRuleService.checkWarning)");
        result.put("logs", logs);
        return R.ok(result);
    }

    /**
     * 手动触发劳动力计划专项预警扫描
     * POST /api/v1/scheduler/trigger-labor-scan
     * 返回详细的处理过程日志
     */
    @PostMapping("/trigger-labor-scan")
    public R<Map<String, Object>> triggerLaborScan() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, String>> logs = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        logs.add(Map.of("time", now(), "level", "INFO", "message", "开始手动触发劳动力计划专项预警扫描"));

        try {
            // 1. 查询所有 SUBMITTED/IN_PROGRESS 的劳动力计划
            List<ResourcePlanLabor> allPlans = laborService.listByParams(null, null, null, null, null);
            List<ResourcePlanLabor> activePlans = allPlans.stream()
                    .filter(p -> "SUBMITTED".equals(p.getStatus()) || "IN_PROGRESS".equals(p.getStatus()))
                    .toList();

            logs.add(Map.of("time", now(), "level", "INFO",
                    "message", String.format("总劳动力计划 %d 条，其中活跃状态(SUBMITTED/IN_PROGRESS) %d 条",
                            allPlans.size(), activePlans.size())));

            LocalDate today = LocalDate.now();
            logs.add(Map.of("time", now(), "level", "INFO",
                    "message", "当前日期: " + today));

            int warningCount = 0;
            int skipCount = 0;

            // 2. 逐条检查
            for (ResourcePlanLabor plan : activePlans) {
                logs.add(Map.of("time", now(), "level", "INFO",
                        "message", String.format("检查: id=%d, 工种=%s, 计划开始=%s, 实际开始=%s",
                                plan.getId(),
                                plan.getWorkTypeName() != null ? plan.getWorkTypeName() : plan.getWorkTypeCode(),
                                plan.getPlanStartDate(),
                                plan.getActualStartDate())));

                boolean needWarning = false;
                String warningMessage = "";

                // 规则a: 计划开始日已过，未登记实际开始
                if (plan.getPlanStartDate() != null && plan.getPlanStartDate().isBefore(today)
                        && plan.getActualStartDate() == null) {
                    needWarning = true;
                    warningMessage = "计划开始日期(" + plan.getPlanStartDate() + ")已过，但未登记实际开始日期";
                    logs.add(Map.of("time", now(), "level", "WARN",
                            "message", "  触发规则a: " + warningMessage));
                }

                // 规则b: 实际开始晚于计划开始
                if (plan.getActualStartDate() != null && plan.getPlanStartDate() != null
                        && plan.getActualStartDate().isAfter(plan.getPlanStartDate())) {
                    needWarning = true;
                    long delayDays = java.time.temporal.ChronoUnit.DAYS.between(plan.getPlanStartDate(), plan.getActualStartDate());
                    warningMessage = "实际开始日期(" + plan.getActualStartDate() + ")晚于计划开始日期(" + plan.getPlanStartDate() + ")，延误" + delayDays + "天";
                    logs.add(Map.of("time", now(), "level", "WARN",
                            "message", "  触发规则b: " + warningMessage));
                }

                if (!needWarning) {
                    logs.add(Map.of("time", now(), "level", "INFO", "message", "  未触发预警"));
                }

                // 3. 写入预警记录（去重检查）
                if (needWarning) {
                    boolean exists = warningRecordService.lambdaQuery()
                            .eq(WarningRecord::getResourceType, "LABOR")
                            .eq(WarningRecord::getResourceId, plan.getId())
                            .eq(WarningRecord::getStatus, "ACTIVE")
                            .eq(WarningRecord::getDeleted, 0)
                            .exists();

                    if (exists) {
                        logs.add(Map.of("time", now(), "level", "INFO",
                                "message", "  跳过: 已存在未处理的相同预警"));
                        skipCount++;
                    } else {
                        WarningRecord record = new WarningRecord();
                        record.setResourceType("LABOR");
                        record.setResourceId(plan.getId());
                        record.setWarningLevel("URGENT");
                        record.setWarningMessage(warningMessage);
                        record.setStatus("ACTIVE");
                        record.setDeleted(0);
                        warningRecordService.save(record);
                        warningCount++;
                        logs.add(Map.of("time", now(), "level", "WARN",
                                "message", "  已生成预警记录 id=" + record.getId()));
                    }
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            logs.add(Map.of("time", now(), "level", "INFO",
                    "message", String.format("劳动力计划预警扫描完成，新增 %d 条预警，跳过 %d 条重复，耗时 %d ms",
                            warningCount, skipCount, elapsed)));

            result.put("success", true);
        } catch (Exception e) {
            logs.add(Map.of("time", now(), "level", "ERROR",
                    "message", "扫描失败: " + e.getMessage()));
            log.error("手动触发劳动力预警扫描失败", e);
            result.put("success", false);
        }

        result.put("scheduler", "劳动力计划专项预警扫描 (LaborWarningScheduler.scanLaborPlanWarnings)");
        result.put("logs", logs);
        return R.ok(result);
    }

    private String now() {
        return java.time.LocalDateTime.now().toString();
    }
}
