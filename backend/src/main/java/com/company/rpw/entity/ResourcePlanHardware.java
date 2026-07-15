package com.company.rpw.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.company.rpw.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 五金计划实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resource_plan_hardware")
public class ResourcePlanHardware extends BaseEntity {

    /** 项目ID */
    private Long projectId;

    /** WBS编码 */
    private String wbsCode;

    /** 五金名称 */
    private String hardwareName;

    /** 规格型号 */
    private String specification;

    /** 单位 */
    private String unit;

    /** 预算数量 */
    private BigDecimal budgetQuantity;

    /** 供应商编码 */
    private String supplierCode;

    /** 采购来源编码 */
    private String purchaseSourceCode;

    /** 采购进度编码 */
    private String purchaseProgressCode;

    /** 发货进度编码 */
    private String shippingProgressCode;

    /** 计划到场日期 */
    private LocalDate planArrivalDate;

    /** 实际到场日期 */
    private LocalDate actualArrivalDate;

    /** 备注 */
    private String remark;

    /** 状态(DRAFT/SUBMITTED/IN_PROGRESS/COMPLETED/TERMINATED) */
    private String status;
}
