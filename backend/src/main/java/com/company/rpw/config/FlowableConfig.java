package com.company.rpw.config;

import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Flowable流程引擎配置类（使用Spring Boot自动配置）
 */
@Configuration
public class FlowableConfig {

    /**
     * Flowable流程引擎配置
     */
    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> processEngineConfigurationConfigurer() {
        return configuration -> {
            // 数据库已预建，不再自动管理
            configuration.setDatabaseSchemaUpdate(org.flowable.engine.ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE);
            // 历史记录级别
            configuration.setHistory("full");
            // 异步执行器（用于定时事件等）
            configuration.setAsyncExecutorActivate(true);
        };
    }
}
