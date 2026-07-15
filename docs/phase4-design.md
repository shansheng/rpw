# 项目资源计划预警系统 - Phase 4 技术方案

## 目录
1. [项目背景](#项目背景)
2. [数据库表结构设计](#数据库表结构设计)
3. [后端API设计](#后端api设计)
4. [前端页面设计](#前端页面设计)
5. [实施优先级建议](#实施优先级建议)
6. [技术架构说明](#技术架构说明)
7. [附录](#附录)

---

## 项目背景

**系统概述**：
项目资源计划管理系统当前已完成 8 种资源计划的管理功能（材料、设备、五金、周转材、办公用品、安全物资、分包、劳动力），Phase 4 需要增加预警功能，实现：
- 自动监控资源计划执行情况
- 当实际值与计划值偏差超出阈值时触发预警
- 通过企业微信推送预警消息
- 在预警看板展示预警信息

**预警场景示例**：
| 资源类型 | 计划字段 | 实际字段 | 预警规则示例 |
|---------|---------|---------|------------|
| 材料 | budgetQuantity（预算数量） | actualArrivalDate（实际到场日期） | 实际到场率 < 80% 触发预警 |
| 设备 | budgetQuantity | 实际进场数量 | 实际进场率 < 90% 触发预警 |
| 劳动力 | budgetQuantity | 实际人数 | 人员到位率 < 85% 触发预警 |

---

## 数据库表结构设计

### 2.1 预警规则表（warning_rule）

**表说明**：配置各类资源的预警规则，支持为不同资源类型、不同项目设置差异化的预警阈值。

| 字段名 | 类型 | 长度 | 可为空 | 默认值 | 说明 |
|-------|------|------|--------|--------|------|
| id | bigint | 20 | NOT NULL | AUTO_INCREMENT | 主键ID |
| rule_name | varchar | 100 | NOT NULL | - | 规则名称（如"材料到场率预警"） |
| resource_type | varchar | 50 | NOT NULL | - | 资源类型（MATERIAL/EQUIPMENT/HARDWARE/CIRCULATION/OFFICE/SAFETY/SUBCONTRACT/LABOR） |
| project_id | bigint | 20 | NULL | NULL | 项目ID（NULL表示适用于所有项目） |
| threshold_type | varchar | 20 | NOT NULL | - | 阈值类型（RATE-比率/ DATE-日期/ QUANTITY-数量） |
| warning_threshold | decimal | 5,2 | NOT NULL | - | 预警阈值（如80.00表示80%） |
| compare_field | varchar | 50 | NOT NULL | - | 对比字段（如budgetQuantity） |
| actual_field | varchar | 50 | NOT NULL | - | 实际值字段（如actualArrivalDate） |
| warning_level | varchar | 20 | NOT NULL | - | 预警等级（GENERAL-一般/ IMPORTANT-重要/ URGENT-紧急） |
| check_frequency | varchar | 20 | NOT NULL | - | 检查频率（REALTIME-实时/ DAILY-每日/ WEEKLY-每周） |
| enabled | tinyint | 1 | NOT NULL | 1 | 是否启用（0-禁用，1-启用） |
| notify_wecom | tinyint | 1 | NOT NULL | 1 | 是否推送企业微信通知（0-否，1-是） |
| notify_users | varchar | 500 | NULL | NULL | 通知用户ID列表（逗号分隔） |
| description | varchar | 500 | NULL | NULL | 规则描述 |
| create_time | datetime | - | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| update_time | datetime | - | NOT NULL | CURRENT_TIMESTAMP | 更新时间 |
| deleted | int | 1 | NOT NULL | 0 | 逻辑删除标记（0-未删除，1-已删除） |

**索引设计**：
- PRIMARY KEY (`id`)
- INDEX `idx_resource_type` (`resource_type`)
- INDEX `idx_project_id` (`project_id`)
- INDEX `idx_enabled` (`enabled`)

**建表SQL**：
```sql
CREATE TABLE `warning_rule` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `resource_type` varchar(50) NOT NULL COMMENT '资源类型',
  `project_id` bigint(20) DEFAULT NULL COMMENT '项目ID（NULL表示所有项目）',
  `threshold_type` varchar(20) NOT NULL COMMENT '阈值类型（RATE/DATE/QUANTITY）',
  `warning_threshold` decimal(5,2) NOT NULL COMMENT '预警阈值',
  `compare_field` varchar(50) NOT NULL COMMENT '对比字段',
  `actual_field` varchar(50) NOT NULL COMMENT '实际值字段',
  `warning_level` varchar(20) NOT NULL COMMENT '预警等级（GENERAL/IMPORTANT/URGENT）',
  `check_frequency` varchar(20) NOT NULL DEFAULT 'DAILY' COMMENT '检查频率',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `notify_wecom` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否推送企业微信',
  `notify_users` varchar(500) DEFAULT NULL COMMENT '通知用户ID列表',
  `description` varchar(500) DEFAULT NULL COMMENT '规则描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  INDEX `idx_resource_type` (`resource_type`),
  INDEX `idx_project_id` (`project_id`),
  INDEX `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警规则表';
```

---

### 2.2 预警记录表（warning_record）

**表说明**：存储触发的预警记录，包括预警详情、处理状态等。

| 字段名 | 类型 | 长度 | 可为空 | 默认值 | 说明 |
|-------|------|------|--------|--------|------|
| id | bigint | 20 | NOT NULL | AUTO_INCREMENT | 主键ID |
| rule_id | bigint | 20 | NOT NULL | - | 预警规则ID |
| resource_type | varchar | 50 | NOT NULL | - | 资源类型 |
| project_id | bigint | 20 | NOT NULL | - | 项目ID |
| resource_id | bigint | 20 | NOT NULL | - | 资源计划ID（关联各资源表的主键） |
| warning_level | varchar | 20 | NOT NULL | - | 预警等级（GENERAL/IMPORTANT/URGENT） |
| warning_message | varchar | 1000 | NOT NULL | - | 预警消息内容 |
| plan_value | decimal | 15,2 | NULL | NULL | 计划值 |
| actual_value | decimal | 15,2 | NULL | NULL | 实际值 |
| deviation_rate | decimal | 5,2 | NULL | NULL | 偏差率（百分比） |
| status | varchar | 20 | NOT NULL | 'PENDING' | 处理状态（PENDING-待处理/ PROCESSING-处理中/ RESOLVED-已解决/ IGNORED-已忽略） |
| handled_by | bigint | 20 | NULL | NULL | 处理人ID |
| handle_time | datetime | - | NULL | NULL | 处理时间 |
| handle_remark | varchar | 500 | NULL | NULL | 处理备注 |
| notify_status | varchar | 20 | NOT NULL | 'NOT_SENT' | 通知状态（NOT_SENT-未发送/SENT-已发送/FAILED-发送失败） |
| wecom_msg_id | varchar | 100 | NULL | NULL | 企业微信消息ID |
| triggered_time | datetime | - | NOT NULL | CURRENT_TIMESTAMP | 触发时间 |
| create_time | datetime | - | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| update_time | datetime | - | NOT NULL | CURRENT_TIMESTAMP | 更新时间 |
| deleted | int | 1 | NOT NULL | 0 | 逻辑删除标记 |

**索引设计**：
- PRIMARY KEY (`id`)
- INDEX `idx_rule_id` (`rule_id`)
- INDEX `idx_resource_type` (`resource_type`)
- INDEX `idx_project_id` (`project_id`)
- INDEX `idx_status` (`status`)
- INDEX `idx_triggered_time` (`triggered_time`)
- INDEX `idx_notify_status` (`notify_status`)

**建表SQL**：
```sql
CREATE TABLE `warning_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_id` bigint(20) NOT NULL COMMENT '预警规则ID',
  `resource_type` varchar(50) NOT NULL COMMENT '资源类型',
  `project_id` bigint(20) NOT NULL COMMENT '项目ID',
  `resource_id` bigint(20) NOT NULL COMMENT '资源计划ID',
  `warning_level` varchar(20) NOT NULL COMMENT '预警等级',
  `warning_message` varchar(1000) NOT NULL COMMENT '预警消息内容',
  `plan_value` decimal(15,2) DEFAULT NULL COMMENT '计划值',
  `actual_value` decimal(15,2) DEFAULT NULL COMMENT '实际值',
  `deviation_rate` decimal(5,2) DEFAULT NULL COMMENT '偏差率（百分比）',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态',
  `handled_by` bigint(20) DEFAULT NULL COMMENT '处理人ID',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `handle_remark` varchar(500) DEFAULT NULL COMMENT '处理备注',
  `notify_status` varchar(20) NOT NULL DEFAULT 'NOT_SENT' COMMENT '通知状态',
  `wecom_msg_id` varchar(100) DEFAULT NULL COMMENT '企业微信消息ID',
  `triggered_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '触发时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  INDEX `idx_rule_id` (`rule_id`),
  INDEX `idx_resource_type` (`resource_type`),
  INDEX `idx_project_id` (`project_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_triggered_time` (`triggered_time`),
  INDEX `idx_notify_status` (`notify_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警记录表';
```

---

## 后端API设计

### 3.1 预警规则管理API

#### 3.1.1 查询预警规则列表
```
GET /api/v1/warning/rule/list
```

**请求参数**：
| 参数名 | 类型 | 必填 | 说明 |
|-------|------|------|------|
| resourceType | String | 否 | 资源类型筛选 |
| projectId | Long | 否 | 项目ID筛选 |
| enabled | Integer | 否 | 是否启用筛选 |
| pageNum | Integer | 否 | 页码（默认1） |
| pageSize | Integer | 否 | 每页数量（默认10） |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "ruleName": "材料到场率预警",
        "resourceType": "MATERIAL",
        "projectId": null,
        "thresholdType": "RATE",
        "warningThreshold": 80.00,
        "compareField": "budgetQuantity",
        "actualField": "actualArrivalDate",
        "warningLevel": "IMPORTANT",
        "checkFrequency": "DAILY",
        "enabled": 1,
        "notifyWecom": 1,
        "notifyUsers": "101,102,103",
        "description": "材料实际到场率低于80%时触发预警",
        "createTime": "2026-05-10 10:00:00",
        "updateTime": "2026-05-10 10:00:00"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 10
  },
  "timestamp": 1715323200000
}
```

#### 3.1.2 根据ID查询预警规则
```
GET /api/v1/warning/rule/{id}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "ruleName": "材料到场率预警",
    "resourceType": "MATERIAL",
    "projectId": null,
    "thresholdType": "RATE",
    "warningThreshold": 80.00,
    "compareField": "budgetQuantity",
    "actualField": "actualArrivalDate",
    "warningLevel": "IMPORTANT",
    "checkFrequency": "DAILY",
    "enabled": 1,
    "notifyWecom": 1,
    "notifyUsers": "101,102,103",
    "description": "材料实际到场率低于80%时触发预警"
  },
  "timestamp": 1715323200000
}
```

#### 3.1.3 新增预警规则
```
POST /api/v1/warning/rule
```

**请求体示例**：
```json
{
  "ruleName": "材料到场率预警",
  "resourceType": "MATERIAL",
  "projectId": null,
  "thresholdType": "RATE",
  "warningThreshold": 80.00,
  "compareField": "budgetQuantity",
  "actualField": "actualArrivalDate",
  "warningLevel": "IMPORTANT",
  "checkFrequency": "DAILY",
  "enabled": 1,
  "notifyWecom": 1,
  "notifyUsers": "101,102,103",
  "description": "材料实际到场率低于80%时触发预警"
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": true,
  "timestamp": 1715323200000
}
```

#### 3.1.4 修改预警规则
```
PUT /api/v1/warning/rule/{id}
```

**请求体示例**：
```json
{
  "ruleName": "材料到场率预警（修改）",
  "warningThreshold": 75.00,
  "enabled": 1
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": true,
  "timestamp": 1715323200000
}
```

#### 3.1.5 删除预警规则
```
DELETE /api/v1/warning/rule/{id}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": true,
  "timestamp": 1715323200000
}
```

#### 3.1.6 启用/禁用预警规则
```
PUT /api/v1/warning/rule/{id}/toggle
```

**请求参数**：
| 参数名 | 类型 | 必填 | 说明 |
|-------|------|------|------|
| enabled | Integer | 是 | 0-禁用，1-启用 |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": true,
  "timestamp": 1715323200000
}
```

---

### 3.2 预警记录管理API

#### 3.2.1 查询预警记录列表
```
GET /api/v1/warning/record/list
```

**请求参数**：
| 参数名 | 类型 | 必填 | 说明 |
|-------|------|------|------|
| resourceType | String | 否 | 资源类型筛选 |
| projectId | Long | 否 | 项目ID筛选 |
| warningLevel | String | 否 | 预警等级筛选 |
| status | String | 否 | 处理状态筛选 |
| startTime | String | 否 | 触发开始时间（yyyy-MM-dd HH:mm:ss） |
| endTime | String | 否 | 触发结束时间（yyyy-MM-dd HH:mm:ss） |
| pageNum | Integer | 否 | 页码（默认1） |
| pageSize | Integer | 否 | 每页数量（默认10） |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "ruleId": 1,
        "resourceType": "MATERIAL",
        "projectId": 1001,
        "resourceId": 50,
        "warningLevel": "IMPORTANT",
        "warningMessage": "材料【钢筋HRB400】到场率偏低，计划100吨，实际到场60吨，到场率仅60%",
        "planValue": 100.00,
        "actualValue": 60.00,
        "deviationRate": -40.00,
        "status": "PENDING",
        "handledBy": null,
        "handleTime": null,
        "handleRemark": null,
        "notifyStatus": "SENT",
        "wecomMsgId": "msg_123456",
        "triggeredTime": "2026-05-10 09:30:00",
        "createTime": "2026-05-10 09:30:00"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 10
  },
  "timestamp": 1715323200000
}
```

#### 3.2.2 根据ID查询预警记录详情
```
GET /api/v1/warning/record/{id}
```

**响应示例**：同列表中的单个记录结构

#### 3.2.3 处理预警记录
```
PUT /api/v1/warning/record/{id}/handle
```

**请求参数**：
| 参数名 | 类型 | 必填 | 说明 |
|-------|------|------|------|
| status | String | 是 | 处理状态（PROCESSING/RESOLVED/IGNORED） |
| handleRemark | String | 否 | 处理备注 |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": true,
  "timestamp": 1715323200000
}
```

#### 3.2.4 批量处理预警记录
```
PUT /api/v1/warning/record/batch-handle
```

**请求体示例**：
```json
{
  "ids": [1, 2, 3],
  "status": "RESOLVED",
  "handleRemark": "已督促供应商加快送货"
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": true,
  "timestamp": 1715323200000
}
```

#### 3.2.5 获取预警统计信息
```
GET /api/v1/warning/record/statistics
```

**请求参数**：
| 参数名 | 类型 | 必填 | 说明 |
|-------|------|------|------|
| projectId | Long | 否 | 项目ID（NULL表示所有项目） |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalCount": 156,
    "pendingCount": 23,
    "processingCount": 15,
    "resolvedCount": 110,
    "ignoredCount": 8,
    "urgentCount": 5,
    "importantCount": 45,
    "generalCount": 106,
    "todayTriggeredCount": 12
  },
  "timestamp": 1715323200000
}
```

---

### 3.3 预警检查API（定时任务/手动触发）

#### 3.3.1 手动触发预警检查
```
POST /api/v1/warning/check
```

**请求参数**：
| 参数名 | 类型 | 必填 | 说明 |
|-------|------|------|------|
| resourceType | String | 否 | 指定资源类型（NULL表示检查所有类型） |
| projectId | Long | 否 | 指定项目（NULL表示检查所有项目） |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "checkedCount": 450,
    "warningTriggeredCount": 12
  },
  "timestamp": 1715323200000
}
```

---

## 前端页面设计

### 4.1 预警看板页面（WarningDashboard.vue）

**页面路径**：`frontend/src/views/warning/dashboard/index.vue`

**功能模块**：
1. **统计卡片区域**：展示总预警数、待处理、处理中、已解决数量
2. **预警等级分布图表**：饼图展示一般/重要/紧急占比
3. **预警趋势图表**：折线图展示近7天预警趋势
4. **预警记录列表**：可筛选、分页、批量操作
5. **快速操作**：处理、忽略、查看详情

**页面布局示意**：
```
┌─────────────────────────────────────────────────────────────┐
│  预警看板                        [手动检查] [规则配置]        │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐ │
│  │ 总预警   │  │ 待处理   │  │ 处理中   │  │ 已解决   │ │
│  │  156    │  │   23     │  │   15     │  │  110    │ │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘ │
├─────────────────────────────────────────────────────────────┤
│  [全部] [材料] [设备] [劳动力] ...  [待处理] [处理中] [已解决]│
├─────────────────────────────────────────────────────────────┤
│  等级  │ 资源类型 │ 项目名 │ 预警消息        │ 状态  │ 操作 │
│  紧急  │ 材料    │ 项目A  │ 钢筋到场率仅60% │ 待处理│ 处理 │
│  重要  │ 劳动力  │ 项目B  │ 人员到位率70%  │ 处理中│ 查看 │
└─────────────────────────────────────────────────────────────┘
```

**关键实现要点**：
- 使用 Element Plus 的 `el-card`, `el-table`, `el-tag` 组件
- 使用 `echarts` 或 `chart.js` 绘制统计图表
- 支持按资源类型、预警等级、处理状态筛选
- 支持批量选择和处理

---

### 4.2 预警规则配置页面（WarningRule.vue）

**页面路径**：`frontend/src/views/warning/rule/index.vue`

**功能模块**：
1. **规则列表**：展示所有预警规则，支持启用/禁用切换
2. **新增/编辑规则对话框**：表单配置规则参数
3. **规则测试**：测试规则是否能正确触发预警

**表单字段**：
| 字段 | 组件类型 | 说明 |
|-----|---------|------|
| 规则名称 | el-input | 文本输入 |
| 资源类型 | el-select | 下拉选择（8种资源类型） |
| 适用项目 | el-select | 下拉选择（支持"全部项目"） |
| 阈值类型 | el-select | RATE/DATE/QUANTITY |
| 预警阈值 | el-input-number | 数值输入 |
| 预警等级 | el-select | 一般/重要/紧急 |
| 检查频率 | el-select | 实时/每日/每周 |
| 是否启用 | el-switch | 开关 |
| 推送企业微信 | el-switch | 开关 |
| 通知用户 | el-select | 多选 |
| 规则描述 | el-input textarea | 多行文本 |

---

### 4.3 前端API接口封装

**文件路径**：`frontend/src/api/warning.ts`

```typescript
import request from '@/utils/request'

// 预警规则相关
export function getWarningRuleList(params: any) {
  return request({
    url: '/api/v1/warning/rule/list',
    method: 'get',
    params
  })
}

export function getWarningRuleById(id: number) {
  return request({
    url: `/api/v1/warning/rule/${id}`,
    method: 'get'
  })
}

export function createWarningRule(data: any) {
  return request({
    url: '/api/v1/warning/rule',
    method: 'post',
    data
  })
}

export function updateWarningRule(id: number, data: any) {
  return request({
    url: `/api/v1/warning/rule/${id}`,
    method: 'put',
    data
  })
}

export function deleteWarningRule(id: number) {
  return request({
    url: `/api/v1/warning/rule/${id}`,
    method: 'delete'
  })
}

export function toggleWarningRule(id: number, enabled: number) {
  return request({
    url: `/api/v1/warning/rule/${id}/toggle`,
    method: 'put',
    params: { enabled }
  })
}

// 预警记录相关
export function getWarningRecordList(params: any) {
  return request({
    url: '/api/v1/warning/record/list',
    method: 'get',
    params
  })
}

export function getWarningRecordById(id: number) {
  return request({
    url: `/api/v1/warning/record/${id}`,
    method: 'get'
  })
}

export function handleWarningRecord(id: number, data: any) {
  return request({
    url: `/api/v1/warning/record/${id}/handle`,
    method: 'put',
    data
  })
}

export function batchHandleWarningRecord(data: any) {
  return request({
    url: '/api/v1/warning/record/batch-handle',
    method: 'put',
    data
  })
}

export function getWarningStatistics(projectId?: number) {
  return request({
    url: '/api/v1/warning/record/statistics',
    method: 'get',
    params: { projectId }
  })
}

// 预警检查
export function triggerWarningCheck(params: any) {
  return request({
    url: '/api/v1/warning/check',
    method: 'post',
    params
  })
}
```

---

## 实施优先级建议

### 5.1 实施阶段划分

#### 第一阶段（Week 1-2）：基础功能
**优先级：P0（核心功能）**

1. **数据库表创建**
   - 创建 `warning_rule` 表
   - 创建 `warning_record` 表
   - 添加索引和初始数据

2. **后端实体类和Mapper**
   - 创建 `WarningRule.java` 实体类
   - 创建 `WarningRecord.java` 实体类
   - 创建对应的 Mapper 接口

3. **预警规则CRUD API**
   - 实现预警规则的增删改查接口
   - 单元测试

4. **预警检查核心逻辑**
   - 实现预警检查服务 `WarningCheckService`
   - 支持按规则对比计划值与实际值
   - 生成预警记录

**交付物**：
- 数据库表结构
- 后端CRUD接口（通过Postman测试）
- 预警检查逻辑（可手动触发）

---

#### 第二阶段（Week 3-4）：预警记录和处理
**优先级：P1（重要功能）**

1. **预警记录API**
   - 实现预警记录查询接口（支持多条件筛选）
   - 实现预警处理接口（单条/批量）
   - 实现预警统计接口

2. **定时任务集成**
   - 使用 Spring Boot 的 `@Scheduled` 注解
   - 实现每日自动检查
   - 记录任务执行日志

3. **前端预警看板页面**
   - 预警记录列表页
   - 统计卡片和图表
   - 筛选和分页功能

**交付物**：
- 预警记录管理功能完整
- 定时任务自动运行
- 前端看板页面可展示数据

---

#### 第三阶段（Week 5-6）：企业微信通知
**优先级：P1（重要功能）**

1. **企业微信消息推送**
   - 集成企业微信API
   - 实现消息发送服务
   - 预警触发时自动推送

2. **通知状态管理**
   - 记录消息发送状态
   - 失败重试机制
   - 消息ID关联

3. **前端规则配置页面**
   - 预警规则配置界面
   - 启用/禁用切换
   - 规则测试功能

**交付物**：
- 企业微信通知成功发送
- 规则配置页面完整可用

---

#### 第四阶段（Week 7-8）：优化和测试
**优先级：P2（优化功能）**

1. **性能优化**
   - 预警检查SQL优化
   - 批量处理优化
   - 缓存机制（如需要）

2. **异常处理和日志**
   - 完善的异常处理
   - 操作日志记录
   - 预警检查报告

3. **完整测试**
   - 集成测试
   - 压力测试
   - 用户验收测试

**交付物**：
- 系统稳定运行
- 测试报告
- 用户手册

---

### 5.2 风险和建议

**技术风险**：
1. **企业微信API限制**：消息发送频率有限制，需要合理设计推送策略
   - **建议**：合并同类预警，避免频繁推送

2. **预警检查性能**：大量数据检查时可能耗时较长
   - **建议**：采用分页查询，异步执行检查任务

3. **误报问题**：预警规则配置不当可能导致大量误报
   - **建议**：提供规则测试功能，先在测试环境验证

**实施建议**：
1. 先实现核心检查和记录功能，再集成通知
2. 为每个资源类型分别测试预警规则
3. 预留规则扩展能力，方便后续增加新预警维度

---

## 技术架构说明

### 6.1 技术栈
- **后端**：Spring Boot 3.2 + MyBatis-Plus + MySQL 8.0
- **前端**：Vue 3 + TypeScript + Element Plus + ECharts
- **定时任务**：Spring Scheduled / Quartz（如需更复杂的调度）
- **消息通知**：企业微信API / 邮件 / 短信（可扩展）

### 6.2 包结构约定

**后端包结构**：
```
com.company.rpw
├── controller
│   ├── WarningRuleController.java
│   └── WarningRecordController.java
├── service
│   ├── WarningRuleService.java
│   ├── WarningRecordService.java
│   ├── impl
│   │   ├── WarningRuleServiceImpl.java
│   │   ├── WarningRecordServiceImpl.java
│   │   └── WarningCheckServiceImpl.java
│   └── WarningCheckService.java
├── entity
│   ├── WarningRule.java
│   └── WarningRecord.java
├── mapper
│   ├── WarningRuleMapper.java
│   └── WarningRecordMapper.java
├── dto
│   ├── WarningRuleDTO.java
│   └── WarningRecordDTO.java
├── vo
│   ├── WarningRuleVO.java
│   └── WarningStatisticsVO.java
└── task
    └── WarningScheduleTask.java
```

### 6.3 关键代码示例

#### 预警规则实体类（WarningRule.java）
```java
package com.company.rpw.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.company.rpw.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

/**
 * 预警规则实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("warning_rule")
public class WarningRule extends BaseEntity {

    /** 规则名称 */
    private String ruleName;

    /** 资源类型 */
    private String resourceType;

    /** 项目ID（NULL表示所有项目） */
    private Long projectId;

    /** 阈值类型（RATE/DATE/QUANTITY） */
    private String thresholdType;

    /** 预警阈值 */
    private BigDecimal warningThreshold;

    /** 对比字段 */
    private String compareField;

    /** 实际值字段 */
    private String actualField;

    /** 预警等级（GENERAL/IMPORTANT/URGENT） */
    private String warningLevel;

    /** 检查频率（REALTIME/DAILY/WEEKLY） */
    private String checkFrequency;

    /** 是否启用（0-禁用，1-启用） */
    private Integer enabled;

    /** 是否推送企业微信（0-否，1-是） */
    private Integer notifyWecom;

    /** 通知用户ID列表（逗号分隔） */
    private String notifyUsers;

    /** 规则描述 */
    private String description;
}
```

#### 预警检查服务（WarningCheckService.java 核心逻辑）
```java
@Service
@RequiredArgsConstructor
public class WarningCheckServiceImpl implements WarningCheckService {

    private final WarningRuleMapper warningRuleMapper;
    private final WarningRecordMapper warningRecordMapper;
    private final ResourcePlanMaterialService materialService;
    // ... 其他资源服务

    @Override
    public int checkAndTriggerWarnings(Long projectId, String resourceType) {
        // 1. 查询启用的预警规则
        List<WarningRule> rules = warningRuleMapper.selectEnabledRules(projectId, resourceType);

        int warningCount = 0;

        // 2. 遍历规则进行检查
        for (WarningRule rule : rules) {
            // 3. 根据资源类型查询对应的资源计划数据
            List<?> resourceList = getResourceListByType(rule.getResourceType(), rule.getProjectId());

            // 4. 对比计划值与实际值
            for (Object resource : resourceList) {
                if (checkDeviation(resource, rule)) {
                    // 5. 生成预警记录
                    createWarningRecord(resource, rule);
                    warningCount++;
                }
            }
        }

        return warningCount;
    }

    private boolean checkDeviation(Object resource, WarningRule rule) {
        // 根据实际字段和阈值类型进行偏差计算
        // 返回是否触发预警
        // ...
        return false;
    }
}
```

---

## 附录

### A. 资源类型字段映射表

**说明**：预警规则配置时，需根据资源类型选择对应的计划字段（compare_field）和实际字段（actual_field）。

| 资源类型 | 数据库表 | 计划字段（compare_field） | 实际字段（actual_field） | 预警示例 |
|---------|---------|------------------------|---------------------|---------|
| MATERIAL（材料） | resource_plan_material | `budget_quantity`（预算数量） | `actual_arrival_date`（实际到场日期） | 到场日期延迟触发预警 |
| EQUIPMENT（设备） | resource_plan_equipment | `budget_quantity`（预算数量） | `actual_arrival_date`（实际到场日期） | 到场日期延迟触发预警 |
| HARDWARE（五金） | resource_plan_hardware | `budget_quantity`（预算数量） | `actual_arrival_date`（实际到场日期） | 到场日期延迟触发预警 |
| CIRCULATION（周转材） | resource_plan_circulation | `budget_quantity`（预算数量） | `actual_arrival_date`（实际到场日期） | 到场日期延迟触发预警 |
| OFFICE（办公用品） | resource_plan_office | `budget_quantity`（预算数量） | `actual_arrival_date`（实际到场日期） | 到场日期延迟触发预警 |
| SAFETY（安全物资） | resource_plan_safety | `budget_quantity`（预算数量） | `actual_arrival_date`（实际到场日期） | 到场日期延迟触发预警 |
| LABOR（劳动力） | resource_plan_labor | `plan_quantity`（计划人数） | `actual_quantity`（实际人数） | 人员到位率不足触发预警 |
| SUBCONTRACT（分包） | resource_plan_subcontract | `plan_start_date`, `plan_end_date`（计划工期） | `actual_start_date`, `actual_end_date`（实际工期） | 工期延误触发预警 |

**字段使用说明**：
1. **数量对比型**（材料、设备、五金、周转材、办公用品、安全物资）：
   - 通过 `actual_arrival_date` 判断是否已到场
   - 若已填写实际到场日期，对比计划数量与实际到场数量（需额外记录实际数量）
   - 若未填写，检查是否超出计划到场日期（日期预警）

2. **人数对比型**（劳动力）：
   - 直接对比 `plan_quantity` 与 `actual_quantity`
   - 计算到场率：`actual_quantity / plan_quantity * 100%`

3. **日期对比型**（分包）：
   - 对比计划开始/结束日期与实际开始/结束日期
   - 计算工期偏差天数

**预警规则配置示例**：
```json
{
  "resourceType": "MATERIAL",
  "thresholdType": "RATE",
  "warningThreshold": 80.00,
  "compareField": "budget_quantity",
  "actualField": "actual_arrival_date"
}
```

---

### B. 资源类型枚举
```java
public enum ResourceType {
    MATERIAL("MATERIAL", "材料"),
    EQUIPMENT("EQUIPMENT", "设备"),
    HARDWARE("HARDWARE", "五金"),
    CIRCULATION("CIRCULATION", "周转材"),
    OFFICE("OFFICE", "办公用品"),
    SAFETY("SAFETY", "安全物资"),
    SUBCONTRACT("SUBCONTRACT", "分包"),
    LABOR("LABOR", "劳动力");

    private String code;
    private String name;
    // ...
}
```

---

### C. 预警等级枚举
```java
public enum WarningLevel {
    GENERAL("GENERAL", "一般", 1),
    IMPORTANT("IMPORTANT", "重要", 2),
    URGENT("URGENT", "紧急", 3);

    private String code;
    private String name;
    private int priority;
    // ...
}
```

---

### D. 参考文档
- MyBatis-Plus 官方文档：https://baomidou.com/
- Element Plus 官方文档：https://element-plus.org/
- 企业微信API文档：https://developer.work.weixin.qq.com/document/

---

### E. 包名确认

**包名**：`com.company.rpw`

已确认与实际项目包名一致。所有后端代码均位于此包下：
- `com.company.rpw.entity`
- `com.company.rpw.controller`
- `com.company.rpw.service`
- `com.company.rpw.mapper`

---

**文档版本**：v1.1
**编写日期**：2026-05-10
**编写人**：backend-architect
**审核状态**：已根据反馈修正（Spring Boot版本、字段映射表、包名确认）
