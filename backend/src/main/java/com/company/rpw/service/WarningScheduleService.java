package com.company.rpw.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 预警调度服务
 * 用于定时执行预警检查
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarningScheduleService {

    private final WarningRuleService warningRuleService;

    /**
     * 定时执行预警检查
     * 每天凌晨1点执行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void scheduledWarningCheck() {
        log.info("开始执行定时预警检查...");
        try {
            String result = warningRuleService.checkWarning(null, null);
            log.info("定时预警检查完成: {}", result);
        } catch (Exception e) {
            log.error("定时预警检查失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 定时执行预警检查（每小时执行一次）
     * 用于频繁发现新的预警，补充每日检查的实时性
     * 注意：凌晨1点与 daily check 重叠，此处跳过避免重复执行
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void hourlyWarningCheck() {
        // 跳过早上8点（由 daily check 负责）
        java.time.LocalTime now = java.time.LocalTime.now();
        if (now.getHour() == 1) {
            return;
        }
        log.info("开始执行每小时预警检查...");
        try {
            String result = warningRuleService.checkWarning(null, null);
            log.info("每小时预警检查完成: {}", result);
        } catch (Exception e) {
            log.error("每小时预警检查失败: {}", e.getMessage(), e);
        }
    }
}
