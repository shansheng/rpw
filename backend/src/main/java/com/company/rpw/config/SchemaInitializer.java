package com.company.rpw.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

/**
 * 启动时幂等建表。
 * rpw 为精简业务后端，缺少 yudao 的部分系统表；此处仅对站内信模块所需的
 * notify_message 表做 CREATE TABLE IF NOT EXISTS，避免依赖外部 mysql 客户端。
 * 表结构与 yudao 的 sys_notify_message 对齐，并补充 BaseEntity 的通用字段。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@Order(1)
public class SchemaInitializer {

    private final DataSource dataSource;

    @PostConstruct
    public void init() {
        String notifyDdl = "CREATE TABLE IF NOT EXISTS notify_message ("
                + "id BIGINT NOT NULL AUTO_INCREMENT, "
                + "user_id BIGINT NOT NULL DEFAULT 0, "
                + "user_type TINYINT NOT NULL DEFAULT 1, "
                + "template_id BIGINT DEFAULT NULL, "
                + "template_code VARCHAR(63) DEFAULT NULL, "
                + "template_nickname VARCHAR(63) DEFAULT NULL, "
                + "template_content VARCHAR(1024) DEFAULT NULL, "
                + "template_type TINYINT DEFAULT NULL, "
                + "template_params VARCHAR(1024) DEFAULT NULL, "
                + "read_status TINYINT NOT NULL DEFAULT 0, "
                + "read_time DATETIME DEFAULT NULL, "
                + "create_time DATETIME DEFAULT NULL, "
                + "update_time DATETIME DEFAULT NULL, "
                + "deleted TINYINT NOT NULL DEFAULT 0, "
                + "PRIMARY KEY (id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";

        // BPM 工作流模块所需表（对标 yudao bpm）
        String bpmCategory = "CREATE TABLE IF NOT EXISTS bpm_category ("
                + "id BIGINT NOT NULL AUTO_INCREMENT, name VARCHAR(64) NOT NULL DEFAULT '', code VARCHAR(64) NOT NULL DEFAULT '', "
                + "status TINYINT NOT NULL DEFAULT 0, description VARCHAR(255) DEFAULT '', sort INT NOT NULL DEFAULT 0, "
                + "create_time DATETIME DEFAULT NULL, update_time DATETIME DEFAULT NULL, deleted TINYINT NOT NULL DEFAULT 0, "
                + "PRIMARY KEY (id), UNIQUE KEY uk_code (code)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";

        String bpmUserGroup = "CREATE TABLE IF NOT EXISTS bpm_user_group ("
                + "id BIGINT NOT NULL AUTO_INCREMENT, name VARCHAR(64) NOT NULL DEFAULT '', description VARCHAR(255) DEFAULT '', "
                + "user_ids VARCHAR(1024) DEFAULT '', status TINYINT NOT NULL DEFAULT 0, remark VARCHAR(255) DEFAULT '', "
                + "create_time DATETIME DEFAULT NULL, update_time DATETIME DEFAULT NULL, deleted TINYINT NOT NULL DEFAULT 0, "
                + "PRIMARY KEY (id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";

        String bpmProcessExpression = "CREATE TABLE IF NOT EXISTS bpm_process_expression ("
                + "id BIGINT NOT NULL AUTO_INCREMENT, name VARCHAR(64) NOT NULL DEFAULT '', status TINYINT NOT NULL DEFAULT 0, "
                + "expression VARCHAR(255) DEFAULT '', remark VARCHAR(255) DEFAULT '', "
                + "create_time DATETIME DEFAULT NULL, update_time DATETIME DEFAULT NULL, deleted TINYINT NOT NULL DEFAULT 0, "
                + "PRIMARY KEY (id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";

        String bpmProcessListener = "CREATE TABLE IF NOT EXISTS bpm_process_listener ("
                + "id BIGINT NOT NULL AUTO_INCREMENT, name VARCHAR(64) NOT NULL DEFAULT '', type VARCHAR(16) DEFAULT '', "
                + "status TINYINT NOT NULL DEFAULT 0, event VARCHAR(32) DEFAULT '', value_type VARCHAR(16) DEFAULT '', "
                + "value VARCHAR(255) DEFAULT '', remark VARCHAR(255) DEFAULT '', "
                + "create_time DATETIME DEFAULT NULL, update_time DATETIME DEFAULT NULL, deleted TINYINT NOT NULL DEFAULT 0, "
                + "PRIMARY KEY (id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";

        String bpmForm = "CREATE TABLE IF NOT EXISTS bpm_form ("
                + "id BIGINT NOT NULL AUTO_INCREMENT, name VARCHAR(64) NOT NULL DEFAULT '', conf LONGTEXT, fields LONGTEXT, "
                + "status TINYINT NOT NULL DEFAULT 0, remark VARCHAR(255) DEFAULT '', "
                + "create_time DATETIME DEFAULT NULL, update_time DATETIME DEFAULT NULL, deleted TINYINT NOT NULL DEFAULT 0, "
                + "PRIMARY KEY (id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";

        String bpmModel = "CREATE TABLE IF NOT EXISTS bpm_model ("
                + "id BIGINT NOT NULL AUTO_INCREMENT, name VARCHAR(64) NOT NULL DEFAULT '', `key` VARCHAR(191) DEFAULT '', "
                + "icon VARCHAR(64) DEFAULT '', category VARCHAR(64) DEFAULT '', type TINYINT NOT NULL DEFAULT 1, form_type TINYINT DEFAULT NULL, form_id BIGINT DEFAULT NULL, "
                + "form_custom_create_path VARCHAR(255) DEFAULT '', form_custom_view_path VARCHAR(255) DEFAULT '', "
                + "process_definition_key VARCHAR(191) DEFAULT '', process_definition_id VARCHAR(191) DEFAULT '', "
                + "process_definition_version INT DEFAULT NULL, deployment_time DATETIME DEFAULT NULL, "
                + "status TINYINT NOT NULL DEFAULT 0, bpmn_xml LONGTEXT, start_user_ids VARCHAR(1024) DEFAULT '', "
                + "sort INT NOT NULL DEFAULT 0, remark VARCHAR(255) DEFAULT '', description VARCHAR(255) DEFAULT '', "
                + "create_time DATETIME DEFAULT NULL, update_time DATETIME DEFAULT NULL, deleted TINYINT NOT NULL DEFAULT 0, "
                + "PRIMARY KEY (id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";

        String bpmComment = "CREATE TABLE IF NOT EXISTS bpm_comment ("
                + "id BIGINT NOT NULL AUTO_INCREMENT, task_id VARCHAR(64) DEFAULT '', process_instance_id VARCHAR(64) DEFAULT '', "
                + "type TINYINT DEFAULT NULL, message LONGTEXT, user_id BIGINT DEFAULT NULL, "
                + "create_time DATETIME DEFAULT NULL, update_time DATETIME DEFAULT NULL, deleted TINYINT NOT NULL DEFAULT 0, "
                + "PRIMARY KEY (id), KEY idx_pi (process_instance_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";

        String bpmOaLeave = "CREATE TABLE IF NOT EXISTS bpm_oa_leave ("
                + "id BIGINT NOT NULL AUTO_INCREMENT, user_id BIGINT DEFAULT NULL, type VARCHAR(16) DEFAULT '', reason VARCHAR(512) DEFAULT '', "
                + "start_time DATETIME DEFAULT NULL, end_time DATETIME DEFAULT NULL, status TINYINT NOT NULL DEFAULT 0, "
                + "process_instance_id VARCHAR(64) DEFAULT '', "
                + "create_time DATETIME DEFAULT NULL, update_time DATETIME DEFAULT NULL, deleted TINYINT NOT NULL DEFAULT 0, "
                + "PRIMARY KEY (id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";

        List<String> ddls = List.of(notifyDdl, bpmCategory, bpmUserGroup, bpmProcessExpression,
                bpmProcessListener, bpmForm, bpmModel, bpmComment, bpmOaLeave);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String ddl : ddls) {
                stmt.execute(ddl);
            }
            // 兼容已存在的 bpm_model 表：补充 description 列（MySQL 8 不支持 ADD COLUMN IF NOT EXISTS）
            try (Statement s2 = conn.createStatement()) {
                s2.execute("ALTER TABLE bpm_model ADD COLUMN description VARCHAR(255) DEFAULT ''");
            } catch (Exception ignored) { /* 列已存在则忽略 */ }

            // 组织管理（统一 局/公司/项目 + 部门 树）扩展列
            addOrganizationColumns(stmt);

            log.info("[SchemaInitializer] BPM 相关表已就绪（CREATE TABLE IF NOT EXISTS）");
        } catch (Exception e) {
            log.error("[SchemaInitializer] 创建 BPM 表失败", e);
        }
    }

    /**
     * 给 organization 表幂等补充组织树所需列。
     * MySQL 8 不支持 ADD COLUMN IF NOT EXISTS，逐列 try/catch 忽略「重复列」错误。
     */
    private void addOrganizationColumns(Statement stmt) {
        String[] alters = {
                "ALTER TABLE organization ADD COLUMN node_type TINYINT NOT NULL DEFAULT 1 COMMENT '节点类型：1组织节点(局/公司/项目) 2部门节点'",
                "ALTER TABLE organization ADD COLUMN sort INT NOT NULL DEFAULT 0 COMMENT '同级排序（越小越靠前）'",
                "ALTER TABLE organization ADD COLUMN project_code VARCHAR(50) DEFAULT '' COMMENT '项目编码（仅项目节点）'",
                "ALTER TABLE organization ADD COLUMN status INT DEFAULT NULL COMMENT '状态（仅项目节点：1进行中 2已完工 3已暂停）'",
                "ALTER TABLE organization ADD COLUMN plan_start_date DATE DEFAULT NULL COMMENT '计划开始日期（仅项目节点）'",
                "ALTER TABLE organization ADD COLUMN plan_end_date DATE DEFAULT NULL COMMENT '计划结束日期（仅项目节点）'",
        };
        for (String sql : alters) {
            try {
                stmt.execute(sql);
            } catch (Exception ignored) { /* 列已存在则忽略 */ }
        }
        log.info("[SchemaInitializer] organization 组织树扩展列已就绪");
    }
}
