package com.company.rpw.dto.flowable;

import lombok.Data;

import java.util.Map;

/**
 * 完成任务请求DTO
 */
@Data
public class CompleteTaskDTO {
    
    /** 任务ID */
    private String taskId;
    
    /** 流程变量 */
    private Map<String, Object> variables;
}
