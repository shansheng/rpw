package com.company.rpw.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.List;

/**
 * 临时解决方案：清除 Flowable 相关的所有表和 Liquibase 记录
 * 在开发环境下使用，生产环境请删除此类
 *
 * 此配置会在 Spring Boot 启动时执行：
 * 1. 删除所有 Flowable 表（ACT_*, FLW_*）
 * 2. 删除 Liquibase 相关表
 * 3. 让 Spring Boot + Flowable 重新初始化数据库
 */
//@Slf4j
//@Component
//@Profile("dev")
// 已禁用：此临时清理类每次启动都会删除 Flowable 表，导致后台线程报错
// 如需重新初始化 Flowable 数据库，取消注释后启动一次再注释回来
@Slf4j
@Component
@Profile("flowable-reset") // 仅在 flowable-reset profile 激活时生效
public class LiquibaseFixConfig implements CommandLineRunner {

    @Autowired
    private DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            DatabaseMetaData metaData = dataSource.getConnection().getMetaData();

            // 获取所有表名
            List<String> tablesToDelete = new ArrayList<>();
            var rs = metaData.getTables(null, null, "%", new String[]{"TABLE"});
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                // 删除 Flowable 表（ACT_*, FLW_*）和 Liquibase 表
                if (tableName.startsWith("ACT_") ||
                    tableName.startsWith("FLW_") ||
                    tableName.equals("DATABASECHANGELOG") ||
                    tableName.equals("DATABASECHANGELOGLOCK")) {
                    tablesToDelete.add(tableName);
                }
            }
            rs.close();

            // 按依赖顺序删除表（先删子表，再删主表）
            // 重要：ACT_HI_* 表依赖 ACT_RU_* 表，所以先删 HI
            List<String> orderedTables = new ArrayList<>();
            for (String table : tablesToDelete) {
                if (table.startsWith("ACT_HI_")) orderedTables.add(0, table); // 先删 HI
                else if (table.startsWith("ACT_RU_")) orderedTables.add(0, table); // 再删 RU
                else if (table.startsWith("ACT_RE_")) orderedTables.add(0, table); // 再删 RE
                else if (table.startsWith("ACT_GE_")) orderedTables.add(0, table); // 再删 GE
                else if (table.startsWith("FLW_")) orderedTables.add(0, table);
                else if (table.startsWith("ACT_")) orderedTables.add(0, table);
                else orderedTables.add(table); // Liquibase 表最后
            }

            // 删除所有表
            int deletedCount = 0;
            for (String tableName : orderedTables) {
                try {
                    jdbcTemplate.execute("DROP TABLE IF EXISTS " + tableName);
                    log.info("已删除表: {}", tableName);
                    deletedCount++;
                } catch (Exception e) {
                    log.warn("删除表 {} 失败: {}", tableName, e.getMessage());
                }
            }

            log.info("===========================================");
            log.info("Flowable 数据库清理完成！");
            log.info("共删除 {} 个表", deletedCount);
            log.info("Spring Boot 将自动重新初始化数据库...");
            log.info("===========================================");

        } catch (Exception e) {
            log.error("LiquibaseFixConfig 执行失败", e);
        }
    }
}
