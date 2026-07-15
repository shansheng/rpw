package com.company.rpw.dto.labor;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

/**
 * 劳动力计划进展登记 DTO
 */
@Data
public class LaborProgressDTO {

    /** 实际人数 */
    private Integer actualQuantity;

    /** 实际开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate actualStartDate;

    /** 实际结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate actualEndDate;

    /** 备注 */
    private String remark;

    /** 出勤记录（JSON格式） */
    private String attendanceRecords;
}
