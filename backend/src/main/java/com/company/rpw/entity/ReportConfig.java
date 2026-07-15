package com.company.rpw.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.company.rpw.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 报表配置实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("report_config")
public class ReportConfig extends BaseEntity {

    /** 用户ID */
    private Long userId;

    /** 报表名称 */
    private String reportName;

    /** 报表类型（SUBTRACT-分包, LABOR-劳动力, EQUIPMENT-设备, etc.） */
    private String reportType;

    /** 配置JSON（字段、筛选条件、排序规则） */
    private String configJson;

    /** 是否默认配置（0-否，1-是） */
    @TableField("is_default")
    private Integer isDefault;
}
