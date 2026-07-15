package com.company.rpw.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.company.rpw.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预警记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("warning_record")
public class WarningRecord extends BaseEntity {

    /** 预警规则ID */
    private Long ruleId;

    /** 资源类型 */
    private String resourceType;

    /** 项目ID */
    private Long projectId;

    /** 资源计划ID（关联各资源表的主键） */
    private Long resourceId;

    /** 预警等级（GENERAL-一般/IMPORTANT-重要/URGENT-紧急） */
    private String warningLevel;

    /** 预警消息内容 */
    private String warningMessage;

    /** 计划值 */
    private BigDecimal planValue;

    /** 实际值 */
    private BigDecimal actualValue;

    /** 偏差率（百分比） */
    private BigDecimal deviationRate;

    /** 处理状态（PENDING-待处理/PROCESSING-处理中/RESOLVED-已解决/IGNORED-已忽略） */
    private String status;

    /** 处理人ID */
    private Long handledBy;

    /** 处理时间 */
    private LocalDateTime handleTime;

    /** 处理备注 */
    private String handleRemark;

    /** 通知状态（NOT_SENT-未发送/SENT-已发送/FAILED-发送失败） */
    private String notifyStatus;

    /** 企业微信消息ID */
    private String wecomMsgId;

    /** 触发时间 */
    private LocalDateTime triggeredTime;
}
