# 项目资源计划预警系统 - Phase 5 开发方案

**版本**: 1.0  
**日期**: 2026-05-10  
**制定人**: 齐活林（主理人）  
**目标**: Flowable审批流程集成，实现物资计划变更申请与审批

---

## 1. 需求概述

基于PRD文档，Phase 5包含以下核心需求：

| 需求ID | 模块 | 需求描述 | 预估工时 |
|--------|------|----------|----------|
| P0-007 | 物资计划 | 物资计划变更申请与审批（Flowable，支持在线调整+后台干预） | 32h |
| P1-001 | 审批 | Flowable流程引擎集成（含在线流程设计器） | 48h |
| P1-004 | 流程管理 | 流程实例后台干预功能 | 16h |

**总计**: 96h（约12人天）

---

## 2. 技术方案

### 2.1 Flowable集成配置

**依赖**: 已在 `backend/pom.xml` 中恢复 Flowable 依赖

```xml
<dependency>
    <groupId>org.flowable</groupId>
    <artifactId>flowable-spring-boot-starter</artifactId>
    <version>7.0.0</version>
</dependency>
```

**自动配置**:
- Flowable 会自动创建 60+ 张表（以 `ACT_` 或 `FLW_` 开头）
- 流程定义存储在 `ACT_RE_PROCDEF`
- 流程实例存储在 `ACT_RU_EXECUTION`
- 任务存储在 `ACT_RU_TASK`

**配置类**:
```java
@Configuration
public class FlowableConfig {
    // Flowable 自动配置已足够，通常无需额外配置
    // 如需自定义，可配置 AsyncExecutor, HistoryLevel 等
}
```

---

### 2.2 数据库设计

#### 2.2.1 扩展现有表 - 资源计划表

在 `resource_plan_*` 表中增加字段：

```sql
-- 示例：resource_plan_material 表增加审批字段
ALTER TABLE resource_plan_material 
ADD COLUMN approval_status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '审批状态(DRAFT/SUBMITTED/APPROVED/REJECTED)',
ADD COLUMN process_instance_id VARCHAR(100) DEFAULT NULL COMMENT 'Flowable流程实例ID',
ADD COLUMN approval_comment TEXT DEFAULT NULL COMMENT '审批意见';
```

**审批状态说明**:
- `DRAFT`: 草稿
- `SUBMITTED`: 已提交审批
- `APPROVED`: 审批通过
- `REJECTED`: 审批驳回

#### 2.2.2 新增表 - 流程设计器保存表（可选）

如需保存用户设计的流程，可创建：

```sql
CREATE TABLE flowable_process_design (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    process_name VARCHAR(100) NOT NULL COMMENT '流程名称',
    process_key VARCHAR(100) NOT NULL COMMENT '流程KEY',
    bpmn_xml TEXT NOT NULL COMMENT 'BPMN XML定义',
    version INT DEFAULT 1 COMMENT '版本号',
    status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态(DRAFT/DEPLOYED)',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Flowable流程设计表';
```

---

### 2.3 后端API设计

#### 2.3.1 流程定义管理 API

| 方法 | URL | 说明 |
|------|-----|------|
| POST | `/api/v1/flowable/process-definition/deploy` | 部署流程定义 |
| GET | `/api/v1/flowable/process-definition/list` | 查询流程定义列表 |
| DELETE | `/api/v1/flowable/process-definition/{deploymentId}` | 删除流程定义 |

**部署流程定义示例**:
```java
@PostMapping("/deploy")
public Result<String> deployProcessDefinition(@RequestParam("file") MultipartFile file) {
    try {
        Deployment deployment = repositoryService.createDeployment()
                .addInputStream(file.getOriginalFilename(), file.getInputStream())
                .deploy();
        return Result.success(deployment.getId());
    } catch (IOException e) {
        return Result.error("部署失败: " + e.getMessage());
    }
}
```

#### 2.3.2 流程实例管理 API

| 方法 | URL | 说明 |
|------|-----|------|
| POST | `/api/v1/flowable/process-instance/start` | 启动流程实例 |
| GET | `/api/v1/flowable/process-instance/{processInstanceId}` | 查询流程实例 |
| DELETE | `/api/v1/flowable/process-instance/{processInstanceId}` | 终止流程实例 |

**启动流程实例示例**:
```java
@PostMapping("/start")
public Result<String> startProcessInstance(@RequestBody StartProcessDTO dto) {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
            dto.getProcessDefinitionKey(),
            dto.getBusinessKey(),
            dto.getVariables());
    return Result.success(processInstance.getId());
}
```

#### 2.3.3 任务管理 API

| 方法 | URL | 说明 |
|------|-----|------|
| GET | `/api/v1/flowable/task/list` | 查询待办任务 |
| GET | `/api/v1/flowable/task/history` | 查询历史任务 |
| POST | `/api/v1/flowable/task/{taskId}/complete` | 完成任务 |
| POST | `/api/v1/flowable/task/{taskId}/claim` | 认领任务 |
| POST | `/api/v1/flowable/task/{taskId}/assign` | 转派任务 |

**查询待办任务示例**:
```java
@GetMapping("/list")
public Result<List<TaskDTO>> getTasks(@RequestParam("assignee") String assignee) {
    List<Task> tasks = taskService.createTaskQuery()
            .taskAssignee(assignee)
            .list();
    // 转换为 DTO
    List<TaskDTO> taskDTOs = tasks.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    return Result.success(taskDTOs);
}
```

#### 2.3.4 物资计划变更审批 API

| 方法 | URL | 说明 |
|------|-----|------|
| POST | `/api/v1/resource-plan/material/submit-approval` | 提交审批 |
| POST | `/api/v1/resource-plan/material/approve` | 审批通过 |
| POST | `/api/v1/resource-plan/material/reject` | 审批驳回 |
| GET | `/api/v1/resource-plan/material/approval-history/{planId}` | 查询审批历史 |

**提交审批示例**:
```java
@PostMapping("/submit-approval")
public Result<Void> submitApproval(@RequestParam("planId") Long planId) {
    ResourcePlanMaterial plan = materialService.getById(planId);
    
    // 启动 Flowable 流程实例
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
            "materialChangeApproval",
            String.valueOf(planId),
            Map.of("planId", planId, "applicant", getCurrentUser())
    );
    
    // 更新计划状态
    plan.setApprovalStatus("SUBMITTED");
    plan.setProcessInstanceId(processInstance.getId());
    materialService.updateById(plan);
    
    return Result.success();
}
```

---

### 2.4 前端页面设计

#### 2.4.1 物资计划列表页 - 增加审批操作

**页面路径**: `frontend/src/views/resource-plan/material/index.vue`

**新增功能**:
- 【提交审批】按钮（状态为 DRAFT 时显示）
- 【查看审批进度】按钮（状态为 SUBMITTED 时显示）
- 【审批历史】按钮（始终显示）

**示例代码**:
```vue
<template>
  <div>
    <!-- 表格列：操作 -->
    <el-table-column label="操作" width="300">
      <template #default="{ row }">
        <el-button v-if="row.approvalStatus === 'DRAFT'" @click="submitApproval(row.id)">
          提交审批
        </el-button>
        <el-button v-if="row.approvalStatus === 'SUBMITTED'" @click="viewProgress(row.processInstanceId)">
          查看进度
        </el-button>
        <el-button @click="viewHistory(row.id)">
          审批历史
        </el-button>
      </template>
    </el-table-column>
  </div>
</template>
```

#### 2.4.2 待办任务页

**页面路径**: `frontend/src/views/flowable/task/index.vue`

**功能**:
- 显示当前用户待办任务列表
- 【处理】按钮：打开任务处理对话框（通过/驳回 + 意见）
- 【查看流程进度】按钮：显示流程图

**API 封装** (`frontend/src/api/flowable.ts`):
```typescript
export function getTaskList(params: any) {
  return request({
    url: '/api/v1/flowable/task/list',
    method: 'get',
    params
  })
}

export function completeTask(taskId: string, data: any) {
  return request({
    url: `/api/v1/flowable/task/${taskId}/complete`,
    method: 'post',
    data
  })
}
```

#### 2.4.3 流程设计器页（可选）

**方案A - 集成 Flowable Modeler**:
- 下载 Flowable Modeler WAR 包，部署到 Tomcat
- 或直接集成 Flowable Modeler 前端（基于 Angular）

**方案B - 自定义简单设计器**:
- 使用 bpmn-js 库（https://bpmn.io/）
- Vue 3 + bpmn-js 实现在线绘制 BPMN 流程图

**推荐**: 先采用方案A（集成 Flowable Modeler），快速上线；后续可优化为方案B。

---

### 2.5 流程实例后台干预功能

#### 2.5.1 后台干预 API

| 方法 | URL | 说明 |
|------|-----|------|
| POST | `/api/v1/flowable/admin/suspend/{processInstanceId}` | 挂起流程实例 |
| POST | `/api/v1/flowable/admin/activate/{processInstanceId}` | 激活流程实例 |
| POST | `/api/v1/flowable/admin/jump/{taskId}` | 任务跳转 |
| POST | `/api/v1/flowable/admin/assign/{taskId}` | 转派任务 |
| GET | `/api/v1/flowable/admin/history/{processInstanceId}` | 查询历史 |

**挂起流程实例示例**:
```java
@PostMapping("/suspend/{processInstanceId}")
public Result<Void> suspendProcessInstance(@PathVariable String processInstanceId) {
    runtimeService.suspendProcessInstanceById(processInstanceId);
    return Result.success();
}
```

#### 2.5.2 后台干预页面

**页面路径**: `frontend/src/views/flowable/admin/index.vue`

**功能**:
- 流程实例列表（可搜索、筛选状态）
- 【挂起/激活】按钮
- 【跳转】按钮：选择目标节点，跳转流程
- 【转派】按钮：选择目标用户，转派任务

---

## 3. 实施步骤

### 阶段一（Week 1）：Flowable 集成与基础功能

**任务**:
1. **Flowable 集成配置** (4h)
   - 确认 `pom.xml` 依赖已添加
   - 启动应用，验证 Flowable 自动建表成功
   - 编写 Flowable 配置类（可选）

2. **数据库扩展** (4h)
   - 在 `resource_plan_material` 表中增加审批字段
   - 更新 `ResourcePlanMaterial` 实体类
   - 更新 MyBatis-Plus Mapper

3. **流程定义管理** (8h)
   - 创建 `ProcessDefinitionController`
   - 实现部署、查询、删除流程定义接口
   - 编写单元测试

4. **流程实例管理** (8h)
   - 创建 `ProcessInstanceController`
   - 实现启动、查询、终止流程实例接口
   - 编写单元测试

**交付物**:
- Flowable 集成配置完成
- 流程定义、流程实例管理 API 可用

---

### 阶段二（Week 2）：任务管理与审批流程

**任务**:
1. **任务管理** (8h)
   - 创建 `TaskController`
   - 实现查询待办任务、完成任务、认领任务、转派任务接口
   - 编写单元测试

2. **物资计划变更审批** (16h)
   - 设计审批流程 BPMN 文件（简单审批流程：提交 → 部门经理审批 → 完成）
   - 创建 `MaterialApprovalController`
   - 实现提交审批、审批通过、审批驳回接口
   - 更新 `ResourcePlanMaterialService`，集成 Flowable 调用

3. **前端待办任务页** (12h)
   - 创建 `frontend/src/views/flowable/task/index.vue`
   - 实现待办任务列表、处理对话框
   - 集成 API

**交付物**:
- 任务管理 API 完整
- 物资计划变更审批流程可用
- 前端待办任务页可以操作

---

### 阶段三（Week 3）：流程设计器与后台干预

**任务**:
1. **在线流程设计器** (16h)
   - 集成 Flowable Modeler（方案A）
   - 或实现自定义设计器（方案B，使用 bpmn-js）
   - 前端页面开发

2. **流程实例后台干预** (16h)
   - 创建 `FlowableAdminController`
   - 实现挂起/激活、跳转、转派接口
   - 前端后台干预页面开发

3. **测试与优化** (16h)
   - 集成测试
   - 性能优化
   - Bug 修复

**交付物**:
- 在线流程设计器可用
- 后台干预功能完整
- 测试通过

---

## 4. 关键技术点

### 4.1 Flowable 与业务表关联

**通过 `businessKey` 关联**:
```java
// 启动流程时，将业务ID作为 businessKey
ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
        "materialChangeApproval",
        String.valueOf(planId),  // businessKey = planId
        variables);
```

**查询流程实例对应的业务数据**:
```java
// 通过 businessKey 查询流程实例
ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
        .processInstanceBusinessKey(String.valueOf(planId))
        .singleResult();
```

### 4.2 任务审批与业务状态同步

**在 Flowable 任务完成监听器里更新业务状态**:
```java
@Service
public class ApprovalTaskListener implements TaskListener {
    
    @Override
    public void notify(DelegateTask delegateTask) {
        String planId = delegateTask.getVariable("planId").toString();
        String approvalResult = delegateTask.getVariable("approvalResult").toString();
        
        ResourcePlanMaterial plan = materialService.getById(planId);
        if ("APPROVED".equals(approvalResult)) {
            plan.setApprovalStatus("APPROVED");
        } else if ("REJECTED".equals(approvalResult)) {
            plan.setApprovalStatus("REJECTED");
        }
        materialService.updateById(plan);
    }
}
```

### 4.3 在线流程设计器集成（方案A - Flowable Modeler）

**步骤**:
1. 下载 Flowable 6.x 或 7.x 的 `flowable-modeler` WAR 包
2. 部署到单独的 Tomcat（端口 8081）
3. 修改 `flowable-modeler` 配置，连接到同一数据库
4. 前端通过 iframe 嵌入 `http://localhost:8081/flowable-modeler`

**优点**: 快速集成，功能完整  
**缺点**: 需要额外 Tomcat，样式可能不统一

---

## 5. 风险与建议

### 5.1 技术风险

| 风险 | 影响 | 应对措施 |
|------|------|----------|
| Flowable 表与现有表冲突 | 启动失败 | 使用独立数据源，或确认表前缀不冲突 |
| 流程设计器集成复杂 | 工期延误 | 优先采用方案A（Flowable Modeler），后续优化 |
| 后台干预功能复杂 | 无法实现 | 先实现基础功能（挂起/激活/转派），高级功能后续迭代 |

### 5.2 实施建议

1. **先实现核心流程，再优化界面**
   - 第一优先级：物资计划变更审批流程跑通
   - 第二优先级：前端待办任务页
   - 第三优先级：在线流程设计器、后台干预

2. **使用 BPMN 2.0 标准文件**
   - 先手动编写简单的 BPMN 2.0 XML 文件（如 `material-approval.bpmn20.xml`)
   - 部署到 Flowable，验证流程可行
   - 后续再集成在线设计器

3. **充分测试**
   - 每个 API 都要编写单元测试
   - 流程测试：覆盖正常审批、驳回、转派等场景

---

## 6. 交付标准

### 6.1 功能交付

- [ ] Flowable 流程引擎集成完成
- [ ] 物资计划变更申请与审批流程可用
- [ ] 在线流程设计器集成完成（或自定义设计器）
- [ ] 流程实例后台干预功能完成
- [ ] 前后端联调通过
- [ ] 单元测试覆盖率 > 60%

### 6.2 文档交付

- [ ] Phase 5 开发方案（本文档）
- [ ] API 接口文档（Swagger/Knife4j 自动生成）
- [ ] 用户操作手册（如何提交审批、如何处理任务）

---

**附录**:

- Flowable 官方文档: https://www.flowable.com/open-source/docs
- BPMN 2.0 规范: https://www.omg.org/spec/BPMN/2.0/
- bpmn-js 官网: https://bpmn.io/

---

**审批**:  
- [ ] 许清楚（产品经理）- 确认需求  
- [x] 高见远（架构师）- 技术方案已制定  
- [ ] 寇豆码（工程师）- 准备实施  
- [ ] 严过关（QA工程师）- 准备测试
