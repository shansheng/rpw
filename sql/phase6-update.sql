-- =====================================================
-- Phase 6 数据库更新脚本
-- 分包队伍计划管理 & 劳动力需求管理
-- 日期: 2026-05-10
-- =====================================================

USE rpw_db;

-- =====================================================
-- 1. 创建工种字典表（dict_work_type）
-- =====================================================
CREATE TABLE IF NOT EXISTS `dict_work_type` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `work_type_code` varchar(50) NOT NULL COMMENT '工种编码',
    `work_type_name` varchar(100) NOT NULL COMMENT '工种名称',
    `sort_order` int(11) NOT NULL DEFAULT 0 COMMENT '排序',
    `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态（1-启用 0-禁用）',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_work_type_code` (`work_type_code`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工种字典表';

-- 插入初始数据
INSERT INTO `dict_work_type` (`work_type_code`, `work_type_name`, `sort_order`) VALUES
('WT001', '钢筋工', 1),
('WT002', '木工', 2),
('WT003', '混凝土工', 3),
('WT004', '电工', 4),
('WT005', '焊工', 5),
('WT006', '架子工', 6),
('WT007', '起重工', 7),
('WT008', '普工', 8);

-- =====================================================
-- 2. 创建劳务类别字典表（dict_labor_category）
-- =====================================================
CREATE TABLE IF NOT EXISTS `dict_labor_category` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `category_code` varchar(50) NOT NULL COMMENT '类别编码',
    `category_name` varchar(100) NOT NULL COMMENT '类别名称',
    `sort_order` int(11) NOT NULL DEFAULT 0 COMMENT '排序',
    `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态（1-启用 0-禁用）',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_category_code` (`category_code`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='劳务类别字典表';

-- 插入初始数据
INSERT INTO `dict_labor_category` (`category_code`, `category_name`, `sort_order`) VALUES
('LC001', '自有员工', 1),
('LC002', '外包队伍', 2),
('LC003', '临时工', 3),
('LC004', '劳务派遣', 4);

-- =====================================================
-- 3. 修改分包计划表（resource_plan_subcontract）
-- =====================================================
-- 新增字段
ALTER TABLE `resource_plan_subcontract` 
  ADD COLUMN IF NOT EXISTS `status` varchar(50) DEFAULT 'DRAFT' COMMENT '状态（DRAFT/SUBMITTED/IN_PROGRESS/COMPLETED/TERMINATED）' AFTER `actual_end_date`,
  ADD COLUMN IF NOT EXISTS `wbs_name` varchar(200) COMMENT 'WBS名称（冗余存储）' AFTER `wbs_code`,
  ADD COLUMN IF NOT EXISTS `supplier_name` varchar(200) COMMENT '供应商名称（冗余存储）' AFTER `supplier_code`,
  ADD COLUMN IF NOT EXISTS `approval_status` varchar(50) DEFAULT 'DRAFT' COMMENT '审批状态（DRAFT/SUBMITTED/APPROVED/REJECTED）' AFTER `status`,
  ADD COLUMN IF NOT EXISTS `process_instance_id` varchar(100) COMMENT 'Flowable流程实例ID' AFTER `approval_status`;

-- 新增索引
ALTER TABLE `resource_plan_subcontract`
  ADD INDEX IF NOT EXISTS `idx_status` (`status`),
  ADD INDEX IF NOT EXISTS `idx_approval_status` (`approval_status`),
  ADD INDEX IF NOT EXISTS `idx_process_instance_id` (`process_instance_id`);

-- =====================================================
-- 4. 修改劳动力计划表（resource_plan_labor）
-- =====================================================
-- 新增字段
ALTER TABLE `resource_plan_labor` 
  ADD COLUMN IF NOT EXISTS `status` varchar(50) DEFAULT 'DRAFT' COMMENT '状态（DRAFT/SUBMITTED/IN_PROGRESS/COMPLETED/TERMINATED）' AFTER `actual_end_date`,
  ADD COLUMN IF NOT EXISTS `wbs_name` varchar(200) COMMENT 'WBS名称（冗余存储）' AFTER `wbs_code`,
  ADD COLUMN IF NOT EXISTS `work_type_name` varchar(100) COMMENT '工种名称（冗余存储）' AFTER `work_type_code`,
  ADD COLUMN IF NOT EXISTS `labor_category_name` varchar(100) COMMENT '劳务类别名称（冗余存储）' AFTER `labor_category_code`,
  ADD COLUMN IF NOT EXISTS `approval_status` varchar(50) DEFAULT 'DRAFT' COMMENT '审批状态（DRAFT/SUBMITTED/APPROVED/REJECTED）' AFTER `status`,
  ADD COLUMN IF NOT EXISTS `process_instance_id` varchar(100) COMMENT 'Flowable流程实例ID' AFTER `approval_status`,
  ADD COLUMN IF NOT EXISTS `attendance_records` json COMMENT '出勤记录（JSON格式）' AFTER `remark`;

-- 新增索引
ALTER TABLE `resource_plan_labor`
  ADD INDEX IF NOT EXISTS `idx_status` (`status`),
  ADD INDEX IF NOT EXISTS `idx_approval_status` (`approval_status`),
  ADD INDEX IF NOT EXISTS `idx_process_instance_id` (`process_instance_id`);

COMMIT;
