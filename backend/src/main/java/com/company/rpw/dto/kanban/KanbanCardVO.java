package com.company.rpw.dto.kanban;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

/**
 * 看板卡片视图对象
 */
@Data
public class KanbanCardVO {

    /** 卡片ID */
    private Long id;

    /** 资源类型（SUBTRACT-分包, LABOR-劳动力, EQUIPMENT-设备, etc.） */
    private String resourceType;

    /** WBS编码 */
    private String wbsCode;

    /** 资源名称（分包名称、劳动力类型等） */
    private String resourceName;

    /** 状态 */
    private String status;

    /** 负责人 */
    private String responsiblePerson;

    /** 计划开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate planStartDate;

    /** 计划结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate planEndDate;

    /** 优先级（HIGH-高, MEDIUM-中, LOW-低） */
    private String priority;
}
