package com.company.rpw.dto.report;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报表配置 VO
 */
@Data
public class ReportConfigVO {

    /** 配置ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 报表名称 */
    private String reportName;

    /** 报表类型 */
    private String reportType;

    /** 配置JSON */
    private String configJson;

    /** 是否默认配置 */
    private Integer isDefault;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
