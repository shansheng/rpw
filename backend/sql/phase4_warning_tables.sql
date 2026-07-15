-- ============================================
-- Phase 4 预警系统数据库表
-- 创建时间：2026-05-10
-- ============================================

-- 预警规则表
CREATE TABLE IF NOT EXISTS `warning_rule` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `resource_type` varchar(50) NOT NULL COMMENT '资源类型（MATERIAL/EQUIPMENT/HARDWARE/CIRCULATION/OFFICE/SAFETY/SUBCONTRACT/LABOR）',
  `project_id` bigint(20) DEFAULT NULL COMMENT '项目ID（NULL表示所有项目）',
  `threshold_type` varchar(20) NOT NULL COMMENT '阈值类型（RATE-比率/DATE-日期/QUANTITY-数量）',
  `warning_threshold` decimal(5,2) NOT NULL COMMENT '预警阈值（如80.00表示80%）',
  `compare_field` varchar(50) NOT NULL COMMENT '对比字段',
  `actual_field` varchar(50) NOT NULL COMMENT '实际值字段',
  `warning_level` varchar(20) NOT NULL COMMENT '预警等级（GENERAL-一般/IMPORTANT-重要/URGENT-紧急）',
  `check_frequency` varchar(20) NOT NULL DEFAULT 'DAILY' COMMENT '检查频率（REALTIME-实时/DAILY-每日/WEEKLY-每周）',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用（0-禁用，1-启用）',
  `notify_wecom` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否推送企业微信（0-否，1-是）',
  `notify_users` varchar(500) DEFAULT NULL COMMENT '通知用户ID列表（逗号分隔）',
  `description` varchar(500) DEFAULT NULL COMMENT '规则描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记（0-未删除，1-已删除）',
  PRIMARY KEY (`id`),
  INDEX `idx_resource_type` (`resource_type`),
  INDEX `idx_project_id` (`project_id`),
  INDEX `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警规则表';

-- 预警记录表
CREATE TABLE IF NOT EXISTS `warning_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_id` bigint(20) NOT NULL COMMENT '预警规则ID',
  `resource_type` varchar(50) NOT NULL COMMENT '资源类型',
  `project_id` bigint(20) NOT NULL COMMENT '项目ID',
  `resource_id` bigint(20) NOT NULL COMMENT '资源计划ID（关联各资源表的主键）',
  `warning_level` varchar(20) NOT NULL COMMENT '预警等级（GENERAL-一般/IMPORTANT-重要/URGENT-紧急）',
  `warning_message` varchar(1000) NOT NULL COMMENT '预警消息内容',
  `plan_value` decimal(15,2) DEFAULT NULL COMMENT '计划值',
  `actual_value` decimal(15,2) DEFAULT NULL COMMENT '实际值',
  `deviation_rate` decimal(5,2) DEFAULT NULL COMMENT '偏差率（百分比）',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态（PENDING-待处理/PROCESSING-处理中/RESOLVED-已解决/IGNORED-已忽略）',
  `handled_by` bigint(20) DEFAULT NULL COMMENT '处理人ID',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `handle_remark` varchar(500) DEFAULT NULL COMMENT '处理备注',
  `notify_status` varchar(20) NOT NULL DEFAULT 'NOT_SENT' COMMENT '通知状态（NOT_SENT-未发送/SENT-已发送/FAILED-发送失败）',
  `wecom_msg_id` varchar(100) DEFAULT NULL COMMENT '企业微信消息ID',
  `triggered_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '触发时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记（0-未删除，1-已删除）',
  PRIMARY KEY (`id`),
  INDEX `idx_rule_id` (`rule_id`),
  INDEX `idx_resource_type` (`resource_type`),
  INDEX `idx_project_id` (`project_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_triggered_time` (`triggered_time`),
  INDEX `idx_notify_status` (`notify_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警记录表';

-- ============================================
-- 初始化数据
-- ============================================

-- 插入默认预警规则（材料到场率预警）
INSERT INTO `warning_rule` (`rule_name`, `resource_type`, `threshold_type`, `warning_threshold`, `compare_field`, `actual_field`, `warning_level`, `check_frequency`, `enabled`, `notify_wecom`, `description`)
VALUES
  ('材料到场率预警', 'MATERIAL', 'RATE', 80.00, 'budget_quantity', 'actual_arrival_date', 'IMPORTANT', 'DAILY', 1, 1, '当材料实际到场率低于80%时触发预警'),
  ('设备到场率预警', 'EQUIPMENT', 'RATE', 90.00, 'budget_quantity', 'actual_arrival_date', 'IMPORTANT', 'DAILY', 1, 1, '当设备实际到场率低于90%时触发预警'),
  ('劳动力到位率预警', 'LABOR', 'RATE', 85.00, 'plan_quantity', 'actual_quantity', 'IMPORTANT', 'DAILY', 1, 1, '当劳动力实际到位率低于85%时触发预警');
