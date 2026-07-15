package com.company.rpw.dto.subcontract;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

/**
 * 分包计划进展登记 DTO
 */
@Data
public class ProgressDTO {

    /** 实际开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate actualStartDate;

    /** 实际结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate actualEndDate;

    /** 备注 */
    private String remark;
}
