-- ============================================================
-- 分包队伍统计功能初始化脚本
-- 包含：建表、菜单、测试数据、角色菜单授权
-- 用法：在 rpw_db 执行一次
-- ============================================================

-- ------------------------------------------------------------
-- 1. 建表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `subcontractor_team` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` bigint DEFAULT NULL COMMENT '项目ID',
  `professional_engineering` varchar(100) NOT NULL COMMENT '专业工程',
  `subcontract_name` varchar(200) NOT NULL COMMENT '分包名称',
  `subcontract_mode` varchar(100) DEFAULT NULL COMMENT '分包模式',
  `team_source` varchar(100) DEFAULT NULL COMMENT '分包队伍来源',
  `latest_entry_date` date DEFAULT NULL COMMENT '最晚进场日期（与总进度计划相符）',
  `actual_entry_date` date DEFAULT NULL COMMENT '实际进场日期（按招标进度推算）',
  `tender_doc_start_date` date DEFAULT NULL COMMENT '开始编制招标文件日期',
  `online_tender_date` date DEFAULT NULL COMMENT '挂网招标日期',
  `bid_award_date` date DEFAULT NULL COMMENT '定标日期',
  `mobilization_period_days` int DEFAULT NULL COMMENT '动员期（天）',
  `remarks` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_professional_engineering` (`professional_engineering`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分包队伍统计';

-- ------------------------------------------------------------
-- 2. 菜单（挂在 基础数据 parent_id=10 下面）
-- ------------------------------------------------------------
INSERT INTO `sys_menu` (
  `parent_id`, `name`, `permission`, `type`, `sort`, `path`, `icon`, `component`, `component_name`,
  `status`, `visible`, `keep_alive`, `always_show`, `create_time`, `update_time`
) VALUES (
  10, '分包队伍统计', NULL, 2, 10, 'subcontractor-team', 'ChartColumn', 'views/rpw/subcontractor-team/index.vue', 'SubcontractorTeam',
  1, 1, 0, 1, NOW(), NOW()
);

-- 给系统管理员角色分配该菜单权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, id FROM `sys_menu` WHERE component_name = 'SubcontractorTeam' AND deleted = 0;

-- ------------------------------------------------------------
-- 3. 测试数据
-- ------------------------------------------------------------
INSERT INTO `subcontractor_team` (
  `project_id`, `professional_engineering`, `subcontract_name`, `subcontract_mode`, `team_source`,
  `latest_entry_date`, `actual_entry_date`, `tender_doc_start_date`, `online_tender_date`, `bid_award_date`,
  `mobilization_period_days`, `remarks`
) VALUES
(
  1, '基坑工程', '基坑支护队伍', '劳务分包模式一', '国内分包',
  '2026-03-20', '2026-02-20', '2025-12-02', '2026-01-01', '2026-01-21',
  30, '示例数据，按总进度计划安排'
),
(
  1, '主体结构', '混凝土施工队', '劳务分包模式一', '自有队伍',
  '2026-05-15', '2026-04-10', '2026-02-01', '2026-03-01', '2026-03-15',
  45, '主体结构阶段关键分包'
),
(
  1, '钢结构安装', '钢结构安装队', '专业分包', '外部引进',
  '2026-08-20', '2026-07-15', '2026-05-10', '2026-06-01', '2026-06-20',
  60, '大型钢结构吊装作业'
);
