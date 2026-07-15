-- ============================================================
-- RPW 系统权限管理（RBAC）建表 + 种子数据
-- 单系统 / 非 SaaS。菜单 id 沿用原 buildMenus() 以保持前端路由 componentName 命中。
-- ============================================================

-- 1. 角色
CREATE TABLE IF NOT EXISTS sys_role (
  id                  BIGINT       NOT NULL AUTO_INCREMENT,
  name                VARCHAR(50)  NOT NULL COMMENT '角色名称',
  code                VARCHAR(50)  NOT NULL COMMENT '角色标识',
  sort                INT           NOT NULL DEFAULT 0 COMMENT '显示顺序',
  status              TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  type                TINYINT      NOT NULL DEFAULT 1 COMMENT '类型：1系统内置 2自定义',
  data_scope          TINYINT      NOT NULL DEFAULT 1 COMMENT '数据范围：1全部 2仅本人 3所在部门 4部门及子部门 5自定义',
  data_scope_dept_ids VARCHAR(500) DEFAULT NULL COMMENT '自定义部门数据范围（逗号分隔）',
  remark              VARCHAR(255) DEFAULT NULL COMMENT '备注',
  create_time         DATETIME     DEFAULT NULL,
  update_time         DATETIME     DEFAULT NULL,
  deleted             TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色';

-- 2. 菜单
CREATE TABLE IF NOT EXISTS sys_menu (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  name            VARCHAR(50)  NOT NULL COMMENT '菜单名称',
  permission      VARCHAR(100) DEFAULT NULL COMMENT '权限标识（按钮类型必填）',
  type            TINYINT      NOT NULL COMMENT '类型：1目录 2菜单 3按钮',
  sort            INT           NOT NULL DEFAULT 0 COMMENT '显示顺序',
  parent_id       BIGINT       NOT NULL DEFAULT 0 COMMENT '父菜单ID（0顶级）',
  path            VARCHAR(200) DEFAULT NULL COMMENT '路由地址',
  icon            VARCHAR(50)  DEFAULT NULL COMMENT '菜单图标',
  component       VARCHAR(255) DEFAULT NULL COMMENT '组件地址',
  component_name  VARCHAR(100) DEFAULT NULL COMMENT '组件名称（路由 name，单页 push 命中用）',
  status          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  visible         TINYINT      NOT NULL DEFAULT 1 COMMENT '显示状态：1显示 0隐藏',
  keep_alive     TINYINT      NOT NULL DEFAULT 1 COMMENT '缓存状态：1缓存 0不缓存',
  always_show     TINYINT      NOT NULL DEFAULT 1 COMMENT '总是显示：1总是 0不是',
  create_time     DATETIME     DEFAULT NULL,
  update_time     DATETIME     DEFAULT NULL,
  deleted         TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单';

-- 3. 角色-菜单关联
CREATE TABLE IF NOT EXISTS sys_role_menu (
  id       BIGINT NOT NULL AUTO_INCREMENT,
  role_id  BIGINT NOT NULL,
  menu_id  BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_menu (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联';

-- 4. 用户-角色关联
CREATE TABLE IF NOT EXISTS sys_user_role (
  id       BIGINT NOT NULL AUTO_INCREMENT,
  user_id  BIGINT NOT NULL,
  role_id  BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联';

-- 5. 部门（数据权限范围）
CREATE TABLE IF NOT EXISTS sys_dept (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  name           VARCHAR(50)  NOT NULL COMMENT '部门名称',
  parent_id      BIGINT       NOT NULL DEFAULT 0 COMMENT '父部门ID（0顶级）',
  sort           INT           NOT NULL DEFAULT 0 COMMENT '显示顺序',
  status         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  leader_user_id BIGINT       DEFAULT NULL COMMENT '负责人用户ID',
  phone          VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
  email          VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  create_time    DATETIME     DEFAULT NULL,
  update_time    DATETIME     DEFAULT NULL,
  deleted        TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统部门';

-- ===================== 种子数据 =====================

-- 角色：内置管理员
INSERT INTO sys_role (id, name, code, sort, status, type, data_scope, remark, create_time, update_time, deleted)
VALUES (1, '系统管理员', 'admin', 1, 1, 1, 1, '内置超级管理员', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE name=VALUES(name), data_scope=VALUES(data_scope);

-- 菜单（id 固定，type: 1目录 2菜单；keep_alive 统一 0；visible/status/always_show 统一 1）
INSERT INTO sys_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, create_time, update_time, deleted) VALUES
-- 工作台（叶子菜单）
(1,  '工作台',     NULL,         2, 1, 0,  '/dashboard',              'laptop',     'views/dashboard/workspace/index.vue', 'Dashboard',     1,1,0,1, NOW(),NOW(),0),
-- 基础数据
(10, '基础数据',   NULL,         1, 2, 0,  '/rpw/basic',           'folder',     'Layout',                                'BasicData',     1,1,0,1, NOW(),NOW(),0),
(11, '组织管理',   NULL,         2, 1, 10, 'organization',          'apartment',  'views/rpw/organization/index.vue',  NULL,           1,1,0,1, NOW(),NOW(),0),
-- 资源计划
(20, '资源计划',   NULL,         1, 3, 0,  '/rpw/resource-plan',    'schedule',   'Layout',                                'ResourcePlan',   1,1,0,1, NOW(),NOW(),0),
(21, '人力计划',   NULL,         2, 1, 20, 'labor',                'team',       'views/rpw/resource-plan/labor/index.vue',       NULL, 1,1,0,1, NOW(),NOW(),0),
(22, '材料计划',   NULL,         2, 2, 20, 'material',             'appstore',   'views/rpw/resource-plan/material/index.vue',     NULL, 1,1,0,1, NOW(),NOW(),0),
(23, '设备计划',   NULL,         2, 3, 20, 'equipment',            'tool',       'views/rpw/resource-plan/equipment/index.vue',   NULL, 1,1,0,1, NOW(),NOW(),0),
(24, '五金计划',   NULL,         2, 4, 20, 'hardware',             'tool',       'views/rpw/resource-plan/hardware/index.vue',     NULL, 1,1,0,1, NOW(),NOW(),0),
(25, '办公计划',   NULL,         2, 5, 20, 'office',               'laptop',     'views/rpw/resource-plan/office/index.vue',       NULL, 1,1,0,1, NOW(),NOW(),0),
(26, '安全物资',   NULL,         2, 6, 20, 'safety',               'safety',     'views/rpw/resource-plan/safety/index.vue',       NULL, 1,1,0,1, NOW(),NOW(),0),
(27, '周转材',     NULL,         2, 7, 20, 'circulation',          'retweet',    'views/rpw/resource-plan/circulation/index.vue',  NULL, 1,1,0,1, NOW(),NOW(),0),
(28, '分包计划',   NULL,         2, 8, 20, 'subcontract',           'apartment',  'views/rpw/resource-plan/subcontract/index.vue', NULL, 1,1,0,1, NOW(),NOW(),0),
-- 预警管理
(30, '预警管理',   NULL,         1, 4, 0,  '/rpw/warning',          'alert',      'Layout',                                'Warning',       1,1,0,1, NOW(),NOW(),0),
(31, '预警规则',   NULL,         2, 1, 30, 'rule',                 'setting',    'views/rpw/warning/rule/index.vue',          NULL, 1,1,0,1, NOW(),NOW(),0),
(32, '预警记录',   NULL,         2, 2, 30, 'record',               'warning',    'views/rpw/warning/record/index.vue',        NULL, 1,1,0,1, NOW(),NOW(),0),
-- BPM 工作流
(40, 'BPM 工作流', NULL,       1, 5, 0,  '/bpm',                 'flow',       'Layout',                                'BpmWorkflow',   1,1,0,1, NOW(),NOW(),0),
(71, '人力计划变更', NULL,       2, 1, 40, 'labor-plan-change',     'swap',       'views/rpw/labor-plan-change/index.vue',    NULL, 1,1,0,1, NOW(),NOW(),0),
(72, '流程模型',   NULL,         2, 2, 40, 'manager/model',        'appstore',   'views/bpm/model/index.vue',              'BpmModel',     1,1,0,1, NOW(),NOW(),0),
(73, '流程定义',   NULL,         2, 3, 40, 'manager/definition',   'apartment',  'views/bpm/model/definition/index.vue',   NULL, 1,1,0,1, NOW(),NOW(),0),
(74, '流程表单',   NULL,         2, 4, 40, 'manager/form',         'form',       'views/bpm/form/index.vue',               NULL, 1,1,0,1, NOW(),NOW(),0),
(75, '流程分类',   NULL,         2, 5, 40, 'manager/category',     'tags',       'views/bpm/category/index.vue',           NULL, 1,1,0,1, NOW(),NOW(),0),
(76, '用户分组',   NULL,         2, 6, 40, 'manager/group',        'usergroup',  'views/bpm/group/index.vue',              NULL, 1,1,0,1, NOW(),NOW(),0),
(77, '流程表达式', NULL,         2, 7, 40, 'manager/process-expression', 'function', 'views/bpm/processExpression/index.vue',   NULL, 1,1,0,1, NOW(),NOW(),0),
(78, '流程监听器', NULL,         2, 8, 40, 'manager/process-listener', 'sound',   'views/bpm/processListener/index.vue',     NULL, 1,1,0,1, NOW(),NOW(),0),
(79, '我的流程',   NULL,         2, 9, 40, 'process-instance/my',  'send',       'views/bpm/processInstance/index.vue',     NULL, 1,1,0,1, NOW(),NOW(),0),
(80, '流程管理',   NULL,         2, 10,40, 'manager/process-instance', 'control',  'views/bpm/processInstance/manager/index.vue', NULL, 1,1,0,1, NOW(),NOW(),0),
(81, '数据报表',   NULL,         2, 11,40, 'process-instance/report', 'bar-chart', 'views/bpm/processInstance/report/index.vue', NULL, 1,1,0,1, NOW(),NOW(),0),
(82, '待办任务',   NULL,         2, 12,40, 'task/my',              'check-circle','views/bpm/task/todo/index.vue',      NULL, 1,1,0,1, NOW(),NOW(),0),
(83, '已办任务',   NULL,         2, 13,40, 'task/done',            'check',      'views/bpm/task/done/index.vue',         NULL, 1,1,0,1, NOW(),NOW(),0),
(84, '任务管理',   NULL,         2, 14,40, 'manager/task',         'tool',       'views/bpm/task/manager/index.vue',       NULL, 1,1,0,1, NOW(),NOW(),0),
(85, '抄送我的',   NULL,         2, 15,40, 'task/copy',            'share',      'views/bpm/task/copy/index.vue',         NULL, 1,1,0,1, NOW(),NOW(),0),
(86, '请假管理',   NULL,         2, 16,40, 'oa/leave/index',       'calendar',   'views/bpm/oa/leave/index.vue',          NULL, 1,1,0,1, NOW(),NOW(),0),
-- 报表与看板
(50, '报表与看板', NULL,       1, 6, 0,  '/rpw/report',           'bar-chart',  'Layout',                                'ReportKanban',  1,1,0,1, NOW(),NOW(),0),
(51, '资源看板',   NULL,         2, 1, 50, 'kanban',               'dashboard',  'views/rpw/kanban/index.vue',             NULL, 1,1,0,1, NOW(),NOW(),0),
(52, '自定义报表', NULL,         2, 2, 50, 'report',               'file',       'views/rpw/report/index.vue',            NULL, 1,1,0,1, NOW(),NOW(),0),
-- 系统管理
(60, '系统管理',   NULL,         1, 7, 0,  '/rpw/system',          'setting',    'Layout',                                'System',       1,1,0,1, NOW(),NOW(),0),
(61, '字典管理',   NULL,         2, 1, 60, 'dict',                 'book',       'views/rpw/dict/index.vue',              NULL, 1,1,0,1, NOW(),NOW(),0),
(62, '用户管理',   NULL,         2, 2, 60, 'user',                 'user',       'views/rpw/system/user/index.vue',        NULL, 1,1,0,1, NOW(),NOW(),0)
,
(90, '角色管理', 'system:role:list', 2, 3, 60, 'role', 'safe', 'views/system/role/index.vue', 'SystemRole', 1,1,0,1, NOW(),NOW(),0),
(91, '菜单管理', 'system:menu:list', 2, 4, 60, 'menu', 'menu', 'views/system/menu/index.vue', 'SystemMenu', 1,1,0,1, NOW(),NOW(),0),
(92, '部门管理', 'system:dept:list', 2, 5, 60, 'dept', 'apartment', 'views/system/dept/index.vue', 'SystemDept', 1,1,0,1, NOW(),NOW(),0)
ON DUPLICATE KEY UPDATE name=VALUES(name), type=VALUES(type), parent_id=VALUES(parent_id), path=VALUES(path), component=VALUES(component), component_name=VALUES(component_name);

-- 角色-菜单：admin 拥有全部菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE deleted = 0
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id);

-- 用户-角色：admin 用户 → admin 角色（若 admin 用户存在）
INSERT INTO sys_user_role (user_id, role_id)
SELECT id, 1 FROM sys_user WHERE username = 'admin' AND deleted = 0
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 部门：根部门
INSERT INTO sys_dept (id, name, parent_id, sort, status, create_time, update_time, deleted)
VALUES (1, '总公司', 0, 1, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE name=VALUES(name);
