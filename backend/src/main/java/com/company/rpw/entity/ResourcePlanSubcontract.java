package com.company.rpw.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.company.rpw.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 分包计划实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resource_plan_subcontract")
public class ResourcePlanSubcontract extends BaseEntity {

    /** 项目ID（关联 organization 表） */
    private Long projectId;

    /** 项目名称（冗余存储） */
    private String projectName;

    /** 专业工程 */
    private String specialtyEngineering;

    /** 分包名称 */
    private String planName;

    /** 分包名称（同 plan_name，用于前端展示） */
    private String subcontractName;

    /** 分包模式 */
    private String subcontractMode;

    /** 分包队伍来源 */
    private String teamSource;

    /** 最晚进场日期（与总进度计划相符） */
    private LocalDate latestEntryDate;

    /** 实际进场日期（按招标进度推算） */
    private LocalDate actualEntryDate;

    /** 开始编制招标文件日期 */
    private LocalDate startPrepareBidDate;

    /** 实际招标日期 */
    private LocalDate actualBidDate;

    /** 挂网招标日期 */
    private LocalDate plannedOnlineBidDate;

    /** 实际挂网日期 */
    private LocalDate actualOnlineBidDate;

    /** 定标日期 */
    private LocalDate plannedAwardDate;

    /** 实际定标日期 */
    private LocalDate actualAwardDate;

    /** 动员期（天） */
    private Integer mobilizationPeriod;

    /** WBS编码（保留兼容） */
    private String wbsCode;

    /** WBS名称（冗余存储） */
    private String wbsName;

    /** 工作内容（保留兼容） */
    private String workContent;

    /** 供应商编码（保留兼容） */
    private String supplierCode;

    /** 供应商名称（冗余存储） */
    private String supplierName;

    /** 计划开始日期（保留兼容） */
    private LocalDate planStartDate;

    /** 计划结束日期（保留兼容） */
    private LocalDate planEndDate;

    /** 实际开始日期（保留兼容） */
    private LocalDate actualStartDate;

    /** 实际结束日期（保留兼容） */
    private LocalDate actualEndDate;

    /** 状态（DRAFT-草稿/SUBMITTED-已提交/IN_PROGRESS-进行中/COMPLETED-已完成/TERMINATED-已终止） */
    private String status;

    /** 审批状态（DRAFT-草稿/SUBMITTED-已提交/APPROVED-已批准/REJECTED-已驳回） */
    private String approvalStatus;

    /** Flowable流程实例ID */
    private String processInstanceId;

    /** 备注 */
    private String remark;
}
