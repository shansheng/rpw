-- =====================================================
-- 资源计划预警系统 (RPW) - 全量数据库初始化脚本
-- 适用于全新部署环境
-- MySQL 8.0+
-- 执行方式: mysql -u root -p < init-all.sql
-- =====================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS rpw_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE rpw_db;

-- =====================================================
-- 提示信息
-- =====================================================
SELECT '>> 开始初始化 RPW 数据库...' AS message;

-- =====================================================
-- 以下 SQL 由各增量脚本合并而来
-- 执行顺序: schema -> ddl-fix -> material-update -> phase6 -> phase7
-- =====================================================

-- [1/5] 基础表结构 (01_schema.sql)
SELECT '>> [1/5] 创建基础表结构...' AS message;

-- 组织架构表
CREATE TABLE IF NOT EXISTS `organization` (
    `id`             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `org_name`       VARCHAR(100)   NOT NULL COMMENT '组织名称',
    `org_level`      TINYINT        NOT NULL COMMENT '组织级别：1局 2公司 3项目',
    `parent_id`      BIGINT         NULL COMMENT '上级组织ID',
    `department`     VARCHAR(100)   NULL COMMENT '部门',
    `section`        VARCHAR(100)   NULL COMMENT '科室/区段',
    `created_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    INDEX idx_parent_id (parent_id),
    INDEX idx_org_level (org_level)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '组织架构表';

-- 公司表
CREATE TABLE IF NOT EXISTS `company` (
    `id`             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `company_name`   VARCHAR(100)   NOT NULL COMMENT '公司名称',
    `org_id`         BIGINT         NOT NULL COMMENT '所属组织ID',
    `created_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    INDEX idx_org_id (org_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '公司表';

-- 项目表
CREATE TABLE IF NOT EXISTS `project` (
    `id`             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `project_code`   VARCHAR(50)    NOT NULL COMMENT '项目编码',
    `project_name`   VARCHAR(200)   NOT NULL COMMENT '项目名称',
    `org_id`         BIGINT         NOT NULL COMMENT '所属组织ID',
    `company_id`     BIGINT         NOT NULL COMMENT '所属公司ID',
    `status`         VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE/COMPLETED',
    `start_date`     DATE           NULL COMMENT '开始日期',
    `end_date`       DATE           NULL COMMENT '结束日期',
    `created_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    UNIQUE KEY uk_project_code (project_code),
    INDEX idx_org_id (org_id),
    INDEX idx_company_id (company_id),
    INDEX idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '项目表';

-- WBS字典表
CREATE TABLE IF NOT EXISTS `dict_wbs` (
    `id`             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `wbs_code`       VARCHAR(50)    NOT NULL COMMENT 'WBS编码',
    `wbs_name`       VARCHAR(200)   NOT NULL COMMENT 'WBS名称',
    `parent_code`    VARCHAR(50)    NULL COMMENT '父级WBS编码',
    `level`          TINYINT        NOT NULL DEFAULT 1 COMMENT '层级',
    `sort_order`     INT            NOT NULL DEFAULT 0 COMMENT '排序',
    `status`         TINYINT        NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    `created_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_wbs_code (wbs_code),
    INDEX idx_parent_code (parent_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'WBS字典表';

-- 资源字典表
CREATE TABLE IF NOT EXISTS `dict_resource` (
    `id`             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `resource_code`  VARCHAR(50)    NOT NULL COMMENT '资源编码',
    `resource_name`  VARCHAR(200)   NOT NULL COMMENT '资源名称',
    `spec`           VARCHAR(200)   NULL COMMENT '规格型号',
    `unit`           VARCHAR(20)    NULL COMMENT '计量单位',
    `resource_type`  VARCHAR(30)    NOT NULL COMMENT '类型：MATERIAL/EQUIPMENT/HARDWARE/CIRCULATION/OFFICE/SAFETY/LABOR/SUBCONTRACT',
    `created_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_resource_code (resource_code),
    INDEX idx_resource_type (resource_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '资源字典表';

-- 供应商字典表
CREATE TABLE IF NOT EXISTS `dict_supplier` (
    `id`             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `supplier_code`  VARCHAR(50)    NOT NULL COMMENT '供应商编码',
    `supplier_name`  VARCHAR(200)   NOT NULL COMMENT '供应商名称',
    `contact_person` VARCHAR(100)   NULL COMMENT '联系人',
    `contact_phone`  VARCHAR(20)    NULL COMMENT '联系电话',
    `status`         TINYINT        NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    `created_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_supplier_code (supplier_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '供应商字典表';

-- 采购进度字典表
CREATE TABLE IF NOT EXISTS `dict_purchase_progress` (
    `id`             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `progress_code`  VARCHAR(50)    NOT NULL COMMENT '进度编码',
    `progress_name`  VARCHAR(100)   NOT NULL COMMENT '进度名称',
    `sort_order`     INT            NOT NULL DEFAULT 0 COMMENT '排序',
    `created_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '采购进度字典表';

-- 采购来源字典表
CREATE TABLE IF NOT EXISTS `dict_purchase_source` (
    `id`             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `source_code`    VARCHAR(50)    NOT NULL COMMENT '来源编码',
    `source_name`    VARCHAR(100)   NOT NULL COMMENT '来源名称',
    `sort_order`     INT            NOT NULL DEFAULT 0 COMMENT '排序',
    `created_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '采购来源字典表';

-- 发货进度字典表
CREATE TABLE IF NOT EXISTS `dict_shipping_progress` (
    `id`             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `progress_code`  VARCHAR(50)    NOT NULL COMMENT '进度编码',
    `progress_name`  VARCHAR(100)   NOT NULL COMMENT '进度名称',
    `sort_order`     INT            NOT NULL DEFAULT 0 COMMENT '排序',
    `created_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '发货进度字典表';

-- 工种字典表 (phase6)
CREATE TABLE IF NOT EXISTS `dict_work_type` (
    `id`             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `work_type_code` VARCHAR(50)    NOT NULL COMMENT '工种编码',
    `work_type_name` VARCHAR(100)   NOT NULL COMMENT '工种名称',
    `sort_order`     INT            NOT NULL DEFAULT 0 COMMENT '排序',
    `status`         TINYINT        NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    `create_time`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_work_type_code (work_type_code),
    INDEX idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '工种字典表';

INSERT IGNORE INTO `dict_work_type` (`work_type_code`, `work_type_name`, `sort_order`) VALUES
('WT001', '钢筋工', 1), ('WT002', '木工', 2), ('WT003', '混凝土工', 3),
('WT004', '电工', 4), ('WT005', '焊工', 5), ('WT006', '架子工', 6),
('WT007', '起重工', 7), ('WT008', '普工', 8);

-- 劳务类别字典表 (phase6)
CREATE TABLE IF NOT EXISTS `dict_labor_category` (
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `category_code`   VARCHAR(50)    NOT NULL COMMENT '类别编码',
    `category_name`   VARCHAR(100)   NOT NULL COMMENT '类别名称',
    `sort_order`     INT            NOT NULL DEFAULT 0 COMMENT '排序',
    `status`         TINYINT        NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    `create_time`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_category_code (category_code),
    INDEX idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '劳务类别字典表';

INSERT IGNORE INTO `dict_labor_category` (`category_code`, `category_name`, `sort_order`) VALUES
('LC001', '自有员工', 1), ('LC002', '外包队伍', 2),
('LC003', '临时工', 3), ('LC004', '劳务派遣', 4);

-- 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `username`       VARCHAR(50)    NOT NULL COMMENT '用户名',
    `password`       VARCHAR(200)   NOT NULL COMMENT '密码(BCrypt)',
    `real_name`      VARCHAR(100)   NULL COMMENT '真实姓名',
    `email`          VARCHAR(100)   NULL COMMENT '邮箱',
    `phone`          VARCHAR(20)    NULL COMMENT '手机号',
    `org_id`         BIGINT         NULL COMMENT '所属组织ID',
    `roles`          VARCHAR(200)   NULL COMMENT '角色',
    `status`         TINYINT        NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    `created_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表';

-- 插入默认管理员 (密码: admin123 的BCrypt加密)
INSERT IGNORE INTO `sys_user` (`username`, `password`, `real_name`, `roles`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKtBiMkhcCQ0wOB7jFKnVD2DvzG6', '系统管理员', 'ADMIN');

-- =====================================================
-- 资源计划各子表 (统一使用 create_time/update_time)
-- =====================================================

-- 物资计划表
CREATE TABLE IF NOT EXISTS `resource_plan_material` (
    `id`                      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `plan_name`               VARCHAR(200)  NOT NULL COMMENT '计划名称',
    `project_id`              BIGINT        NOT NULL COMMENT '项目ID',
    `wbs_code`                VARCHAR(50)   NULL COMMENT 'WBS编码',
    `wbs_name`                VARCHAR(200)  NULL COMMENT 'WBS名称',
    `resource_code`           VARCHAR(50)   NULL COMMENT '资源编码',
    `resource_name`           VARCHAR(200)  NULL COMMENT '资源名称',
    `spec`                    VARCHAR(200)  NULL COMMENT '规格型号',
    `unit`                    VARCHAR(20)   NULL COMMENT '计量单位',
    `planned_quantity`        DECIMAL(15,4) NULL COMMENT '计划数量',
    `planned_price`           DECIMAL(15,2) NULL COMMENT '计划单价',
    `planned_amount`          DECIMAL(15,2) NULL COMMENT '计划金额',
    `actual_quantity`         DECIMAL(15,4) NULL COMMENT '实际数量',
    `actual_price`            DECIMAL(15,2) NULL COMMENT '实际单价',
    `actual_amount`           DECIMAL(15,2) NULL COMMENT '实际金额',
    `supplier_code`           VARCHAR(50)   NULL COMMENT '供应商编码',
    `supplier_name`           VARCHAR(200)  NULL COMMENT '供应商名称',
    `purchase_progress_code`  VARCHAR(50)   NULL COMMENT '采购进度编码',
    `purchase_source_code`    VARCHAR(50)   NULL COMMENT '采购来源编码',
    `shipping_progress_code` VARCHAR(50)   NULL COMMENT '发货进度编码',
    `plan_date`               DATE          NULL COMMENT '计划日期',
    `delivery_date`           DATE          NULL COMMENT '交货日期',
    `plan_start_date`         DATE          NULL COMMENT '计划开始日期',
    `plan_end_date`           DATE          NULL COMMENT '计划结束日期',
    `actual_start_date`       DATE          NULL COMMENT '实际开始日期',
    `actual_end_date`         DATE          NULL COMMENT '实际结束日期',
    `approval_status`         VARCHAR(20)   DEFAULT 'DRAFT' COMMENT '审批状态(DRAFT/SUBMITTED/APPROVED/REJECTED)',
    `process_instance_id`     VARCHAR(100)  NULL COMMENT 'Flowable流程实例ID',
    `approval_comment`        TEXT          NULL COMMENT '审批意见',
    `remark`                  TEXT          NULL COMMENT '备注',
    `create_time`             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`                 TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_project_id (project_id),
    INDEX idx_wbs_code (wbs_code),
    INDEX idx_approval_status (approval_status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '物资计划表';

-- 设备计划表
CREATE TABLE IF NOT EXISTS `resource_plan_equipment` (
    `id`                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    `plan_name`               VARCHAR(200)  NOT NULL COMMENT '计划名称',
    `project_id`              BIGINT        NOT NULL COMMENT '项目ID',
    `wbs_code`                VARCHAR(50)   NULL COMMENT 'WBS编码',
    `resource_code`           VARCHAR(50)   NULL COMMENT '资源编码',
    `resource_name`           VARCHAR(200)  NULL COMMENT '资源名称',
    `spec`                    VARCHAR(200)  NULL COMMENT '规格型号',
    `unit`                    VARCHAR(20)   NULL COMMENT '计量单位',
    `planned_quantity`        DECIMAL(15,4) NULL COMMENT '计划数量',
    `planned_price`           DECIMAL(15,2) NULL COMMENT '计划单价',
    `planned_amount`          DECIMAL(15,2) NULL COMMENT '计划金额',
    `actual_quantity`         DECIMAL(15,4) NULL COMMENT '实际数量',
    `actual_price`            DECIMAL(15,2) NULL COMMENT '实际单价',
    `actual_amount`           DECIMAL(15,2) NULL COMMENT '实际金额',
    `supplier_code`           VARCHAR(50)   NULL COMMENT '供应商编码',
    `plan_start_date`         DATE          NULL COMMENT '计划开始日期',
    `plan_end_date`           DATE          NULL COMMENT '计划结束日期',
    `actual_start_date`       DATE          NULL COMMENT '实际开始日期',
    `actual_end_date`         DATE          NULL COMMENT '实际结束日期',
    `remark`                  TEXT          NULL COMMENT '备注',
    `create_time`             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                 TINYINT       NOT NULL DEFAULT 0,
    INDEX idx_project_id (project_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '设备计划表';

-- 周材计划表
CREATE TABLE IF NOT EXISTS `resource_plan_hardware` (
    `id`                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    `plan_name`           VARCHAR(200)  NOT NULL,
    `project_id`          BIGINT        NOT NULL,
    `wbs_code`            VARCHAR(50)   NULL,
    `resource_code`       VARCHAR(50)   NULL,
    `resource_name`       VARCHAR(200)  NULL,
    `spec`                VARCHAR(200)  NULL,
    `unit`                VARCHAR(20)   NULL,
    `planned_quantity`    DECIMAL(15,4) NULL,
    `planned_price`       DECIMAL(15,2) NULL,
    `planned_amount`      DECIMAL(15,2) NULL,
    `actual_quantity`     DECIMAL(15,4) NULL,
    `actual_price`        DECIMAL(15,2) NULL,
    `actual_amount`       DECIMAL(15,2) NULL,
    `plan_start_date`     DATE          NULL,
    `plan_end_date`       DATE          NULL,
    `actual_start_date`   DATE          NULL,
    `actual_end_date`     DATE          NULL,
    `remark`              TEXT          NULL,
    `create_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`             TINYINT       NOT NULL DEFAULT 0,
    INDEX idx_project_id (project_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '周材计划表';

-- 周转材料计划表
CREATE TABLE IF NOT EXISTS `resource_plan_circulation` (
    `id`                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    `plan_name`           VARCHAR(200)  NOT NULL,
    `project_id`          BIGINT        NOT NULL,
    `wbs_code`            VARCHAR(50)   NULL,
    `resource_code`       VARCHAR(50)   NULL,
    `resource_name`       VARCHAR(200)  NULL,
    `spec`                VARCHAR(200)  NULL,
    `unit`                VARCHAR(20)   NULL,
    `planned_quantity`    DECIMAL(15,4) NULL,
    `planned_price`       DECIMAL(15,2) NULL,
    `planned_amount`      DECIMAL(15,2) NULL,
    `actual_quantity`     DECIMAL(15,4) NULL,
    `actual_price`        DECIMAL(15,2) NULL,
    `actual_amount`       DECIMAL(15,2) NULL,
    `plan_start_date`     DATE          NULL,
    `plan_end_date`       DATE          NULL,
    `actual_start_date`   DATE          NULL,
    `actual_end_date`     DATE          NULL,
    `remark`              TEXT          NULL,
    `create_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`             TINYINT       NOT NULL DEFAULT 0,
    INDEX idx_project_id (project_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '周转材料计划表';

-- 办公用品计划表
CREATE TABLE IF NOT EXISTS `resource_plan_office` (
    `id`                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    `plan_name`           VARCHAR(200)  NOT NULL,
    `project_id`          BIGINT        NOT NULL,
    `wbs_code`            VARCHAR(50)   NULL,
    `resource_code`       VARCHAR(50)   NULL,
    `resource_name`       VARCHAR(200)  NULL,
    `spec`                VARCHAR(200)  NULL,
    `unit`                VARCHAR(20)   NULL,
    `planned_quantity`    DECIMAL(15,4) NULL,
    `planned_price`       DECIMAL(15,2) NULL,
    `planned_amount`      DECIMAL(15,2) NULL,
    `actual_quantity`     DECIMAL(15,4) NULL,
    `actual_price`        DECIMAL(15,2) NULL,
    `actual_amount`       DECIMAL(15,2) NULL,
    `plan_start_date`     DATE          NULL,
    `plan_end_date`       DATE          NULL,
    `actual_start_date`   DATE          NULL,
    `actual_end_date`     DATE          NULL,
    `remark`              TEXT          NULL,
    `create_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`             TINYINT       NOT NULL DEFAULT 0,
    INDEX idx_project_id (project_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '办公用品计划表';

-- 安全用品计划表
CREATE TABLE IF NOT EXISTS `resource_plan_safety` (
    `id`                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    `plan_name`           VARCHAR(200)  NOT NULL,
    `project_id`          BIGINT        NOT NULL,
    `wbs_code`            VARCHAR(50)   NULL,
    `resource_code`       VARCHAR(50)   NULL,
    `resource_name`       VARCHAR(200)  NULL,
    `spec`                VARCHAR(200)  NULL,
    `unit`                VARCHAR(20)   NULL,
    `planned_quantity`    DECIMAL(15,4) NULL,
    `planned_price`       DECIMAL(15,2) NULL,
    `planned_amount`      DECIMAL(15,2) NULL,
    `actual_quantity`     DECIMAL(15,4) NULL,
    `actual_price`        DECIMAL(15,2) NULL,
    `actual_amount`       DECIMAL(15,2) NULL,
    `plan_start_date`     DATE          NULL,
    `plan_end_date`       DATE          NULL,
    `actual_start_date`   DATE          NULL,
    `actual_end_date`     DATE          NULL,
    `remark`              TEXT          NULL,
    `create_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`             TINYINT       NOT NULL DEFAULT 0,
    INDEX idx_project_id (project_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '安全用品计划表';

-- 分包计划表 (按截图字段)
CREATE TABLE IF NOT EXISTS `resource_plan_subcontract` (
    `id`                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    `project_id`              BIGINT        NOT NULL,
    `project_name`            VARCHAR(200)  NULL COMMENT '项目名称（冗余存储）',
    `specialty_engineering`   VARCHAR(200)  NULL COMMENT '专业工程',
    `subcontract_name`        VARCHAR(200)  NOT NULL,
    `subcontract_mode`        VARCHAR(100)  NULL COMMENT '分包模式',
    `team_source`             VARCHAR(100)  NULL COMMENT '分包队伍来源',
    `latest_entry_date`       DATE          NULL COMMENT '最晚进场日期（与总进度计划相符）',
    `actual_entry_date`       DATE          NULL COMMENT '实际进场日期（按招标进度推算）',
    `start_prepare_bid_date`  DATE          NULL COMMENT '开始编制招标文件日期',
    `actual_bid_date`         DATE          NULL COMMENT '实际招标日期',
    `planned_online_bid_date` DATE        NULL COMMENT '挂网招标日期',
    `actual_online_bid_date`  DATE         NULL COMMENT '实际挂网日期',
    `planned_award_date`      DATE          NULL COMMENT '定标日期',
    `actual_award_date`       DATE          NULL COMMENT '实际定标日期',
    `mobilization_period`     INT           NULL COMMENT '动员期（天）',
    `wbs_code`                VARCHAR(50)   NULL,
    `wbs_name`                VARCHAR(200)  NULL,
    `work_content`            TEXT          NULL,
    `supplier_code`           VARCHAR(50)   NULL,
    `supplier_name`           VARCHAR(200)  NULL,
    `plan_start_date`         DATE          NULL,
    `plan_end_date`           DATE          NULL,
    `actual_start_date`       DATE          NULL,
    `actual_end_date`         DATE          NULL,
    `status`                  VARCHAR(50)   DEFAULT 'DRAFT' COMMENT '状态(DRAFT/SUBMITTED/IN_PROGRESS/COMPLETED/TERMINATED)',
    `approval_status`         VARCHAR(50)   DEFAULT 'DRAFT' COMMENT '审批状态(DRAFT/SUBMITTED/APPROVED/REJECTED)',
    `process_instance_id`     VARCHAR(100)  NULL COMMENT 'Flowable流程实例ID',
    `remark`                  TEXT          NULL,
    `create_time`             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                 TINYINT       NOT NULL DEFAULT 0,
    INDEX idx_project_id (project_id),
    INDEX idx_status (status),
    INDEX idx_approval_status (approval_status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '分包计划表';

-- 劳动力计划表 (含phase6字段)
CREATE TABLE IF NOT EXISTS `resource_plan_labor` (
    `id`                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    `plan_name`             VARCHAR(200)  NOT NULL,
    `project_id`            BIGINT        NOT NULL,
    `wbs_code`              VARCHAR(50)   NULL,
    `wbs_name`              VARCHAR(200)  NULL,
    `work_type_code`        VARCHAR(50)   NULL,
    `work_type_name`        VARCHAR(100)  NULL,
    `labor_category_code`   VARCHAR(50)   NULL,
    `labor_category_name`   VARCHAR(100)  NULL,
    `planned_count`         INT           NULL COMMENT '计划人数',
    `actual_count`          INT           NULL COMMENT '实际人数',
    `planned_start_date`    DATE          NULL,
    `planned_end_date`      DATE          NULL,
    `actual_start_date`     DATE          NULL,
    `actual_end_date`       DATE          NULL,
    `unit_price`            DECIMAL(15,2) NULL COMMENT '单价',
    `status`                VARCHAR(50)   DEFAULT 'DRAFT' COMMENT '状态',
    `approval_status`       VARCHAR(50)   DEFAULT 'DRAFT' COMMENT '审批状态',
    `process_instance_id`    VARCHAR(100)  NULL COMMENT 'Flowable流程实例ID',
    `attendance_records`    JSON          NULL COMMENT '出勤记录',
    `remark`                TEXT          NULL,
    `create_time`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`               TINYINT       NOT NULL DEFAULT 0,
    INDEX idx_project_id (project_id),
    INDEX idx_status (status),
    INDEX idx_approval_status (approval_status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '劳动力计划表';

-- 预警设置表
CREATE TABLE IF NOT EXISTS `warning_setting` (
    `id`                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name`                  VARCHAR(200)  NOT NULL COMMENT '预警名称',
    `warning_type`          VARCHAR(50)   NOT NULL COMMENT '预警类型',
    `conditions`            JSON          NOT NULL COMMENT '预警条件',
    `enabled`               TINYINT       NOT NULL DEFAULT 1 COMMENT '是否启用',
    `create_time`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`               TINYINT       NOT NULL DEFAULT 0,
    INDEX idx_warning_type (warning_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '预警设置表';

-- 预警记录表
CREATE TABLE IF NOT EXISTS `warning_record` (
    `id`                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    `setting_id`            BIGINT        NOT NULL COMMENT '预警设置ID',
    `warning_type`          VARCHAR(50)   NOT NULL,
    `project_id`            BIGINT        NULL COMMENT '项目ID',
    `resource_plan_id`      BIGINT        NULL COMMENT '资源计划ID',
    `resource_type`         VARCHAR(30)   NULL COMMENT '资源类型',
    `warning_content`       TEXT          NOT NULL COMMENT '预警内容',
    `warning_level`         VARCHAR(20)   NULL COMMENT '预警级别',
    `status`                VARCHAR(20)   DEFAULT 'PENDING' COMMENT '状态(PENDING/PROCESSING/RESOLVED/IGNORED)',
    `handler`               VARCHAR(50)   NULL COMMENT '处理人',
    `handle_time`           DATETIME      NULL COMMENT '处理时间',
    `handle_remark`         TEXT          NULL COMMENT '处理备注',
    `create_time`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`               TINYINT       NOT NULL DEFAULT 0,
    INDEX idx_setting_id (setting_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '预警记录表';

-- 报表配置表 (phase7)
CREATE TABLE IF NOT EXISTS `report_config` (
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`         BIGINT         NOT NULL COMMENT '用户ID',
    `report_name`     VARCHAR(100)   NOT NULL COMMENT '报表名称',
    `report_type`     VARCHAR(50)    NOT NULL COMMENT '报表类型',
    `config_json`     TEXT           NOT NULL COMMENT '配置JSON',
    `is_default`      TINYINT        DEFAULT 0 COMMENT '是否默认配置',
    `create_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT        NOT NULL DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_report_type (report_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '报表配置表';

-- 报表导出日志表 (phase7)
CREATE TABLE IF NOT EXISTS `report_export_log` (
    `id`                BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`           BIGINT       NOT NULL COMMENT '用户ID',
    `report_config_id`  BIGINT       NULL COMMENT '报表配置ID',
    `export_type`       VARCHAR(20)  NOT NULL COMMENT '导出类型(EXCEL/PDF)',
    `file_path`         VARCHAR(500) NULL COMMENT '文件路径',
    `record_count`      INT          DEFAULT 0 COMMENT '导出记录数',
    `export_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`           TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_export_time (export_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '报表导出日志表';

SELECT '>> [1/5] 基础表结构创建完成' AS message;

-- =====================================================
-- 注意：以下为可选的列名统一修复
-- 如果是新部署，上面的表已直接使用 create_time/update_time
-- 如果是旧库升级，取消下面的注释执行
-- =====================================================
-- [2/5] 列名统一修复 (fix-ddl-columns.sql)
-- 新部署可跳过此步骤，表已使用正确列名

-- [3/5] 物资表审批字段 (update-resource-plan-material.sql)
-- 新部署已包含，无需额外处理

-- [4/5] Phase6 字段已在建表时包含，无需额外处理

-- [5/5] Phase7 报表表已创建

SELECT '>> 数据库初始化完成!' AS message;
SELECT '>> Flowable 引擎表将在应用首次启动时自动创建 (database-schema-update: true)' AS message;
SELECT '>> 默认管理员账号: admin / admin123' AS message;
SELECT '>> 生产环境请立即修改默认密码!' AS message;
