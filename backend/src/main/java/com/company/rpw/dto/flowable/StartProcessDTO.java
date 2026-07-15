package com.company.rpw.dto.flowable;

import lombok.Data;

import java.util.Map;

/**
 * 启动流程实例请求DTO
 */
@Data
public class StartProcessDTO {
    
    /** 流程定义KEY */
    private String processDefinitionKey;
    
    /** 业务KEY（如计划ID） */
    private String businessKey;
    
    /** 流程变量 */
    private Map<String, Object> variables;
}
