package com.company.rpw.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.rpw.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 分包计划变更明细表（主从结构之明细表）
 *
 * <p>每条明细对应一种日期类型的调整：原日期（取自所选分包计划当前值）与
 * 调整后日期（用户填写）。日期类型枚举：
 * 1=最晚进场日期, 2=招标文件日期, 3=挂网日期, 4=定标日期。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resource_plan_subcontract_change_detail")
public class ResourcePlanSubcontractChangeDetail extends BaseEntity {

    /** 关联变更主表ID */
    private Long changeId;

    /** 序号（明细行顺序） */
    private Integer seq;

    /** 日期类型：1最晚进场 2招标文件 3挂网 4定标 */
    private Integer dateType;

    /** 原日期（取自所选分包计划当前值，只读） */
    private LocalDate originalDate;

    /** 调整后日期（用户填写，审批通过后写回分包计划） */
    private LocalDate adjustedDate;
}
