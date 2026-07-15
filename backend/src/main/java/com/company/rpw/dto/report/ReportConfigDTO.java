package com.company.rpw.dto.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 报表配置保存 DTO
 */
@Data
public class ReportConfigDTO {

    /** 配置ID（更新时传入） */
    private Long id;

    /** 用户ID */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /** 报表名称 */
    @NotBlank(message = "报表名称不能为空")
    private String reportName;

    /** 报表类型 */
    @NotBlank(message = "报表类型不能为空")
    private String reportType;

    /** 配置JSON */
    @NotBlank(message = "配置内容不能为空")
    private String configJson;

    /** 是否默认配置 */
    private Boolean isDefault;
}
