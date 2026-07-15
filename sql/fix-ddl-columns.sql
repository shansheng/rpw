-- =============================================================
-- 数据库修复：统一列命名 + 创建缺失表
-- =============================================================

-- 表集合：需要统一 created_at/updated_at → create_time/update_time
-- 规则：如果表已有 create_time 列则删旧列，否则重命名

-- 先处理有 BOTH 列的表（删旧保留新）
ALTER TABLE resource_plan_material DROP COLUMN created_at, DROP COLUMN updated_at;
ALTER TABLE resource_plan_equipment DROP COLUMN created_at, DROP COLUMN updated_at;
ALTER TABLE resource_plan_hardware DROP COLUMN created_at, DROP COLUMN updated_at;
ALTER TABLE resource_plan_circulation DROP COLUMN created_at, DROP COLUMN updated_at;
ALTER TABLE resource_plan_office DROP COLUMN created_at, DROP COLUMN updated_at;
ALTER TABLE resource_plan_safety DROP COLUMN created_at, DROP COLUMN updated_at;
ALTER TABLE resource_plan_subcontract DROP COLUMN created_at, DROP COLUMN updated_at;
ALTER TABLE resource_plan_labor DROP COLUMN created_at, DROP COLUMN updated_at;

-- 再处理只有旧列的表（重命名）
ALTER TABLE organization CHANGE COLUMN created_at create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE organization CHANGE COLUMN updated_at update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

ALTER TABLE company CHANGE COLUMN created_at create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE company CHANGE COLUMN updated_at update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

ALTER TABLE project CHANGE COLUMN created_at create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE project CHANGE COLUMN updated_at update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

ALTER TABLE dict_wbs CHANGE COLUMN created_at create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE dict_wbs CHANGE COLUMN updated_at update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE dict_resource CHANGE COLUMN created_at create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE dict_resource CHANGE COLUMN updated_at update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE dict_supplier CHANGE COLUMN created_at create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE dict_supplier CHANGE COLUMN updated_at update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE dict_purchase_progress CHANGE COLUMN created_at create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE dict_purchase_progress CHANGE COLUMN updated_at update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE dict_purchase_source CHANGE COLUMN created_at create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE dict_purchase_source CHANGE COLUMN updated_at update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE dict_shipping_progress CHANGE COLUMN created_at create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE dict_shipping_progress CHANGE COLUMN updated_at update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE dict_work_type CHANGE COLUMN created_at create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE dict_work_type CHANGE COLUMN updated_at update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE dict_labor_category CHANGE COLUMN created_at create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE dict_labor_category CHANGE COLUMN updated_at update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE warning_setting CHANGE COLUMN created_at create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE warning_setting CHANGE COLUMN updated_at update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 创建 report_config 表
CREATE TABLE IF NOT EXISTS report_config (
  id bigint NOT NULL AUTO_INCREMENT,
  user_id bigint NOT NULL COMMENT '用户ID',
  report_name varchar(100) NOT NULL COMMENT '报表名称',
  report_type varchar(50) NOT NULL COMMENT '报表类型',
  config_json text COMMENT '配置JSON',
  is_default tinyint DEFAULT '0' COMMENT '是否默认配置',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表配置表';

-- 创建 report_export_log 表
CREATE TABLE IF NOT EXISTS report_export_log (
  id bigint NOT NULL AUTO_INCREMENT,
  user_id bigint NOT NULL COMMENT '用户ID',
  report_type varchar(50) DEFAULT NULL COMMENT '报表类型',
  export_type varchar(20) NOT NULL COMMENT '导出格式(excel/pdf/csv)',
  record_count int DEFAULT '0' COMMENT '导出记录数',
  export_time datetime NOT NULL COMMENT '导出时间',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表导出日志表';

SELECT '✅ All DDL fix completed' AS status;
