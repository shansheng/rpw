# 项目资源计划预警系统 — 架构设计方案

> **设计人**：高见远（架构师）
> **版本**：v1.0
> **日期**：2026-05-09

---

## 目录

1. [系统架构总览](#1-系统架构总览)
2. [技术选型](#2-技术选型)
3. [数据库设计](#3-数据库设计)
4. [核心模块设计](#4-核心模块设计)
5. [API 设计概要](#5-api-设计概要)
6. [项目文件结构](#6-项目文件结构)
7. [实现任务列表](#7-实现任务列表)

---

## 1. 系统架构总览

### 1.1 系统分层架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端展示层 (Vue 3)                        │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐   │
│  │ 局级视图  │ │ 公司视图  │ │ 项目视图  │ │  统计分析仪表盘   │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────────┘   │
├─────────────────────────────────────────────────────────────────┤
│                      API 网关 / Nginx 反向代理                    │
├─────────────────────────────────────────────────────────────────┤
│                     后端服务层 (Spring Boot)                      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐   │
│  │ 组织架构  │ │ 资源计划  │ │ 审批流程  │ │  预警 + 统计分析   │   │
│  │  模块    │ │  模块    │ │  模块    │ │      模块         │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────────┘   │
├─────────────────────────────────────────────────────────────────┤
│                        基础设施层                                │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐   │
│  │  MySQL   │ │ Flowable │ │  Redis   │ │  MinIO/OSS       │   │
│  │  数据库   │ │ 流程引擎  │ │  缓存    │ │  文件存储         │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 部署架构

```
                    ┌──────────────┐
                    │   Nginx/LB   │
                    └──────┬───────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
        ┌─────▼─────┐ ┌───▼────┐ ┌────▼─────┐
        │  前端静态  │ │ 后端   │ │ Flowable │
        │   资源    │ │ 服务   │ │  引擎    │
        └───────────┘ └───┬────┘ └────┬─────┘
                          │           │
                    ┌─────▼───────────▼─────┐
                    │       MySQL 主从        │
                    └───────────────────────┘
                    ┌───────────────────────┐
                    │       Redis Cluster    │
                    └───────────────────────┘
```

---

## 2. 技术选型

### 2.1 前端技术栈

| 类别 | 技术选型 | 说明 |
|------|---------|------|
| 框架 | Vue 3 + TypeScript | 组合式 API，类型安全 |
| 构建工具 | Vite 5 | 快速冷启动和热更新 |
| 状态管理 | Pinia | Vue 3 官方推荐 |
| 路由 | Vue Router 4 | 动态路由 + 权限守卫 |
| UI 组件库 | Element Plus | 企业级后台首选 |
| 图表 | ECharts 5 | 统计分析和仪表盘 |
| HTTP 客户端 | Axios | 拦截器统一处理鉴权和错误 |
| 表格 | vxe-table | 复杂可编辑表格（资源计划填报核心） |
| 工具库 | dayjs、lodash-es | 日期处理、工具函数 |

### 2.2 后端技术栈

| 类别 | 技术选型 | 说明 |
|------|---------|------|
| 框架 | Spring Boot 3.2 | JDK 17+，Jakarta EE |
| ORM | MyBatis-Plus 3.5 | 单表 CRUD 自动化 |
| 流程引擎 | Flowable 7.0 | 审批工作流 |
| 数据库 | MySQL 8.0 | InnoDB，JSON 字段支持 |
| 缓存 | Redis (Spring Data Redis) | 字典缓存、会话管理 |
| 权限框架 | Spring Security + JWT | 三级组织权限控制 |
| 定时任务 | Spring Scheduling / XXL-Job | 预警扫描 |
| 文档 | Knife4j (OpenAPI 3) | 接口文档自动生成 |
| 工具库 | Hutool、MapStruct | 通用工具、对象映射 |

### 2.3 开发规范

| 维度 | 规范 |
|------|------|
| Java 代码规范 | 阿里巴巴 Java 开发手册 |
| API 风格 | RESTful，统一 `/api/v1/` 前缀 |
| 数据库命名 | 下划线命名法，表名加业务前缀 |
| 前端命名 | 文件 kebab-case，组件 PascalCase |
| Git 分支 | main / develop / feature / hotfix |

---

## 3. 数据库设计

### 3.1 E-R 关系概览

```
┌─────────────┐       ┌─────────────┐       ┌──────────────────────┐
│organization │──1:N──│   company   │──1:N──│      project         │
│  (组织架构)  │       │   (公司)     │       │      (项目)          │
└──────┬──────┘       └──────┬──────┘       └──────────┬───────────┘
       │                     │                         │
       │              ┌──────▼──────┐          ┌───────▼────────┐
       │              │  warning    │          │  resource_plan │
       │              │  _setting   │          │  (8张资源计划表)  │
       │              │  (预警设置)   │          └───────┬────────┘
       │              └──────┬──────┘                  │
       │                     │                   ┌──────▼──────┐
       │              ┌──────▼──────┐            │ dict_wbs    │
       │              │  warning    │            │ dict_resource│
       │              │  _record    │            │ dict_supplier│
       │              │  (预警记录)   │            │ ... (字典表)  │
       │              └─────────────┘            └─────────────┘
       │
       │              ┌─────────────┐
       └──────────────│  Flowable   │
                      │  流程引擎表   │
                      └─────────────┘
```

**核心关系说明**：
- `organization` 1:N `company`：组织下有多个公司
- `company` 1:N `project`：公司下有多个项目
- `project` 1:N `resource_plan_*`：每个项目有 8 种资源计划
- `project` 1:N `warning_record`：项目产生预警记录
- `company` 1:1 `warning_setting`：每个公司可配置预警规则

### 3.2 基础表结构

#### 3.2.1 组织架构表 `organization`

```sql
CREATE TABLE `organization` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `org_code`    VARCHAR(32)  NOT NULL                COMMENT '组织编码（唯一）',
  `org_name`    VARCHAR(128) NOT NULL                COMMENT '组织名称',
  `org_type`    TINYINT     NOT NULL                COMMENT '组织类型：1-局 2-公司 3-项目',
  `parent_id`   BIGINT       DEFAULT NULL            COMMENT '上级组织ID',
  `level`       TINYINT      NOT NULL DEFAULT 1      COMMENT '层级：1-局 2-公司 3-项目',
  `department`  VARCHAR(128) DEFAULT NULL           COMMENT '部门名称（局/公司/项目均可能有）',
  `section`     VARCHAR(128) DEFAULT NULL           COMMENT '处室名称（局/公司/项目均可能有）',
  `sort_order`  INT          NOT NULL DEFAULT 0      COMMENT '排序号',
  `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：0-禁用 1-启用',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删 1-已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_org_code` (`org_code`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_org_type` (`org_type`),
  KEY `idx_department` (`department`),
  KEY `idx_section` (`section`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织架构表（含部门/处室）';
```

**字段说明**：
- `department`：部门名称（如"工程管理部"、"财务部"）
- `section`：处室名称（如"计划处"、"合同处"）
- 局、公司、项目三级都可能有部门和处室
- 查询时支持按 `department` 和 `section` 过滤

#### 3.2.2 公司表 `company`

```sql
CREATE TABLE `company` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `org_id`        BIGINT       NOT NULL                COMMENT '关联组织ID',
  `company_code`  VARCHAR(32)  NOT NULL                COMMENT '公司编码',
  `company_name`  VARCHAR(128) NOT NULL                COMMENT '公司名称',
  `contact_person` VARCHAR(64)  DEFAULT NULL           COMMENT '联系人',
  `contact_phone` VARCHAR(20)  DEFAULT NULL           COMMENT '联系电话',
  `address`       VARCHAR(256) DEFAULT NULL           COMMENT '地址',
  `status`        TINYINT      NOT NULL DEFAULT 1      COMMENT '状态',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_code` (`company_code`),
  KEY `idx_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司表';
```

#### 3.2.3 项目表 `project`

```sql
CREATE TABLE `project` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `org_id`        BIGINT       NOT NULL                COMMENT '关联组织ID',
  `company_id`    BIGINT       NOT NULL                COMMENT '所属公司ID',
  `project_code`  VARCHAR(32)  NOT NULL                COMMENT '项目编码',
  `project_name`  VARCHAR(256) NOT NULL                COMMENT '项目名称',
  `project_type`  VARCHAR(64)  DEFAULT NULL           COMMENT '项目类型',
  `start_date`    DATE         DEFAULT NULL           COMMENT '计划开工日期',
  `end_date`      DATE         DEFAULT NULL           COMMENT '计划完工日期',
  `manager_name`  VARCHAR(64)  DEFAULT NULL           COMMENT '项目经理',
  `manager_phone` VARCHAR(20)  DEFAULT NULL           COMMENT '经理电话',
  `address`       VARCHAR(512) DEFAULT NULL           COMMENT '项目地址',
  `status`        TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：0-筹备 1-在建 2-竣工 3-停工',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_code` (`project_code`),
  KEY `idx_company_id` (`company_id`),
  KEY `idx_org_id` (`org_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';
```

#### 3.2.4 预警设置表 `warning_setting`

```sql
CREATE TABLE `warning_setting` (
  `id`                BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `company_id`        BIGINT   NOT NULL                COMMENT '公司ID',
  `advance_days`      INT      NOT NULL DEFAULT 10     COMMENT '提前预警天数',
  `warning_types`     JSON     NOT NULL                COMMENT '预警资源类型数组（1-8对应8种）',
  `notify_enabled`    TINYINT  NOT NULL DEFAULT 1      COMMENT '是否启用通知：0-否 1-是',
  `notify_channels`   JSON     DEFAULT NULL            COMMENT '通知渠道数组：["wecom","system"]',
  `notify_timing`     VARCHAR(32) DEFAULT 'generation' COMMENT '推送时机：generation-生成时 immediate-立即',
  `create_time`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警设置表（含通知配置）';
```

**字段说明**：
- `notify_channels`：JSON数组，可选值 `"wecom"`（企业微信）、`"system"`（系统消息）
- `notify_timing`：推送时机，`generation` 表示生成预警后立即推送
- `warning_types`：启用预警的资源类型数组，如 `[1,2,3,4,5,6,7,8]`

#### 3.2.5 预警记录表 `warning_record`

```sql
CREATE TABLE `warning_record` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `company_id`        BIGINT       NOT NULL                COMMENT '公司ID',
  `project_id`        BIGINT       NOT NULL                COMMENT '项目ID',
  `resource_type`     TINYINT      NOT NULL                COMMENT '资源类型：1-8',
  `plan_id`           BIGINT       NOT NULL                COMMENT '资源计划ID',
  `plan_table`        VARCHAR(64)  NOT NULL                COMMENT '资源计划表名',
  `warning_level`     TINYINT      NOT NULL                COMMENT '预警级别：1-黄色 2-红色',
  `require_date`      DATE         NOT NULL                COMMENT '需求日期',
  `warning_date`      DATETIME     NOT NULL                COMMENT '预警触发时间',
  `description`       VARCHAR(512) DEFAULT NULL           COMMENT '预警描述',
  `is_handled`        TINYINT      NOT NULL DEFAULT 0      COMMENT '是否已处理',
  `handle_time`       DATETIME     DEFAULT NULL           COMMENT '处理时间',
  `handle_remark`     VARCHAR(512) DEFAULT NULL           COMMENT '处理备注',
  `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_company_project` (`company_id`, `project_id`),
  KEY `idx_resource_type` (`resource_type`),
  KEY `idx_warning_level` (`warning_level`),
  KEY `idx_require_date` (`require_date`),
  KEY `idx_is_handled` (`is_handled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警记录表';
```

### 3.3 字典表结构

#### 3.3.1 WBS 字典 `dict_wbs`

```sql
CREATE TABLE `dict_wbs` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `wbs_code`    VARCHAR(64)  NOT NULL COMMENT 'WBS编码',
  `wbs_name`    VARCHAR(256) NOT NULL COMMENT 'WBS名称',
  `parent_id`   BIGINT       DEFAULT NULL COMMENT '父级WBS ID',
  `level`       TINYINT      NOT NULL DEFAULT 1 COMMENT '层级',
  `full_path`   VARCHAR(512) DEFAULT NULL COMMENT '完整路径（编码链）',
  `sort_order`  INT          NOT NULL DEFAULT 0,
  `status`      TINYINT      NOT NULL DEFAULT 1,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wbs_code` (`wbs_code`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WBS字典';
```

#### 3.3.2 资源字典 `dict_resource`

```sql
CREATE TABLE `dict_resource` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `resource_code` VARCHAR(64)  NOT NULL COMMENT '资源编码',
  `resource_name` VARCHAR(256) NOT NULL COMMENT '资源名称',
  `category`      VARCHAR(64)  NOT NULL COMMENT '资源大类',
  `spec`          VARCHAR(256) DEFAULT NULL COMMENT '规格型号',
  `unit`          VARCHAR(32)  DEFAULT NULL COMMENT '计量单位',
  `remark`        VARCHAR(512) DEFAULT NULL,
  `status`        TINYINT      NOT NULL DEFAULT 1,
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_resource_code` (`resource_code`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源字典';
```

#### 3.3.3 供应商字典 `dict_supplier`

```sql
CREATE TABLE `dict_supplier` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `supplier_code` VARCHAR(32)  NOT NULL COMMENT '供应商编码',
  `supplier_name` VARCHAR(256) NOT NULL COMMENT '供应商名称',
  `contact_person` VARCHAR(64) DEFAULT NULL,
  `contact_phone` VARCHAR(20) DEFAULT NULL,
  `address`       VARCHAR(512) DEFAULT NULL,
  `qualification` VARCHAR(256) DEFAULT NULL COMMENT '资质等级',
  `status`        TINYINT      NOT NULL DEFAULT 1,
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_supplier_code` (`supplier_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商字典';
```

#### 3.3.4 采购来源字典 `dict_purchase_source`

```sql
CREATE TABLE `dict_purchase_source` (
  `id`        BIGINT      NOT NULL AUTO_INCREMENT,
  `code`      VARCHAR(32) NOT NULL COMMENT '编码',
  `name`      VARCHAR(64) NOT NULL COMMENT '名称（如：集中采购/自行采购/调拨）',
  `sort_order` INT        NOT NULL DEFAULT 0,
  `status`    TINYINT     NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购来源字典';
```

#### 3.3.5 采购进展字典 `dict_purchase_progress`

```sql
CREATE TABLE `dict_purchase_progress` (
  `id`        BIGINT      NOT NULL AUTO_INCREMENT,
  `code`      VARCHAR(32) NOT NULL COMMENT '编码',
  `name`      VARCHAR(64) NOT NULL COMMENT '名称（如：待采购/采购中/已下单/已到货）',
  `sort_order` INT        NOT NULL DEFAULT 0,
  `status`    TINYINT     NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购进展字典';
```

#### 3.3.6 发运进展字典 `dict_shipping_progress`

```sql
CREATE TABLE `dict_shipping_progress` (
  `id`        BIGINT      NOT NULL AUTO_INCREMENT,
  `code`      VARCHAR(32) NOT NULL COMMENT '编码',
  `name`      VARCHAR(64) NOT NULL COMMENT '名称（如：待发运/发运中/已送达/已签收）',
  `sort_order` INT        NOT NULL DEFAULT 0,
  `status`    TINYINT     NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发运进展字典';
```

#### 3.3.7 工种字典 `dict_work_type`

```sql
CREATE TABLE `dict_work_type` (
  `id`        BIGINT      NOT NULL AUTO_INCREMENT,
  `code`      VARCHAR(32) NOT NULL COMMENT '工种编码',
  `name`      VARCHAR(64) NOT NULL COMMENT '工种名称',
  `sort_order` INT        NOT NULL DEFAULT 0,
  `status`    TINYINT     NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工种字典';
```

#### 3.3.8 劳动力分类字典 `dict_labor_category`

```sql
CREATE TABLE `dict_labor_category` (
  `id`        BIGINT      NOT NULL AUTO_INCREMENT,
  `code`      VARCHAR(32) NOT NULL COMMENT '分类编码',
  `name`      VARCHAR(64) NOT NULL COMMENT '分类名称（如：普工/技工/管理人员）',
  `sort_order` INT        NOT NULL DEFAULT 0,
  `status`    TINYINT     NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='劳动力分类字典';
```

### 3.4 资源计划核心表结构

> **设计策略**：8 张资源计划表采用「公共字段抽取 + 各自特有字段」的设计模式。每张表都包含计划编号、项目归属、WBS 关联、审批状态等公共要素，同时根据资源类型特点增加专属字段。

#### 3.4.1 工程实体材料计划 `resource_plan_material`

```sql
CREATE TABLE `resource_plan_material` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `plan_no`             VARCHAR(32)  NOT NULL COMMENT '计划编号',
  `project_id`          BIGINT       NOT NULL COMMENT '项目ID',
  `company_id`          BIGINT       NOT NULL COMMENT '公司ID',
  `wbs_id`              BIGINT       DEFAULT NULL COMMENT 'WBS节点ID',
  `resource_id`         BIGINT       NOT NULL COMMENT '资源ID（关联dict_resource）',
  `resource_name`       VARCHAR(256) NOT NULL COMMENT '资源名称（冗余）',
  `spec`                VARCHAR(256) DEFAULT NULL COMMENT '规格型号',
  `unit`                VARCHAR(32)  DEFAULT NULL COMMENT '计量单位',
  `plan_quantity`       DECIMAL(16,4) NOT NULL COMMENT '计划数量',
  `require_date`        DATE         NOT NULL COMMENT '需求日期',
  `supplier_id`         BIGINT       DEFAULT NULL COMMENT '供应商ID',
  `purchase_source`     VARCHAR(32)  DEFAULT NULL COMMENT '采购来源',
  `purchase_progress`   VARCHAR(32)  DEFAULT NULL COMMENT '采购进展',
  `shipping_progress`   VARCHAR(32)  DEFAULT NULL COMMENT '发运进展',
  `actual_quantity`     DECIMAL(16,4) DEFAULT NULL COMMENT '实际数量',
  `remark`              VARCHAR(512) DEFAULT NULL COMMENT '备注',
  `approval_status`     TINYINT      NOT NULL DEFAULT 0 COMMENT '审批状态：0-草稿 1-审批中 2-已通过 3-已驳回',
  `process_instance_id` VARCHAR(64)  DEFAULT NULL COMMENT 'Flowable流程实例ID',
  `create_by`           BIGINT       DEFAULT NULL COMMENT '创建人',
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`           BIGINT       DEFAULT NULL COMMENT '更新人',
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`             TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_no` (`plan_no`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_company_id` (`company_id`),
  KEY `idx_require_date` (`require_date`),
  KEY `idx_approval_status` (`approval_status`),
  KEY `idx_resource_id` (`resource_id`),
  KEY `idx_wbs_id` (`wbs_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工程实体材料计划';
```

#### 3.4.2 机械设备需求计划 `resource_plan_equipment`

```sql
CREATE TABLE `resource_plan_equipment` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `plan_no`             VARCHAR(32)  NOT NULL COMMENT '计划编号',
  `project_id`          BIGINT       NOT NULL COMMENT '项目ID',
  `company_id`          BIGINT       NOT NULL COMMENT '公司ID',
  `wbs_id`              BIGINT       DEFAULT NULL COMMENT 'WBS节点ID',
  `equipment_name`      VARCHAR(256) NOT NULL COMMENT '设备名称',
  `spec_model`          VARCHAR(256) DEFAULT NULL COMMENT '规格型号',
  `plan_quantity`       INT          NOT NULL COMMENT '计划数量（台/套）',
  `require_date`        DATE         NOT NULL COMMENT '需求日期',
  `release_date`        DATE         DEFAULT NULL COMMENT '退场日期',
  `supplier_id`         BIGINT       DEFAULT NULL COMMENT '供应商ID',
  `rental_or_purchase`  TINYINT      DEFAULT NULL COMMENT '租赁/购买：1-租赁 2-购买',
  `purchase_progress`   VARCHAR(32)  DEFAULT NULL COMMENT '采购进展',
  `actual_quantity`     INT          DEFAULT NULL COMMENT '实际数量',
  `remark`              VARCHAR(512) DEFAULT NULL,
  `approval_status`     TINYINT      NOT NULL DEFAULT 0,
  `process_instance_id` VARCHAR(64)  DEFAULT NULL,
  `create_by`           BIGINT       DEFAULT NULL,
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`           BIGINT       DEFAULT NULL,
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`             TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_no` (`plan_no`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_company_id` (`company_id`),
  KEY `idx_require_date` (`require_date`),
  KEY `idx_approval_status` (`approval_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机械设备需求计划';
```

#### 3.4.3 五金机具辅材耗材计划 `resource_plan_hardware`

```sql
CREATE TABLE `resource_plan_hardware` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `plan_no`             VARCHAR(32)  NOT NULL COMMENT '计划编号',
  `project_id`          BIGINT       NOT NULL COMMENT '项目ID',
  `company_id`          BIGINT       NOT NULL COMMENT '公司ID',
  `wbs_id`              BIGINT       DEFAULT NULL COMMENT 'WBS节点ID',
  `resource_id`         BIGINT       NOT NULL COMMENT '资源ID',
  `resource_name`       VARCHAR(256) NOT NULL COMMENT '资源名称',
  `spec`                VARCHAR(256) DEFAULT NULL COMMENT '规格型号',
  `unit`                VARCHAR(32)  DEFAULT NULL COMMENT '计量单位',
  `plan_quantity`       DECIMAL(16,4) NOT NULL COMMENT '计划数量',
  `require_date`        DATE         NOT NULL COMMENT '需求日期',
  `supplier_id`         BIGINT       DEFAULT NULL COMMENT '供应商ID',
  `purchase_source`     VARCHAR(32)  DEFAULT NULL COMMENT '采购来源',
  `purchase_progress`   VARCHAR(32)  DEFAULT NULL COMMENT '采购进展',
  `actual_quantity`     DECIMAL(16,4) DEFAULT NULL COMMENT '实际数量',
  `remark`              VARCHAR(512) DEFAULT NULL,
  `approval_status`     TINYINT      NOT NULL DEFAULT 0,
  `process_instance_id` VARCHAR(64)  DEFAULT NULL,
  `create_by`           BIGINT       DEFAULT NULL,
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`           BIGINT       DEFAULT NULL,
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`             TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_no` (`plan_no`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_company_id` (`company_id`),
  KEY `idx_require_date` (`require_date`),
  KEY `idx_approval_status` (`approval_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='五金机具辅材耗材计划';
```

#### 3.4.4 周转材需求计划 `resource_plan_circulation`

```sql
CREATE TABLE `resource_plan_circulation` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `plan_no`             VARCHAR(32)  NOT NULL COMMENT '计划编号',
  `project_id`          BIGINT       NOT NULL COMMENT '项目ID',
  `company_id`          BIGINT       NOT NULL COMMENT '公司ID',
  `wbs_id`              BIGINT       DEFAULT NULL COMMENT 'WBS节点ID',
  `resource_id`         BIGINT       NOT NULL COMMENT '资源ID',
  `resource_name`       VARCHAR(256) NOT NULL COMMENT '资源名称',
  `spec`                VARCHAR(256) DEFAULT NULL COMMENT '规格型号',
  `unit`                VARCHAR(32)  DEFAULT NULL COMMENT '计量单位',
  `plan_quantity`       DECIMAL(16,4) NOT NULL COMMENT '计划数量',
  `require_date`        DATE         NOT NULL COMMENT '需求日期',
  `return_date`         DATE         DEFAULT NULL COMMENT '预计归还日期',
  `source_type`         TINYINT      DEFAULT NULL COMMENT '来源类型：1-公司调拨 2-外部租赁 3-新购',
  `supplier_id`         BIGINT       DEFAULT NULL COMMENT '供应商ID',
  `actual_quantity`     DECIMAL(16,4) DEFAULT NULL COMMENT '实际数量',
  `remark`              VARCHAR(512) DEFAULT NULL,
  `approval_status`     TINYINT      NOT NULL DEFAULT 0,
  `process_instance_id` VARCHAR(64)  DEFAULT NULL,
  `create_by`           BIGINT       DEFAULT NULL,
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`           BIGINT       DEFAULT NULL,
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`             TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_no` (`plan_no`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_company_id` (`company_id`),
  KEY `idx_require_date` (`require_date`),
  KEY `idx_approval_status` (`approval_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='周转材需求计划';
```

#### 3.4.5 办公用品生活物资计划 `resource_plan_office`

```sql
CREATE TABLE `resource_plan_office` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `plan_no`             VARCHAR(32)  NOT NULL COMMENT '计划编号',
  `project_id`          BIGINT       NOT NULL COMMENT '项目ID',
  `company_id`          BIGINT       NOT NULL COMMENT '公司ID',
  `resource_id`         BIGINT       NOT NULL COMMENT '资源ID',
  `resource_name`       VARCHAR(256) NOT NULL COMMENT '资源名称',
  `spec`                VARCHAR(256) DEFAULT NULL COMMENT '规格型号',
  `unit`                VARCHAR(32)  DEFAULT NULL COMMENT '计量单位',
  `plan_quantity`       DECIMAL(16,4) NOT NULL COMMENT '计划数量',
  `require_date`        DATE         NOT NULL COMMENT '需求日期',
  `supplier_id`         BIGINT       DEFAULT NULL COMMENT '供应商ID',
  `purchase_source`     VARCHAR(32)  DEFAULT NULL COMMENT '采购来源',
  `purchase_progress`   VARCHAR(32)  DEFAULT NULL COMMENT '采购进展',
  `actual_quantity`     DECIMAL(16,4) DEFAULT NULL COMMENT '实际数量',
  `remark`              VARCHAR(512) DEFAULT NULL,
  `approval_status`     TINYINT      NOT NULL DEFAULT 0,
  `process_instance_id` VARCHAR(64)  DEFAULT NULL,
  `create_by`           BIGINT       DEFAULT NULL,
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`           BIGINT       DEFAULT NULL,
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`             TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_no` (`plan_no`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_company_id` (`company_id`),
  KEY `idx_require_date` (`require_date`),
  KEY `idx_approval_status` (`approval_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='办公用品生活物资计划';
```

#### 3.4.6 安全物资需求计划 `resource_plan_safety`

```sql
CREATE TABLE `resource_plan_safety` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `plan_no`             VARCHAR(32)  NOT NULL COMMENT '计划编号',
  `project_id`          BIGINT       NOT NULL COMMENT '项目ID',
  `company_id`          BIGINT       NOT NULL COMMENT '公司ID',
  `resource_id`         BIGINT       NOT NULL COMMENT '资源ID',
  `resource_name`       VARCHAR(256) NOT NULL COMMENT '资源名称',
  `spec`                VARCHAR(256) DEFAULT NULL COMMENT '规格型号',
  `unit`                VARCHAR(32)  DEFAULT NULL COMMENT '计量单位',
  `plan_quantity`       DECIMAL(16,4) NOT NULL COMMENT '计划数量',
  `require_date`        DATE         NOT NULL COMMENT '需求日期',
  `safety_standard`     VARCHAR(256) DEFAULT NULL COMMENT '安全标准/规范',
  `supplier_id`         BIGINT       DEFAULT NULL COMMENT '供应商ID',
  `purchase_source`     VARCHAR(32)  DEFAULT NULL COMMENT '采购来源',
  `purchase_progress`   VARCHAR(32)  DEFAULT NULL COMMENT '采购进展',
  `actual_quantity`     DECIMAL(16,4) DEFAULT NULL COMMENT '实际数量',
  `remark`              VARCHAR(512) DEFAULT NULL,
  `approval_status`     TINYINT      NOT NULL DEFAULT 0,
  `process_instance_id` VARCHAR(64)  DEFAULT NULL,
  `create_by`           BIGINT       DEFAULT NULL,
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`           BIGINT       DEFAULT NULL,
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`             TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_no` (`plan_no`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_company_id` (`company_id`),
  KEY `idx_require_date` (`require_date`),
  KEY `idx_approval_status` (`approval_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全物资需求计划';
```

#### 3.4.7 分包队伍需求计划 `resource_plan_subcontract`

```sql
CREATE TABLE `resource_plan_subcontract` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `plan_no`             VARCHAR(32)  NOT NULL COMMENT '计划编号',
  `project_id`          BIGINT       NOT NULL COMMENT '项目ID',
  `company_id`          BIGINT       NOT NULL COMMENT '公司ID',
  `wbs_id`              BIGINT       DEFAULT NULL COMMENT 'WBS节点ID',
  `work_type`           VARCHAR(32)  DEFAULT NULL COMMENT '工种',
  `work_content`        VARCHAR(512) DEFAULT NULL COMMENT '工作内容描述',
  `plan_persons`        INT          NOT NULL COMMENT '计划人数',
  `plan_duration`       INT          DEFAULT NULL COMMENT '计划工期（天）',
  `require_date`        DATE         NOT NULL COMMENT '进场日期',
  `end_date`            DATE         DEFAULT NULL COMMENT '退场日期',
  `qualification_req`   VARCHAR(512) DEFAULT NULL COMMENT '资质要求',
  `supplier_id`         BIGINT       DEFAULT NULL COMMENT '分包商ID',
  `actual_persons`      INT          DEFAULT NULL COMMENT '实际人数',
  `remark`              VARCHAR(512) DEFAULT NULL,
  `approval_status`     TINYINT      NOT NULL DEFAULT 0,
  `process_instance_id` VARCHAR(64)  DEFAULT NULL,
  `create_by`           BIGINT       DEFAULT NULL,
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`           BIGINT       DEFAULT NULL,
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`             TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_no` (`plan_no`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_company_id` (`company_id`),
  KEY `idx_require_date` (`require_date`),
  KEY `idx_approval_status` (`approval_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分包队伍需求计划';
```

#### 3.4.8 劳动力需求计划 `resource_plan_labor`

```sql
CREATE TABLE `resource_plan_labor` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `plan_no`             VARCHAR(32)  NOT NULL COMMENT '计划编号',
  `project_id`          BIGINT       NOT NULL COMMENT '项目ID',
  `company_id`          BIGINT       NOT NULL COMMENT '公司ID',
  `wbs_id`              BIGINT       DEFAULT NULL COMMENT 'WBS节点ID',
  `labor_category`      VARCHAR(32)  DEFAULT NULL COMMENT '劳动力分类',
  `work_type`           VARCHAR(32)  DEFAULT NULL COMMENT '工种',
  `plan_persons`        INT          NOT NULL COMMENT '计划人数',
  `require_date`        DATE         NOT NULL COMMENT '进场日期',
  `end_date`            DATE         DEFAULT NULL COMMENT '退场日期',
  `work_content`        VARCHAR(512) DEFAULT NULL COMMENT '工作内容描述',
  `skill_requirement`   VARCHAR(256) DEFAULT NULL COMMENT '技能要求',
  `actual_persons`      INT          DEFAULT NULL COMMENT '实际人数',
  `remark`              VARCHAR(512) DEFAULT NULL,
  `approval_status`     TINYINT      NOT NULL DEFAULT 0,
  `process_instance_id` VARCHAR(64)  DEFAULT NULL,
  `create_by`           BIGINT       DEFAULT NULL,
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`           BIGINT       DEFAULT NULL,
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`             TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_no` (`plan_no`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_company_id` (`company_id`),
  KEY `idx_require_date` (`require_date`),
  KEY `idx_approval_status` (`approval_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='劳动力需求计划';
```

### 3.5 索引设计策略

| 索引类型 | 设计原则 |
|---------|---------|
| 主键索引 | 所有表使用 BIGINT 自增主键 |
| 唯一索引 | 编码类字段（plan_no、org_code 等） |
| 联合索引 | 高频查询组合（company_id + project_id） |
| 日期索引 | require_date 用于预警扫描 |
| 状态索引 | approval_status、status 用于过滤查询 |

---

## 4. 核心模块设计

### 4.1 模块总览

```
┌──────────────────────────────────────────────────────────────┐
│                     项目资源计划预警系统                        │
├──────────┬──────────┬──────────┬──────────┬─────────────────┤
│ 组织架构  │ 资源计划  │ 审批流程  │ 预警引擎  │   统计分析      │
│ 管理模块  │ 管理模块  │  模块    │  模块    │    模块         │
├──────────┼──────────┼──────────┼──────────┼─────────────────┤
│ 局/公司/  │ 8种资源   │ Flowable │ 定时扫描  │   多维汇总      │
│ 项目管理  │ 计划CRUD  │ 审批集成  │ 黄/红预警  │   趋势分析      │
│ 权限控制  │ 批量导入  │ 变更审批  │ 通知推送  │   图表展示      │
│ 字典管理  │ 计划变更  │ 流程监控  │ 记录管理  │   报表导出      │
└──────────┴──────────┴──────────┴──────────┴─────────────────┘
```

### 4.2 组织架构管理模块

**职责**：管理局→公司→项目三级组织树，提供数据权限隔离基础。

**核心设计**：
- 树形结构存储（`parent_id` 递归查询，或闭包表优化）
- 数据权限：基于组织树的数据范围控制（本部门 / 本部门及下级 / 全部）
- Spring Security 集成：组织信息注入 SecurityContext，`@DataScope` 注解实现自动过滤

```
┌─────────────────────────────────┐
│      OrganizationService        │
├─────────────────────────────────┤
│ + getOrgTree(userId)            │
│ + getCompanyList(orgId)        │
│ + getProjectList(companyId)    │
│ + getDataScope(userId)         │
│ + getOrgPath(orgId): List      │
└─────────────┬───────────────────┘
              │ uses
┌─────────────▼───────────────────┐
│    DataScopeInterceptor         │
├─────────────────────────────────┤
│ 拦截 MyBatis 查询，自动追加     │
│ org_id IN (...) 数据范围条件    │
└─────────────────────────────────┘
```

### 4.3 资源计划管理模块

**职责**：管理 8 种资源类型的需求计划全生命周期。

**设计策略 —— 策略模式消除重复**：

8 张表结构相似但各异，采用 **策略模式 + 模板方法** 避免为每种资源类型编写几乎相同的 CRUD 代码。

```
                    ┌──────────────────────┐
                    │  ResourcePlanService  │  (公共接口)
                    ├──────────────────────┤
                    │ + createPlan(dto)    │
                    │ + updatePlan(id,dto) │
                    │ + deletePlan(id)     │
                    │ + getPlanPage(query) │
                    │ + submitApproval(id) │
                    └──────────┬───────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
    ┌─────────▼──────┐ ┌──────▼───────┐ ┌──────▼───────┐
    │ MaterialPlan   │ │ EquipmentPlan│ │  LaborPlan   │  ...
    │ Strategy       │ │  Strategy    │ │  Strategy    │
    └────────────────┘ └──────────────┘ └──────────────┘
              │
    ┌─────────▼──────────────┐
    │ ResourcePlanHandler    │  (模板方法)
    ├────────────────────────┤
    │ + resolveTableName()   │  ← 各策略返回对应表名
    │ + validateDTO(dto)     │  ← 各策略自定义校验
    │ + enrichDTO(dto)       │  ← 补充字典关联数据
    └────────────────────────┘
```

**公共抽象处理器（模板方法）**：

```java
public abstract class AbstractResourcePlanHandler<T> {

    /** 获取资源类型编号 1-8 */
    public abstract int getResourceType();

    /** 获取对应 Mapper */
    public abstract BaseMapper<T> getMapper();

    /** 创建计划（模板方法） */
    @Transactional
    public Long createPlan(T plan, Long userId) {
        validatePlan(plan);
        generatePlanNo(plan);
        fillAuditFields(plan, userId);
        getMapper().insert(plan);
        return ((ResourcePlanEntity) plan).getId();
    }

    /** 提交审批 */
    @Transactional
    public void submitApproval(Long planId, Long userId) {
        T plan = getMapper().selectById(planId);
        updateApprovalStatus(planId, 1); // 审批中
        processService.startApproval(planId, getResourceType(), userId);
    }
}
```

**8 种资源类型枚举**：

```java
public enum ResourceType {
    MATERIAL(1, "工程实体材料", "resource_plan_material"),
    EQUIPMENT(2, "机械设备", "resource_plan_equipment"),
    HARDWARE(3, "五金机具", "resource_plan_hardware"),
    CIRCULATION(4, "周转材", "resource_plan_circulation"),
    OFFICE(5, "办公用品", "resource_plan_office"),
    SAFETY(6, "安全物资", "resource_plan_safety"),
    SUBCONTRACT(7, "分包队伍", "resource_plan_subcontract"),
    LABOR(8, "劳动力", "resource_plan_labor");

    private final int code;
    private final String name;
    private final String tableName;
}
```

### 4.4 审批流程模块（Flowable）

**职责**：管理资源计划变更审批流程（项目发起 → 公司审批），支持在线流程调整和后台干预。

**核心功能**：
1. **在线流程设计器**：通过Web界面设计 BPMN 流程定义
2. **流程实例管理**：后台可查看、挂起、激活、终止流程实例
3. **任务管理**：查询、转派、加签、减签任务
4. **审批流程**：正常的提交/通过/驳回

**流程定义**：

```
┌──────────┐     ┌──────────────┐     ┌──────────┐
│  项目提交  │────▶│  公司审批     │────▶│  审批通过  │
│          │     │              │     │          │
└──────────┘     └──────┬───────┘     └──────────┘
                        │
                        │ 驳回
                        ▼
                 ┌──────────────┐
                 │  退回项目修改  │
                 └──────────────┘
```

**在线流程设计器集成**：

系统需要集成 Flowable 在线流程设计器，允许用户通过Web界面创建和修改流程定义。

```
┌─────────────────────────────────────────────┐
│         在线流程设计器（bpmn-js）            │
├─────────────────────────────────────────────┤
│ 功能：                                      │
│ 1. 在线绘制 BPMN 2.0 流程图               │
│ 2. 配置用户任务、网关、事件                  │
│ 3. 设置任务候选人、变量                      │
│ 4. 导出/部署流程定义                        │
│                                              │
│ 集成方式：                                   │
│ - 前端：集成 bpmn-js 可视化库                │
│ - 后端：提供流程定义 CRUD API                │
│ - 部署：将 BPMN XML 部署到 Flowable 引擎     │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│        ProcessDesignService                  │
├─────────────────────────────────────────────┤
│ + createModel(name, key, description)      │
│ + updateModel(modelId, xml)               │
│ + deployModel(modelId)                     │
│ + getModelXML(modelId)                     │
│ + listModels()                             │
│ + deleteModel(modelId)                     │
└─────────────────────────────────────────────┘
```

**流程实例后台干预**：

```
┌─────────────────────────────────────────────┐
│      ProcessInstanceManageService            │
├─────────────────────────────────────────────┤
│ 实例管理：                                  │
│ + listProcessInstances(status, startTime)  │
│ + suspendProcessInstance(instanceId)       │
│ + activateProcessInstance(instanceId)      │
│ + terminateProcessInstance(instanceId,     │
│     reason)                                │
│ + deleteProcessInstance(instanceId,        │
│     cascade)                               │
│                                              │
│ 任务管理：                                  │
│ + listTasks(processInstanceId)             │
│ + reassignTask(taskId, newAssignee)       │
│ + addCandidateUser(taskId, userId)         │
│ + claimTask(taskId, userId)                │
│ + completeTask(taskId, variables)          │
└─────────────────────────────────────────────┘
```

**BPMN 流程设计要点**：

```
流程 Key: resourcePlanApproval
候选人表达式: ${projectManager} (项目审批人)
候选人表达式: ${companyApprover} (公司审批人)
变量:
  - resourceType: 资源类型
  - planId: 计划ID
  - companyName: 公司名称
  
支持动态候选人：
  - 通过表达式从流程变量获取任务候选人
  - 候选人存储在数据库，可在流程实例中动态调整
```

**Spring Boot 集成设计**：

```
┌─────────────────────────────────────────────┐
│            FlowableConfig                    │
├─────────────────────────────────────────────┤
│ - 自动部署流程定义 (classpath:processes/)     │
│ - 集成 Spring Security 用户体系               │
│ - 自定义历史级别                              │
│ - 开启流程设计器支持                          │
│ - 配置流程实例查询历史级别                     │
└─────────────────────────────────────────────┘
              │
┌─────────────▼──────────────┐
│     ApprovalService        │
├────────────────────────────┤
│ + startApproval(planId,    │
│     resourceType, userId)  │
│ + approve(taskId, userId,  │
│     comment)               │
│ + reject(taskId, userId,   │
│     reason)                │
│ + getApprovalHistory(      │
│     processInstanceId)     │
│ + getMyPendingTasks(userId)│
│ + suspendProcess(instanceId)│
│ + activateProcess(instanceId)│
│ + terminateProcess(instanceId, reason)│
└────────────────────────────┘
```

**状态同步**：审批完成后，Flowable 监听器自动更新资源计划表的 `approval_status` 和 `process_instance_id`。

```java
@Component
public class ApprovalEndListener implements FlowableEventListener {
    @Autowired
    private Map<String, AbstractResourcePlanHandler<?>> handlerMap;

    @Override
    public void onEvent(FlowableEvent event) {
        if (event instanceof FlowableProcessCompletedEvent) {
            String processInstId = event.getProcessInstanceId();
            // 根据 processInstanceId 查找对应计划，更新状态
        }
    }
}
```

### 4.5 预警引擎模块

**职责**：定时扫描资源计划，按规则生成黄色/红色预警记录并通知。

**预警规则**：
- **黄色预警**：需求日期 - 当前日期 ≤ 提前预警天数（默认10天）且 > 0
- **红色预警**：需求日期 = 当前日期
- **触发条件**：审批状态为「已通过」且实际数量未满足计划数量

**引擎设计**：

```
┌──────────────────────────────────────────────────┐
│              WarningEngine (定时任务)              │
│          @Scheduled(cron = "0 0 8 * * ?")        │
│              每天早上8点执行                       │
├──────────────────────────────────────────────────┤
│                                                  │
│  1. 加载所有公司的预警设置 (warning_setting)       │
│  2. 遍历8种资源类型                                │
│  3. 查询各类型中:                                 │
│     - approval_status = 2 (已通过)               │
│     - require_date <= now + advance_days         │
│     - require_date >= now                        │
│  4. 计算预警级别:                                 │
│     - require_date = today → RED                 │
│     - require_date <= today+N → YELLOW           │
│  5. 写入 warning_record（去重：同计划同日期不重复） │
│  6. 触发通知（系统消息/邮件/短信）                  │
│                                                  │
└──────────────────┬───────────────────────────────┘
                   │
     ┌─────────────▼─────────────┐
     │    WarningRecordService    │
     ├───────────────────────────┤
     │ + scanAndGenerateWarnings │
     │ + getWarningsByProject()  │
     │ + handleWarning(id,remark)│
     │ + getWarningStatistics() │
     └───────────────────────────┘
```

**扫描 SQL 模板**（以材料计划为例）：

```sql
SELECT id, project_id, company_id, resource_name, require_date, plan_quantity, actual_quantity
FROM resource_plan_material
WHERE approval_status = 2
  AND deleted = 0
  AND require_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL ? DAY)
  AND (actual_quantity IS NULL OR actual_quantity < plan_quantity)
```

**去重策略**：每次扫描前检查 `warning_record` 中是否已存在相同 `plan_id` + `plan_table` + `warning_level` + `require_date` 的记录。

### 4.6 企业微信消息推送模块

**职责**：通过企业微信API推送预警通知和审批待办通知。

**推送场景**：
1. **预警通知**：生成预警记录后，立即推送消息给相关人员
2. **审批通知**：待审批任务到达时，推送消息给审批人
3. **审批结果通知**：审批通过/驳回后，通知申请人

**企业微信接口集成**：

```
┌─────────────────────────────────────────────┐
│         WeComMessageService                  │
├─────────────────────────────────────────────┤
│ 核心功能：                                   │
│ + sendTextMessage(userId, content)          │  │
│ + sendCardMessage(userId, title, desc,      │  │
│     detailUrl)                              │  │
│ + sendApprovalNotify(approverId,           │  │
│     applyUserName, resourceType)            │  │
│ + sendWarningNotify(userId, warningLevel,   │  │
│     resourceName, requireDate)              │  │
│                                              │
│ 企业微信API封装：                            │
│ + getAccessToken()                          │  │
│ + sendMessage(accessToken, message)         │  │
│ + uploadMedia(accessToken, file)           │  │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│          WeComConfig                        │
├─────────────────────────────────────────────┤
│ 配置项（application.yml）：                   │
│ - corpId: 企业ID                           │
│ - agentId: 应用ID                          │
│ - secret: 应用密钥                         │
│ - apiBaseUrl: 企业微信API地址               │
│                                              │
│ Token管理：                                  │
│ - access_token 缓存（Redis，有效期7200秒）    │
│ - 自动刷新机制                              │
└─────────────────────────────────────────────┘
```

**消息类型**：

1. **文本消息**：简单通知
```json
{
  "touser": "UserID",
  "msgtype": "text",
  "agentid": 1000002,
  "text": {
    "content": "您有新的预警：防火门预计离港日期已到"
  }
}
```

2. **卡片消息**（推荐）：内容丰富，可点击跳转
```json
{
  "touser": "UserID",
  "msgtype": "textcard",
  "agentid": 1000002,
  "textcard": {
    "title": "⚠️ 资源计划预警通知",
    "description": "项目：XX项目\n资源：防火门\n预警级别：🔴 红色预警",
    "url": "http://your-system.com/warning/123",
    "btntxt": "查看详情"
  }
}
```

**推送时机**：

```
预警生成流程：
1. WarningEngine 扫描并生成 warning_record
2. 调用 WeComMessageService.sendWarningNotify()
3. 查询接收人（项目管理员/公司审批人）
4. 构造消息内容（包含预警级别、资源名称、需求日期）
5. 调用企业微信API发送
6. 记录推送日志（可选）

审批通知流程：
1. 用户提交审批 → startApproval()
2. 调用 WeComMessageService.sendApprovalNotify()
3. 查询审批人（通过 Flowable 候选人查询）
4. 发送待审批通知
5. 审批完成后，通知申请人审批结果
```

**实现要点**：

```java
@Service
public class WeComMessageService {
    
    @Autowired
    private WeComConfig weComConfig;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    /**
     * 发送预警通知
     */
    public void sendWarningNotify(Long userId, int warningLevel, 
                                String resourceName, Date requireDate) {
        // 1. 查询用户企业微信ID
        String weComUserId = getUserWeComId(userId);
        
        // 2. 构造消息
        String title = warningLevel == 1 ? "🟡 黄色预警" : "🔴 红色预警";
        String content = String.format(
            "资源：%s\n需求日期：%s\n请及时跟进处理",
            resourceName, 
            DateUtil.formatDate(requireDate)
        );
        
        // 3. 发送卡片消息
        sendCardMessage(weComUserId, title, content, 
                       buildDetailUrl(resourceName));
    }
    
    /**
     * 获取 access_token（带缓存）
     */
    private String getAccessToken() {
        String cacheKey = "wecom:access:token";
        String token = redisTemplate.opsForValue().get(cacheKey);
        if (token != null) {
            return token;
        }
        
        // 调用企业微信API获取
        token = fetchAccessTokenFromWeCom();
        redisTemplate.opsForValue().set(cacheKey, token, 7200, TimeUnit.SECONDS);
        return token;
    }
}
```

### 4.7 统计分析模块

**职责**：提供多维度的资源计划统计分析和可视化展示。

**统计维度**：

| 维度 | 说明 |
|------|------|
| 按资源类型 | 8种资源的计划数量/金额汇总 |
| 按项目 | 各项目资源需求总量和进度 |
| 按时间 | 月度/季度资源需求趋势 |
| 按预警 | 预警数量统计和处理率 |
| 按审批 | 审批通过率、平均审批时长 |

**核心报表接口**：

```
统计模块 API:
├── GET /api/v1/statistics/plan-summary          # 资源计划总览
├── GET /api/v1/statistics/by-resource-type      # 按资源类型统计
├── GET /api/v1/statistics/by-project            # 按项目统计
├── GET /api/v1/statistics/trend                 # 时间趋势
├── GET /api/v1/statistics/warning-overview      # 预警总览
├── GET /api/v1/statistics/approval-overview     # 审批总览
└── GET /api/v1/statistics/export                # 导出报表
```

---

## 5. API 设计概要

### 5.1 命名规范

| 规则 | 说明 | 示例 |
|------|------|------|
| 基础路径 | `/api/v1/{模块}` | `/api/v1/material-plans` |
| 资源命名 | 中划线分隔的复数名词 | `material-plans`, `projects` |
| 操作 | HTTP Method 语义化 | GET 查询 / POST 新增 / PUT 修改 / DELETE 删除 |
| 分页 | `page` + `size` 参数 | `?page=1&size=20` |
| 响应格式 | 统一 `{code, message, data}` | 见下方响应模板 |

### 5.2 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1715200000000
}
```

分页响应：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [],
    "total": 100,
    "page": 1,
    "size": 20
  }
}
```

### 5.3 核心接口列表

#### 5.3.1 组织架构接口

```
GET    /api/v1/org/tree                    # 获取组织树
GET    /api/v1/org/{id}                    # 获取组织详情
POST   /api/v1/org                         # 创建组织
PUT    /api/v1/org/{id}                    # 修改组织
DELETE /api/v1/org/{id}                    # 删除组织

GET    /api/v1/companies                   # 公司列表（支持分页）
GET    /api/v1/companies/{id}              # 公司详情
POST   /api/v1/companies                   # 创建公司
PUT    /api/v1/companies/{id}              # 修改公司

GET    /api/v1/projects                    # 项目列表
GET    /api/v1/projects/{id}               # 项目详情
POST   /api/v1/projects                    # 创建项目
PUT    /api/v1/projects/{id}               # 修改项目
```

#### 5.3.2 资源计划接口（以材料计划为例，其余7种结构相同）

```
GET    /api/v1/material-plans              # 材料计划列表（分页+筛选）
GET    /api/v1/material-plans/{id}         # 计划详情
POST   /api/v1/material-plans              # 新建计划
PUT    /api/v1/material-plans/{id}         # 修改计划
DELETE /api/v1/material-plans/{id}         # 删除计划
POST   /api/v1/material-plans/batch        # 批量创建
POST   /api/v1/material-plans/import       # Excel导入
GET    /api/v1/material-plans/export       # Excel导出

# 其余7种：
GET    /api/v1/equipment-plans             # 机械设备计划
GET    /api/v1/hardware-plans              # 五金机具计划
GET    /api/v1/circulation-plans           # 周转材计划
GET    /api/v1/office-plans                # 办公用品计划
GET    /api/v1/safety-plans                # 安全物资计划
GET    /api/v1/subcontract-plans           # 分包队伍计划
GET    /api/v1/labor-plans                 # 劳动力计划
# ... 每种资源均有完整的 CRUD + 批量 + 导入导出
```

#### 5.3.3 审批流程接口

```
POST   /api/v1/approval/submit             # 提交审批
POST   /api/v1/approval/approve            # 审批通过
POST   /api/v1/approval/reject             # 审批驳回
GET    /api/v1/approval/pending-tasks      # 我的待办任务
GET    /api/v1/approval/history/{planId}   # 审批历史
GET    /api/v1/approval/process-diagram/{instanceId}  # 流程图
GET    /api/v1/approval/my-apply           # 我的申请记录
```

#### 5.3.4 预警接口

```
GET    /api/v1/warnings                    # 预警记录列表
GET    /api/v1/warnings/my-project         # 我项目的预警
PUT    /api/v1/warnings/{id}/handle        # 处理预警
GET    /api/v1/warnings/statistics         # 预警统计

GET    /api/v1/warning-settings            # 预警设置列表
GET    /api/v1/warning-settings/{id}       # 设置详情
PUT    /api/v1/warning-settings/{id}       # 修改设置
```

#### 5.3.5 统计分析接口

```
GET    /api/v1/statistics/plan-summary     # 资源计划总览
GET    /api/v1/statistics/by-resource-type # 按资源类型统计
GET    /api/v1/statistics/by-project       # 按项目统计
GET    /api/v1/statistics/trend            # 时间趋势（月/季/年）
GET    /api/v1/statistics/warning-overview # 预警总览
GET    /api/v1/statistics/approval-overview# 审批总览
GET    /api/v1/statistics/export           # 导出报表
```

#### 5.3.6 字典接口

```
GET    /api/v1/dict/wbs                    # WBS字典（树形）
GET    /api/v1/dict/resources              # 资源字典
GET    /api/v1/dict/suppliers              # 供应商字典
GET    /api/v1/dict/purchase-sources       # 采购来源
GET    /api/v1/dict/purchase-progress      # 采购进展
GET    /api/v1/dict/shipping-progress      # 发运进展
GET    /api/v1/dict/work-types             # 工种
GET    /api/v1/dict/labor-categories       # 劳动力分类
```

---

## 6. 项目文件结构

### 6.1 后端项目结构

```
resource-plan-warning/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/company/rpw/
│   │   │   ├── ResourcePlanWarningApplication.java
│   │   │   │
│   │   │   ├── common/                          # 公共模块
│   │   │   │   ├── config/
│   │   │   │   │   ├── MyBatisPlusConfig.java
│   │   │   │   │   ├── FlowableConfig.java
│   │   │   │   │   ├── SecurityConfig.java
│   │   │   │   │   ├── RedisConfig.java
│   │   │   │   │   └── CorsConfig.java
│   │   │   │   ├── exception/
│   │   │   │   │   ├── BizException.java
│   │   │   │   │   └── GlobalExceptionHandler.java
│   │   │   │   ├── result/
│   │   │   │   │   └── R.java                  # 统一响应
│   │   │   │   ├── util/
│   │   │   │   │   ├── JwtUtil.java
│   │   │   │   │   └── ExcelUtil.java
│   │   │   │   ├── annotation/
│   │   │   │   │   └── DataScope.java          # 数据权限注解
│   │   │   │   └── base/
│   │   │   │       ├── BaseEntity.java
│   │   │   │       └── BaseQuery.java
│   │   │   │
│   │   │   ├── auth/                            # 认证授权
│   │   │   │   ├── controller/AuthController.java
│   │   │   │   ├── service/AuthService.java
│   │   │   │   ├── dto/LoginDTO.java
│   │   │   │   └── vo.LoginVO.java
│   │   │   │
│   │   │   ├── org/                             # 组织架构模块
│   │   │   │   ├── controller/OrgController.java
│   │   │   │   ├── controller/CompanyController.java
│   │   │   │   ├── controller/ProjectController.java
│   │   │   │   ├── service/
│   │   │   │   │   ├── OrgService.java
│   │   │   │   │   ├── CompanyService.java
│   │   │   │   │   └── ProjectService.java
│   │   │   │   ├── mapper/
│   │   │   │   │   ├── OrganizationMapper.java
│   │   │   │   │   ├── CompanyMapper.java
│   │   │   │   │   └── ProjectMapper.java
│   │   │   │   ├── entity/
│   │   │   │   │   ├── Organization.java
│   │   │   │   │   ├── Company.java
│   │   │   │   │   └── Project.java
│   │   │   │   └── dto/
│   │   │   │       ├── OrgTreeVO.java
│   │   │   │       └── ProjectQuery.java
│   │   │   │
│   │   │   ├── plan/                            # 资源计划模块（核心）
│   │   │   │   ├── controller/
│   │   │   │   │   ├── MaterialPlanController.java
│   │   │   │   │   ├── EquipmentPlanController.java
│   │   │   │   │   ├── HardwarePlanController.java
│   │   │   │   │   ├── CirculationPlanController.java
│   │   │   │   │   ├── OfficePlanController.java
│   │   │   │   │   ├── SafetyPlanController.java
│   │   │   │   │   ├── SubcontractPlanController.java
│   │   │   │   │   └── LaborPlanController.java
│   │   │   │   ├── service/
│   │   │   │   │   ├── ResourcePlanService.java        # 公共接口
│   │   │   │   │   ├── impl/
│   │   │   │   │   │   ├── MaterialPlanServiceImpl.java
│   │   │   │   │   │   ├── EquipmentPlanServiceImpl.java
│   │   │   │   │   │   └── ... (其余6种)
│   │   │   │   │   └── handler/
│   │   │   │   │       ├── AbstractResourcePlanHandler.java
│   │   │   │   │       ├── MaterialPlanHandler.java
│   │   │   │   │       ├── EquipmentPlanHandler.java
│   │   │   │   │       └── ... (其余6种)
│   │   │   │   ├── mapper/
│   │   │   │   │   ├── MaterialPlanMapper.java
│   │   │   │   │   ├── EquipmentPlanMapper.java
│   │   │   │   │   └── ... (其余6种)
│   │   │   │   ├── entity/
│   │   │   │   │   ├── ResourcePlanMaterial.java
│   │   │   │   │   ├── ResourcePlanEquipment.java
│   │   │   │   │   └── ... (其余6种)
│   │   │   │   ├── dto/                         # 请求DTO
│   │   │   │   └── vo/                          # 响应VO
│   │   │   │
│   │   │   ├── approval/                        # 审批流程模块
│   │   │   │   ├── controller/ApprovalController.java
│   │   │   │   ├── service/ApprovalService.java
│   │   │   │   ├── listener/
│   │   │   │   │   └── ApprovalEndListener.java
│   │   │   │   └── vo/
│   │   │   │       ├── PendingTaskVO.java
│   │   │   │       └── ApprovalHistoryVO.java
│   │   │   │
│   │   │   ├── warning/                         # 预警模块
│   │   │   │   ├── controller/WarningController.java
│   │   │   │   ├── controller/WarningSettingController.java
│   │   │   │   ├── service/
│   │   │   │   │   ├── WarningEngine.java       # 定时扫描引擎
│   │   │   │   │   ├── WarningRecordService.java
│   │   │   │   │   └── WarningSettingService.java
│   │   │   │   ├── mapper/
│   │   │   │   │   ├── WarningRecordMapper.java
│   │   │   │   │   └── WarningSettingMapper.java
│   │   │   │   └── entity/
│   │   │   │       ├── WarningRecord.java
│   │   │   │       └── WarningSetting.java
│   │   │   │
│   │   │   ├── statistics/                      # 统计分析模块
│   │   │   │   ├── controller/StatisticsController.java
│   │   │   │   └── service/StatisticsService.java
│   │   │   │
│   │   │   ├── wecom/                          # 企业微信推送模块 (新增)
│   │   │   │   ├── controller/WeComConfigController.java
│   │   │   │   ├── service/WeComMessageService.java
│   │   │   │   ├── config/WeComConfig.java
│   │   │   │   ├── dto/WeComMessageDTO.java
│   │   │   │   └── vo/WeComConfigVO.java
│   │   │   │
│   │   │   ├── process/                         # 流程设计器模块 (新增)
│   │   │   │   ├── controller/ProcessDesignerController.java
│   │   │   │   ├── service/ProcessDesignService.java
│   │   │   │   ├── service/ProcessInstanceManageService.java
│   │   │   │   ├── dto/ProcessModelDTO.java
│   │   │   │   └── vo/ProcessInstanceVO.java
│   │   │   │
│   │   │   ├── importer/                        # Excel导入模块 (新增)
│   │   │   │   ├── controller/ImportController.java
│   │   │   │   ├── service/
│   │   │   │   │   ├── WbsImportService.java
│   │   │   │   │   ├── MaterialPlanImportService.java
│   │   │   │   │   └── ... (其余7种)
│   │   │   │   └── listener/
│   │   │   │       ├── WbsDataListener.java
│   │   │   │       └── PlanDataListener.java
│   │   │   │
│   │   │   ├── dict/                            # 字典模块
│   │   │   │   ├── controller/DictController.java
│   │   │   │   ├── service/DictService.java
│   │   │   │   └── entity/
│   │   │   │       ├── DictWbs.java
│   │   │   │       ├── DictResource.java
│   │   │   │       └── ...
│   │   │   │
│   │   │   └── intercept/
│   │   │       └── DataScopeInterceptor.java     # 数据权限拦截器
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       ├── mapper/                          # MyBatis XML
│   │       │   ├── org/
│   │       │   ├── plan/
│   │       │   └── warning/
│   │       └── processes/                       # Flowable BPMN
│   │           └── resourcePlanApproval.bpmn20.xml
│   │
│   └── test/
│       └── java/com/company/rpw/
│           ├── plan/                            # 单元测试
│           ├── warning/
│           └── approval/
│
└── sql/
    ├── 01_schema.sql                            # 表结构
    ├── 02_dict_data.sql                         # 字典初始数据
    └── 03_flowable.sql                          # Flowable 初始化
```

### 6.2 前端项目结构

```
resource-plan-warning-web/
├── index.html
├── vite.config.ts
├── tsconfig.json
├── package.json
├── .env.development
├── .env.production
│
├── public/
│   └── favicon.ico
│
└── src/
    ├── main.ts
    ├── App.vue
    ├── env.d.ts
    │
    ├── api/                              # API 请求层
    │   ├── request.ts                   # Axios 封装
    │   ├── auth.ts
    │   ├── org.ts
    │   ├── project.ts
    │   ├── plan/
    │   │   ├── material.ts
    │   │   ├── equipment.ts
    │   │   ├── hardware.ts
    │   │   ├── circulation.ts
    │   │   ├── office.ts
    │   │   ├── safety.ts
    │   │   ├── subcontract.ts
    │   │   └── labor.ts
    │   ├── approval.ts
    │   ├── warning.ts
    │   ├── statistics.ts
    │   ├── dict.ts
    │   ├── wecom.ts                      # 企业微信API (新增)
    │   └── process-designer.ts           # 流程设计器API (新增)
    │
    ├── views/                            # 页面
    │   ├── login/
    │   │   └── index.vue
    │   ├── dashboard/
    │   │   └── index.vue                # 首页仪表盘
    │   ├── org/
    │   │   ├── company/index.vue         # 公司管理
    │   │   └── project/index.vue        # 项目管理
    │   ├── plan/
    │   │   ├── material/index.vue        # 材料计划
    │   │   ├── equipment/index.vue       # 设备计划
    │   │   ├── hardware/index.vue        # 五金机具计划
    │   │   ├── circulation/index.vue     # 周转材计划
    │   │   ├── office/index.vue          # 办公用品计划
    │   │   ├── safety/index.vue          # 安全物资计划
    │   │   ├── subcontract/index.vue     # 分包队伍计划
    │   │   └── labor/index.vue           # 劳动力计划
    │   ├── import/                        # 导入页面 (新增)
    │   │   ├── wbs/index.vue              # WBS导入
    │   │   ├── material/index.vue         # 材料计划导入
    │   │   └── ... (其余7种)
    │   ├── approval/
    │   │   ├── pending/index.vue         # 待办任务
    │   │   ├── my-apply/index.vue        # 我的申请
    │   │   ├── detail/index.vue          # 审批详情/流程图
    │   │   ├── process-designer/index.vue # 流程设计器 (新增)
    │   │   └── process-instance/index.vue # 流程实例管理 (新增)
    │   ├── warning/
    │   │   ├── records/index.vue         # 预警记录
    │   │   └── settings/index.vue        # 预警设置
    │   ├── wecom/                         # 企业微信配置 (新增)
    │   │   └── config/index.vue         # 企业微信配置页
    │   └── statistics/
    │       ├── plan-summary/index.vue    # 计划总览
    │       ├── trend/index.vue           # 趋势分析
    │       └── export/index.vue          # 报表导出
    │
    ├── components/                       # 公共组件
    │   ├── layout/
    │   │   ├── AppLayout.vue             # 主布局
    │   │   ├── Sidebar.vue               # 侧边栏
    │   │   └── Header.vue                # 顶部栏
    │   ├── plan/
    │   │   ├── PlanForm.vue              # 计划表单（复用）
    │   │   ├── PlanTable.vue             # 计划列表（复用）
    │   │   └── PlanImport.vue            # 导入组件
    │   ├── approval/
    │   │   ├── ProcessDiagram.vue        # 流程图组件
    │   │   └── ProcessDesigner.vue       # 流程设计器组件 (新增)
    │   ├── warning/
    │   │   └── WarningBadge.vue          # 预警标识
    │   └── common/
    │       ├── DictSelect.vue            # 字典下拉
    │       ├── OrgTreeSelect.vue         # 组织树选择
    │       └── ExportButton.vue          # 导出按钮
    │
    ├── router/
    │   └── index.ts                      # 路由配置
    │
    ├── stores/                           # Pinia 状态管理
    │   ├── user.ts
    │   ├── permission.ts
    │   ├── app.ts
    │   └── dict.ts                       # 字典缓存
    │
    ├── composables/                      # 组合式函数
    │   ├── useDict.ts                    # 字典获取
    │   ├── usePermission.ts              # 权限判断
    │   └── usePagination.ts              # 分页逻辑
    │
    ├── styles/
    │   ├── variables.scss
    │   └── global.scss
    │
    └── utils/
        ├── format.ts                     # 格式化工具
        └── validate.ts                   # 校验规则
```

---

## 7. 实现任务列表

按依赖关系从基础到高级排序，标记并行可执行的任务。

### Phase 1：基础框架搭建（2 周）

| 序号 | 任务 | 依赖 | 优先级 | 说明 |
|------|------|------|--------|------|
| 1.1 | 初始化后端 Spring Boot 项目 | 无 | P0 | 脚手架、依赖引入、多环境配置 |
| 1.2 | 初始化前端 Vue 3 项目 | 无 | P0 | Vite + TS + Pinia + Vue Router |
| 1.3 | 数据库建表 | 无 | P0 | 执行 SQL 脚本，创建全部表结构（含部门/处室字段） |
| 1.4 | 公共基础代码 | 1.1 | P0 | 统一响应 R、全局异常处理、MyBatis-Plus 配置 |
| 1.5 | 登录认证模块 | 1.1, 1.2 | P0 | Spring Security + JWT，前端登录页 |
| 1.6 | 字典管理模块 | 1.3, 1.4 | P1 | 8 张字典表 CRUD，前端字典缓存 |
| 1.7 | 企业微信配置 | 1.1 | P1 | 企业微信 API 集成、access_token 管理 |

### Phase 2：组织架构（1 周）

| 序号 | 任务 | 依赖 | 优先级 | 说明 |
|------|------|------|--------|------|
| 2.1 | 组织架构后端（含部门/处室） | 1.5 | P0 | 组织树 CRUD、公司/项目管理（含部门/处室字段） |
| 2.2 | 数据权限拦截器 | 2.1 | P0 | @DataScope 注解 + MyBatis 拦截器 |
| 2.3 | 组织架构前端 | 2.1, 1.2 | P0 | 组织树组件、公司/项目列表页（含部门/处室） |

### Phase 3：资源计划核心（3 周）

| 序号 | 任务 | 依赖 | 优先级 | 说明 |
|------|------|------|--------|------|
| 3.1 | 资源计划抽象基类 | 2.1 | P0 | AbstractResourcePlanHandler、枚举、策略注册 |
| 3.2 | 工程实体材料计划 | 3.1 | P0 | 完整 CRUD + 导入导出（作为模板） |
| 3.3 | 机械设备需求计划 | 3.2 | P0 | 基于模板实现 |
| 3.4 | 五金机具辅材计划 | 3.2 | P0 | 可与3.3并行 |
| 3.5 | 周转材需求计划 | 3.2 | P0 | 可与3.3/3.4并行 |
| 3.6 | 办公用品计划 | 3.2 | P1 | |
| 3.7 | 安全物资计划 | 3.2 | P1 | |
| 3.8 | 分包队伍计划 | 3.2 | P0 | 人员类计划，字段差异较大 |
| 3.9 | 劳动力需求计划 | 3.2 | P0 | 人员类计划 |
| 3.10 | 前端资源计划页面 | 3.2-3.9 | P0 | 8 个计划列表页 + 表单组件 |
| 3.11 | Excel 导入功能 | 3.2 | P0 | WBS 导入 + 8 种资源计划导入（EasyExcel） |

### Phase 4：审批流程（2.5 周）

| 序号 | 任务 | 依赖 | 优先级 | 说明 |
|------|------|------|--------|------|
| 4.1 | Flowable 集成配置 | 1.1 | P0 | 引入依赖、流程引擎配置 |
| 4.2 | 审批流程定义 | 4.1 | P0 | BPMN 流程设计、部署 |
| 4.3 | 审批服务实现 | 4.2, 3.1 | P0 | 提交/审批/驳回、状态同步 |
| 4.4 | 在线流程设计器 | 4.1 | P0 | 前端集成 bpmn-js，在线绘制流程图 |
| 4.5 | 流程实例管理 | 4.3 | P0 | 后台干预：挂起/激活/终止流程实例 |
| 4.6 | 审批前端页面 | 4.3-4.5 | P0 | 待办任务、审批详情、流程图、流程设计器页面 |

### Phase 5：预警引擎（2 周）

| 序号 | 任务 | 依赖 | 优先级 | 说明 |
|------|------|------|--------|------|
| 5.1 | 预警设置管理 | 2.1 | P1 | 公司级预警参数配置（含通知渠道） |
| 5.2 | 预警引擎实现 | 3.1, 5.1 | P0 | 定时任务、扫描逻辑、去重 |
| 5.3 | 预警记录管理 | 5.2 | P0 | 预警列表、处理操作 |
| 5.4 | 企业微信推送 | 5.2, 1.7 | P0 | 预警通知、审批通知（企业微信 API） |
| 5.5 | 预警前端页面 | 5.1-5.4 | P0 | 预警记录列表、设置页、企业微信配置页 |

### Phase 6：统计分析（1.5 周）

| 序号 | 任务 | 依赖 | 优先级 | 说明 |
|------|------|------|--------|------|
| 6.1 | 统计服务实现 | 3.1, 5.2 | P1 | 多维聚合查询 |
| 6.2 | 首页仪表盘 | 6.1 | P1 | ECharts 图表、关键指标卡片 |
| 6.3 | 统计分析页面 | 6.1 | P1 | 计划总览、趋势图 |
| 6.4 | 报表导出 | 6.1 | P2 | Excel 导出 |

### Phase 7：联调优化（1 周）

| 序号 | 任务 | 依赖 | 优先级 | 说明 |
|------|------|------|--------|------|
| 7.1 | 前后端联调 | Phase 3-6 | P0 | 接口联调、数据流验证 |
| 7.2 | 权限联调 | 7.1 | P0 | 三级组织数据隔离验证（含部门/处室） |
| 7.3 | 性能优化 | 7.1 | P1 | 慢 SQL 优化、Redis 缓存、前端懒加载 |
| 7.4 | 测试 | 7.2 | P0 | 功能测试、预警引擎测试、企业微信推送测试 |

### 工作量估算汇总

| 阶段 | 内容 | 预估工期 | 人力 |
|------|------|---------|------|
| Phase 1 | 基础框架 + 企业微信配置 | 2 周 | 2 前端 + 2 后端 |
| Phase 2 | 组织架构（含部门/处室） | 1 周 | 1 前端 + 1 后端 |
| Phase 3 | 资源计划 + Excel 导入 | 3 周 | 2 前端 + 2 后端 |
| Phase 4 | 审批流程 + 流程设计器 + 实例管理 | 2.5 周 | 1 前端 + 1 后端 |
| Phase 5 | 预警引擎 + 企业微信推送 | 2 周 | 1 前端 + 1 后端 |
| Phase 6 | 统计分析 | 1.5 周 | 1 前端 + 1 后端 |
| Phase 7 | 联调优化 | 1 周 | 全员 |
| **合计** | | **13 周** | **4-5 人** |

> **关键路径**：Phase 1 → Phase 2 → Phase 3.1 → Phase 3.2 → Phase 4 → Phase 5.2 → Phase 6
> **新增工作量大头**：Flowable 在线流程设计器（+0.5周）、流程实例管理（+0.5周）、企业微信推送（+0.5周）、Excel导入（+0.5周）
> 建议优先投入资源到关键路径上的任务，非关键路径任务可在空闲时并行推进。
