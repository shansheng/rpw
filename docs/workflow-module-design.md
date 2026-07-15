# 工作流模块优化设计文档

> 基准参考：`web-antdv-next/src/views/bpm`（完整 yudao 工作流前端）
> 目标应用：`web-antdv-next`（dev 端口 5666，`pnpm dev:antdv-next`）
> 后端：`rpw`（Spring Boot 3.1.6 + Flowable 7.0.0 + MyBatis-Plus 3.5.7，端口 8080）
> 文档版本：2026-07-08

---

## 1. 背景与目标

`web-antdv-next/src/views/bpm` 提供了一套**设计态 → 部署态 → 运行态 → 任务 → 表单**完整分层的工作流前端：

- `bpm/model`（流程模型，含 BPMN 设计器）
- `bpm/process-definition`（部署态流程定义）
- `bpm/processInstance`（运行态实例 + 审批进度图）
- `bpm/task`（待办/已办/抄送/管理）
- `bpm/form`（表单设计器，任务通过 `formId` + `formConf` 绑定）

rpw 项目当前已有真实可运行的工作流基础设施（Flowable 引擎 + `ApprovalController` + `ProcessDefinitionController` + 2 个 BPMN），但**前端页面调用的端点在后端根本不存在**，导致"暂无数据"；且后端存在 4 个阻断真实审批链路的 bug。

**目标**：以 yudao 工作流前端的分层设计为参考，优化 rpw 工作流模块，做到：
1. 前端页面全部对齐后端**真实存在**的端点；
2. 后端修通完整的审批生命周期（提交 → 待办 → 审批 → 已办 → 我发起的）；
3. 流程定义可视、可部署、可删除；
4. 为后续接入 yudao 式设计器 / 表单 / 抄送 / 加签预留扩展点。

---

## 2. 现状诊断（问题清单）

### 2.1 前端调用不存在的端点（404 → 暂无数据）

| 页面 | 调用的函数 | 请求路径 | 后端是否存在 |
|---|---|---|---|
| 待办任务 todo | `getTodoTasks` | `GET /flowable/task/todo` | ❌ 不存在 |
| 待办任务 todo | `getTaskDiagram` | `GET /flowable/task/{id}/diagram` | ❌ 不存在 |
| 已办任务 done | `getHistoryTasks` | `GET /flowable/task/history` | ❌ 不存在 |
| 我发起的 my | `getMyProcessInstances` | `GET /flowable/process-instance/my` | ❌ 不存在 |
| 我发起的 my | `getProcessInstanceDetail` | `GET /flowable/process-instance/{id}/detail` | ❌ 不存在 |
| 流程定义 definition | `getProcessDefinitions` | `GET /flowable/process-definition/list` | ✅ 存在(`/api/v1/flowable/process-definition/list`) |
| 流程定义 definition | `deleteProcessDefinition` | `DELETE /flowable/process-definition/{id}` | ✅ 存在 |
| 流程定义 definition | `deployProcessDefinitionByXml` | `POST /flowable/process-definition/deploy/xml` | ✅ 存在 |

### 2.2 后端 4 个阻断 bug

1. **缺失监听器类**：`material-approval.bpmn20.xml` 的 userTask 引用 `com.company.rpw.listener.ApprovalTaskListener`（类不存在）→ 审批完成时抛 `ClassNotFound`。
2. **submit 硬编码流程 key**：`ApprovalController.submit` 固定启动 `materialChangeApproval`，提交 labor 会启动错误流程。
3. **网关变量名不一致**：material 流程网关判断 `approvalResult`，labor 流程网关判断 `approved`；但 `approve/reject` 只设置 `approvalResult` → labor 流程在第二节点网关处因条件都不满足而报错。
4. **次节点无人认领**：两 BPMN 的 `directorApproval` 用 `candidateGroups="directors"`，单 admin 环境下无此组，第二节点任务永远不被 `pending`（按 assignee 过滤）列出 → 流程卡死在第一节点之后。

### 2.3 业务概念冲突

`LaborPlanChangeController.create()` **已经**启动 `laborChangeApproval` 流程（businessKey = `LABOR_CHANGE_<id>`），但其 `approve/reject` 端点**绕过 Flowable 直接改数据库状态**，导致：
- Flowable 中的 userTask 永远是孤儿任务（永不 complete）；
- labor-plan-change 页的"通过/驳回"与"待办任务"页的审批**双重审批、状态不一致**。

### 2.4 businessKey 格式不统一

- `ApprovalController.submit`：`planType:planId`（如 `labor:123`）
- `LaborPlanChangeController.create`：`LABOR_CHANGE_<id>`（无法被 `:` 解析）

导致待办页无法从 labor 流程的 businessKey 解析出 planType/planId。

---

## 3. 目标架构

```
┌───────────────────────── 前端 web-antdv-next ─────────────────────────┐
│  工作流菜单                                                            │
│  ├─ 人力计划变更 labor-plan-change  (create 即启动流程, 不再自带审批)   │
│  ├─ 待办任务   todo      ── /approval/pending + /approval/approve|reject│
│  ├─ 已办任务   done      ── /approval/done                            │
│  ├─ 流程定义   definition ── /flowable/process-definition/*          │
│  └─ 我发起的   my        ── /approval/my + /approval/history/{t}/{id} │
└──────────────────────────────────────────────────────────────────────┘
                                 │  HTTP (proxy /api/v1 → :8080)
┌───────────────────────── 后端 rpw (Spring Boot) ─────────────────────┐
│  ApprovalController        (/api/v1/approval)                         │
│    submit / approve / reject / history / pending / done / my         │
│  ProcessDefinitionController (/api/v1/flowable/process-definition)   │
│    deploy / deploy/xml / list / delete / {id}/xml                    │
│  LaborPlanChangeController  (/api/v1/resource-plan/labor-change)     │
│    create(启动流程) / list  （审批交给工作流, 不再自带 approve/reject） │
│  Flowable Engine (Runtime/Task/History/Repository Service)           │
└──────────────────────────────────────────────────────────────────────┘
```

### 3.1 businessKey 约定（统一）

所有流程实例 businessKey 统一为 **`planType:planId`**：
- `material:<id>` → 物资计划变更
- `labor:<id>` → 劳动力计划变更（`LaborPlanChange.id`）
- `subcontract:<id>` → 分包计划变更

`pending` / `done` / `my` 端点据此解析出 `planType` 与 `planId`，前端可据此钻取业务详情。

---

## 4. 端点映射表（前端应改成的真实端点）

| 能力 | 旧（不存在） | 新（真实存在/新增） |
|---|---|---|
| 待办列表 | `GET /flowable/task/todo` | `GET /approval/pending` |
| 审批通过 | `POST /approval/approve`（已存在） | 保留 |
| 审批驳回 | `POST /approval/reject`（已存在） | 保留 |
| 已办列表 | `GET /flowable/task/history` | `GET /approval/done`（新增） |
| 我发起的 | `GET /flowable/process-instance/my` | `GET /approval/my`（新增） |
| 审批历史(某业务) | `/approval/history/{t}/{id}`（已存在） | 保留（用于 my 详情） |
| 流程定义列表 | `GET /flowable/process-definition/list`（已存在） | 保留 |
| 删除流程定义 | `DELETE /flowable/process-definition/{id}`（已存在） | 保留 |
| 部署流程定义 | `POST /flowable/process-definition/deploy/xml`（已存在） | 保留 |
| 查看 BPMN XML | （无） | `GET /flowable/process-definition/{id}/xml`（已存在，前端新增调用） |
| 提交审批 | `POST /flowable/process-instance/start`（不存在） | `POST /approval/submit`（已存在，labor-plan-change 复用） |

---

## 5. 各页面详细设计

### 5.1 待办任务 `workflow/todo`
- 数据源：`getPendingTasks()` → `GET /approval/pending`，返回 `PendingTaskDTO[]`（`taskId, taskName, assignee, createTime, planType, planId`）。
- 列表列：任务名称、处理人、业务类型(planType 中文)、业务ID(planId)、创建时间。
- 操作：【办理】打开弹窗填写意见 → `approveTask`（通过）/ `rejectTask`（驳回，必填意见）。
- 完成后刷新列表。

### 5.2 已办任务 `workflow/done`
- 数据源：`getDoneTasks()` → `GET /approval/done`（新增），返回 `DoneTaskDTO`（`taskId, taskName, assignee, startTime, endTime, planType, planId`）。
- 列表列：任务名称、处理人、业务类型、业务ID、开始时间、结束时间。
- 只读，无操作（可后续加"进度图"钻取）。

### 5.3 我发起的 `workflow/my`
- 数据源：`getMyProcesses()` → `GET /approval/my`（新增），返回 `MyProcessDTO`（`processInstanceId, processDefinitionKey, businessKey, status(RUNNING/COMPLETED), startTime, endTime`）。
- 列表列：流程实例ID、流程标识、业务标识、状态、开始/结束时间。
- 操作：【详情】→ 调用 `getApprovalHistory(planType, planId)` 展示各节点审批历史（任务名/处理人/时间/意见）。

### 5.4 流程定义 `workflow/definition`
- 数据源：`getProcessDefinitions()` → `GET /flowable/process-definition/list`，返回 `ProcessDefinitionDTO[]`（`id, key, name, version, deploymentId`）。
- 列表列：流程名称、流程标识、版本、部署ID。
- 操作：【查看BPMN】→ `getProcessDefinitionXml(id)` 以只读文本/高亮展示 XML；【删除】→ `deleteProcessDefinition(deploymentId, cascade=true)`。
- 工具栏：【部署(XML)】粘贴 BPMN XML → `deployProcessDefinitionByXml`。

### 5.5 人力计划变更 `labor-plan-change`
- `create` 已启动 `laborChangeApproval` 流程（businessKey 改为 `labor:<id>`）。
- **移除**页面直接的"通过/驳回"按钮（消除孤儿任务与双重审批）；审批统一在"待办任务"完成。
- 保留"审批状态"列；状态=REJECTED 时提供"重新提交"→ `POST /approval/submit?planType=labor&planId=<id>`。
- 新增变更后，回到"待办任务"即可看到待审批任务。

---

## 6. 后端改动清单

### 6.1 BPMN（Phase 1 必改）
- `material-approval.bpmn20.xml`：删除两处 `<flowable:taskListener event="complete" class="com.company.rpw.listener.ApprovalTaskListener"/>`；`directorApproval` 的 `flowable:candidateGroups="directors"` → `flowable:assignee="${applicant}"`。
- `labor-change-approval.bpmn20.xml`：`directorApproval` 的 `flowable:candidateGroups="directors"` → `flowable:assignee="${applicant}"`（该文件无监听器，无需删）。
- 网关变量：**保持两流程各自的 `approvalResult` / `approved` 不变**，由 approve/reject 同时设置两个变量来满足（见 6.2）。

### 6.2 ApprovalController（Phase 1 必改）
- `submit`：`planType` → 流程 key 映射表
  - `material → materialChangeApproval`
  - `labor → laborChangeApproval`
  - `subcontract → subcontractChangeApproval`
  - businessKey 统一为 `planType:planId`；变量增加 `applicant = getCurrentUser()`。
- `approve` / `reject`：完成时同时设置 `approvalResult`（APPROVED/REJECTED）与 `approved`（true/false）两个流程变量，兼容两种网关。
- `updatePlanStatus`：
  - `labor` 改为更新 `LaborPlanChange` 记录（注入 `LaborPlanChangeService`），最终 APPROVED 时把新值回写到 `ResourcePlanLabor`；
  - `material` / `subcontract` 维持更新各自实体。
- 新增 `GET /approval/done`：基于 `HistoryService` 查询 `taskAssignee=currentUser` 且已结束的 `HistoricTaskInstance`，附带从 `businessKey` 解析 `planType/planId`。
- 新增 `GET /approval/my`：基于 `HistoryService`（含运行中+已结束）查询 `startUserId=currentUser` 的流程实例；运行中实例从 `RuntimeService` 取状态，已结束取 `endTime` 判定 `COMPLETED`。返回 `MyProcessDTO`。

### 6.3 LaborPlanChangeController（Phase 1 必改）
- `create`：businessKey 由 `LABOR_CHANGE_<id>` 改为 `labor:<id>`，保持启动 `laborChangeApproval`。
- 移除 `approve` / `reject` 端点（审批改由工作流 `/approval/*` 统一处理），避免孤儿任务与状态冲突。

---

## 7. 分阶段开发计划

### Phase 1 — 打通真实可跑的审批生命周期（本次实现）
1. 后端：BPMN 修复（6.1）+ ApprovalController 修复（6.2）+ 新增 done/my（6.2）+ LaborPlanChangeController（6.3）。
2. 前端：workflow API 重写；待办/已办/我发起的/流程定义 四页重写；labor-plan-change 接入工作流。
3. 验证：构建后端、curl 验证全链路、前端 HMR 编译通过、菜单端到端走通。

### Phase 2 — 参考 bpm 增强体验
- 审批进度图（BPMN 高亮当前节点）：复用 `bpm/components/bpmn-process-designer` 的 `ProcessViewer`，结合 `GET /flowable/process-definition/{id}/xml` + 运行时活跃节点。
- 发起流程入口页（选择计划类型 + 业务ID 调用 `/approval/submit`）。
- 流程定义可视化部署 UI（接入 bpm 设计器，替代纯 XML 粘贴）。

### Phase 3 — 高级能力（可选）
- 表单管理（`bpm/form`）：任务绑定业务表单。
- 抄送、转派、加签、退回（`/bpm/task/*` 动作），需在后端补齐对应 Flowable 动作端点。
- 多候选人/候选组审批（`directors` 组真实化）。

---

## 8. 验证方案

**后端（curl，base=http://127.0.0.1:8080/api/v1）**
1. `GET /flowable/process-definition/list` → 应返回 materialChangeApproval / laborChangeApproval。
2. `POST /resource-plan/labor-change` 创建变更 → 返回含 `processInstanceId`。
3. `GET /approval/pending` → 出现刚创建的待办（assignee=当前用户）。
4. `POST /approval/approve?taskId=xxx` → 进入 director 节点；再次 pending 出现 director 任务。
5. 再次 approve → 流程结束；`GET /approval/done` 出现该任务；`GET /approval/my` 出现该实例(status=COMPLETED)；`GET /approval/history/labor/<id>` 出现两条历史。
6. `GET /flowable/process-definition/{id}/xml` → 返回 BPMN 文本。

**前端**
- dev 5666 打开 工作流 → 待办任务可见数据、可审批；已办/我发起的/流程定义均有数据；labor-plan-change 创建后流转到待办。

---

## 9. 风险与回滚
- BPMN 改 assignee 为 `${applicant}` 仅影响新部署流程；已部署旧版本仍在，验证用最新版本即可。
- 移除 `LaborPlanChangeController.approve/reject` 会影响任何直接调用方；当前仅 labor-plan-change 前端页面使用，已同步改为走工作流，无残留调用。
- 所有改动向后兼容：新增端点不影响既有 `/approval/submit|approve|reject|history|pending` 契约。
