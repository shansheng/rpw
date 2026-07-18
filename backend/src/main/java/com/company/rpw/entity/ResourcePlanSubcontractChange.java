package com.company.rpw.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.rpw.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 分包计划变更主表（主从结构之主表）
 *
 * <p>主表记录一条变更申请，关联被选中进行变更的分包计划（planId），
 * 并冗余存储项目名称/专业工程/分包名称/分包模式/队伍来源等字段，
 * 用于列表展示与流程表单。明细见 {@link ResourcePlanSubcontractChangeDetail}。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resource_plan_subcontract_change")
public class ResourcePlanSubcontractChange extends BaseEntity {

    /** 选中的分包计划ID（即编辑界面主表展示的"id"字段） */
    private Long planId;

    /** 项目ID（关联 organization 表） */
    private Long projectId;

    /** 项目名称（冗余存储） */
    private String projectName;

    /** 专业工程 */
    private String specialtyEngineering;

    /** 分包名称 */
    private String subcontractName;

    /** 分包模式 */
    private String subcontractMode;

    /** 队伍来源 */
    private String teamSource;

    /** 备注 */
    private String remark;

    /** 状态：RUNNING 审批中 / APPROVED 通过 / CANCEL 已取消 */
    private String status;

    /** 关联流程实例ID */
    private String processInstanceId;

    /** 发起人用户名（冗余存储） */
    private String creator;
}
