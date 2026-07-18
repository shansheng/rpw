-- 分包计划变更主从结构：建表 + 菜单注册
-- 执行：mysql -uroot -pFundTracker2024! rpw_db < sql/create-subcontract-change.sql

-- 主表：分包计划变更
CREATE TABLE IF NOT EXISTS `resource_plan_subcontract_change` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `plan_id` BIGINT DEFAULT NULL COMMENT '选中的分包计划ID（主表id字段）',
    `project_id` BIGINT DEFAULT NULL COMMENT '项目ID',
    `project_name` VARCHAR(200) DEFAULT NULL COMMENT '项目名称',
    `specialty_engineering` VARCHAR(200) DEFAULT NULL COMMENT '专业工程',
    `subcontract_name` VARCHAR(200) DEFAULT NULL COMMENT '分包名称',
    `subcontract_mode` VARCHAR(100) DEFAULT NULL COMMENT '分包模式',
    `team_source` VARCHAR(100) DEFAULT NULL COMMENT '队伍来源',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `status` VARCHAR(20) DEFAULT 'RUNNING' COMMENT '状态：RUNNING/APPROVED/CANCEL',
    `process_instance_id` VARCHAR(200) DEFAULT '' COMMENT '流程实例ID',
    `creator` VARCHAR(100) DEFAULT NULL COMMENT '发起人',
    `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除：0未删/1已删',
    PRIMARY KEY (`id`),
    KEY `idx_plan_id` (`plan_id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分包计划变更主表';

-- 明细表：分包计划变更明细
CREATE TABLE IF NOT EXISTS `resource_plan_subcontract_change_detail` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `change_id` BIGINT DEFAULT NULL COMMENT '关联变更主表ID',
    `seq` INT DEFAULT NULL COMMENT '序号',
    `date_type` INT DEFAULT NULL COMMENT '日期类型：1最晚进场/2招标文件/3挂网/4定标',
    `original_date` DATE DEFAULT NULL COMMENT '原日期',
    `adjusted_date` DATE DEFAULT NULL COMMENT '调整后日期',
    `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除：0未删/1已删',
    PRIMARY KEY (`id`),
    KEY `idx_change_id` (`change_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分包计划变更明细表';

-- 菜单：在"资源计划"(id=20)下新增"分包计划变更"(id=101)，与"分包计划"(id=28)平级
-- 注意：列表页(id=101)与创建页(id=102)必须是【兄弟】关系(都挂在 id=20 下)，
-- 不能把 id=102 设为 id=101 的子菜单，否则 convertServerMenuToRouteRecordStringComponent
-- 会把父菜单 component 清空，导致列表页被重定向到创建页。
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `permission`, `type`, `sort`, `path`, `component`, `component_name`, `icon`, `visible`, `status`, `keep_alive`, `always_show`)
VALUES (101, 20, '分包计划变更', '', 2, 9, 'subcontract-change', 'views/rpw/resource-plan/subcontract-change/index.vue', 'RpwResourcePlanSubcontractChange', 'file-edit', 1, 1, 0, 1)
ON DUPLICATE KEY UPDATE
    `parent_id` = VALUES(`parent_id`),
    `name` = VALUES(`name`),
    `path` = VALUES(`path`),
    `component_name` = VALUES(`component_name`),
    `component` = VALUES(`component`);

-- 隐藏菜单：分包计划变更-创建页（与列表页平级挂在 id=20 下，仅作为路由，侧栏不显示）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `permission`, `type`, `sort`, `path`, `component`, `component_name`, `icon`, `visible`, `status`, `keep_alive`, `always_show`)
VALUES (102, 20, '分包计划变更-创建', '', 2, 10, 'subcontract-change/create', 'views/rpw/resource-plan/subcontract-change/create.vue', 'RpwResourcePlanSubcontractChangeCreate', '', 0, 1, 0, 0)
ON DUPLICATE KEY UPDATE
    `parent_id` = VALUES(`parent_id`),
    `name` = VALUES(`name`),
    `path` = VALUES(`path`),
    `component_name` = VALUES(`component_name`),
    `component` = VALUES(`component`);
