package com.company.rpw.controller;

import com.company.rpw.common.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 数据库操作Controller
 * 用于执行SQL脚本等数据库维护操作
 */
@RestController
@RequestMapping("/api/v1/database")
public class DatabaseController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 执行 resource_plan_material 表更新SQL
     */
    @PostMapping("/execute-update-sql")
    public R<Void> executeUpdateSql() {
        try {
            // 读取SQL文件
            String sqlFilePath = "sql/update-resource-plan-material.sql";
            String sqlContent = new String(Files.readAllBytes(Paths.get(sqlFilePath)));
            
            // 按分号分割并执行每条SQL
            String[] sqlStatements = sqlContent.split(";");
            for (String sql : sqlStatements) {
                sql = sql.trim();
                if (!sql.isEmpty() && !sql.startsWith("--")) {
                    try {
                        jdbcTemplate.execute(sql);
                    } catch (Exception e) {
                        // 如果字段已存在，忽略错误
                        if (!e.getMessage().contains("Duplicate column name")) {
                            throw e;
                        }
                    }
                }
            }
            
            return R.ok();
        } catch (Exception e) {
            return R.fail(500, "执行SQL失败: " + e.getMessage());
        }
    }
}
