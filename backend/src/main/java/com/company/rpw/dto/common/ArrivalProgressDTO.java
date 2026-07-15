package com.company.rpw.dto.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

/**
 * 到场进展登记 DTO（适用于材料、设备、办公用品、安全物资、周转材、五金）
 */
@Data
public class ArrivalProgressDTO {

    /** 实际到场日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate actualArrivalDate;

    /** 备注 */
    private String remark;
}
