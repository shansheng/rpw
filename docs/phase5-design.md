# 项目资源计划预警系统 - Phase 5 技术方案设计

## 文档信息

| 版本 | 日期 | 作者 | 说明 |
|------|------|------|------|
| 1.0 | 2026-05-10 | 高见远（Gao） | 初始版本 |

---

## 目录

1. [项目背景](#项目背景)
2. [Flowable流程引擎集成方案](#flowable流程引擎集成方案)
3. [审批流程设计](#审批流程设计)
4. [在线流程设计器集成方案](#在线流程设计器集成方案)
5. [流程实例后台干预功能设计](#流程实例后台干预功能设计)
6. [数据库表结构设计](#数据库表结构设计)
7. [后端API设计](#后端api设计)
8. [前端页面设计](#前端页面设计)
9. [任务分解和实施顺序](#任务分解和实施顺序)
10. [附录](#附录)

---

## 项目背景

### 当前系统状态

Phase 4 已完成预警功能模块，包括：
- 预警规则配置管理
- 预警记录生成与处理
- 企业微信消息推送
- 预警看板展示

### Phase 5 目标

根据PRD需求，Phase 5 需要实现：
1. **物资计划变更申请与审批流程**（P0-007，32h）
2. **Flowable流程引擎集成**（P1-001，48h）
3. **在线流程设计器**（P1-001，48h）
4. **流程实例后台干预功能**（P1-004，16h）

### 核心业务场景

```
项目级用户            公司级用户
    │                    │
    ├── 发起变更申请 ───►│
    │   (填写变更原因)   │── 审批通过 ──► 更新计划
    │                    │── 审批驳回 ──► 返回修改
    │                    │
    ├── 查看审批进度 ◄──┤
    │                    │
    └────────────────────┘
```

---

## Flowable流程引擎集成方案

### 1. 技术选型确认

| 组件 | 版本 | 说明 |
|------|------|------|
| Flowable | 7.0.0 | 已包含在pom.xml中 |
| Spring Boot | 3.1.6 | 兼容Flowable 7.x |
| MySQL | 8.0 | Flowable自动建表 |

### 2. Flowable依赖配置

**pom.xml已有依赖**：
```xml
<dependency>
    <groupId>org.flowable</groupId>
    <artifactId>flowable-spring-boot-starter</artifactId>
    <version>7.0.0</version>
</dependency>
```

### 3. 应用配置文件

**application.yml 添加Flowable配置**：

```yaml
spring:
  flowable:
    # 自动部署流程定义
    process-definition-location-prefix: classpath*:processes/
    process-definition-location-suffixes: .bpmn20.xml,.bpmn
    # 数据库配置
    database-schema-update: true
    # 历史数据级别（none/activity/audit/full）
    history-level: audit
    # 关闭定时任务（如有需要可开启）
    async-executor-activate: true
    # JPA配置
    jpa-enable-process-engine: true
```

### 4. Flowable数据库表说明

Flowable会自动创建以下表（约60+张）：

| 表前缀 | 说明 |
|--------|------|
| ACT_RE_* | 仓库数据（Repository），存储流程定义和部署信息 |
| ACT_RU_* | 运行时数据（Runtime），存储流程实例、任务等运行中数据 |
| ACT_HI_* | 历史数据（History），存储历史流程实例、任务等 |
| ACT_ID_* | 身份数据（Identity），存储用户、组信息（可选） |
| ACT_GE_* | 通用数据（General），存储二进制文件、属性等 |

**重要**：Flowable表与业务表分离，通过`BUSINESS_KEY_`字段关联业务数据。

### 5. 集成架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                     业务服务层                               │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  ProcessService (流程服务)                          │   │
│  │  - startProcessInstance() 启动流程实例              │   │
│  │  - submitTask() 提交任务                           │   │
│  │  - approveTask() 审批任务                          │   │
│  │  - rejectTask() 驳回任务                           │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  ProcessManagementService (流程管理服务)             │   │
│  │  - suspendProcessInstance() 挂起流程实例            │   │
│  │  - activateProcessInstance() 激活流程实例           │   │
│  │  - deleteProcessInstance() 删除流程实例             │   │
│  │  - addComment() 添加批注                          │   │
│  └─────────────────────────────────────────────────────┘   │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   Flowable引擎层                              │
│  - RepositoryService (仓库服务)                              │
│  - RuntimeService (运行时服务)                               │
│  - TaskService (任务服务)                                    │
│  - HistoryService (历史服务)                                 │
│  - ManagementService (管理服务)                              │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    数据库层                                   │
│  ACT_* (Flowable自动管理) + 业务表 (手动管理)                │
└─────────────────────────────────────────────────────────────┘
```

---

## 审批流程设计

### 1. 物资计划变更审批流程

#### 1.1 流程定义（BPMN 2.0）

**流程文件**：`resources/processes/material-plan-change.bpmn20.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://www.company.com/rpw">

    <process id="materialPlanChange" name="物资计划变更审批流程" isExecutable="true">
        
        <!-- 开始事件 -->
        <startEvent id="startEvent" name="变更申请发起" 
                    flowable:formKey="materialPlanChangeForm" />
        
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
        
        <!-- 部门负责人审批 -->
        <userTask id="deptApproval" name="部门负责人审批">
            <potentialOwner>
                <resourceAssignmentExpression>
                    <formalExpression>${deptLeader}</formalExpression>
                </resourceAssignmentExpression>
            </potentialOwner>
        </userTask>
        
        <!-- 公司级审批（会签） -->
        <userTask id="companyApproval" name="公司审批">
            <potentialOwner>
                <resourceAssignmentExpression>
                    <formalExpression>${companyApprovers}</formalExpression>
                </resourceAssignmentExpression>
            </potentialOwner>
        </userTask>
        
        <!-- 排他网关 - 审批判断 -->
        <exclusiveGateway id="approvalGateway" name="审批结果判断" />
        
        <!-- 审批通过 -->
        <sequenceFlow id="approveFlow" sourceRef="companyApproval" 
                      targetRef="updatePlanTask">
            <conditionExpression xsi:type="tFormalExpression">
                ${approved == true}
            </conditionExpression>
        </sequenceFlow>
        
        <!-- 审批驳回 -->
        <sequenceFlow id="rejectFlow" sourceRef="companyApproval" 
                      targetRef="fillChangeInfo">
            <conditionExpression xsi:type="tFormalExpression">
                ${approved == false}
            </conditionExpression>
        </sequenceFlow>
        
        <!-- 更新计划数据 -->
        <serviceTask id="updatePlanTask" name="更新计划" 
                     flowable:class="com.company.rpw.process.listener.UpdatePlanTaskListener" />
        
        <!-- 结束事件 -->
        <endEvent id="endEvent" name="流程结束" />
        
        <!-- 流程连线 -->
        <sequenceFlow sourceRef="startEvent" targetRef="fillChangeInfo" />
        <sequenceFlow sourceRef="fillChangeInfo" targetRef="deptApproval" />
        <sequenceFlow sourceRef="deptApproval" targetRef="companyApproval" />
        <sequenceFlow sourceRef="updatePlanTask" targetRef="endEvent" />
        
    </process>
</definitions>
```

#### 1.2 流程节点说明

| 节点ID | 节点名称 | 节点类型 | 处理人 | 说明 |
|--------|----------|----------|--------|------|
| startEvent | 变更申请发起 | 开始事件 | - | 流程起点 |
| fillChangeInfo | 填写变更信息 | 用户任务 | 流程发起人 | 填写变更原因和详情 |
| deptApproval | 部门负责人审批 | 用户任务 | 部门负责人 | 第一级审批 |
| companyApproval | 公司审批 | 用户任务 | 公司审批人 | 最终审批（可配置会签/或签） |
| approvalGateway | 审批结果判断 | 排他网关 | - | 根据approved变量判断 |
| updatePlanTask | 更新计划 | 服务任务 | 系统 | 自动更新计划数据 |
| endEvent | 流程结束 | 结束事件 | - | 流程终点 |

#### 1.3 流程变量定义

| 变量名 | 类型 | 说明 | 来源 |
|--------|------|------|------|
| initiator | String | 流程发起人ID | 启动流程时设置 |
| deptLeader | String | 部门负责人ID | 根据发起人自动计算 |
| companyApprovers | List<String> | 公司审批人列表 | 配置表读取 |
| approved | boolean | 审批是否通过 | 审批任务完成时设置 |
| changeReason | String | 变更原因 | 申请表单填写 |
| changeDetails | String | 变更详情JSON | 申请表单填写 |
| planId | Long | 关联的物资计划ID | 启动流程时传入 |
| businessKey | String | 业务主键 | 格式：`CHG-{planId}-{timestamp}` |

---

## 在线流程设计器集成方案

### 1. 方案对比

| 方案 | 优点 | 缺点 | 推荐度 |
|------|------|------|--------|
| **Flowable Modeler** | 官方提供，功能完整 | 需要单独部署，界面老旧 | ⭐⭐⭐ |
| **bpmn-js** | 轻量级，可嵌入，界面现代 | 需要自行开发后端接口 | ⭐⭐⭐⭐⭐ |
| **Activiti Modeler** | 功能强大 | 与Flowable兼容性需验证 | ⭐⭐ |

**推荐方案**：使用 **bpmn-js** 集成，理由：
1. 可完全嵌入系统，无需单独部署
2. 界面现代化，用户体验好
3. 社区活跃，文档完善
4. 可自定义属性和扩展

### 2. bpmn-js集成方案

#### 2.1 前端依赖安装

```bash
cd frontend
npm install bpmn-js bpmn-js-properties-panel camunda-bpmn-moddle --save
```

#### 2.2 流程设计器组件结构

```
frontend/src/components/ProcessDesigner/
├── index.vue                  # 设计器主组件
├── Toolbar.vue               # 工具栏
├── PropertyPanel.vue         # 属性面板
├── Palette.vue               # 节点调色板
└── utils/
    ├── bpmnUtils.js          # BPMN工具函数
    └── customConfig.js       # 自定义配置
```

#### 2.3 设计器页面布局

```
┌─────────────────────────────────────────────────────────────┐
│  流程设计器 - 物资计划变更审批流程        [保存] [部署] [导出]│
├─────────────────────────────────────────────────────────────┤
│ [新建] [打开] [保存] [撤销] [重做] [缩放] [对齐]           │  ← 工具栏
├────────────────────────────┬────────────────────────────────┤
│                            │  属性面板                      │
│   流程图绘制区域            │  - 节点属性                    │
│   (bpmn-js Canvas)        │  - 表单配置                    │
│                            │  - 监听器配置                  │
│                            │  - 候选人配置                  │
│                            │                                │
└────────────────────────────┴────────────────────────────────┘
```

#### 2.4 后端接口设计

```java
// 流程定义管理接口
@RestController
@RequestMapping("/api/v1/process/definition")
public class ProcessDefinitionController {
    
    // 部署流程定义（从前端接收BPMN XML）
    @PostMapping("/deploy")
    public Result<String> deploy(@RequestBody DeployProcessReq req);
    
    // 查询流程定义列表
    @GetMapping("/list")
    public Result<PageResult<ProcessDefinitionVO>> list(@ModelAttribute QueryProcessReq req);
    
    // 获取流程定义XML
    @GetMapping("/{processDefinitionId}/xml")
    public ResponseEntity<byte[]> getProcessXml(@PathVariable String processDefinitionId);
    
    // 获取流程定义流程图
    @GetMapping("/{processDefinitionId}/diagram")
    public ResponseEntity<byte[]> getProcessDiagram(@PathVariable String processDefinitionId);
    
    // 删除流程定义
    @DeleteMapping("/{processDefinitionId}")
    public Result<Void> delete(@PathVariable String processDefinitionId);
    
    // 激活/挂起流程定义
    @PutMapping("/{processDefinitionId}/state")
    public Result<Void> updateState(@PathVariable String processDefinitionId, 
                                    @RequestParam Boolean active);
}
```

---

## 流程实例后台干预功能设计

### 1. 功能需求

系统管理员或流程管理员需要能够：
1. **查看所有流程实例**：包括运行中、已结束、已挂起的
2. **挂起/激活流程实例**：临时暂停或恢复流程
3. **删除流程实例**：删除异常或测试的流程实例
4. **修改流程变量**：手动修正流程变量值
5. **跳转节点**：将流程跳转到指定节点
6. **添加批注**：记录干预原因
7. **查看流程历史**：查看流程执行轨迹

### 2. 核心API设计

#### 2.1 流程实例管理API

```java
@RestController
@RequestMapping("/api/v1/process/instance")
public class ProcessInstanceController {
    
    // 查询流程实例列表
    @GetMapping("/list")
    public Result<PageResult<ProcessInstanceVO>> list(@ModelAttribute QueryInstanceReq req);
    
    // 获取流程实例详情
    @GetMapping("/{processInstanceId}")
    public Result<ProcessInstanceVO> getDetail(@PathVariable String processInstanceId);
    
    // 挂起流程实例
    @PutMapping("/{processInstanceId}/suspend")
    public Result<Void> suspend(@PathVariable String processInstanceId, 
                                @RequestBody @Valid SuspendInstanceReq req);
    
    // 激活流程实例
    @PutMapping("/{processInstanceId}/activate")
    public Result<Void> activate(@PathVariable String processInstanceId);
    
    // 删除流程实例
    @DeleteMapping("/{processInstanceId}")
    public Result<Void> delete(@PathVariable String processInstanceId,
                              @RequestParam String deleteReason);
    
    // 修改流程变量
    @PutMapping("/{processInstanceId}/variables")
    public Result<Void> updateVariables(@PathVariable String processInstanceId,
                                        @RequestBody Map<String, Object> variables);
    
    // 跳转至指定节点
    @PutMapping("/{processInstanceId}/jump")
    public Result<Void> jumpToActivity(@PathVariable String processInstanceId,
                                       @RequestBody @Valid JumpActivityReq req);
    
    // 获取流程历史记录
    @GetMapping("/{processInstanceId}/history")
    public Result<List<HistoricActivityVO>> getHistory(@PathVariable String processInstanceId);
    
    // 添加批注
    @PostMapping("/{processInstanceId}/comment")
    public Result<Void> addComment(@PathVariable String processInstanceId,
                                   @RequestBody @Valid AddCommentReq req);
}
```

### 3. 前端管理页面设计

#### 3.1 流程实例管理页面

```
┌─────────────────────────────────────────────────────────────┐
│  流程实例管理                          [导出] [高级查询]      │
├─────────────────────────────────────────────────────────────┤
│  筛选条件：                                                   │
│  流程定义：[全部 ▼]  状态：[全部 ▼]  发起人：[输入]         │
│  开始时间：[日期范围]                                         │
├─────────────────────────────────────────────────────────────┤
│  ☐ │ 流程实例ID │ 流程名称 │ 发起人 │ 状态 │ 开始时间 │ 操作   │
│  ├───┼──────────┼──────────┼──────┼──────┼──────────┼──────│
│  │ ☐ │ 12345    │ 变更审批 │ 张三 │ 运行中│2026-05-10│[查看]│
│  │ ☐ │ 12346    │ 变更审批 │ 李四 │ 已挂起│2026-05-09│[编辑]│
├─────────────────────────────────────────────────────────────┤
│  [批量挂起] [批量激活] [批量删除]                           │
└─────────────────────────────────────────────────────────────┘
```

#### 3.2 流程实例详情/干预页面

```
┌─────────────────────────────────────────────────────────────┐
│  流程实例详情 - 12345                                      │
├─────────────────────────────────────────────────────────────┤
│  [挂起] [激活] [删除] [跳转] [添加批注]                    │
├─────────────────────────────────────────────────────────────┤
│  流程信息：                                                   │
│  - 流程定义：物资计划变更审批流程                             │
│  - 流程实例ID：12345                                        │
│  - 业务键：CHG-1001-1715323200000                          │
│  - 发起人：张三（项目级用户）                                │
│  - 状态：运行中                                             │
│  - 开始时间：2026-05-10 09:30:00                           │
├─────────────────────────────────────────────────────────────┤
│  当前任务：                                                   │
│  - 节点名称：公司审批                                        │
│  - 处理人：王五、赵六                                        │
│  - 创建时间：2026-05-10 10:00:00                           │
├─────────────────────────────────────────────────────────────┤
│  流程历史：                                                   │
│  09:30 - 开始流程                                           │
│  09:31 - 填写变更信息（张三）                                │
│  09:35 - 提交审批                                           │
│  09:36 - 部门审批通过（部门经理）                             │
│  10:00 - 等待公司审批                                       │
├─────────────────────────────────────────────────────────────┤
│  流程变量：                                                   │
│  - approved: (空)                                           │
│  - changeReason: "材料到货延期"                              │
│  - changeDetails: "{...}"                                   │
└─────────────────────────────────────────────────────────────┘
```

---

## 数据库表结构设计

### 1. 业务表设计

#### 1.1 物资计划变更申请表（material_plan_change）

**表说明**：存储物资计划变更申请的主表信息。

| 字段名 | 类型 | 长度 | 可为空 | 默认值 | 说明 |
|--------|------|------|--------|--------|------|
| id | bigint | 20 | NOT NULL | AUTO_INCREMENT | 主键ID |
| change_no | varchar | 50 | NOT NULL | - | 变更单号（规则：CHG + YYMM + 4位流水） |
| project_id | bigint | 20 | NOT NULL | - | 项目ID |
| plan_id | bigint | 20 | NOT NULL | - | 物资计划ID |
| plan_type | varchar | 50 | NOT NULL | - | 计划类型 |
| change_type | varchar | 20 | NOT NULL | - | 变更类型（DELAY-延期/MODIFY-修改/CANCEL-取消） |
| change_reason | varchar | 1000 | NOT NULL | - | 变更原因 |
| change_details | json | - | NOT NULL | - | 变更详情（JSON格式，包含旧值新值） |
| status | varchar | 20 | NOT NULL | 'DRAFT' | 状态（DRAFT-草稿/SUBMITTED-已提交/APPROVED-已批准/REJECTED-已驳回/CANCELLED-已取消） |
| process_instance_id | varchar | 100 | NULL | NULL | Flowable流程实例ID |
| business_key | varchar | 100 | NULL | NULL | 业务键（关联Flowable） |
| applicant_id | bigint | 20 | NOT NULL | - | 申请人ID |
| applicant_name | varchar | 50 | NOT NULL | - | 申请人姓名 |
| apply_time | datetime | - | NOT NULL | CURRENT_TIMESTAMP | 申请时间 |
| approved_time | datetime | - | NULL | NULL | 审批通过时间 |
| rejection_reason | varchar | 500 | NULL | NULL | 驳回原因 |
| remark | varchar | 500 | NULL | NULL | 备注 |
| create_time | datetime | - | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| update_time | datetime | - | NOT NULL | CURRENT_TIMESTAMP | 更新时间 |
| deleted | int | 1 | NOT NULL | 0 | 逻辑删除标记 |

**索引设计**：
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_change_no` (`change_no`)
- INDEX `idx_project_id` (`project_id`)
- INDEX `idx_plan_id` (`plan_id`)
- INDEX `idx_status` (`status`)
- INDEX `idx_process_instance_id` (`process_instance_id`)
- INDEX `idx_applicant_id` (`applicant_id`)

**建表SQL**：
```sql
CREATE TABLE `material_plan_change` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `change_no` varchar(50) NOT NULL COMMENT '变更单号',
  `project_id` bigint(20) NOT NULL COMMENT '项目ID',
  `plan_id` bigint(20) NOT NULL COMMENT '物资计划ID',
  `plan_type` varchar(50) NOT NULL COMMENT '计划类型',
  `change_type` varchar(20) NOT NULL COMMENT '变更类型（DELAY/MODIFY/CANCEL）',
  `change_reason` varchar(1000) NOT NULL COMMENT '变更原因',
  `change_details` json NOT NULL COMMENT '变更详情（JSON）',
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
  `process_instance_id` varchar(100) DEFAULT NULL COMMENT 'Flowable流程实例ID',
  `business_key` varchar(100) DEFAULT NULL COMMENT '业务键',
  `applicant_id` bigint(20) NOT NULL COMMENT '申请人ID',
  `applicant_name` varchar(50) NOT NULL COMMENT '申请人姓名',
  `apply_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `approved_time` datetime DEFAULT NULL COMMENT '审批通过时间',
  `rejection_reason` varchar(500) DEFAULT NULL COMMENT '驳回原因',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_change_no` (`change_no`),
  INDEX `idx_project_id` (`project_id`),
  INDEX `idx_plan_id` (`plan_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_process_instance_id` (`process_instance_id`),
  INDEX `idx_applicant_id` (`applicant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物资计划变更申请表';
```

#### 1.2 审批记录表（approval_record）

**表说明**：存储每个审批操作的记录，包括审批人、审批时间、审批意见等。

| 字段名 | 类型 | 长度 | 可为空 | 默认值 | 说明 |
|--------|------|------|--------|--------|------|
| id | bigint | 20 | NOT NULL | AUTO_INCREMENT | 主键ID |
| change_id | bigint | 20 | NOT NULL | - | 变更申请ID |
| process_instance_id | varchar | 100 | NOT NULL | - | 流程实例ID |
| task_id | varchar | 100 | NOT NULL | - | 任务ID |
| task_name | varchar | 100 | NOT NULL | - | 任务名称 |
| approver_id | bigint | 20 | NOT NULL | - | 审批人ID |
| approver_name | varchar | 50 | NOT NULL | - | 审批人姓名 |
| approve_action | varchar | 20 | NOT NULL | - | 审批动作（SUBMIT-提交/APPROVE-通过/REJECT-驳回） |
| approve_opinion | varchar | 500 | NULL | NULL | 审批意见 |
| approve_time | datetime | - | NOT NULL | CURRENT_TIMESTAMP | 审批时间 |
| create_time | datetime | - | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| update_time | datetime | - | NOT NULL | CURRENT_TIMESTAMP | 更新时间 |

**索引设计**：
- PRIMARY KEY (`id`)
- INDEX `idx_change_id` (`change_id`)
- INDEX `idx_process_instance_id` (`process_instance_id`)
- INDEX `idx_approver_id` (`approver_id`)

**建表SQL**：
```sql
CREATE TABLE `approval_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `change_id` bigint(20) NOT NULL COMMENT '变更申请ID',
  `process_instance_id` varchar(100) NOT NULL COMMENT '流程实例ID',
  `task_id` varchar(100) NOT NULL COMMENT '任务ID',
  `task_name` varchar(100) NOT NULL COMMENT '任务名称',
  `approver_id` bigint(20) NOT NULL COMMENT '审批人ID',
  `approver_name` varchar(50) NOT NULL COMMENT '审批人姓名',
  `approve_action` varchar(20) NOT NULL COMMENT '审批动作',
  `approve_opinion` varchar(500) DEFAULT NULL COMMENT '审批意见',
  `approve_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审批时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_change_id` (`change_id`),
  INDEX `idx_process_instance_id` (`process_instance_id`),
  INDEX `idx_approver_id` (`approver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录表';
```

#### 1.3 流程配置表（process_config）

**表说明**：配置各业务类型的流程定义，支持动态切换流程。

| 字段名 | 类型 | 长度 | 可为空 | 默认值 | 说明 |
|--------|------|------|--------|--------|------|
| id | bigint | 20 | NOT NULL | AUTO_INCREMENT | 主键ID |
| business_type | varchar | 50 | NOT NULL | - | 业务类型（MATERIAL_CHANGE/PLAN_CHANGE等） |
| process_definition_key | varchar | 100 | NOT NULL | - | Flowable流程定义Key |
| process_definition_name | varchar | 100 | NOT NULL | - | 流程定义名称 |
| description | varchar | 500 | NULL | NULL | 配置说明 |
| enabled | tinyint | 1 | NOT NULL | 1 | 是否启用（0-禁用，1-启用） |
| is_default | tinyint | 1 | NOT NULL | 0 | 是否默认流程 |
| create_time | datetime | - | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| update_time | datetime | - | NOT NULL | CURRENT_TIMESTAMP | 更新时间 |

**索引设计**：
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_business_type` (`business_type`, `process_definition_key`)
- INDEX `idx_enabled` (`enabled`)

**建表SQL**：
```sql
CREATE TABLE `process_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `business_type` varchar(50) NOT NULL COMMENT '业务类型',
  `process_definition_key` varchar(100) NOT NULL COMMENT '流程定义Key',
  `process_definition_name` varchar(100) NOT NULL COMMENT '流程定义名称',
  `description` varchar(500) DEFAULT NULL COMMENT '配置说明',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `is_default` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否默认流程',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_business_type` (`business_type`, `process_definition_key`),
  INDEX `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程配置表';
```

---

## 后端API设计

### 1. 物资计划变更申请API

#### 1.1 创建变更申请

```
POST /api/v1/change/material/create
```

**请求体示例**：
```json
{
  "projectId": 1001,
  "planId": 5001,
  "planType": "MATERIAL",
  "changeType": "DELAY",
  "changeReason": "供应商生产延期，预计延期15天",
  "changeDetails": {
    "oldValues": {
      "estimatedArrivalDate": "2026-05-20",
      "quantity": 100
    },
    "newValues": {
      "estimatedArrivalDate": "2026-06-04",
      "quantity": 100
    },
    "changedFields": ["estimatedArrivalDate"]
  }
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "changeId": 1,
    "changeNo": "CHG-260510-0001",
    "status": "DRAFT"
  }
}
```

#### 1.2 提交变更申请（启动流程）

```
POST /api/v1/change/material/{changeId}/submit
```

**说明**：提交后自动启动Flowable流程实例

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "processInstanceId": "12501",
    "taskId": "task-12345",
    "currentTaskName": "部门负责人审批"
  }
}
```

#### 1.3 审批变更申请

```
POST /api/v1/change/material/approve
```

**请求体示例**：
```json
{
  "taskId": "task-12345",
  "approveAction": "APPROVE",
  "approveOpinion": "同意变更，请尽快调整计划",
  "variables": {
    "approved": true
  }
}
```

#### 1.4 查询变更申请列表

```
GET /api/v1/change/material/list
```

**请求参数**：
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| projectId | Long | 否 | 项目ID筛选 |
| status | String | 否 | 状态筛选 |
| applicantId | Long | 否 | 申请人ID |
| startTime | String | 否 | 申请开始时间 |
| endTime | String | 否 | 申请结束时间 |
| pageNum | Integer | 否 | 页码 |
| pageSize | Integer | 否 | 每页数量 |

### 2. 流程管理API

#### 2.1 查询我的待办任务

```
GET /api/v1/process/task/todo
```

**响应示例**：
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "taskId": "task-12345",
        "taskName": "部门负责人审批",
        "processInstanceId": "12501",
        "businessKey": "CHG-1001-1715323200000",
        "processDefinitionName": "物资计划变更审批流程",
        "createTime": "2026-05-10 10:00:00",
        "assignee": "101"
      }
    ]
  }
}
```

#### 2.2 查询我的已办任务

```
GET /api/v1/process/task/done
```

#### 2.3 查询流程历史

```
GET /api/v1/process/instance/{processInstanceId}/history
```

---

## 前端页面设计

### 1. 变更申请页面

**页面路径**：`frontend/src/views/change/material/Apply.vue`

**功能模块**：
1. **变更申请表单**：选择项目、计划，填写变更原因和详情
2. **变更对比展示**：并排展示变更前后的数据对比
3. **提交流程**：提交后自动启动审批流程
4. **草稿保存**：支持暂存为草稿

**页面布局**：
```
┌─────────────────────────────────────────────────────────────┐
│  物资计划变更申请                           [保存草稿] [提交]│
├─────────────────────────────────────────────────────────────┤
│  项目：[选择项目 ▼]    计划类型：[材料 ▼]                  │
├─────────────────────────────────────────────────────────────┤
│  变更信息：                                                   │
│  变更类型：[延期 ▼]     原计划到货日期：2026-05-20           │
│  变更后到货日期：2026-06-04     延期天数：15天               │
│  变更原因：[输入__________________________________________]   │
│  变更详情：[输入__________________________________________]   │
├─────────────────────────────────────────────────────────────┤
│  变更对比：                                                   │
│  ┌──────────────┬──────────────┬──────────────┐           │
│  │ 字段         │ 原值         │ 新值         │           │
│  ├──────────────┼──────────────┼──────────────┤           │
│  │ 到货日期     │ 2026-05-20   │ 2026-06-04   │           │
│  │ 采购数量     │ 100          │ 100          │           │
│  └──────────────┴──────────────┴──────────────┘           │
└─────────────────────────────────────────────────────────────┘
```

### 2. 审批管理页面

**页面路径**：`frontend/src/views/approval/index.vue`

**子页面**：
- `TodoList.vue` - 待我审批
- `DoneList.vue` - 我审批的
- `MyApplyList.vue` - 我发起的

**待办列表页面布局**：
```
┌─────────────────────────────────────────────────────────────┐
│  待我审批                                      [批量审批]   │
├─────────────────────────────────────────────────────────────┤
│  ☐ │ 变更单号   │ 项目名称 │ 变更类型 │ 申请人 │ 当前节点   │
│  ├───┼──────────┼──────────┼──────────┼──────┼──────────┤   │
│  │ ☐ │CHG-0001  │ 某大桥   │ 延期     │ 张三 │公司审批  │   │
│  │ ☐ │CHG-0002  │ 某隧道   │ 修改     │ 李四 │部门审批  │   │
├─────────────────────────────────────────────────────────────┤
│  [审批] [驳回] [查看详情]                                   │
└─────────────────────────────────────────────────────────────┘
```

### 3. 流程设计器页面

**页面路径**：`frontend/src/views/process/designer/index.vue`

**功能**：
1. 使用bpmn-js绘制流程图
2. 配置节点属性（处理人、表单、监听器）
3. 保存并部署流程定义
4. 预览流程图

---

## 任务分解和实施顺序

### 1. 任务分解（WBS）

#### 第一阶段：基础集成（Week 1-2）

| 任务ID | 任务名称 | 预估工时 | 优先级 | 依赖 |
|--------|----------|----------|--------|------|
| T1-1 | Flowable依赖确认和配置 | 4h | P0 | - |
| T1-2 | Flowable自动建表验证 | 4h | P0 | T1-1 |
| T1-3 | 流程定义BPMN文件编写 | 8h | P0 | T1-2 |
| T1-4 | 流程部署功能实现 | 8h | P0 | T1-3 |
| T1-5 | 启动流程实例功能 | 8h | P0 | T1-4 |
| T1-6 | 查询流程实例状态 | 4h | P0 | T1-5 |

#### 第二阶段：审批功能（Week 2-3）

| 任务ID | 任务名称 | 预估工时 | 优先级 | 依赖 |
|--------|----------|----------|--------|------|
| T2-1 | 变更申请表设计和创建 | 4h | P0 | T1-6 |
| T2-2 | 变更申请API实现 | 12h | P0 | T2-1 |
| T2-3 | 流程任务查询API | 8h | P0 | T2-2 |
| T2-4 | 审批提交API实现 | 12h | P0 | T2-3 |
| T2-5 | 审批记录保存 | 4h | P0 | T2-4 |

#### 第三阶段：前端页面（Week 3-4）

| 任务ID | 任务名称 | 预估工时 | 优先级 | 依赖 |
|--------|----------|----------|--------|------|
| T3-1 | 变更申请页面开发 | 16h | P0 | T2-5 |
| T3-2 | 待办列表页面开发 | 12h | P0 | T3-1 |
| T3-3 | 审批页面开发 | 12h | P0 | T3-2 |
| T3-4 | 审批历史页面开发 | 8h | P1 | T3-3 |

#### 第四阶段：流程设计器（Week 4-5）

| 任务ID | 任务名称 | 预估工时 | 优先级 | 依赖 |
|--------|----------|----------|--------|------|
| T4-1 | bpmn-js前端集成 | 16h | P1 | T3-4 |
| T4-2 | 流程设计器组件开发 | 24h | P1 | T4-1 |
| T4-3 | 流程定义管理后端API | 16h | P1 | T4-2 |
| T4-4 | 流程定义部署和版本管理 | 8h | P1 | T4-3 |

#### 第五阶段：后台干预（Week 5-6）

| 任务ID | 任务名称 | 预估工时 | 优先级 | 依赖 |
|--------|----------|----------|--------|------|
| T5-1 | 流程实例挂起/激活API | 4h | P1 | T4-4 |
| T5-2 | 流程实例删除API | 4h | P1 | T5-1 |
| T5-3 | 流程变量修改API | 4h | P1 | T5-2 |
| T5-4 | 流程节点跳转API | 8h | P1 | T5-3 |
| T5-5 | 流程实例管理前端页面 | 16h | P1 | T5-4 |

#### 第六阶段：测试和优化（Week 6-7）

| 任务ID | 任务名称 | 预估工时 | 优先级 | 依赖 |
|--------|----------|----------|--------|------|
| T6-1 | 单元测试编写 | 16h | P0 | T5-5 |
| T6-2 | 集成测试 | 16h | P0 | T6-1 |
| T6-3 | 性能优化 | 8h | P1 | T6-2 |
| T6-4 | 用户文档编写 | 8h | P1 | T6-3 |

### 2. 实施顺序图

```
Week 1-2:  [T1-1][T1-2][T1-3][T1-4][T1-5][T1-6]
             │
Week 2-3:    └──► [T2-1][T2-2][T2-3][T2-4][T2-5]
                   │
Week 3-4:          └──► [T3-1][T3-2][T3-3][T3-4]
                         │
Week 4-5:                └──► [T4-1][T4-2][T4-3][T4-4]
                               │
Week 5-6:                      └──► [T5-1][T5-2][T5-3][T5-4][T5-5]
                                     │
Week 6-7:                            └──► [T6-1][T6-2][T6-3][T6-4]
```

### 3. 里程碑定义

| 里程碑 | 完成标准 | 预计时间 |
|--------|----------|----------|
| M1: 流程引擎集成完成 | Flowable可正常部署和启动流程 | Week 2 |
| M2: 审批功能完成 | 变更申请和审批流程可完整运行 | Week 3 |
| M3: 前端页面完成 | 用户可在线提交申请和审批 | Week 4 |
| M4: 流程设计器完成 | 可在线设计和部署流程 | Week 5 |
| M5: 后台干预完成 | 管理员可干预流程实例 | Week 6 |
| M6: 测试完成 | 所有功能测试通过 | Week 7 |

---

## 附录

### A. Flowable常用API速查表

| 功能 | Service | 方法 |
|------|---------|------|
| 部署流程定义 | RepositoryService | createDeployment() |
| 启动流程实例 | RuntimeService | startProcessInstanceByKey() |
| 查询待办任务 | TaskService | createTaskQuery() |
| 完成任务 | TaskService | complete() |
| 查询历史 | HistoryService | createHistoricActivityInstanceQuery() |
| 挂起流程实例 | RuntimeService | suspendProcessInstanceById() |
| 删除流程实例 | RuntimeService | deleteProcessInstance() |

### B. BPMN 2.0 元素说明

| 元素类型 | 说明 | 图标 |
|----------|------|------|
| Start Event | 开始事件 | ○ |
| End Event | 结束事件 | ⬤ |
| User Task | 用户任务 | □ |
| Service Task | 服务任务 | □ (齿轮) |
| Exclusive Gateway | 排他网关 | ◇ |
| Parallel Gateway | 并行网关 | ◇ (带+) |

### C. 参考文档

- Flowable官方文档：https://www.flowable.com/open-source/docs
- bpmn-js官方文档：https://bpmn.io/toolkit/bpmn-js/
- Spring Boot集成Flowable：https://flowable.com/open-source/docs/bpmn/ch05-Spring-Boot/

---

**文档版本**：v1.0  
**编写日期**：2026-05-10  
**编写人**：高见远（Gao）  
**审核状态**：待审核
