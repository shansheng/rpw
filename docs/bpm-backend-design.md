# BPM 工作流模块后端实现 设计 / 详细设计 / 开发计划

> 范围修正说明：原理解偏差为「优化现有 rpw/workflow 轻量页」；正确目标是——**以 `web-antdv-next/src/views/bpm` 下完整 yudao 工作流前端为基准，在后端实现其所需的全部 API，并添加 BPM 菜单让这些页面可访问**。即把 yudao bpm 后端能力移植到 rpw（Spring Boot 3.1.6 + Flowable 7 + MyBatis-Plus 3.5.7）。

## 1. 总体架构

```
前端 web-antdv-next/src/views/bpm/*  (已存在，完整 yudao bpm 前端)
        │  requestClient  (baseURL=/api/v1, successCode=200)
        ▼
后端 rpw  com.company.rpw.controller.bpm.*  (新增)
        ├─ DB 支撑表 (bpm_*)        ← category/userGroup/processExpression/processListener/form/model/comment/oa_leave
        └─ Flowable 引擎            ← processDefinition / processInstance / task 直接查询/驱动
        ▲
菜单 SystemAuthAdapterController.buildMenus() 新增 BPM 节点（component=views/bpm/.../index.vue）
```

- 响应统一 `R<T>`（`code/message/data`），成功码 200。
- 分页统一 `PageResult<T>`（`{list, total}`），匹配前端 vxe-table。
- 流程引擎复用项目已有的 Flowable 7（已依赖 `flowable-spring-boot-starter-process`）。
- 业务表沿用 `BaseEntity`（id/createTime/updateTime/deleted 逻辑删除）。
- 新表创建复用 `SchemaInitializer` 的 `CREATE TABLE IF NOT EXISTS` 幂等方式。

## 2. 模块与端点设计（全部对标前端契约）

### 2.1 流程模型 `bpm/model`
| 端点 | 说明 | 数据来源 |
|---|---|---|
| GET /bpm/model/list | 模型列表（卡片）。需返 `type,startUserIds,deploymentTime,categoryName` | `bpm_model` 表 + category 名 |
| GET /bpm/model/get?id= | 详情 | `bpm_model` |
| POST /bpm/model/create | 新建（存 bpmnXml/formType/formId，不部署） | `bpm_model` |
| PUT /bpm/model/update | 改名/备注 | `bpm_model` |
| PUT /bpm/model/update-bpmn | 保存 BPMN | `bpm_model` |
| PUT /bpm/model/update-state | 启用/停用 | `bpm_model.status` |
| POST /bpm/model/deploy?id= | 部署到 Flowable | repositoryService + 回写 processDefinitionId/version |
| DELETE /bpm/model/clean?id= | 清除部署关联 | `bpm_model` |
| DELETE /bpm/model/delete?id= | 删除 | `bpm_model`（逻辑删除） |
| PUT /bpm/model/update-sort-batch?ids= | 拖拽排序 | `bpm_model.sort` |

### 2.2 流程定义 `bpm/process-definition`（部署态，来自 Flowable）
`/page /list /simple-list /get /{id}/xml /update-state` —— 查 Flowable `ProcessDefinition`，并关联 `bpm_model` 补全 `formType/formId/formName/formConf/bpmnXml/categoryName/modelType`。

### 2.3 流程实例 `bpm/process-instance`（运行态）
`/my-page /manager-page /get /create /cancel-by-start-user /cancel-by-admin /get-approval-detail /get-bpmn-model-view /get-print-data /copy/page`
- my-page/manager-page：查 Flowable `HistoricProcessInstance` → `ProcessInstance` VO（含 startUser、status、summary）。
- create：`runtimeService.startProcessInstanceById(processDefinitionId, variables, businessKey)`。
- get-approval-detail（详情页核心）：聚合 `processInstance + processDefinition(表单) + activityNodes(已完成/进行中节点+候选/任务) + todoTask`。
- get-bpmn-model-view：返回 bpmnXml + 当前活动节点 id 列表（前端高亮）。

### 2.4 任务 `bpm/task`
`/todo-page /done-page /manager-page`：查 `TaskService`/`HistoryService` 映射 `Task` VO（assigneeUser、formId/formConf、nodeType、buttonsSetting…）。
动作：`/approve /reject /return /delegate /transfer /create-sign /delete-sign /copy /withdraw` + 辅助 `/list-by-return /list-by-process-instance-id /list-by-parent-task-id`。
- approve/reject：完成任务（`taskService.complete`），回写 variables；reject 走 `return` 到开始节点或终止（本实现：reject = 退回到发起节点并结束）。
- 退回/转办/委派/加签/抄送/撤回：基于 Flowable API 实现。

### 2.5 流程表单 `bpm/form`
`/page /get /create /update /delete /simple-list` —— `bpm_form` 表（conf=JSON 配置串，fields=字段名 JSON 数组）。

### 2.6 流程分类 `bpm/category` / 用户分组 `bpm/user-group` / 流程表达式 `bpm/process-expression` / 流程监听器 `bpm/process-listener`
纯 DB CRUD（page/get/create/update/delete/simple-list），支撑模型表单的选择项。

### 2.7 评论 `bpm/comment` / OA 请假 `bpm/oa/leave`
- comment：`/list-by-process-instance-id`、`/create`（存 `bpm_comment`）。
- oa/leave：示例业务表单 CRUD（`/create /get /page`），演示「业务表单 + 流程」。

## 3. 数据表设计（新增 8 张）

| 表 | 关键字段 |
|---|---|
| `bpm_category` | id,name,code,status,description,sort |
| `bpm_user_group` | id,name,description,user_ids(JSON),status,remark |
| `bpm_process_expression` | id,name,status,expression |
| `bpm_process_listener` | id,name,type,status,event,value_type,value |
| `bpm_form` | id,name,conf(longtext),fields(longtext),status,remark |
| `bpm_model` | id,name,key,icon,category,form_type,form_id,form_custom_*,process_definition_key/id/version,deployment_time,status,bpmn_xml(longtext),start_user_ids(JSON),sort,remark |
| `bpm_comment` | id,task_id,process_instance_id,type,message,user_id |
| `bpm_oa_leave` | id,user_id,type,reason,start_time,end_time,status,process_instance_id |

> 抄送/加签等运行态数据优先利用 Flowable 自身（历史任务/变量），不额外建大表；`copy` 列表由 `taskService` 的 candidate 抄送标识或 `bpm_comment` 类型扩展提供。

## 4. 菜单方案（SystemAuthAdapterController.buildMenus）

将原 `id=40 工作流` 节点升级为 **BPM 工作流** 父节点（`component=Layout`），子项（component 均为 `views/bpm/.../index.vue`）：
流程模型(`manager/model`)、流程定义(`manager/definition`)、流程表单(`manager/form`)、流程分类(`manager/category`)、用户分组(`manager/group`)、流程表达式(`manager/process-expression`)、流程监听器(`manager/process-listener`)、我的流程(`process-instance/my`)、流程管理(`manager/process-instance`)、数据报表(`process-instance/report`)、待办任务(`task/my`)、已办任务(`task/done`)、任务管理(`manager/task`)、抄送我的(`task/copy`)、请假管理(`oa/leave/index`)、保留「人力计划变更」(`labor-plan-change`)。

## 5. 开发计划（分阶段）

- **Phase 1（基础设施）**：PageResult/枚举/当前用户工具 + SchemaInitializer DDL（任务 #40）。
- **Phase 2（DB 支撑模块）**：category/userGroup/processExpression/processListener/form/oa-leave 全 CRUD（#41）。
- **Phase 3（Flowable 核心）**：model(#42)→definition(#43)→processInstance(#44)→task(#45)→comment(#46)。
- **Phase 4（接入）**：菜单(#47)；编译重启+冒烟测试(#48)。
- **Phase 5（可选增强）**：候选策略预测、发起流程预测分支、表单设计器绑定业务表单、抄送独立表、字典数据端点。

## 6. 验证方案
1. `mvn compile` 通过；重启 8080。
2. curl 验证：`/bpm/model/list`、`/bpm/process-definition/list`、`/bpm/process-instance/my-page`、`/bpm/task/todo-page`、`/bpm/form/simple-list`、`/bpm/category/page` 均返回 `code=200` 且 `data` 结构正确。
3. 前端 dev(5666) 菜单出现 BPM 工作流，各页可进、可列表。
4. 端到端：模型 create→deploy→发起实例→待办 approve→流程结束。
