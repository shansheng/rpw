-- Phase 7: 自定义报表功能 - 数据库表创建
-- 执行时间: 2026-05-10

-- 报表配置表
CREATE TABLE IF NOT EXISTS report_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    report_name VARCHAR(100) NOT NULL COMMENT '报表名称',
    report_type VARCHAR(50) NOT NULL COMMENT '报表类型（SUBTRACT-分包, LABOR-劳动力, EQUIPMENT-设备, etc.）',
    config_json TEXT NOT NULL COMMENT '配置JSON（字段、筛选条件、排序规则）',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认配置（0-否，1-是）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_report_type (report_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表配置表';

-- 报表导出记录表
CREATE TABLE IF NOT EXISTS report_export_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    report_config_id BIGINT COMMENT '报表配置ID',
    export_type VARCHAR(20) NOT NULL COMMENT '导出类型（EXCEL, PDF）',
    file_path VARCHAR(500) COMMENT '文件路径',
    record_count INT DEFAULT 0 COMMENT '导出记录数',
    export_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_export_time (export_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表导出记录表';
