package com.company.rpw;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 资源计划预警系统启动类
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.company.rpw.mapper")
public class RpwApplication {

    public static void main(String[] args) {
        SpringApplication.run(RpwApplication.class, args);
    }
}
