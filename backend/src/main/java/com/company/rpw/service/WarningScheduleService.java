package com.company.rpw.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 预警调度服务：按规则定义定时生成预警。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarningScheduleService {

    private final WarningRuleService warningRuleService;

    /**
     * 每日凌晨 1 点执行全部启用规则的预警检查。
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void dailyWarningCheck() {
        log.info("开始执行每日预警检查（凌晨1点）...");
        try {
        int count = warningRuleService.checkAll(null, null);
        log.info("每日预警检查完成，共产生 {} 条预警", count);
        } catch (Exception e) {
            log.error("每日预警检查失败: {}", e.getMessage(), e);
        }
    }
}
