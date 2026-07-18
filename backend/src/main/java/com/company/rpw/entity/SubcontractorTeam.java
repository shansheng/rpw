package com.company.rpw.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.rpw.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 分包队伍统计实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("subcontractor_team")
public class SubcontractorTeam extends BaseEntity {

    /** 项目ID */
    private Long projectId;

    /** 专业工程 */
    private String professionalEngineering;

    /** 分包名称 */
    private String subcontractName;

    /** 分包模式 */
    private String subcontractMode;

    /** 分包队伍来源 */
    private String teamSource;

    /** 最晚进场日期（与总进度计划相符） */
    private LocalDate latestEntryDate;

    /** 实际进场日期（按招标进度推算） */
    private LocalDate actualEntryDate;

    /** 开始编制招标文件日期 */
    private LocalDate tenderDocStartDate;

    /** 挂网招标日期 */
    private LocalDate onlineTenderDate;

    /** 定标日期 */
    private LocalDate bidAwardDate;

    /** 动员期（天） */
    private Integer mobilizationPeriodDays;

    /** 备注 */
    private String remarks;
}
