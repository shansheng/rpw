package com.company.rpw.dto.labor;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

/**
 * 劳动力计划变更申请 DTO
 */
@Data
public class LaborChangeDTO {

    /** 劳动力计划ID */
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

    /** 新劳务类别编码（修改时填写） */
    private String newLaborCategoryCode;

    /** 新劳务类别名称 */
    private String newLaborCategoryName;
}
