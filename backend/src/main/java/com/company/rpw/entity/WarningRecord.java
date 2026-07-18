package com.company.rpw.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.company.rpw.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 预警记录实体
 *
 * <p>由 {@code WarningRuleService} 根据规则定义与计划数据生成，记录触发该预警的计划、
 * 预警类型与具体的计算原因。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("warning_record")
public class WarningRecord extends BaseEntity {

    /** 触发规则ID */
    private Long ruleId;

    /** 预警对象类型 */
    private String objectType;

    /** 计划ID（关联各资源计划表主键） */
    private Long planId;

    /** 计划名称（冗余存储，便于展示与检索） */
    private String planName;

    /** 预警类型（RED-红色/ORANGE-橙色/YELLOW-黄色） */
    private String warningLevel;

    /** 预警原因（规则定义 + 代入实际数据后的计算值） */
    private String reason;

    /** 触发时的规则表达式快照 */
    private String conditionExpr;

    /** 处理状态（PENDING-待处理/RESOLVED-已解决/IGNORED-已忽略） */
    private String status;

    /** 触发时间 */
    private LocalDateTime triggeredTime;

    /** 处理时间 */
    private LocalDateTime handleTime;

    /** 处理备注 */
    private String handleRemark;
}
