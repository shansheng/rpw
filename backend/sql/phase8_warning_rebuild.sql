-- ============================================
-- Phase 8 预警系统重构（表达式驱动）
-- 重建 warning_rule / warning_record 表结构，
-- 并写入分包招标文件预警示例规则（黄/橙/红三档）。
-- 应用：mysql -u root -p rpw_db < phase8_warning_rebuild.sql
-- ============================================

DROP TABLE IF EXISTS `warning_record`;
DROP TABLE IF EXISTS `warning_rule`;

-- 预警规则表（表达式驱动）
CREATE TABLE IF NOT EXISTS `warning_rule` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '规则名称',
  `object_type` varchar(50) NOT NULL COMMENT '预警对象（SUBCONTRACT/MATERIAL/EQUIPMENT/LABOR/HARDWARE/CIRCULATION/OFFICE/SAFETY）',
  `condition_expr` text COMMENT '条件表达式（如 isnull(实际招标日期) and 3 < 开始编制招标文件日期 - 当前日期 and 开始编制招标文件日期 - 当前日期 < 7）',
  `warning_level` varchar(20) NOT NULL COMMENT '预警类型（RED-红色/ORANGE-橙色/YELLOW-黄色）',
  `priority` int(11) DEFAULT 100 COMMENT '优先级（数字越小越优先）',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用（0-禁用，1-启用）',
  `remark` varchar(500) DEFAULT NULL COMMENT '规则说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记（0-未删除，1-已删除）',
  PRIMARY KEY (`id`),
  INDEX `idx_object_type` (`object_type`),
  INDEX `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警规则表';

-- 预警记录表
CREATE TABLE IF NOT EXISTS `warning_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_id` bigint(20) NOT NULL COMMENT '触发规则ID',
  `object_type` varchar(50) NOT NULL COMMENT '预警对象类型',
  `plan_id` bigint(20) NOT NULL COMMENT '计划ID',
  `plan_name` varchar(200) DEFAULT NULL COMMENT '计划名称',
  `warning_level` varchar(20) NOT NULL COMMENT '预警类型（RED/ORANGE/YELLOW）',
  `reason` text COMMENT '预警原因（规则定义 + 代入实际数据后的计算值）',
  `condition_expr` text COMMENT '触发时的规则表达式快照',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态（PENDING-待处理/RESOLVED-已解决/IGNORED-已忽略）',
  `triggered_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '触发时间',
  `handle_remark` varchar(500) DEFAULT NULL COMMENT '处理备注',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记（0-未删除，1-已删除）',
  PRIMARY KEY (`id`),
  INDEX `idx_rule_id` (`rule_id`),
  INDEX `idx_object_type` (`object_type`),
  INDEX `idx_plan_id` (`plan_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_triggered_time` (`triggered_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警记录表';

-- ============================================
-- 示例规则：分包计划「招标文件」预警（黄/橙/红三档）
-- 字段中文名来自后端 WarningAttributeRegistry：
--   实际招标日期 = actualBidDate
--   开始编制招标文件日期 = startPrepareBidDate
--   当前日期 = 系统属性（today）
-- ============================================
INSERT INTO `warning_rule` (`name`, `object_type`, `condition_expr`, `warning_level`, `priority`, `enabled`, `remark`)
VALUES
  ('分包-招标文件黄色预警', 'SUBCONTRACT',
   'isnull(实际招标日期) and 3 < 开始编制招标文件日期 - 当前日期 and 开始编制招标文件日期 - 当前日期 < 7',
   'YELLOW', 30, 1, '实际未招标，且距开始编制招标文件还有 3~7 天'),
  ('分包-招标文件橙色预警', 'SUBCONTRACT',
   'isnull(实际招标日期) and 0 < 开始编制招标文件日期 - 当前日期 and 开始编制招标文件日期 - 当前日期 < 3',
   'ORANGE', 20, 1, '实际未招标，且距开始编制招标文件还有 0~3 天'),
  ('分包-招标文件红色预警', 'SUBCONTRACT',
   'isnull(实际招标日期) and 开始编制招标文件日期 - 当前日期 < 0',
   'RED', 10, 1, '实际未招标，且开始编制招标文件日期已过期');
