package com.company.rpw.controller;

import com.company.rpw.common.R;
import com.company.rpw.entity.WarningRecord;
import com.company.rpw.entity.WarningRule;
import com.company.rpw.service.WarningRecordService;
import com.company.rpw.service.WarningRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
    private final WarningRecordService warningRecordService;

    /**
     * 手动触发通用预警规则检查（按规则表达式生成预警）
     * POST /api/v1/scheduler/trigger-rule-check
     */
    @PostMapping("/trigger-rule-check")
    public R<Map<String, Object>> triggerRuleCheck(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long ruleId) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, String>> logs = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        logs.add(Map.of("time", now(), "level", "INFO", "message", "开始手动触发预警规则检查"));

        try {
            List<WarningRule> rules = warningRuleService.lambdaQuery()
                    .eq(ruleId != null, WarningRule::getId, ruleId)
                    .eq(WarningRule::getEnabled, 1)
                    .list();
            logs.add(Map.of("time", now(), "level", "INFO",
                    "message", String.format("查询到 %d 条启用的预警规则", rules.size())));

            int newCount = warningRuleService.checkAll(projectId, ruleId);
            logs.add(Map.of("time", now(), "level", "INFO",
                    "message", String.format("检查结果: 预警检查完成，共产生 %d 条预警", newCount)));

            if (newCount > 0) {
                List<WarningRecord> recent = warningRecordService.lambdaQuery()
                        .orderByDesc(WarningRecord::getTriggeredTime)
                        .last("LIMIT " + newCount)
                        .list();
                for (WarningRecord record : recent) {
                    logs.add(Map.of("time", now(), "level", "WARN",
                            "message", String.format("  [预警] 规则=%s | 对象=%s | 计划=%s | 级别=%s",
                                    record.getRuleId(), record.getObjectType(),
                                    record.getPlanName(), record.getWarningLevel())));
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            logs.add(Map.of("time", now(), "level", "INFO",
                    "message", String.format("预警规则检查完成，耗时 %d ms", elapsed)));
            result.put("success", true);
        } catch (Exception e) {
            logs.add(Map.of("time", now(), "level", "ERROR", "message", "检查失败: " + e.getMessage()));
            result.put("success", false);
        }

        result.put("scheduler", "预警规则检查 (WarningRuleService.checkAll)");
        result.put("logs", logs);
        return R.ok(result);
    }

    /**
     * 手动触发劳动力计划预警扫描（等同于全量规则检查，覆盖 LABOR 对象）
     * POST /api/v1/scheduler/trigger-labor-scan
     */
    @PostMapping("/trigger-labor-scan")
    public R<Map<String, Object>> triggerLaborScan() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, String>> logs = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        logs.add(Map.of("time", now(), "level", "INFO", "message", "开始劳动力计划预警扫描（规则引擎）"));

        try {
            int newCount = warningRuleService.checkAll(null, null);
            logs.add(Map.of("time", now(), "level", "INFO",
                    "message", String.format("检查结果: 预警检查完成，共产生 %d 条预警", newCount)));

            long elapsed = System.currentTimeMillis() - startTime;
            logs.add(Map.of("time", now(), "level", "INFO",
                    "message", String.format("劳动力计划预警扫描完成，耗时 %d ms", elapsed)));
            result.put("success", true);
        } catch (Exception e) {
            logs.add(Map.of("time", now(), "level", "ERROR", "message", "扫描失败: " + e.getMessage()));
            result.put("success", false);
        }

        result.put("scheduler", "劳动力计划预警扫描 (WarningRuleService.checkAll)");
        result.put("logs", logs);
        return R.ok(result);
    }

    private String now() {
        return LocalDateTime.now().toString();
    }
}
