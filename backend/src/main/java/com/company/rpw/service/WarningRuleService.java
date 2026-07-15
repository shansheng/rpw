package com.company.rpw.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.rpw.entity.*;
import com.company.rpw.mapper.WarningRuleMapper;
import com.company.rpw.mapper.WarningRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 预警规则服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarningRuleService extends ServiceImpl<WarningRuleMapper, WarningRule> {

    private final WarningRecordMapper warningRecordMapper;
    private final ResourcePlanMaterialService materialService;
    private final ResourcePlanEquipmentService equipmentService;
    private final ResourcePlanHardwareService hardwareService;
    private final ResourcePlanCirculationService circulationService;
    private final ResourcePlanOfficeService officeService;
    private final ResourcePlanSafetyService safetyService;
    private final ResourcePlanSubcontractService subcontractService;
    private final ResourcePlanLaborService laborService;
    private final WeComMessageService weComMessageService;

    // ==================== 入口方法 ====================

    /**
     * 执行预警检查
     * @param projectId 项目ID（可选，为空则检查所有项目）
     * @param ruleId 规则ID（可选，为空则检查所有启用规则）
     * @return 检查结果信息
     */
    public String checkWarning(Long projectId, Long ruleId) {
        List<WarningRule> rules = getEnabledRules(ruleId);
        int totalTriggered = 0;

        for (WarningRule rule : rules) {
            try {
                int triggered = checkByRule(rule, projectId);
                totalTriggered += triggered;
                log.info("规则检查完成: ruleId={}, type={}, triggered={}", rule.getId(), rule.getResourceType(), triggered);
            } catch (Exception e) {
                log.error("检查规则失败: ruleId={}, error={}", rule.getId(), e.getMessage(), e);
            }
        }

        return String.format("预警检查完成，共触发 %d 条预警", totalTriggered);
    }

    /**
     * 获取启用的规则列表
     */
    private List<WarningRule> getEnabledRules(Long ruleId) {
        LambdaQueryWrapper<WarningRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WarningRule::getEnabled, 1);
        if (ruleId != null) {
            wrapper.eq(WarningRule::getId, ruleId);
        }
        return list(wrapper);
    }

    /**
     * 根据单个规则执行检查
     */
    private int checkByRule(WarningRule rule, Long projectId) {
        String resourceType = rule.getResourceType();
        int triggeredCount = 0;

        switch (resourceType) {
            case "MATERIAL":
                triggeredCount = checkArrivalType(rule, projectId, "MATERIAL");
                break;
            case "EQUIPMENT":
                triggeredCount = checkArrivalType(rule, projectId, "EQUIPMENT");
                break;
            case "HARDWARE":
                triggeredCount = checkArrivalType(rule, projectId, "HARDWARE");
                break;
            case "CIRCULATION":
                triggeredCount = checkArrivalType(rule, projectId, "CIRCULATION");
                break;
            case "OFFICE":
                triggeredCount = checkArrivalType(rule, projectId, "OFFICE");
                break;
            case "SAFETY":
                triggeredCount = checkArrivalType(rule, projectId, "SAFETY");
                break;
            case "SUBCONTRACT":
                triggeredCount = checkSubcontract(rule, projectId);
                break;
            case "LABOR":
                triggeredCount = checkLabor(rule, projectId);
                break;
            default:
                log.warn("未知的资源类型: {}", resourceType);
        }

        return triggeredCount;
    }

    // ==================== 到场类资源通用检查（MATERIAL/EQUIPMENT/HARDWARE/CIRCULATION/OFFICE/SAFETY） ====================

    /**
     * 到场类资源的通用检查逻辑
     * thresholdType=DATE: 检查每个计划的到场延误天数是否超过阈值
     * thresholdType=RATE: 计算整体到场率，低于阈值则触发一条预警
     */
    private int checkArrivalType(WarningRule rule, Long projectId, String resourceType) {
        List<? extends BaseArrivalPlan> plans = getArrivalPlans(resourceType, projectId);
        if (plans.isEmpty()) {
            return 0;
        }

        String thresholdType = rule.getThresholdType();
        BigDecimal threshold = rule.getWarningThreshold();
        int triggered = 0;

        switch (thresholdType) {
            case "DATE":
                // 逐个检查：到场延误天数 > 阈值 则触发
                for (BaseArrivalPlan plan : plans) {
                    Long delayDays = calculateDelayDays(plan.getPlanArrivalDate(), plan.getActualArrivalDate());
                    if (delayDays != null && delayDays > threshold.longValue()) {
                        BigDecimal actualValue = new BigDecimal(delayDays);
                        createWarningRecord(rule, plan.getProjectId(), plan.getId(),
                                plan.getResourceName(), actualValue, threshold,
                                buildArrivalDelayMessage(rule, plan, delayDays));
                        triggered++;
                    }
                }
                break;

            case "RATE":
                // 聚合检查：计算整体到场率
                long total = plans.size();
                long arrived = plans.stream()
                        .filter(p -> p.getActualArrivalDate() != null)
                        .count();
                if (total > 0) {
                    BigDecimal arrivalRate = new BigDecimal(arrived * 100)
                            .divide(new BigDecimal(total), 2, RoundingMode.HALF_UP);
                    if (arrivalValueCompare(arrivalRate, threshold) < 0) {
                        // 到场率低于阈值，创建一条聚合预警
                        String resourceNames = String.join(", ",
                                plans.stream().limit(5).map(BaseArrivalPlan::getResourceName).toList());
                        createWarningRecord(rule, projectId, null,
                                resourceNames + (total > 5 ? " 等" + total + "项" : ""),
                                arrivalRate, threshold,
                                buildArrivalRateMessage(rule, arrivalRate, total, arrived));
                        triggered = 1;
                    }
                }
                break;

            case "QUANTITY":
                log.warn("到场类资源不支持 QUANTITY 阈值类型: resourceType={}", resourceType);
                break;

            default:
                log.warn("未知的阈值类型: {}", thresholdType);
        }

        return triggered;
    }

    /**
     * 根据资源类型获取到场类计划列表
     */
    private List<? extends BaseArrivalPlan> getArrivalPlans(String resourceType, Long projectId) {
        LambdaQueryWrapper<ResourcePlanMaterial> mWrapper = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<ResourcePlanEquipment> eWrapper = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<ResourcePlanHardware> hWrapper = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<ResourcePlanCirculation> cWrapper = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<ResourcePlanOffice> oWrapper = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<ResourcePlanSafety> sWrapper = new LambdaQueryWrapper<>();

        if (projectId != null) {
            mWrapper.eq(ResourcePlanMaterial::getProjectId, projectId);
            eWrapper.eq(ResourcePlanEquipment::getProjectId, projectId);
            hWrapper.eq(ResourcePlanHardware::getProjectId, projectId);
            cWrapper.eq(ResourcePlanCirculation::getProjectId, projectId);
            oWrapper.eq(ResourcePlanOffice::getProjectId, projectId);
            sWrapper.eq(ResourcePlanSafety::getProjectId, projectId);
        }

        return switch (resourceType) {
            case "MATERIAL" -> materialService.list(mWrapper).stream()
                    .map(p -> (BaseArrivalPlan) new ArrivalPlanAdapter.MaterialAdapter(p)).toList();
            case "EQUIPMENT" -> equipmentService.list(eWrapper).stream()
                    .map(p -> (BaseArrivalPlan) new ArrivalPlanAdapter.EquipmentAdapter(p)).toList();
            case "HARDWARE" -> hardwareService.list(hWrapper).stream()
                    .map(p -> (BaseArrivalPlan) new ArrivalPlanAdapter.HardwareAdapter(p)).toList();
            case "CIRCULATION" -> circulationService.list(cWrapper).stream()
                    .map(p -> (BaseArrivalPlan) new ArrivalPlanAdapter.CirculationAdapter(p)).toList();
            case "OFFICE" -> officeService.list(oWrapper).stream()
                    .map(p -> (BaseArrivalPlan) new ArrivalPlanAdapter.OfficeAdapter(p)).toList();
            case "SAFETY" -> safetyService.list(sWrapper).stream()
                    .map(p -> (BaseArrivalPlan) new ArrivalPlanAdapter.SafetyAdapter(p)).toList();
            default -> List.of();
        };
    }

    // ==================== 劳动力检查 ====================

    /**
     * 劳动力资源检查逻辑
     * thresholdType=QUANTITY: 实际人数与计划人数的短缺 > 阈值 则触发
     * thresholdType=RATE: 实际人数/计划人数 < 阈值% 则触发
     * thresholdType=DATE: 开工延误天数 > 阈值 则触发
     */
    private int checkLabor(WarningRule rule, Long projectId) {
        LambdaQueryWrapper<ResourcePlanLabor> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(ResourcePlanLabor::getProjectId, projectId);
        }
        List<ResourcePlanLabor> plans = laborService.list(wrapper);
        if (plans.isEmpty()) {
            return 0;
        }

        String thresholdType = rule.getThresholdType();
        BigDecimal threshold = rule.getWarningThreshold();
        int triggered = 0;

        switch (thresholdType) {
            case "QUANTITY":
                // 逐个检查：计划人数 - 实际人数 > 阈值 则触发
                for (ResourcePlanLabor plan : plans) {
                    if (plan.getActualQuantity() == null || plan.getPlanQuantity() == null) {
                        continue;
                    }
                    int shortfall = plan.getPlanQuantity() - plan.getActualQuantity();
                    if (shortfall > threshold.intValue()) {
                        BigDecimal actualValue = new BigDecimal(plan.getActualQuantity());
                        BigDecimal planValue = new BigDecimal(plan.getPlanQuantity());
                        createWarningRecord(rule, plan.getProjectId(), plan.getId(),
                                plan.getWorkTypeCode(), actualValue, planValue,
                                buildLaborQuantityMessage(rule, plan, shortfall));
                        triggered++;
                    }
                }
                break;

            case "RATE":
                // 逐个检查：实际人数/计划人数 < 阈值% 则触发
                for (ResourcePlanLabor plan : plans) {
                    if (plan.getActualQuantity() == null || plan.getPlanQuantity() == null
                            || plan.getPlanQuantity() == 0) {
                        continue;
                    }
                    BigDecimal rate = new BigDecimal(plan.getActualQuantity() * 100)
                            .divide(new BigDecimal(plan.getPlanQuantity()), 2, RoundingMode.HALF_UP);
                    if (arrivalValueCompare(rate, threshold) < 0) {
                        createWarningRecord(rule, plan.getProjectId(), plan.getId(),
                                plan.getWorkTypeCode(), rate, threshold,
                                buildLaborRateMessage(rule, plan, rate));
                        triggered++;
                    }
                }
                break;

            case "DATE":
                // 逐个检查：开工延误天数 > 阈值 则触发
                for (ResourcePlanLabor plan : plans) {
                    Long delayDays = calculateDelayDays(plan.getPlanStartDate(), plan.getActualStartDate());
                    if (delayDays != null && delayDays > threshold.longValue()) {
                        BigDecimal actualValue = new BigDecimal(delayDays);
                        createWarningRecord(rule, plan.getProjectId(), plan.getId(),
                                plan.getWorkTypeCode(), actualValue, threshold,
                                buildLaborDateMessage(rule, plan, delayDays));
                        triggered++;
                    }
                }
                break;

            default:
                log.warn("未知的阈值类型: {}", thresholdType);
        }

        return triggered;
    }

    // ==================== 分包检查 ====================

    /**
     * 分包资源检查逻辑
     * thresholdType=DATE: 开工延误天数 > 阈值 则触发
     */
    private int checkSubcontract(WarningRule rule, Long projectId) {
        LambdaQueryWrapper<ResourcePlanSubcontract> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(ResourcePlanSubcontract::getProjectId, projectId);
        }
        List<ResourcePlanSubcontract> plans = subcontractService.list(wrapper);
        if (plans.isEmpty()) {
            return 0;
        }

        String thresholdType = rule.getThresholdType();
        BigDecimal threshold = rule.getWarningThreshold();
        int triggered = 0;

        if ("DATE".equals(thresholdType)) {
            for (ResourcePlanSubcontract plan : plans) {
                Long delayDays = calculateDelayDays(plan.getPlanStartDate(), plan.getActualStartDate());
                if (delayDays != null && delayDays > threshold.longValue()) {
                    BigDecimal actualValue = new BigDecimal(delayDays);
                    createWarningRecord(rule, plan.getProjectId(), plan.getId(),
                            plan.getSubcontractName(), actualValue, threshold,
                            buildSubcontractDateMessage(rule, plan, delayDays));
                    triggered++;
                }
            }
        } else {
            log.warn("分包资源仅支持 DATE 阈值类型: thresholdType={}", thresholdType);
        }

        return triggered;
    }

    // ==================== 通用工具方法 ====================

    /**
     * 计算延误天数
     * @return 实际日期 - 计划日期（正数=延误，负数=提前，null=未填写实际日期）
     */
    private Long calculateDelayDays(LocalDate planDate, LocalDate actualDate) {
        if (planDate == null || actualDate == null) {
            return null;
        }
        return ChronoUnit.DAYS.between(planDate, actualDate);
    }

    /**
     * 比较实际率值与阈值（用于RATE类型）
     * @return 负数表示actual < threshold
     */
    private int arrivalValueCompare(BigDecimal actual, BigDecimal threshold) {
        return actual.compareTo(threshold);
    }

    /**
     * 通用创建预警记录
     * @param rule 预警规则
     * @param projectId 项目ID
     * @param resourceId 资源ID（聚合预警时为null）
     * @param resourceName 资源名称（用于消息展示）
     * @param actualValue 实际值
     * @param planValue 计划值/阈值
     * @param warningMessage 预警消息
     */
    private void createWarningRecord(WarningRule rule, Long projectId, Long resourceId,
                                     String resourceName, BigDecimal actualValue, BigDecimal planValue,
                                     String warningMessage) {
        // 防止重复预警：同一规则+同一资源+同一状态未处理时不再重复插入
        LambdaQueryWrapper<WarningRecord> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(WarningRecord::getRuleId, rule.getId())
                .eq(WarningRecord::getResourceType, rule.getResourceType());
        if (resourceId != null) {
            existWrapper.eq(WarningRecord::getResourceId, resourceId);
        }
        existWrapper.in(WarningRecord::getStatus, "PENDING", "PROCESSING");
        if (warningRecordMapper.selectCount(existWrapper) > 0) {
            log.debug("预警已存在，跳过: ruleId={}, resourceId={}", rule.getId(), resourceId);
            return;
        }

        WarningRecord record = new WarningRecord();
        record.setRuleId(rule.getId());
        record.setProjectId(projectId);
        record.setResourceType(rule.getResourceType());
        record.setResourceId(resourceId);
        record.setWarningLevel(rule.getWarningLevel());
        record.setPlanValue(planValue);
        record.setActualValue(actualValue);

        // 计算偏差率（%）
        if (planValue != null && planValue.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal deviation = planValue.subtract(actualValue)
                    .divide(planValue.abs(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            record.setDeviationRate(deviation);
        }

        record.setWarningMessage(warningMessage);
        record.setStatus("PENDING");
        record.setNotifyStatus("NOT_SENT");
        record.setTriggeredTime(LocalDateTime.now());

        warningRecordMapper.insert(record);

        // 发送企业微信通知
        if (rule.getNotifyWecom() == 1) {
            sendWeComNotification(rule, record);
        }
    }

    /**
     * 发送企业微信通知
     */
    private void sendWeComNotification(WarningRule rule, WarningRecord record) {
        try {
            String content = String.format(
                    "【资源预警通知】\n规则：%s\n资源类型：%s\n实际值：%.2f\n阈值/计划值：%.2f\n预警级别：%s\n消息：%s",
                    rule.getRuleName(),
                    rule.getResourceType(),
                    record.getActualValue(),
                    record.getPlanValue(),
                    record.getWarningLevel(),
                    record.getWarningMessage()
            );

            if (rule.getNotifyUsers() != null && !rule.getNotifyUsers().isEmpty()) {
                String[] userIds = rule.getNotifyUsers().split(",");
                for (String userId : userIds) {
                    weComMessageService.sendTextMessage(userId.trim(), content);
                }
            }

            record.setNotifyStatus("SENT");
            warningRecordMapper.updateById(record);
        } catch (Exception e) {
            log.error("发送企业微信通知失败: {}", e.getMessage(), e);
            record.setNotifyStatus("FAILED");
            warningRecordMapper.updateById(record);
        }
    }

    // ==================== 消息构建方法 ====================

    private String buildArrivalDelayMessage(WarningRule rule, BaseArrivalPlan plan, Long delayDays) {
        return String.format("规则【%s】触发预警：%s「%s」到场延误 %d 天（计划：%s，实际：%s）",
                rule.getRuleName(), getResourceTypeLabel(rule.getResourceType()), plan.getResourceName(),
                delayDays, plan.getPlanArrivalDate(), plan.getActualArrivalDate());
    }

    private String buildArrivalRateMessage(WarningRule rule, BigDecimal rate, long total, long arrived) {
        return String.format("规则【%s】触发预警：%s整体到场率 %.2f%%，低于阈值 %.2f%%（共 %d 项，已到场 %d 项）",
                rule.getRuleName(), getResourceTypeLabel(rule.getResourceType()),
                rate, rule.getWarningThreshold(), total, arrived);
    }

    private String buildLaborQuantityMessage(WarningRule rule, ResourcePlanLabor plan, int shortfall) {
        return String.format("规则【%s】触发预警：工种「%s」人员短缺 %d 人（计划：%d 人，实际：%d 人）",
                rule.getRuleName(), plan.getWorkTypeCode(), shortfall,
                plan.getPlanQuantity(), plan.getActualQuantity());
    }

    private String buildLaborRateMessage(WarningRule rule, ResourcePlanLabor plan, BigDecimal rate) {
        return String.format("规则【%s】触发预警：工种「%s」到位率 %.2f%%，低于阈值 %.2f%%（计划：%d 人，实际：%d 人）",
                rule.getRuleName(), plan.getWorkTypeCode(), rate, rule.getWarningThreshold(),
                plan.getPlanQuantity(), plan.getActualQuantity());
    }

    private String buildLaborDateMessage(WarningRule rule, ResourcePlanLabor plan, Long delayDays) {
        return String.format("规则【%s】触发预警：工种「%s」开工延误 %d 天（计划：%s，实际：%s）",
                rule.getRuleName(), plan.getWorkTypeCode(), delayDays,
                plan.getPlanStartDate(), plan.getActualStartDate());
    }

    private String buildSubcontractDateMessage(WarningRule rule, ResourcePlanSubcontract plan, Long delayDays) {
        return String.format("规则【%s】触发预警：分包「%s」开工延误 %d 天（计划：%s，实际：%s）",
                rule.getRuleName(), plan.getSubcontractName(), delayDays,
                plan.getPlanStartDate(), plan.getActualStartDate());
    }

    private String getResourceTypeLabel(String resourceType) {
        return switch (resourceType) {
            case "MATERIAL" -> "材料";
            case "EQUIPMENT" -> "设备";
            case "HARDWARE" -> "五金";
            case "CIRCULATION" -> "周转材";
            case "OFFICE" -> "办公用品";
            case "SAFETY" -> "安全物资";
            case "LABOR" -> "劳动力";
            case "SUBCONTRACT" -> "分包";
            default -> resourceType;
        };
    }

    // ==================== 到场类资源适配器 ====================
    // 使用适配器模式统一到场类资源的字段访问

    private interface BaseArrivalPlan {
        Long getProjectId();
        Long getId();
        String getResourceName();
        LocalDate getPlanArrivalDate();
        LocalDate getActualArrivalDate();
    }

    private static class ArrivalPlanAdapter {
        record MaterialAdapter(ResourcePlanMaterial p) implements BaseArrivalPlan {
            public Long getProjectId() { return p.getProjectId(); }
            public Long getId() { return p.getId(); }
            public String getResourceName() { return p.getResourceName(); }
            public LocalDate getPlanArrivalDate() { return p.getPlanArrivalDate(); }
            public LocalDate getActualArrivalDate() { return p.getActualArrivalDate(); }
        }
        record EquipmentAdapter(ResourcePlanEquipment p) implements BaseArrivalPlan {
            public Long getProjectId() { return p.getProjectId(); }
            public Long getId() { return p.getId(); }
            public String getResourceName() { return p.getEquipmentName(); }
            public LocalDate getPlanArrivalDate() { return p.getPlanArrivalDate(); }
            public LocalDate getActualArrivalDate() { return p.getActualArrivalDate(); }
        }
        record HardwareAdapter(ResourcePlanHardware p) implements BaseArrivalPlan {
            public Long getProjectId() { return p.getProjectId(); }
            public Long getId() { return p.getId(); }
            public String getResourceName() { return p.getHardwareName(); }
            public LocalDate getPlanArrivalDate() { return p.getPlanArrivalDate(); }
            public LocalDate getActualArrivalDate() { return p.getActualArrivalDate(); }
        }
        record CirculationAdapter(ResourcePlanCirculation p) implements BaseArrivalPlan {
            public Long getProjectId() { return p.getProjectId(); }
            public Long getId() { return p.getId(); }
            public String getResourceName() { return p.getCirculationName(); }
            public LocalDate getPlanArrivalDate() { return p.getPlanArrivalDate(); }
            public LocalDate getActualArrivalDate() { return p.getActualArrivalDate(); }
        }
        record OfficeAdapter(ResourcePlanOffice p) implements BaseArrivalPlan {
            public Long getProjectId() { return p.getProjectId(); }
            public Long getId() { return p.getId(); }
            public String getResourceName() { return p.getOfficeName(); }
            public LocalDate getPlanArrivalDate() { return p.getPlanArrivalDate(); }
            public LocalDate getActualArrivalDate() { return p.getActualArrivalDate(); }
        }
        record SafetyAdapter(ResourcePlanSafety p) implements BaseArrivalPlan {
            public Long getProjectId() { return p.getProjectId(); }
            public Long getId() { return p.getId(); }
            public String getResourceName() { return p.getSafetyName(); }
            public LocalDate getPlanArrivalDate() { return p.getPlanArrivalDate(); }
            public LocalDate getActualArrivalDate() { return p.getActualArrivalDate(); }
        }
    }
}
