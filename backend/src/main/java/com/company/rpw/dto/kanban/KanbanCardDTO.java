package com.company.rpw.dto.kanban;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

/**
 * 卡片状态更新 DTO
 */
@Data
public class KanbanCardDTO {

    /** 卡片ID */
    @NotNull(message = "卡片ID不能为空")
    private Long id;

    /** 资源类型（SUBTRACT-分包, LABOR-劳动力, etc.） */
    @NotBlank(message = "资源类型不能为空")
    private String resourceType;

    /** 新状态 */
    @NotBlank(message = "新状态不能为空")
    private String newStatus;

    /** 备注（可选） */
    private String remark;
}
