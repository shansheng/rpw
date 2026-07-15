package com.company.rpw.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.company.rpw.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 预警规则实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("warning_rule")
public class WarningRule extends BaseEntity {

    /** 规则名称 */
    private String ruleName;

    /** 资源类型（MATERIAL/EQUIPMENT/HARDWARE/CIRCULATION/OFFICE/SAFETY/SUBCONTRACT/LABOR） */
    private String resourceType;

    /** 项目ID（NULL表示适用于所有项目） */
    private Long projectId;

    /** 阈值类型（RATE-比率/DATE-日期/QUANTITY-数量） */
    private String thresholdType;

    /** 预警阈值（如80.00表示80%） */
    private BigDecimal warningThreshold;

    /** 对比字段 */
    private String compareField;

    /** 实际值字段 */
    private String actualField;

    /** 预警等级（GENERAL-一般/IMPORTANT-重要/URGENT-紧急） */
    private String warningLevel;

    /** 检查频率（REALTIME-实时/DAILY-每日/WEEKLY-每周） */
    private String checkFrequency;

    /** 是否启用（0-禁用，1-启用） */
    private Integer enabled;

    /** 是否推送企业微信（0-否，1-是） */
    private Integer notifyWecom;

    /** 通知用户ID列表（逗号分隔） */
    private String notifyUsers;

    /** 规则描述 */
    private String description;
}
