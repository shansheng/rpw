package com.company.rpw.scheduler;

import com.company.rpw.entity.ResourcePlanLabor;
import com.company.rpw.entity.WarningRecord;
import com.company.rpw.service.ResourcePlanLaborService;
import com.company.rpw.service.WarningRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 劳动力计划预警定时任务
 */
@Component
@Slf4j
public class LaborWarningScheduler {

    @Autowired
    private ResourcePlanLaborService laborService;

    @Autowired
    private WarningRecordService warningRecordService;

    /**
     * 每天凌晨1点扫描劳动力计划预警
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void scanLaborPlanWarnings() {
        log.info("开始扫描劳动力计划预警...");

        try {
            // 1. 查询所有 status IN ('SUBMITTED','IN_PROGRESS') 的劳动力计划
            List<ResourcePlanLabor> laborPlans = laborService.listByParams(
                    null, null, null, null, null)
                    .stream()
                    .filter(p -> "SUBMITTED".equals(p.getStatus()) || "IN_PROGRESS".equals(p.getStatus()))
                    .toList();

            LocalDate today = LocalDate.now();
            int warningCount = 0;

            // 2. 对每条计划进行预警检查
            for (ResourcePlanLabor plan : laborPlans) {
                boolean needWarning = false;
                String warningMessage = "";

                // a. 如果 planStartDate < today 且 actualStartDate == null → 生成红色预警
                if (plan.getPlanStartDate() != null && plan.getPlanStartDate().isBefore(today)
                        && plan.getActualStartDate() == null) {
                    needWarning = true;
                    warningMessage = "计划开始日期(" + plan.getPlanStartDate() + ")已过，但未登记实际开始日期";
                }

                // b. 如果 actualStartDate != null 且 actualStartDate > planStartDate → 生成红色预警
                if (plan.getActualStartDate() != null && plan.getPlanStartDate() != null
                        && plan.getActualStartDate().isAfter(plan.getPlanStartDate())) {
                    needWarning = true;
                    warningMessage = "实际开始日期(" + plan.getActualStartDate() + ")晚于计划开始日期(" + plan.getPlanStartDate() + ")";
                }

                // 3. 写入 warning_record 表
                if (needWarning) {
                    // 检查是否已存在未处理的相同预警
                    boolean exists = warningRecordService.lambdaQuery()
                            .eq(WarningRecord::getResourceType, "LABOR")
                            .eq(WarningRecord::getResourceId, plan.getId())
                            .eq(WarningRecord::getStatus, "ACTIVE")
                            .eq(WarningRecord::getDeleted, 0)
                            .exists();

                    if (!exists) {
                        WarningRecord warningRecord = new WarningRecord();
                        warningRecord.setResourceType("LABOR");
                        warningRecord.setResourceId(plan.getId());
                        warningRecord.setWarningLevel("URGENT");
                        warningRecord.setWarningMessage(warningMessage);
                        warningRecord.setStatus("ACTIVE");
                        warningRecord.setDeleted(0);

                        warningRecordService.save(warningRecord);
                        warningCount++;
                        
                        log.warn("发现劳动力计划预警: planId={}, message={}", plan.getId(), warningMessage);
                    }
                }
            }

            log.info("劳动力计划预警扫描完成，共生成{}条预警记录", warningCount);

        } catch (Exception e) {
            log.error("扫描劳动力计划预警失败", e);
        }
    }
}
