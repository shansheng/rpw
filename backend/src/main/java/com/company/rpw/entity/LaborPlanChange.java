package com.company.rpw.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.company.rpw.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 劳动力计划变更记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("labor_plan_change")
public class LaborPlanChange extends BaseEntity {

    /** 劳动力计划ID */
    private Long laborPlanId;

    /** 变更类型: DELAY/MODIFY_QUANTITY/MODIFY_DATE */
    private String changeType;

    /** 变更原因 */
    private String changeReason;

    /** 原计划开始日期 */
    private LocalDate oldPlanStartDate;

    /** 新计划开始日期 */
    private LocalDate newPlanStartDate;

    /** 原计划结束日期 */
    private LocalDate oldPlanEndDate;

    /** 新计划结束日期 */
    private LocalDate newPlanEndDate;

    /** 原计划人数 */
    private Integer oldPlanQuantity;

    /** 新计划人数 */
    private Integer newPlanQuantity;

    /** 审批状态: PENDING/APPROVED/REJECTED */
    private String approvalStatus;

    /** Flowable流程实例ID */
    private String processInstanceId;

    /** 申请人 */
    private String applicant;
}
