package com.company.rpw.dto.report;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 报表查询 DTO
 */
@Data
public class ReportQueryDTO {

    /** 报表类型 */
    @NotBlank(message = "报表类型不能为空")
    private String reportType;

    /** 配置JSON */
    @NotBlank(message = "配置内容不能为空")
    private String configJson;

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页大小 */
    private Integer pageSize = 50;
}
