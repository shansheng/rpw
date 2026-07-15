package com.company.rpw.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.company.rpw.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 报表导出记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("report_export_log")
public class ReportExportLog extends BaseEntity {

    /** 用户ID */
    private Long userId;

    /** 报表配置ID */
    private Long reportConfigId;

    /** 导出类型（EXCEL, PDF） */
    private String exportType;

    /** 文件路径 */
    private String filePath;

    /** 导出记录数 */
    private Integer recordCount;

    /** 导出时间 */
    private java.time.LocalDateTime exportTime;
}
