package com.company.rpw.dto.subcontract;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

/**
 * 分包计划变更申请 DTO
 */
@Data
public class ChangeDTO {

    /** 分包计划ID */
    private Long planId;

    /** 变更类型（DELAY-延期/MODIFY-修改/TERMINATE-终止） */
    private String changeType;

    /** 变更原因 */
    private String changeReason;

    /** 变更详情（JSON格式） */
    private String changeDetails;

    /** 新计划开始日期（延期时填写） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate newPlanStartDate;

    /** 新计划结束日期（延期时填写） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate newPlanEndDate;

    /** 新供应商编码（修改供应商时填写） */
    private String newSupplierCode;

    /** 新供应商名称 */
    private String newSupplierName;
}
