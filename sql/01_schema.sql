-- =====================================================
-- 资源计划预警系统 - 数据库初始化脚本
-- MySQL 8.0+
-- =====================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS rpw_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE rpw_db;

-- =====================================================
-- 1. 组织架构表
-- =====================================================
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
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='组织架构表';

-- =====================================================
-- 2. 公司表
-- =====================================================
CREATE TABLE IF NOT EXISTS `company` (
    `id`             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `company_name`   VARCHAR(100)   NOT NULL COMMENT '公司名称',
    `org_id`         BIGINT         NOT NULL COMMENT '所属组织ID',
    `created_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    INDEX idx_org_id (org_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='公司表';

-- =====================================================
-- 3. 项目表
-- =====================================================
CREATE TABLE IF NOT EXISTS `project` (
    `id`               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `project_name`     VARCHAR(200)   NOT NULL COMMENT '项目名称',
    `company_id`       BIGINT         NOT NULL COMMENT '所属公司ID',
    `status`           TINYINT        NOT NULL DEFAULT 1 COMMENT '状态：1进行中 2已完工 3已暂停',
    `plan_start_date`  DATE           NULL COMMENT '计划开始日期',
    `plan_end_date`    DATE           NULL COMMENT '计划结束日期',
    `created_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    INDEX idx_company_id (company_id),
    INDEX idx_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='项目表';

-- =====================================================
-- 4. 预警配置表
-- =====================================================
CREATE TABLE IF NOT EXISTS `warning_setting` (
    `id`               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `project_id`       BIGINT         NOT NULL COMMENT '项目ID',
    `warning_type`     VARCHAR(50)    NOT NULL COMMENT '预警类型：material/equipment/hardware/circulation/office/safety/subcontract/labor',
    `threshold_value`  INT            NOT NULL COMMENT '阈值（天数）',
    `notify_channels`  JSON           NOT NULL COMMENT '通知渠道数组，如["email","wecom"]',
    `notify_timing`    VARCHAR(50)    NOT NULL COMMENT '通知时机：before/after/overdue',
    `enabled`          TINYINT        NOT NULL DEFAULT 1 COMMENT '是否启用：0停用 1启用',
    `created_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    INDEX idx_project_id (project_id),
    INDEX idx_warning_type (warning_type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='预警配置表';

-- =====================================================
-- 5. 预警记录表
-- =====================================================
CREATE TABLE IF NOT EXISTS `warning_record` (
    `id`             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `project_id`     BIGINT         NOT NULL COMMENT '项目ID',
    `warning_type`   VARCHAR(50)    NOT NULL COMMENT '预警类型',
    `warning_level`  TINYINT        NOT NULL COMMENT '预警级别：1一般 2重要 3紧急',
    `message`        TEXT           NOT NULL COMMENT '预警消息',
    `is_sent`        TINYINT        NOT NULL DEFAULT 0 COMMENT '是否已发送：0未发送 1已发送',
    `sent_at`        DATETIME       NULL COMMENT '发送时间',
    `created_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_project_id (project_id),
    INDEX idx_warning_type (warning_type),
    INDEX idx_is_sent (is_sent)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='预警记录表';

-- =====================================================
-- 6. 字典表 - WBS字典
-- =====================================================
CREATE TABLE IF NOT EXISTS `dict_wbs` (
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `code`        VARCHAR(50)  NOT NULL COMMENT '编码',
    `name`        VARCHAR(100) NOT NULL COMMENT '名称',
    `sort_order`  INT         NOT NULL DEFAULT 0 COMMENT '排序号',
    `enabled`     TINYINT     NOT NULL DEFAULT 1 COMMENT '是否启用：0停用 1启用',
    `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='WBS字典表';

-- =====================================================
-- 7. 字典表 - 资源类型字典
-- =====================================================
CREATE TABLE IF NOT EXISTS `dict_resource` (
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `code`        VARCHAR(50)  NOT NULL COMMENT '编码',
    `name`        VARCHAR(100) NOT NULL COMMENT '名称',
    `sort_order`  INT         NOT NULL DEFAULT 0 COMMENT '排序号',
    `enabled`     TINYINT     NOT NULL DEFAULT 1 COMMENT '是否启用：0停用 1启用',
    `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='资源类型字典表';

-- =====================================================
-- 8. 字典表 - 供应商字典
-- =====================================================
CREATE TABLE IF NOT EXISTS `dict_supplier` (
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `code`        VARCHAR(50)  NOT NULL COMMENT '编码',
    `name`        VARCHAR(200) NOT NULL COMMENT '供应商名称',
    `sort_order`  INT         NOT NULL DEFAULT 0 COMMENT '排序号',
    `enabled`     TINYINT     NOT NULL DEFAULT 1 COMMENT '是否启用：0停用 1启用',
    `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='供应商字典表';

-- =====================================================
-- 9. 字典表 - 采购来源字典
-- =====================================================
CREATE TABLE IF NOT EXISTS `dict_purchase_source` (
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `code`        VARCHAR(50)  NOT NULL COMMENT '编码',
    `name`        VARCHAR(100) NOT NULL COMMENT '名称',
    `sort_order`  INT         NOT NULL DEFAULT 0 COMMENT '排序号',
    `enabled`     TINYINT     NOT NULL DEFAULT 1 COMMENT '是否启用：0停用 1启用',
    `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='采购来源字典表';

-- =====================================================
-- 10. 字典表 - 采购进度字典
-- =====================================================
CREATE TABLE IF NOT EXISTS `dict_purchase_progress` (
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `code`        VARCHAR(50)  NOT NULL COMMENT '编码',
    `name`        VARCHAR(100) NOT NULL COMMENT '名称',
    `sort_order`  INT         NOT NULL DEFAULT 0 COMMENT '排序号',
    `enabled`     TINYINT     NOT NULL DEFAULT 1 COMMENT '是否启用：0停用 1启用',
    `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='采购进度字典表';

-- =====================================================
-- 11. 字典表 - 发货进度字典
-- =====================================================
CREATE TABLE IF NOT EXISTS `dict_shipping_progress` (
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `code`        VARCHAR(50)  NOT NULL COMMENT '编码',
    `name`        VARCHAR(100) NOT NULL COMMENT '名称',
    `sort_order`  INT         NOT NULL DEFAULT 0 COMMENT '排序号',
    `enabled`     TINYINT     NOT NULL DEFAULT 1 COMMENT '是否启用：0停用 1启用',
    `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='发货进度字典表';

-- =====================================================
-- 12. 字典表 - 工种字典
-- =====================================================
CREATE TABLE IF NOT EXISTS `dict_work_type` (
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `code`        VARCHAR(50)  NOT NULL COMMENT '编码',
    `name`        VARCHAR(100) NOT NULL COMMENT '名称',
    `sort_order`  INT         NOT NULL DEFAULT 0 COMMENT '排序号',
    `enabled`     TINYINT     NOT NULL DEFAULT 1 COMMENT '是否启用：0停用 1启用',
    `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='工种字典表';

-- =====================================================
-- 13. 字典表 - 劳务类别字典
-- =====================================================
CREATE TABLE IF NOT EXISTS `dict_labor_category` (
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `code`        VARCHAR(50)  NOT NULL COMMENT '编码',
    `name`        VARCHAR(100) NOT NULL COMMENT '名称',
    `sort_order`  INT         NOT NULL DEFAULT 0 COMMENT '排序号',
    `enabled`     TINYINT     NOT NULL DEFAULT 1 COMMENT '是否启用：0停用 1启用',
    `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='劳务类别字典表';

-- =====================================================
-- 14. 资源计划表 - 材料计划
-- =====================================================
CREATE TABLE IF NOT EXISTS `resource_plan_material` (
    `id`                    BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `project_id`            BIGINT         NOT NULL COMMENT '项目ID',
    `wbs_code`            VARCHAR(50)    NOT NULL COMMENT 'WBS编码',
    `resource_name`         VARCHAR(200)   NOT NULL COMMENT '材料名称',
    `specification`         VARCHAR(200)   NULL COMMENT '规格型号',
    `unit`                  VARCHAR(20)    NULL COMMENT '单位',
    `budget_quantity`       DECIMAL(18, 2) NULL COMMENT '预算数量',
    `supplier_code`         VARCHAR(50)    NULL COMMENT '供应商编码',
    `purchase_source_code`  VARCHAR(50)    NULL COMMENT '采购来源编码',
    `purchase_progress_code` VARCHAR(50)   NULL COMMENT '采购进度编码',
    `shipping_progress_code` VARCHAR(50)   NULL COMMENT '发货进度编码',
    `plan_arrival_date`     DATE           NULL COMMENT '计划到场日期',
    `actual_arrival_date`   DATE           NULL COMMENT '实际到场日期',
    `remark`                VARCHAR(500)   NULL COMMENT '备注',
    `created_at`            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`               TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    INDEX idx_project_id (project_id),
    INDEX idx_wbs_code (wbs_code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='材料资源计划表';

-- =====================================================
-- 15. 资源计划表 - 设备计划
-- =====================================================
CREATE TABLE IF NOT EXISTS `resource_plan_equipment` (
    `id`                    BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `project_id`            BIGINT         NOT NULL COMMENT '项目ID',
    `wbs_code`            VARCHAR(50)    NOT NULL COMMENT 'WBS编码',
    `equipment_name`        VARCHAR(200)   NOT NULL COMMENT '设备名称',
    `specification`         VARCHAR(200)   NULL COMMENT '规格型号',
    `unit`                  VARCHAR(20)    NULL COMMENT '单位',
    `budget_quantity`       DECIMAL(18, 2) NULL COMMENT '预算数量',
    `supplier_code`         VARCHAR(50)    NULL COMMENT '供应商编码',
    `purchase_source_code`  VARCHAR(50)    NULL COMMENT '采购来源编码',
    `purchase_progress_code` VARCHAR(50)   NULL COMMENT '采购进度编码',
    `shipping_progress_code` VARCHAR(50)   NULL COMMENT '发货进度编码',
    `plan_arrival_date`     DATE           NULL COMMENT '计划到场日期',
    `actual_arrival_date`   DATE           NULL COMMENT '实际到场日期',
    `remark`                VARCHAR(500)   NULL COMMENT '备注',
    `created_at`            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`               TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    INDEX idx_project_id (project_id),
    INDEX idx_wbs_code (wbs_code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='设备资源计划表';

-- =====================================================
-- 16. 资源计划表 - 五金计划
-- =====================================================
CREATE TABLE IF NOT EXISTS `resource_plan_hardware` (
    `id`                    BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `project_id`            BIGINT         NOT NULL COMMENT '项目ID',
    `wbs_code`            VARCHAR(50)    NOT NULL COMMENT 'WBS编码',
    `hardware_name`         VARCHAR(200)   NOT NULL COMMENT '五金名称',
    `specification`         VARCHAR(200)   NULL COMMENT '规格型号',
    `unit`                  VARCHAR(20)    NULL COMMENT '单位',
    `budget_quantity`       DECIMAL(18, 2) NULL COMMENT '预算数量',
    `supplier_code`         VARCHAR(50)    NULL COMMENT '供应商编码',
    `purchase_source_code`  VARCHAR(50)    NULL COMMENT '采购来源编码',
    `purchase_progress_code` VARCHAR(50)   NULL COMMENT '采购进度编码',
    `shipping_progress_code` VARCHAR(50)   NULL COMMENT '发货进度编码',
    `plan_arrival_date`     DATE           NULL COMMENT '计划到场日期',
    `actual_arrival_date`   DATE           NULL COMMENT '实际到场日期',
    `remark`                VARCHAR(500)   NULL COMMENT '备注',
    `created_at`            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`               TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    INDEX idx_project_id (project_id),
    INDEX idx_wbs_code (wbs_code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='五金资源计划表';

-- =====================================================
-- 17. 资源计划表 - 周转材计划
-- =====================================================
CREATE TABLE IF NOT EXISTS `resource_plan_circulation` (
    `id`                    BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `project_id`            BIGINT         NOT NULL COMMENT '项目ID',
    `wbs_code`            VARCHAR(50)    NOT NULL COMMENT 'WBS编码',
    `circulation_name`      VARCHAR(200)   NOT NULL COMMENT '周转材名称',
    `specification`         VARCHAR(200)   NULL COMMENT '规格型号',
    `unit`                  VARCHAR(20)    NULL COMMENT '单位',
    `budget_quantity`       DECIMAL(18, 2) NULL COMMENT '预算数量',
    `supplier_code`         VARCHAR(50)    NULL COMMENT '供应商编码',
    `purchase_source_code`  VARCHAR(50)    NULL COMMENT '采购来源编码',
    `purchase_progress_code` VARCHAR(50)   NULL COMMENT '采购进度编码',
    `shipping_progress_code` VARCHAR(50)   NULL COMMENT '发货进度编码',
    `plan_arrival_date`     DATE           NULL COMMENT '计划到场日期',
    `actual_arrival_date`   DATE           NULL COMMENT '实际到场日期',
    `remark`                VARCHAR(500)   NULL COMMENT '备注',
    `created_at`            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`               TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    INDEX idx_project_id (project_id),
    INDEX idx_wbs_code (wbs_code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='周转材资源计划表';

-- =====================================================
-- 18. 资源计划表 - 办公用品计划
-- =====================================================
CREATE TABLE IF NOT EXISTS `resource_plan_office` (
    `id`                    BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `project_id`            BIGINT         NOT NULL COMMENT '项目ID',
    `wbs_code`            VARCHAR(50)    NOT NULL COMMENT 'WBS编码',
    `office_name`           VARCHAR(200)   NOT NULL COMMENT '办公用品名称',
    `specification`         VARCHAR(200)   NULL COMMENT '规格型号',
    `unit`                  VARCHAR(20)    NULL COMMENT '单位',
    `budget_quantity`       DECIMAL(18, 2) NULL COMMENT '预算数量',
    `supplier_code`         VARCHAR(50)    NULL COMMENT '供应商编码',
    `purchase_source_code`  VARCHAR(50)    NULL COMMENT '采购来源编码',
    `purchase_progress_code` VARCHAR(50)   NULL COMMENT '采购进度编码',
    `shipping_progress_code` VARCHAR(50)   NULL COMMENT '发货进度编码',
    `plan_arrival_date`     DATE           NULL COMMENT '计划到场日期',
    `actual_arrival_date`   DATE           NULL COMMENT '实际到场日期',
    `remark`                VARCHAR(500)   NULL COMMENT '备注',
    `created_at`            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`               TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    INDEX idx_project_id (project_id),
    INDEX idx_wbs_code (wbs_code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='办公用品资源计划表';

-- =====================================================
-- 19. 资源计划表 - 安全物资计划
-- =====================================================
CREATE TABLE IF NOT EXISTS `resource_plan_safety` (
    `id`                    BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `project_id`            BIGINT         NOT NULL COMMENT '项目ID',
    `wbs_code`            VARCHAR(50)    NOT NULL COMMENT 'WBS编码',
    `safety_name`           VARCHAR(200)   NOT NULL COMMENT '安全物资名称',
    `specification`         VARCHAR(200)   NULL COMMENT '规格型号',
    `unit`                  VARCHAR(20)    NULL COMMENT '单位',
    `budget_quantity`       DECIMAL(18, 2) NULL COMMENT '预算数量',
    `supplier_code`         VARCHAR(50)    NULL COMMENT '供应商编码',
    `purchase_source_code`  VARCHAR(50)    NULL COMMENT '采购来源编码',
    `purchase_progress_code` VARCHAR(50)   NULL COMMENT '采购进度编码',
    `shipping_progress_code` VARCHAR(50)   NULL COMMENT '发货进度编码',
    `plan_arrival_date`     DATE           NULL COMMENT '计划到场日期',
    `actual_arrival_date`   DATE           NULL COMMENT '实际到场日期',
    `remark`                VARCHAR(500)   NULL COMMENT '备注',
    `created_at`            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`               TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    INDEX idx_project_id (project_id),
    INDEX idx_wbs_code (wbs_code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='安全物资资源计划表';

-- =====================================================
-- 20. 资源计划表 - 分包计划
-- =====================================================
CREATE TABLE IF NOT EXISTS `resource_plan_subcontract` (
    `id`                    BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `project_id`            BIGINT         NOT NULL COMMENT '项目ID（关联 organization 表）',
    `project_name`          VARCHAR(200)   NULL COMMENT '项目名称（冗余存储）',
    `specialty_engineering` VARCHAR(200)   NULL COMMENT '专业工程',
    `subcontract_name`      VARCHAR(200)   NOT NULL COMMENT '分包名称',
    `subcontract_mode`      VARCHAR(100)   NULL COMMENT '分包模式',
    `team_source`           VARCHAR(100)   NULL COMMENT '分包队伍来源',
    `latest_entry_date`     DATE           NULL COMMENT '最晚进场日期（与总进度计划相符）',
    `actual_entry_date`     DATE           NULL COMMENT '实际进场日期（按招标进度推算）',
    `start_prepare_bid_date` DATE          NULL COMMENT '开始编制招标文件日期',
    `actual_bid_date`       DATE           NULL COMMENT '实际招标日期',
    `planned_online_bid_date` DATE       NULL COMMENT '挂网招标日期',
    `actual_online_bid_date`  DATE        NULL COMMENT '实际挂网日期',
    `planned_award_date`    DATE           NULL COMMENT '定标日期',
    `actual_award_date`     DATE           NULL COMMENT '实际定标日期',
    `mobilization_period`   INT            NULL COMMENT '动员期（天）',
    `wbs_code`              VARCHAR(50)    NULL COMMENT 'WBS编码（保留兼容）',
    `wbs_name`              VARCHAR(200)   NULL COMMENT 'WBS名称（冗余存储）',
    `work_content`          TEXT           NULL COMMENT '工作内容（保留兼容）',
    `supplier_code`         VARCHAR(50)    NULL COMMENT '供应商编码（保留兼容）',
    `supplier_name`         VARCHAR(200)   NULL COMMENT '供应商名称（冗余存储）',
    `plan_start_date`       DATE           NULL COMMENT '计划开始日期（保留兼容）',
    `plan_end_date`         DATE           NULL COMMENT '计划结束日期（保留兼容）',
    `actual_start_date`     DATE           NULL COMMENT '实际开始日期（保留兼容）',
    `actual_end_date`       DATE           NULL COMMENT '实际结束日期（保留兼容）',
    `status`                VARCHAR(50)  DEFAULT 'DRAFT' COMMENT '状态（DRAFT/SUBMITTED/IN_PROGRESS/COMPLETED/TERMINATED）',
    `approval_status`       VARCHAR(50)  DEFAULT 'DRAFT' COMMENT '审批状态（DRAFT/SUBMITTED/APPROVED/REJECTED）',
    `process_instance_id`   VARCHAR(100)   NULL COMMENT 'Flowable流程实例ID',
    `remark`                VARCHAR(500)   NULL COMMENT '备注',
    `created_at`            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`               TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    INDEX idx_project_id (project_id),
    INDEX idx_status (status),
    INDEX idx_approval_status (approval_status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='分包资源计划表';

-- =====================================================
-- 21. 资源计划表 - 劳动力计划
-- =====================================================
CREATE TABLE IF NOT EXISTS `resource_plan_labor` (
    `id`                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `project_id`          BIGINT       NOT NULL COMMENT '项目ID',
    `wbs_code`            VARCHAR(50)  NOT NULL COMMENT 'WBS编码',
    `work_type_code`      VARCHAR(50)  NOT NULL COMMENT '工种编码',
    `labor_category_code` VARCHAR(50)  NULL COMMENT '劳务类别编码',
    `plan_quantity`       INT          NULL COMMENT '计划人数',
    `plan_start_date`     DATE         NULL COMMENT '计划开始日期',
    `plan_end_date`       DATE         NULL COMMENT '计划结束日期',
    `actual_quantity`     INT          NULL COMMENT '实际人数',
    `actual_start_date`   DATE         NULL COMMENT '实际开始日期',
    `actual_end_date`     DATE         NULL COMMENT '实际结束日期',
    `remark`              VARCHAR(500) NULL COMMENT '备注',
    `created_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`             TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    INDEX idx_project_id (project_id),
    INDEX idx_wbs_code (wbs_code),
    INDEX idx_work_type_code (work_type_code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='劳动力资源计划表';

-- =====================================================
-- 初始化字典数据
-- =====================================================

-- WBS字典示例数据
INSERT INTO dict_wbs (code, name, sort_order, enabled) VALUES
('WBS001', '基础工程', 1, 1),
('WBS002', '主体结构', 2, 1),
('WBS003', '装饰装修', 3, 1),
('WBS004', '机电安装', 4, 1),
('WBS005', '室外工程', 5, 1);

-- 资源类型字典示例数据
INSERT INTO dict_resource (code, name, sort_order, enabled) VALUES
('MAT', '材料', 1, 1),
('EQU', '设备', 2, 1),
('HAR', '五金', 3, 1),
('CIR', '周转材', 4, 1),
('OFF', '办公用品', 5, 1),
('SAF', '安全物资', 6, 1);

-- =====================================================
-- 22. 系统用户表
-- =====================================================
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `username`     VARCHAR(50)    NOT NULL COMMENT '用户名',
    `password`     VARCHAR(255)   NOT NULL COMMENT '密码（加密存储）',
    `real_name`    VARCHAR(100)   NULL COMMENT '真实姓名',
    `email`        VARCHAR(200)   NULL COMMENT '邮箱',
    `phone`        VARCHAR(20)    NULL COMMENT '手机号',
    `org_id`       BIGINT         NULL COMMENT '所属组织ID',
    `status`       TINYINT        NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    `created_at`   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    UNIQUE KEY uk_username (username),
    INDEX idx_org_id (org_id),
    INDEX idx_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='系统用户表';

-- 初始化管理员用户（密码：123456，加密后）
INSERT INTO sys_user (username, password, real_name, status) VALUES
('admin', '$2a$10$LIOH5fsZPmTvtWqBHWhbme6G6bWWjcQ/wtiUfNsm4sx.dt5rkmKi', '管理员', 1);

COMMIT;
