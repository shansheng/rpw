package com.company.rpw.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 企业微信配置类
 */
@Data
@Component
@ConfigurationProperties(prefix = "wecom")
public class WeComConfig {
    
    /** API基础URL */
    private String apiUrl = "https://qyapi.weixin.qq.com";
    
    /** 企业ID */
    private String corpId;
    
    /** 应用Secret */
    private String secret;
    
    /** 应用AgentId */
    private String agentId;
}
