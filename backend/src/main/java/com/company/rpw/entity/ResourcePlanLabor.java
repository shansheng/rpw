package com.company.rpw.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.company.rpw.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 劳动力计划实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resource_plan_labor")
public class ResourcePlanLabor extends BaseEntity {

    /** 项目ID */
    private Long projectId;

    /** WBS编码 */
    private String wbsCode;

    /** WBS名称（冗余存储，减少关联查询） */
    private String wbsName;

    /** 工种编码 */
    private String workTypeCode;

    /** 工种名称（冗余存储） */
    private String workTypeName;

    /** 劳务类别编码 */
    private String laborCategoryCode;

    /** 劳务类别名称（冗余存储） */
    private String laborCategoryName;

    /** 计划人数 */
    private Integer planQuantity;

    /** 计划开始日期 */
    private LocalDate planStartDate;

    /** 计划结束日期 */
    private LocalDate planEndDate;

    /** 实际人数 */
    private Integer actualQuantity;

    /** 实际开始日期 */
    private LocalDate actualStartDate;

    /** 实际结束日期 */
    private LocalDate actualEndDate;

    /** 状态（DRAFT-草稿/SUBMITTED-已提交/IN_PROGRESS-进行中/COMPLETED-已完成/TERMINATED-已终止） */
    private String status;

    /** 审批状态（DRAFT-草稿/SUBMITTED-已提交/APPROVED-已批准/REJECTED-已驳回） */
    private String approvalStatus;

    /** Flowable流程实例ID */
    private String processInstanceId;

    /** 出勤记录（JSON格式） */
    private String attendanceRecords;

    /** 备注 */
    private String remark;
}
