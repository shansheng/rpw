package com.company.rpw.dto.flowable;

import lombok.Data;

import java.util.Date;

/**
 * 任务DTO
 */
@Data
public class TaskDTO {
    
    /** 任务ID */
    private String id;
    
    /** 任务名称 */
    private String name;
    
    /** 任务描述 */
    private String description;
    
    /** 流程实例ID */
    private String processInstanceId;
    
    /** 流程定义ID */
    private String processDefinitionId;
    
    /** 任务负责人 */
    private String assignee;
    
    /** 创建时间 */
    private Date createTime;
    
    /** 任务定义KEY */
    private String taskDefinitionKey;
    
    /** 业务KEY */
    private String businessKey;
}
