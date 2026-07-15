# 项目资源计划预警系统 - Phase 6 技术架构设计

## 文档信息

| 版本 | 日期 | 作者 | 说明 |
|------|------|------|------|
| 1.0 | 2026-05-10 | 高见远（Gao） | 初始版本 |

---

## 目录

1. [项目背景](#项目背景)
2. [技术选型](#技术选型)
3. [数据库设计](#数据库设计)
4. [后端架构](#后端架构)
5. [前端架构](#前端架构)
6. [接口设计](#接口设计)
7. [任务分解](#任务分解)
8. [依赖关系](#依赖关系)
9. [附录](#附录)

---

## 项目背景

### 当前系统状态

Phase 1-5 已完成核心功能，包括：
- 物资资源计划管理（8种类型统一模型）
- 预警规则配置与自动生成
- Flowable 流程引擎集成
- 审批流程管理
- 在线流程设计器
- 流程实例后台干预

### Phase 6 目标

根据 PRD 需求，Phase 6 需要实现：
1. **P1-002**: 分包队伍计划管理（24h）
2. **P1-003**: 劳动力需求管理（24h）

### 核心业务场景

```
项目级用户            公司级用户                局级用户
   │                    │                         │
   ├── 登记分包计划 ───►│                         │
   ├── 登记劳动力需求 ──►│                         │
   ├── 发起变更申请 ───►│── 审批通过 ──► 更新计划  │
   │                    │── 审批驳回 ──► 返回修改  │
   │                    │                         │
   ├── 登记进展 ───────►│                         │
   │                    │── 监督预警 ──►          │
   │                    │                         │
   │                    │                         ├── 统计汇总
   │                    │                         ├── 预警监控
```

---

## 技术选型

### 技术栈（延续现有技术栈）

| 层级 | 技术 | 版本 | 说明 |
|------|------|------|------|
| **前端** | Vue | 3.x | 延续现有框架 |
| | Element Plus | 2.x | UI 组件库 |
| | Pinia | 2.x | 状态管理 |
| | Vue Router | 4.x | 路由管理 |
| | Axios | 1.x | HTTP 客户端 |
| **后端** | Spring Boot | 3.1.6 | 延续现有版本 |
| | MyBatis-Plus | 3.5.x | ORM 框架 |
| | Flowable | 7.0.0 | 流程引擎（已集成） |
| | Spring Security | 6.x | 权限框架 |
| | JWT | 0.11.x | 认证机制 |
| **数据库** | MySQL | 8.0 | 主数据库 |
| | Redis | 7.x | 缓存（可选） |
| **构建工具** | Maven | 3.8+ | 后端构建 |
| | Vite | 4.x | 前端构建 |

### 技术选型理由

1. **延续性**：完全延续 Phase 1-5 技术栈，降低学习成本
2. **稳定性**：所有技术均为生产级稳定版本
3. **可扩展性**：MyBatis-Plus 和 Flowable 均支持良好扩展
4. **开发效率**：复用现有代码模板和工具类

---

## 数据库设计

### 1. 现有表结构分析

#### 1.1 resource_plan_subcontract（分包计划表）- 需要扩展

**现有字段**：
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | bigint | 主键（BaseEntity） |
| project_id | bigint | 项目ID |
| wbs_code | varchar | WBS编码 |
| subcontract_name | varchar | 分包项目名称 |
| work_content | varchar | 工作内容 |
| supplier_code | varchar | 供应商编码 |
| plan_start_date | date | 计划开始日期 |
| plan_end_date | date | 计划结束日期 |
| actual_start_date | date | 实际开始日期 |
| actual_end_date | date | 实际结束日期 |
| remark | varchar | 备注 |
| create_time | datetime | 创建时间（BaseEntity） |
| update_time | datetime | 更新时间（BaseEntity） |
| deleted | int | 逻辑删除标记（BaseEntity） |

**需要新增的字段**：
| 字段名 | 类型 | 说明 |
|--------|------|------|
| status | varchar(50) | 状态（DRAFT/SUBMITTED/IN_PROGRESS/COMPLETED/TERMINATED） |
| wbs_name | varchar(200) | WBS名称（冗余存储，减少关联查询） |
| supplier_name | varchar(200) | 供应商名称（冗余存储） |
| approval_status | varchar(50) | 审批状态（DRAFT/SUBMITTED/APPROVED/REJECTED） |
| process_instance_id | varchar(100) | Flowable流程实例ID |

#### 1.2 resource_plan_labor（劳动力计划表）- 需要扩展

**现有字段**：
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | bigint | 主键（BaseEntity） |
| project_id | bigint | 项目ID |
| wbs_code | varchar | WBS编码 |
| work_type_code | varchar | 工种编码 |
| labor_category_code | varchar | 劳务类别编码 |
| plan_quantity | int | 计划人数 |
| plan_start_date | date | 计划开始日期 |
| plan_end_date | date | 计划结束日期 |
| actual_quantity | int | 实际人数 |
| actual_start_date | date | 实际开始日期 |
| actual_end_date | date | 实际结束日期 |
| remark | varchar | 备注 |
| create_time | datetime | 创建时间（BaseEntity） |
| update_time | datetime | 更新时间（BaseEntity） |
| deleted | int | 逻辑删除标记（BaseEntity） |

**需要新增的字段**：
| 字段名 | 类型 | 说明 |
|--------|------|------|
| status | varchar(50) | 状态（DRAFT/SUBMITTED/IN_PROGRESS/COMPLETED/TERMINATED） |
| wbs_name | varchar(200) | WBS名称（冗余存储） |
| work_type_name | varchar(100) | 工种名称（冗余存储） |
| labor_category_name | varchar(100) | 劳务类别名称（冗余存储） |
| approval_status | varchar(50) | 审批状态 |
| process_instance_id | varchar(100) | Flowable流程实例ID |
| attendance_records | json | 每日出勤记录（可选） |

### 2. 新增表设计

#### 2.1 dict_work_type（工种字典表）

**表说明**：存储工种字典，供劳动力计划选择工种使用。

| 字段名 | 类型 | 长度 | 可为空 | 默认值 | 说明 |
|--------|------|------|--------|--------|------|
| id | bigint | 20 | NOT NULL | AUTO_INCREMENT | 主键ID |
| work_type_code | varchar | 50 | NOT NULL | - | 工种编码（唯一） |
| work_type_name | varchar | 100 | NOT NULL | - | 工种名称 |
| sort_order | int | 11 | NOT NULL | 0 | 排序 |
| status | tinyint | 1 | NOT NULL | 1 | 状态（1-启用 0-禁用） |
| create_time | datetime | - | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| update_time | datetime | - | NOT NULL | CURRENT_TIMESTAMP | 更新时间 |

**索引设计**：
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_work_type_code` (`work_type_code`)
- INDEX `idx_status` (`status`)

**建表SQL**：
```sql
CREATE TABLE `dict_work_type` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工种字典表';

-- 初始数据
INSERT INTO `dict_work_type` (`work_type_code`, `work_type_name`, `sort_order`) VALUES
('WT001', '钢筋工', 1),
('WT002', '木工', 2),
('WT003', '混凝土工', 3),
('WT004', '电工', 4),
('WT005', '焊工', 5),
('WT006', '架子工', 6),
('WT007', '起重工', 7),
('WT008', '普工', 8);
```

#### 2.2 dict_labor_category（劳务类别字典表）

**表说明**：存储劳务类别字典，供劳动力计划选择劳务类别使用。

| 字段名 | 类型 | 长度 | 可为空 | 默认值 | 说明 |
|--------|------|------|--------|--------|------|
| id | bigint | 20 | NOT NULL | AUTO_INCREMENT | 主键ID |
| category_code | varchar | 50 | NOT NULL | - | 类别编码（唯一） |
| category_name | varchar | 100 | NOT NULL | - | 类别名称 |
| sort_order | int | 11 | NOT NULL | 0 | 排序 |
| status | tinyint | 1 | NOT NULL | 1 | 状态（1-启用 0-禁用） |
| create_time | datetime | - | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| update_time | datetime | - | NOT NULL | CURRENT_TIMESTAMP | 更新时间 |

**索引设计**：
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_category_code` (`category_code`)
- INDEX `idx_status` (`status`)

**建表SQL**：
```sql
CREATE TABLE `dict_labor_category` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='劳务类别字典表';

-- 初始数据
INSERT INTO `dict_labor_category` (`category_code`, `category_name`, `sort_order`) VALUES
('LC001', '自有员工', 1),
('LC002', '外包队伍', 2),
('LC003', '临时工', 3),
('LC004', '劳务派遣', 4);
```

### 3. 现有表结构修改

#### 3.1 修改 resource_plan_subcontract 表

```sql
-- 新增字段
ALTER TABLE `resource_plan_subcontract` 
  ADD COLUMN `status` varchar(50) DEFAULT 'DRAFT' COMMENT '状态' AFTER `actual_end_date`,
  ADD COLUMN `wbs_name` varchar(200) COMMENT 'WBS名称' AFTER `wbs_code`,
  ADD COLUMN `supplier_name` varchar(200) COMMENT '供应商名称' AFTER `supplier_code`,
  ADD COLUMN `approval_status` varchar(50) DEFAULT 'DRAFT' COMMENT '审批状态' AFTER `status`,
  ADD COLUMN `process_instance_id` varchar(100) COMMENT 'Flowable流程实例ID' AFTER `approval_status`;

-- 新增索引
ALTER TABLE `resource_plan_subcontract`
  ADD INDEX `idx_status` (`status`),
  ADD INDEX `idx_approval_status` (`approval_status`),
  ADD INDEX `idx_process_instance_id` (`process_instance_id`);

-- 修改唯一键（加入status排除已删除数据）
ALTER TABLE `resource_plan_subcontract`
  DROP INDEX `uk_project_wbs_subcontract`,
  ADD UNIQUE KEY `uk_project_wbs_subcontract` (`project_id`, `wbs_code`, `subcontract_name`);
```

#### 3.2 修改 resource_plan_labor 表

```sql
-- 新增字段
ALTER TABLE `resource_plan_labor` 
  ADD COLUMN `status` varchar(50) DEFAULT 'DRAFT' COMMENT '状态' AFTER `actual_end_date`,
  ADD COLUMN `wbs_name` varchar(200) COMMENT 'WBS名称' AFTER `wbs_code`,
  ADD COLUMN `work_type_name` varchar(100) COMMENT '工种名称' AFTER `work_type_code`,
  ADD COLUMN `labor_category_name` varchar(100) COMMENT '劳务类别名称' AFTER `labor_category_code`,
  ADD COLUMN `approval_status` varchar(50) DEFAULT 'DRAFT' COMMENT '审批状态' AFTER `status`,
  ADD COLUMN `process_instance_id` varchar(100) COMMENT 'Flowable流程实例ID' AFTER `approval_status`,
  ADD COLUMN `attendance_records` json COMMENT '出勤记录' AFTER `remark`;

-- 新增索引
ALTER TABLE `resource_plan_labor`
  ADD INDEX `idx_status` (`status`),
  ADD INDEX `idx_approval_status` (`approval_status`),
  ADD INDEX `idx_process_instance_id` (`process_instance_id`);

-- 修改唯一键
ALTER TABLE `resource_plan_labor`
  DROP INDEX `uk_project_wbs_type_category`,
  ADD UNIQUE KEY `uk_project_wbs_type_category` (`project_id`, `wbs_code`, `work_type_code`, `labor_category_code`);
```

### 4. 预警规则配置扩展

在现有 `warning_setting` 表中增加资源类型：
- 资源类型 7：分包队伍（subcontract）
- 资源类型 8：劳动力（labor）

**预警规则配置示例**：

| 资源类型 | 预警类型 | 预警条件 | 预警级别 |
|----------|----------|----------|----------|
| subcontract | 未按时进场 | 实际开始日期 > 计划开始日期 | 红色 |
| subcontract | 即将到期 | 计划结束日期 - 当前日期 <= 7天 | 黄色 |
| subcontract | 超期未完工 | 当前日期 > 计划结束日期 且 实际结束日期为空 | 红色 |
| labor | 人力不足 | 实际人数 < 计划人数 × 0.8 | 红色 |
| labor | 人力即将不足 | 实际人数 < 计划人数 | 黄色 |
| labor | 未按时到位 | 实际开始日期 > 计划开始日期 | 红色 |

---

## 后端架构

### 1. 分包队伍计划管理架构

#### 1.1 Controller 层

**类路径**：`com.company.rpw.controller.ResourcePlanSubcontractController`

**现有方法**：
- `GET /api/v1/resource-plan/subcontract/list` - 查询列表
- `GET /api/v1/resource-plan/subcontract/{id}` - 根据ID查询
- `POST /api/v1/resource-plan/subcontract` - 新增
- `PUT /api/v1/resource-plan/subcontract/{id}` - 修改
- `DELETE /api/v1/resource-plan/subcontract/{id}` - 删除

**需要新增的方法**：
- `POST /api/v1/resource-plan/subcontract/{id}/submit` - 提交审批
- `POST /api/v1/resource-plan/subcontract/{id}/progress` - 登记进展
- `POST /api/v1/resource-plan/subcontract/change` - 发起变更申请
- `GET /api/v1/resource-plan/subcontract/{id}/change-history` - 查询变更历史
- `POST /api/v1/resource-plan/subcontract/import` - 导入Excel
- `GET /api/v1/resource-plan/subcontract/export` - 导出Excel

**代码扩展示例**：
```java
@RestController
@RequestMapping("/api/v1/resource-plan/subcontract")
@RequiredArgsConstructor
@Slf4j
public class ResourcePlanSubcontractController {
    
    private final ResourcePlanSubcontractService service;
    
    // 现有方法...
    
    /**
     * 提交审批
     * POST /api/v1/resource-plan/subcontract/{id}/submit
     */
    @PostMapping("/{id}/submit")
    public R<Boolean> submit(@PathVariable Long id) {
        boolean result = service.submit(id);
        return result ? R.ok(true) : R.fail(500, "提交失败");
    }
    
    /**
     * 登记进展
     * POST /api/v1/resource-plan/subcontract/{id}/progress
     */
    @PostMapping("/{id}/progress")
    public R<Boolean> registerProgress(@PathVariable Long id, 
                                       @RequestBody @Valid ProgressDTO dto) {
        boolean result = service.registerProgress(id, dto);
        return result ? R.ok(true) : R.fail(500, "登记失败");
    }
    
    /**
     * 发起变更申请
     * POST /api/v1/resource-plan/subcontract/change
     */
    @PostMapping("/change")
    public R<Long> createChange(@RequestBody @Valid ChangeDTO dto) {
        Long changeId = service.createChange(dto);
        return R.ok(changeId);
    }
}
```

#### 1.2 Service 层

**类路径**：`com.company.rpw.service.ResourcePlanSubcontractService`

**需要新增的方法**：
```java
public interface ResourcePlanSubcontractService extends IService<ResourcePlanSubcontract> {
    
    // 现有方法...
    
    /**
     * 提交审批
     */
    boolean submit(Long id);
    
    /**
     * 登记进展
     */
    boolean registerProgress(Long id, ProgressDTO dto);
    
    /**
     * 创建变更申请
     */
    Long createChange(ChangeDTO dto);
    
    /**
     * 查询变更历史
     */
    List<ChangeRecordVO> getChangeHistory(Long id);
    
    /**
     * 导入Excel
     */
    ImportResultVO importExcel(MultipartFile file);
    
    /**
     * 导出Excel
     */
    void exportExcel(HttpServletResponse response, Long projectId);
}
```

#### 1.3 Entity 层

**类路径**：`com.company.rpw.entity.ResourcePlanSubcontract`

**需要新增的字段**：
```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resource_plan_subcontract")
public class ResourcePlanSubcontract extends BaseEntity {
    
    // 现有字段...
    
    /** WBS名称（冗余存储） */
    @TableField("wbs_name")
    private String wbsName;
    
    /** 分包项目名称 */
    @TableField("subcontract_name")
    private String subcontractName;
    
    /** 工作内容 */
    @TableField("work_content")
    private String workContent;
    
    /** 供应商编码 */
    @TableField("supplier_code")
    private String supplierCode;
    
    /** 供应商名称（冗余存储） */
    @TableField("supplier_name")
    private String supplierName;
    
    /** 计划开始日期 */
    @TableField("plan_start_date")
    private LocalDate planStartDate;
    
    /** 计划结束日期 */
    @TableField("plan_end_date")
    private LocalDate planEndDate;
    
    /** 实际开始日期 */
    @TableField("actual_start_date")
    private LocalDate actualStartDate;
    
    /** 实际结束日期 */
    @TableField("actual_end_date")
    private LocalDate actualEndDate;
    
    /** 状态（DRAFT/SUBMITTED/IN_PROGRESS/COMPLETED/TERMINATED） */
    @TableField("status")
    private String status;
    
    /** 审批状态（DRAFT/SUBMITTED/APPROVED/REJECTED） */
    @TableField("approval_status")
    private String approvalStatus;
    
    /** Flowable流程实例ID */
    @TableField("process_instance_id")
    private String processInstanceId;
    
    /** 备注 */
    @TableField("remark")
    private String remark;
}
```

### 2. 劳动力需求管理架构

#### 2.1 Controller 层

**类路径**：`com.company.rpw.controller.ResourcePlanLaborController`

**需要新增的方法**：
- `POST /api/v1/resource-plan/labor/{id}/submit` - 提交审批
- `POST /api/v1/resource-plan/labor/{id}/progress` - 登记进展
- `POST /api/v1/resource-plan/labor/change` - 发起变更申请
- `GET /api/v1/resource-plan/labor/{id}/change-history` - 查询变更历史
- `POST /api/v1/resource-plan/labor/import` - 导入Excel
- `GET /api/v1/resource-plan/labor/export` - 导出Excel
- `GET /api/v1/resource-plan/labor/{id}/warning-status` - 查询预警状态

#### 2.2 Service 层

**类路径**：`com.company.rpw.service.ResourcePlanLaborService`

**需要新增的方法**：
```java
public interface ResourcePlanLaborService extends IService<ResourcePlanLabor> {
    
    // 现有方法...
    
    /**
     * 提交审批
     */
    boolean submit(Long id);
    
    /**
     * 登记进展
     */
    boolean registerProgress(Long id, LaborProgressDTO dto);
    
    /**
     * 创建变更申请
     */
    Long createChange(LaborChangeDTO dto);
    
    /**
     * 查询预警状态
     */
    WarningStatusVO getWarningStatus(Long id);
}
```

#### 2.3 Entity 层

**类路径**：`com.company.rpw.entity.ResourcePlanLabor`

**需要新增的字段**：
```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resource_plan_labor")
public class ResourcePlanLabor extends BaseEntity {
    
    // 现有字段...
    
    /** WBS名称（冗余存储） */
    @TableField("wbs_name")
    private String wbsName;
    
    /** 工种编码 */
    @TableField("work_type_code")
    private String workTypeCode;
    
    /** 工种名称（冗余存储） */
    @TableField("work_type_name")
    private String workTypeName;
    
    /** 劳务类别编码 */
    @TableField("labor_category_code")
    private String laborCategoryCode;
    
    /** 劳务类别名称（冗余存储） */
    @TableField("labor_category_name")
    private String laborCategoryName;
    
    /** 计划人数 */
    @TableField("plan_quantity")
    private Integer planQuantity;
    
    /** 计划开始日期 */
    @TableField("plan_start_date")
    private LocalDate planStartDate;
    
    /** 计划结束日期 */
    @TableField("plan_end_date")
    private LocalDate planEndDate;
    
    /** 实际人数 */
    @TableField("actual_quantity")
    private Integer actualQuantity;
    
    /** 实际开始日期 */
    @TableField("actual_start_date")
    private LocalDate actualStartDate;
    
    /** 实际结束日期 */
    @TableField("actual_end_date")
    private LocalDate actualEndDate;
    
    /** 状态（DRAFT/SUBMITTED/IN_PROGRESS/COMPLETED/TERMINATED） */
    @TableField("status")
    private String status;
    
    /** 审批状态（DRAFT/SUBMITTED/APPROVED/REJECTED） */
    @TableField("approval_status")
    private String approvalStatus;
    
    /** Flowable流程实例ID */
    @TableField("process_instance_id")
    private String processInstanceId;
    
    /** 出勤记录（JSON格式） */
    @TableField("attendance_records")
    private String attendanceRecords;
    
    /** 备注 */
    @TableField("remark")
    private String remark;
}
```

### 3. 字典管理架构

#### 3.1 工种字典管理

**Controller**: `com.company.rpw.controller.DictWorkTypeController`
- `GET /api/v1/dict/work-type/list` - 查询列表
- `POST /api/v1/dict/work-type` - 新增
- `PUT /api/v1/dict/work-type/{id}` - 修改
- `DELETE /api/v1/dict/work-type/{id}` - 删除

**Service**: `com.company.rpw.service.DictWorkTypeService`
**Entity**: `com.company.rpw.entity.DictWorkType`
**Mapper**: `com.company.rpw.mapper.DictWorkTypeMapper`

#### 3.2 劳务类别字典管理

**Controller**: `com.company.rpw.controller.DictLaborCategoryController`
**Service**: `com.company.rpw.service.DictLaborCategoryService`
**Entity**: `com.company.rpw.entity.DictLaborCategory`
**Mapper**: `com.company.rpw.mapper.DictLaborCategoryMapper`

---

## 前端架构

### 1. 分包队伍计划管理前端

#### 1.1 页面结构

**页面路径**：`frontend/src/views/resource-plan/subcontract/`

| 文件名 | 说明 |
|--------|------|
| `index.vue` | 列表页面（已有，需扩展） |
| `detail.vue` | 详情/进展登记页面（新增） |
| `change.vue` | 变更申请页面（新增） |
| `components/ProgressDialog.vue` | 进展登记对话框组件（新增） |
| `components/ChangeDialog.vue` | 变更申请对话框组件（新增） |

#### 1.2 列表页面扩展

**现有功能**：
- 表格展示
- 新增/编辑/删除

**需要新增的功能**：
- 状态筛选
- 进展登记按钮
- 发起变更按钮
- 导入按钮
- 导出按钮
- 预警标识

**扩展示例**：
```vue
<template>
  <div class="resource-plan-subcontract-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>分包队伍计划管理</span>
          <div>
            <el-button type="primary" @click="handleAdd">新增</el-button>
            <el-button type="success" @click="handleImport">导入</el-button>
            <el-button type="warning" @click="handleExport">导出</el-button>
          </div>
        </div>
      </template>
      
      <!-- 搜索表单（扩展） -->
      <el-form :inline="true" class="search-form">
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="全部" value="" />
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已提交" value="SUBMITTED" />
            <el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="已完成" value="COMPLETED" />
          </el-select>
        </el-form-item>
        <!-- 其他筛选条件... -->
      </el-form>
      
      <!-- 表格（扩展） -->
      <el-table :data="tableData" v-loading="loading" border style="width: 100%">
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <!-- 其他列... -->
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="success" size="small" @click="handleProgress(row)">进展登记</el-button>
            <el-button type="warning" size="small" @click="handleChange(row)">发起变更</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <!-- 进展登记对话框 -->
    <ProgressDialog v-model:visible="progressVisible" :data="currentRow" @success="loadData" />
    
    <!-- 变更申请对话框 -->
    <ChangeDialog v-model:visible="changeVisible" :data="currentRow" @success="loadData" />
  </div>
</template>
```

#### 1.3 API 文件扩展

**文件路径**：`frontend/src/api/resourcePlanSubcontract.ts`

**现有接口**：
- `getSubcontractList()` - 查询列表
- `getSubcontractById(id)` - 根据ID查询
- `createSubcontract(data)` - 新增
- `updateSubcontract(id, data)` - 修改
- `deleteSubcontract(id)` - 删除

**需要新增的接口**：
```typescript
/**
 * 提交审批
 */
export function submitSubcontract(id: number): Promise<R<boolean>> {
  return request({
    url: `/v1/resource-plan/subcontract/${id}/submit`,
    method: 'post'
  })
}

/**
 * 登记进展
 */
export function registerProgress(id: number, data: ProgressDTO): Promise<R<boolean>> {
  return request({
    url: `/v1/resource-plan/subcontract/${id}/progress`,
    method: 'post',
    data
  })
}

/**
 * 发起变更申请
 */
export function createChange(data: ChangeDTO): Promise<R<number>> {
  return request({
    url: '/v1/resource-plan/subcontract/change',
    method: 'post',
    data
  })
}

/**
 * 导入Excel
 */
export function importExcel(file: File): Promise<R<ImportResultVO>> {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/v1/resource-plan/subcontract/import',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 导出Excel
 */
export function exportExcel(params: { projectId?: number }): Promise<Blob> {
  return request({
    url: '/v1/resource-plan/subcontract/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}
```

### 2. 劳动力需求管理前端

#### 2.1 页面结构

**页面路径**：`frontend/src/views/resource-plan/labor/`

| 文件名 | 说明 |
|--------|------|
| `index.vue` | 列表页面（已有，需扩展） |
| `detail.vue` | 详情/进展登记页面（新增） |
| `change.vue` | 变更申请页面（新增） |
| `components/ProgressDialog.vue` | 进展登记对话框组件（新增） |
| `components/WarningIndicator.vue` | 人力不足预警标识组件（新增） |

#### 2.2 列表页面扩展

**需要新增的功能**：
- 人力不足预警标识（红色/黄色）
- 实际人数与计划人数对比显示

**扩展示例**：
```vue
<el-table-column label="人力状态" width="120">
  <template #default="{ row }">
    <el-tag v-if="row.actualQuantity < row.planQuantity * 0.8" type="danger">
      人力不足
    </el-tag>
    <el-tag v-else-if="row.actualQuantity < row.planQuantity" type="warning">
      人力即将不足
    </el-tag>
    <el-tag v-else type="success">
      人力充足
    </el-tag>
  </template>
</el-table-column>
```

#### 2.3 API 文件扩展

**文件路径**：`frontend/src/api/resourcePlanLabor.ts`

**需要新增的接口**：
```typescript
/**
 * 提交审批
 */
export function submitLabor(id: number): Promise<R<boolean>> {
  return request({
    url: `/v1/resource-plan/labor/${id}/submit`,
    method: 'post'
  })
}

/**
 * 登记进展
 */
export function registerProgress(id: number, data: LaborProgressDTO): Promise<R<boolean>> {
  return request({
    url: `/v1/resource-plan/labor/${id}/progress`,
    method: 'post',
    data
  })
}

/**
 * 查询预警状态
 */
export function getWarningStatus(id: number): Promise<R<WarningStatusVO>> {
  return request({
    url: `/v1/resource-plan/labor/${id}/warning-status`,
    method: 'get'
  })
}
```

---

## 接口设计

### 1. RESTful API 设计原则

1. **URL命名**：使用名词复数形式，小写，连字符分隔
2. **HTTP方法**：
   - GET：查询资源
   - POST：创建资源
   - PUT：更新资源
   - DELETE：删除资源
3. **响应格式**：统一使用 `R<T>` 包装类
4. **错误码**：使用HTTP状态码 + 业务错误码

### 2. 分包队伍计划管理API

#### 2.1 查询分包计划列表

```
GET /api/v1/resource-plan/subcontract/list
```

**请求参数**：
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| projectId | Long | 否 | 项目ID筛选 |
| status | String | 否 | 状态筛选 |
| wbsCode | String | 否 | WBS编码（模糊搜索） |
| subcontractName | String | 否 | 分包项目名称（模糊搜索） |
| supplierCode | String | 否 | 供应商编码 |
| startDateFrom | Date | 否 | 计划开始日期起始 |
| startDateTo | Date | 否 | 计划开始日期结束 |
| pageNum | Integer | 否 | 页码（默认1） |
| pageSize | Integer | 否 | 每页数量（默认20） |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "projectId": 1001,
        "wbsCode": "WBS-001",
        "wbsName": "基础施工",
        "subcontractName": "桩基分包",
        "workContent": "桩基开挖及混凝土浇筑",
        "supplierCode": "SUP001",
        "supplierName": "XX建设工程公司",
        "planStartDate": "2026-05-01",
        "planEndDate": "2026-06-30",
        "actualStartDate": "2026-05-02",
        "actualEndDate": null,
        "status": "IN_PROGRESS",
        "approvalStatus": "APPROVED",
        "processInstanceId": "12501",
        "remark": ""
      }
    ],
    "total": 100,
    "pageNum": 1,
    "pageSize": 20
  }
}
```

#### 2.2 提交审批

```
POST /api/v1/resource-plan/subcontract/{id}/submit
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

#### 2.3 登记进展

```
POST /api/v1/resource-plan/subcontract/{id}/progress
```

**请求体示例**：
```json
{
  "actualStartDate": "2026-05-02",
  "actualEndDate": null,
  "remark": "进展正常"
}
```

#### 2.4 发起变更申请

```
POST /api/v1/resource-plan/subcontract/change
```

**请求体示例**：
```json
{
  "planId": 1,
  "changeType": "DELAY",
  "changeReason": "供应商生产延期",
  "changeDetails": {
    "oldPlanEndDate": "2026-06-30",
    "newPlanEndDate": "2026-07-15"
  }
}
```

### 3. 劳动力需求管理API

#### 3.1 查询劳动力需求列表

```
GET /api/v1/resource-plan/labor/list
```

**请求参数**：
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| projectId | Long | 否 | 项目ID筛选 |
| status | String | 否 | 状态筛选 |
| workTypeCode | String | 否 | 工种编码 |
| laborCategoryCode | String | 否 | 劳务类别编码 |
| warningStatus | String | 否 | 预警状态（NORMAL/WARNING/CRITICAL） |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "projectId": 1001,
        "wbsCode": "WBS-001",
        "workTypeCode": "WT001",
        "workTypeName": "钢筋工",
        "laborCategoryCode": "LC002",
        "laborCategoryName": "外包队伍",
        "planQuantity": 10,
        "actualQuantity": 8,
        "planStartDate": "2026-05-01",
        "planEndDate": "2026-06-30",
        "status": "IN_PROGRESS",
        "warningStatus": "WARNING"
      }
    ]
  }
}
```

#### 3.2 查询预警状态

```
GET /api/v1/resource-plan/labor/{id}/warning-status
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "warningLevel": "WARNING",
    "warningMessage": "实际人数(8) < 计划人数(10)",
    "suggestion": "建议尽快补充人力"
  }
}
```

---

## 任务分解

### 1. 任务分解（WBS）

#### 第一阶段：数据库设计与创建（Week 1，Day 1-2）

| 任务ID | 任务名称 | 预估工时 | 优先级 | 依赖 |
|--------|----------|----------|--------|------|
| T1-1 | 创建工种字典表（dict_work_type） | 2h | P0 | - |
| T1-2 | 创建劳务类别字典表（dict_labor_category） | 2h | P0 | - |
| T1-3 | 修改分包计划表（新增字段） | 2h | P0 | T1-1 |
| T1-4 | 修改劳动力计划表（新增字段） | 2h | P0 | T1-2 |
| T1-5 | 创建预警规则配置扩展 | 2h | P0 | T1-3, T1-4 |

#### 第二阶段：后端基础架构（Week 1，Day 3-4）

| 任务ID | 任务名称 | 预估工时 | 优先级 | 依赖 |
|--------|----------|----------|--------|------|
| T2-1 | 扩展分包计划Entity类 | 2h | P0 | T1-3 |
| T2-2 | 扩展劳动力计划Entity类 | 2h | P0 | T1-4 |
| T2-3 | 创建工种字典Controller/Service/Mapper | 4h | P0 | T1-1 |
| T2-4 | 创建劳务类别字典Controller/Service/Mapper | 4h | P0 | T1-2 |
| T2-5 | 扩展分包计划Service（新增方法） | 6h | P0 | T2-1 |
| T2-6 | 扩展劳动力计划Service（新增方法） | 6h | P0 | T2-2 |

#### 第三阶段：后端高级功能（Week 1，Day 5 - Week 2，Day 1）

| 任务ID | 任务名称 | 预估工时 | 优先级 | 依赖 |
|--------|----------|----------|--------|------|
| T3-1 | 实现分包计划提交审批功能 | 4h | P0 | T2-5 |
| T3-2 | 实现分包计划进展登记功能 | 4h | P0 | T2-5 |
| T3-3 | 实现分包计划变更申请功能 | 6h | P1 | T3-1 |
| T3-4 | 实现劳动力计划提交审批功能 | 4h | P0 | T2-6 |
| T3-5 | 实现劳动力计划进展登记功能 | 4h | P0 | T2-6 |
| T3-6 | 实现劳动力预警状态查询功能 | 4h | P0 | T2-6 |
| T3-7 | 实现导入导出功能 | 6h | P1 | T3-1, T3-4 |

#### 第四阶段：前端基础页面（Week 2，Day 2-3）

| 任务ID | 任务名称 | 预估工时 | 优先级 | 依赖 |
|--------|----------|----------|--------|------|
| T4-1 | 扩展分包计划列表页面（状态筛选、按钮） | 6h | P0 | T3-1 |
| T4-2 | 创建分包计划详情/进展登记页面 | 6h | P0 | T3-2 |
| T4-3 | 创建分包计划变更申请页面 | 4h | P1 | T3-3 |
| T4-4 | 扩展劳动力需求列表页面（预警标识） | 6h | P0 | T3-4 |
| T4-5 | 创建劳动力需求详情/进展登记页面 | 6h | P0 | T3-5 |
| T4-6 | 创建劳动力需求变更申请页面 | 4h | P1 | T3-6 |

#### 第五阶段：前端高级功能（Week 2，Day 4-5）

| 任务ID | 任务名称 | 预估工时 | 优先级 | 依赖 |
|--------|----------|----------|--------|------|
| T5-1 | 实现导入Excel功能 | 4h | P1 | T3-7 |
| T5-2 | 实现导出Excel功能 | 4h | P1 | T3-7 |
| T5-3 | 实现预警标识组件 | 3h | P0 | T4-4 |
| T5-4 | 扩展API文件（resourcePlanSubcontract.ts） | 3h | P0 | T3-1 |
| T5-5 | 扩展API文件（resourcePlanLabor.ts） | 3h | P0 | T3-4 |

#### 第六阶段：测试与优化（Week 3，Day 1-2）

| 任务ID | 任务名称 | 预估工时 | 优先级 | 依赖 |
|--------|----------|----------|--------|------|
| T6-1 | 单元测试编写 | 8h | P0 | T5-5 |
| T6-2 | 集成测试 | 8h | P0 | T6-1 |
| T6-3 | 性能优化 | 4h | P1 | T6-2 |
| T6-4 | 用户文档编写 | 4h | P1 | T6-3 |

### 2. 实施顺序图

```
Week 1:
  Day 1-2: [T1-1][T1-2][T1-3][T1-4][T1-5]
                 │
  Day 3-4:       └──► [T2-1][T2-2][T2-3][T2-4][T2-5][T2-6]
                           │
  Day 5:                 └──► [T3-1][T3-2][T3-3]
                                     │
Week 2:
  Day 1:                             └──► [T3-4][T3-5][T3-6][T3-7]
                                               │
  Day 2-3:                                  └──► [T4-1][T4-2][T4-3][T4-4][T4-5][T4-6]
                                                         │
  Day 4-5:                                            └──► [T5-1][T5-2][T5-3][T5-4][T5-5]
                                                                   │
Week 3:
  Day 1-2:                                                          └──► [T6-1][T6-2][T6-3][T6-4]
```

### 3. 里程碑定义

| 里程碑 | 完成标准 | 预计时间 |
|--------|----------|----------|
| M1: 数据库设计完成 | 所有表结构设计和创建脚本完成 | Week 1 Day 2 |
| M2: 后端基础架构完成 | Entity/Service/Mapper扩展完成 | Week 1 Day 4 |
| M3: 后端高级功能完成 | 审批、进展登记、变更、导入导出完成 | Week 2 Day 1 |
| M4: 前端基础页面完成 | 列表、详情、进展登记页面完成 | Week 2 Day 3 |
| M5: 前端高级功能完成 | 导入导出、预警标识、API扩展完成 | Week 2 Day 5 |
| M6: 测试完成 | 所有功能测试通过 | Week 3 Day 2 |

---

## 依赖关系

### 1. 与现有模块的集成点

| 集成点 | 说明 | 方式 | 负责人 |
|--------|------|------|--------|
| **供应商字典** | 分包计划选择供应商 | 复用现有 `supplier` 表 | 后端 |
| **WBS字典** | 分包/劳动力计划关联WBS | 复用现有 `wbs` 表 | 后端 |
| **审批流程** | 分包/劳动力计划变更审批 | 复用 Flowable 流程引擎 | 后端 |
| **预警机制** | 生成分包/劳动力预警记录 | 复用 `warning_record` 表 | 后端 |
| **消息推送** | 推送预警消息 | 复用企业微信推送接口 | 后端 |
| **用户权限** | 数据权限控制 | 复用 Spring Security | 后端 |

### 2. 依赖关系图

```
┌─────────────────────────────────────────────────────────────┐
│                   Phase 6 技术架构                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐     ┌──────────────┐                   │
│  │  数据库设计   │────►│  后端架构   │                   │
│  └──────────────┘     └──────┬───────┘                   │
│                               │                             │
│                               ▼                             │
│  ┌──────────────┐     ┌──────────────┐                   │
│  │  前端架构   │◄────│  API 设计   │                   │
│  └──────┬───────┘     └──────────────┘                   │
│         │                                               │
│         ▼                                               │
│  ┌──────────────┐                                       │
│  │  任务分解     │                                       │
│  └──────┬───────┘                                       │
│         │                                               │
│         ▼                                               │
│  ┌──────────────┐                                       │
│  │  依赖关系     │                                       │
│  └──────────────┘                                       │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│  集成点：                                                  │
│  - 供应商字典（supplier表）                                │
│  - WBS字典（wbs表）                                       │
│  - 审批流程（Flowable）                                    │
│  - 预警机制（warning_record表）                            │
│  - 消息推送（企业微信接口）                                 │
└─────────────────────────────────────────────────────────────┘
```

### 3. 风险与应对

| 风险 | 影响 | 应对措施 | 负责人 |
|------|------|----------|--------|
| Flowable流程引擎变更 | 审批流程无法复用 | 提前验证Flowable集成稳定性 | 后端 |
| 字典数据不完整 | 用户无法选择工种/劳务类别 | 提供字典初始化脚本和界面 | 后端 |
| 导入数据质量差 | 导入失败率高 | 提供详细错误提示和模板下载 | 全栈 |
| 预警规则配置错误 | 误报或漏报 | 提供预警规则配置界面和验证 | 前端 |
| 性能问题 | 列表查询慢 | 添加索引、分页、缓存 | 后端 |

---

## 附录

### A. Flowable流程引擎集成指南

#### A.1 分包计划变更审批流程定义

**流程文件**：`resources/processes/subcontract-change.bpmn20.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://www.company.com/rpw">

    <process id="subcontractChange" name="分包计划变更审批流程" isExecutable="true">
        
        <!-- 开始事件 -->
        <startEvent id="startEvent" name="变更申请发起" 
                    flowable:formKey="subcontractChangeForm" />
        
        <!-- 申请人填写变更信息 -->
        <userTask id="fillChangeInfo" name="填写变更信息" 
                  flowable:assignee="${initiator}">
            <extensionElements>
                <flowable:formProperty id="changeReason" name="变更原因" 
                                       type="string" required="true" />
                <flowable:formProperty id="changeDetails" name="变更详情" 
                                       type="string" required="true" />
            </extensionElements>
        </userTask>
        
        <!-- 审批节点 -->
        <userTask id="approval" name="审批">
            <potentialOwner>
                <resourceAssignmentExpression>
                    <formalExpression>${approver}</formalExpression>
                </resourceAssignmentExpression>
            </potentialOwner>
        </userTask>
        
        <!-- 排他网关 - 审批判断 -->
        <exclusiveGateway id="approvalGateway" name="审批结果判断" />
        
        <!-- 审批通过 -->
        <sequenceFlow id="approveFlow" sourceRef="approval" 
                      targetRef="updatePlanTask">
            <conditionExpression xsi:type="tFormalExpression">
                ${approved == true}
            </conditionExpression>
        </sequenceFlow>
        
        <!-- 审批驳回 -->
        <sequenceFlow id="rejectFlow" sourceRef="approval" 
                      targetRef="fillChangeInfo">
            <conditionExpression xsi:type="tFormalExpression">
                ${approved == false}
            </conditionExpression>
        </sequenceFlow>
        
        <!-- 更新计划数据 -->
        <serviceTask id="updatePlanTask" name="更新计划" 
                     flowable:class="com.company.rpw.process.listener.UpdateSubcontractPlanListener" />
        
        <!-- 结束事件 -->
        <endEvent id="endEvent" name="流程结束" />
        
        <!-- 流程连线 -->
        <sequenceFlow sourceRef="startEvent" targetRef="fillChangeInfo" />
        <sequenceFlow sourceRef="fillChangeInfo" targetRef="approval" />
        <sequenceFlow sourceRef="updatePlanTask" targetRef="endEvent" />
        
    </process>
</definitions>
```

#### A.2 劳动力计划变更审批流程定义

**流程文件**：`resources/processes/labor-change.bpmn20.xml`

（与分包计划变更审批流程类似，只需修改Listener类）

### B. 导入导出功能设计

#### B.1 导入模板格式

**分包队伍计划导入模板（Excel）**：

| 列号 | 列名 | 必填 | 说明 |
|------|------|------|------|
| A | 项目名称 | 是 | 必须匹配系统中的项目 |
| B | WBS编码 | 是 | 必须匹配WBS字典 |
| C | 分包项目名称 | 是 | - |
| D | 工作内容 | 否 | - |
| E | 供应商名称 | 是 | 必须匹配供应商字典 |
| F | 计划开始日期 | 是 | yyyy-MM-dd |
| G | 计划结束日期 | 是 | yyyy-MM-dd |
| H | 备注 | 否 | - |

**劳动力需求计划导入模板（Excel）**：

| 列号 | 列名 | 必填 | 说明 |
|------|------|------|------|
| A | 项目名称 | 是 | 必须匹配系统中的项目 |
| B | WBS编码 | 是 | 必须匹配WBS字典 |
| C | 工种 | 是 | 必须匹配工种字典 |
| D | 劳务类别 | 是 | 必须匹配劳务类别字典 |
| E | 计划人数 | 是 | 正整数 |
| F | 计划开始日期 | 是 | yyyy-MM-dd |
| G | 计划结束日期 | 是 | yyyy-MM-dd |
| H | 备注 | 否 | - |

#### B.2 导出格式

复用现有导出基础设施，导出字段与列表页保持一致。

### C. 预警规则配置

#### C.1 分包队伍计划预警规则

| 预警类型 | 触发条件 | 预警级别 | 消息模板 |
|----------|----------|----------|------------|
| 未按时进场 | 实际开始日期 > 计划开始日期 | 红色 | "分包队伍{name}未按时进场，计划开始日期{planDate}，实际开始日期{actualDate}" |
| 即将到期 | 计划结束日期 - 当前日期 <= 7天 | 黄色 | "分包队伍{name}即将到期，计划结束日期为{endDate}" |
| 超期未完工 | 当前日期 > 计划结束日期 且 实际结束日期为空 | 红色 | "分包队伍{name}超期未完工，计划结束日期为{endDate}" |

#### C.2 劳动力需求预警规则

| 预警类型 | 触发条件 | 预警级别 | 消息模板 |
|----------|----------|----------|------------|
| 人力不足 | 实际人数 < 计划人数 × 0.8 | 红色 | "工种{workType}人力不足，计划{plan}人，实际{actual}人" |
| 人力即将不足 | 实际人数 < 计划人数 | 黄色 | "工种{workType}人力即将不足，计划{plan}人，实际{actual}人" |
| 未按时到位 | 实际开始日期 > 计划开始日期 | 红色 | "工种{workType}未按时到位，计划开始日期{planDate}，实际开始日期{actualDate}" |

### D. 参考文档

| 文档名称 | 路径 | 说明 |
|----------|------|------|
| Phase 6 产品需求文档 | `docs/phase6-prd.md` | 详细产品需求 |
| Phase 5 技术方案设计 | `docs/phase5-design.md` | Flowable集成方案 |
| 现有实体类：ResourcePlanSubcontract.java | `backend/.../entity/ResourcePlanSubcontract.java` | 现有实体类 |
| 现有实体类：ResourcePlanLabor.java | `backend/.../entity/ResourcePlanLabor.java` | 现有实体类 |
| Flowable官方文档 | https://www.flowable.com/open-source/docs | 流程引擎文档 |
| MyBatis-Plus官方文档 | https://baomidou.com/guide/ | ORM框架文档 |
| Vue 3官方文档 | https://vuejs.org/ | 前端框架文档 |
| Element Plus官方文档 | https://element-plus.org/ | UI组件库文档 |

---

**文档版本**：v1.0  
**编写日期**：2026-05-10  
**编写人**：高见远（Gao）  
**审核状态**：待审核
