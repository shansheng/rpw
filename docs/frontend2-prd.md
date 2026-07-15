# Frontend2 — 项目资源计划预警系统（Ant Design Vue 重构版）

## 产品需求文档（PRD）

| 版本 | 日期 | 作者 | 说明 |
|------|------|------|------|
| 1.0 | 2026-05-11 | 许清楚 | Frontend2 初始版本 |

---

## 1. 项目信息

- **Language**: 中文
- **Programming Language**: Vite + Vue 3 + TypeScript + Ant Design Vue 4.x + Tailwind CSS
- **Project Name**: `frontend2`
- **原始需求**：现有前端使用 Element Plus，现需新建一套 frontend2，使用 Ant Design Vue 全新开发，保持与现有后端 API 完全兼容，同时提供更加专业大气的 UI 体验。

---

## 2. 产品定义

### 2.1 Product Goals

| # | 目标 | 描述 |
|---|------|------|
| G1 | **UI 体验升级** | 使用 Ant Design Vue 组件库，打造专业、大气的企业级界面，提升用户视觉体验和操作效率 |
| G2 | **功能全量覆盖** | 完整覆盖现有前端所有路由页面和功能模块，与后端 API 100% 兼容，无功能遗漏 |
| G3 | **架构清晰可维护** | 采用模块化路由和组件设计，统一的布局、状态管理、请求封装规范，便于后续迭代和维护 |

### 2.2 User Stories

| ID | User Story |
|----|------------|
| US-01 | As a **项目人员**, I want 一个清晰的工作台首页，以便快速了解我的待办任务、预警信息和流程状态 |
| US-02 | As a **公司管理员**, I want 通过组织架构树形表格管理公司和项目层级关系，以便维护组织数据 |
| US-03 | As a **计划填报人员**, I want 在统一的资源计划列表中管理8种资源类型（材料/设备/五金/周转材/办公用品/安全物资/分包/劳动力），以便快速完成计划填报和编辑 |
| US-04 | As a **预警负责人**, I want 通过预警看板和规则配置全面掌控预警状态，以便及时处理异常 |
| US-05 | As a **审批人员**, I want 通过流程审批模块处理待办任务、查看审批历史和设计流程，以便高效完成审批流转 |

---

## 3. 页面清单

### 3.1 页面总览

| # | 页面 | 路由路径 | 优先级 | 功能要点 |
|---|------|---------|--------|---------|
| 1 | **登录页** | `/login` | P0 | 居中卡片式登录，背景渐变，记住密码 |
| 2 | **首页（工作台）** | `/dashboard` | P0 | 统计卡片(待办数/预警数/流程数) + 待办任务列表 + 预警概览 |
| 3 | **组织架构** | `/organization` | P0 | 树形表格展示局-公司-项目三级结构，支持CRUD |
| 4 | **公司管理** | `/company` | P0 | 表格+搜索+新建编辑弹窗 |
| 5 | **项目管理** | `/project` | P0 | 表格+搜索+新建编辑弹窗 |
| 6 | **资源计划（8种类型）** | `/resource-plan/:type` | P0 | 统一的CRUD列表+详情+创建+编辑，类型包含material/equipment/hardware/circulation/office/safety/subcontract/labor |
| 7 | **预警管理** | 见预警子页面 | P0 | 看板/规则配置/历史/统计四个子页面 |
| 8 | **流程审批** | 见流程子页面 | P0 | 待办任务/我发起的/审批历史/流程管理/设计器 |
| 9 | **看板** | `/kanban` | P1 | Kanban拖拽看板 |
| 10 | **报表** | 见报表子页面 | P1 | 配置/预览/导出 |
| 11 | **字典管理** | `/dict` | P1 | 字典值管理 |
| 12 | **企业微信配置** | `/wecom/config` | P2 | 企微集成配置 |

---

### 3.2 页面详细设计

#### 3.2.1 登录页 `/login`

**布局**：居中卡片式，全屏渐变背景

**设计要点**：
- 背景使用深蓝到浅蓝的渐变色（`#001529` → `#1890ff`）
- 登录卡片居中，白色背景，圆角阴影
- 卡片左侧可放置系统Logo和名称"资源计划预警系统"
- 右侧表单包含：用户名输入、密码输入、记住密码复选框、登录按钮
- 支持回车提交

**功能**：
- 调用 `AuthController` 登录接口
- 登录成功后将 token 存入 localStorage，跳转至首页
- 登录失败显示错误提示

---

#### 3.2.2 首页（工作台）`/dashboard`

**布局**：顶部统计卡片 + 中间待办任务列表 + 底部预警概览

**设计要点**：
- 顶部行：3-4个统计卡片（a-card），分别展示：
  - 待办任务数（蓝色主题）
  - 待处理预警数（红色主题）
  - 进行中流程数（绿色主题）
  - 本月计划数（橙色主题）
- 中间区域：左侧待办任务列表（a-table，显示最新5条），右侧日历或快捷入口
- 底部：预警概览表格（显示最新的预警记录）

**数据来源**：
- 任务统计：`TaskController`
- 预警统计：`WarningRecordController`
- 流程统计：`ProcessInstanceController`

---

#### 3.2.3 组织架构 `/organization`

**布局**：树形表格

**设计要点**：
- 使用 a-table + 树形数据展示（`children` 字段）
- 层级：局 → 公司 → 项目（固定3级）
- 每级包含：部门、处室字段
- 顶部工具栏：新增根节点、展开/收起、刷新
- 每行操作：编辑、新增子节点、删除

**设计规范中的斑马纹表格配置**：
```typescript
// a-table 斑马纹
:rowClassName="(_record, index) => (index % 2 === 1 ? 'table-row-striped' : '')"
```

**后端API**：`OrganizationController`

---

#### 3.2.4 公司管理 `/company`

**布局**：表格 + 搜索筛选区 + 新建/编辑弹窗

**设计要点**：
- 搜索筛选区使用 a-form inline 布局：
  - 公司名称（输入框）
  - 状态（下拉选择）
  - 搜索/重置按钮
- 表格操作栏：新建公司按钮
- 表格列：公司名称、编码、联系人、联系电话、状态、创建时间、操作（编辑/删除）
- 弹窗使用 a-modal，内含 a-form 表单

**后端API**：`CompanyController`

---

#### 3.2.5 项目管理 `/project`

**布局**：表格 + 搜索筛选区 + 新建/编辑弹窗

**设计要点**：
- 搜索筛选区：项目名称、所属公司、状态
- 表格列：项目名称、项目编码、所属公司、负责人、开工日期、计划完工日期、状态、操作
- 新建/编辑弹窗表单字段：项目名称、编码、所属公司（下拉选择）、负责人、日期等
- 支持分页

**后端API**：`ProjectController`

---

#### 3.2.6 资源计划 `/resource-plan/:type`

**支持8种类型**（通过 type 参数区分）：

| 类型值 | 显示名称 | 后端Controller |
|--------|---------|----------------|
| material | 材料计划 | ResourcePlanMaterialController |
| equipment | 设备计划 | ResourcePlanEquipmentController |
| hardware | 五金计划 | ResourcePlanHardwareController |
| circulation | 周转材计划 | ResourcePlanCirculationController |
| office | 办公用品计划 | ResourcePlanOfficeController |
| safety | 安全物资计划 | ResourcePlanSafetyController |
| subcontract | 分包计划管理 | ResourcePlanSubcontractController |
| labor | 劳动力计划管理 | ResourcePlanLaborController |

**布局**：统一的CRUD列表 + 详情 + 创建 + 编辑

**设计要点**：
- 列表页包含：搜索筛选区 + 表格 + 新建按钮
- 搜索筛选：项目名称、物资名称、状态、日期范围
- 表格列：序号、项目名称、计划类型、物资名称、规格型号、单位、数量、状态、创建时间、操作（查看/编辑/删除）
- 创建/编辑使用 a-modal 或独立页面，包含：
  - 基本信息区：使用部位、物资名称、规格型号、单位、数量
  - 生产备货区：采购进展、供应商、时间节点
  - 发运进展区：发运情况、船型、预计离港/到港日期
- 详情页展示完整计划信息及进度

**后端API**：对应的 ResourcePlan*Controller

---

#### 3.2.7 预警管理

包含4个子页面：

##### 预警看板 `/warning/board`
- 卡片布局展示预警概览统计
- 红色/黄色预警数量卡片
- 按公司/项目分组的预警分布图表
- 最新预警滚动列表

##### 规则配置 `/warning/rule`
- 表格展示各公司的预警规则
- 字段：公司名称、预警提前天数、启用类型、通知渠道、状态
- 新建/编辑弹窗
- **后端API**：`WarningRuleController`

##### 预警历史 `/warning/history`
- 表格展示所有已处理的预警记录
- 筛选：预警类型、时间范围、公司/项目
- 支持导出

##### 预警统计 `/warning/statistics`
- 图表展示预警趋势
- 按月份/季度统计预警数量
- 按公司排名

**后端API**：`WarningRecordController`, `WarningRuleController`

---

#### 3.2.8 流程审批

包含5个子页面：

##### 待办任务 `/flowable/task`
- 表格展示当前用户的待办任务
- 列：任务名称、流程名称、发起人、创建时间、操作（办理）
- 办理弹窗：审批意见、附件上传、通过/驳回按钮
- **后端API**：`TaskController`, `FlowableAdminController`

##### 我发起的 `/flowable/my-initiated`
- 表格展示当前用户发起的审批流程
- 列：流程名称、当前节点、状态、发起时间、操作（查看流程图）

##### 审批历史 `/flowable/history`
- 表格展示已完成的审批记录
- 可按时间范围、流程类型筛选

##### 流程管理 `/flowable/admin`
- 流程定义管理
- 部署新流程、挂起/激活流程
- **后端API**：`ProcessDefinitionController`, `FlowableAdminController`

##### 流程设计器 `/flowable/designer`
- 使用 Flowable 流程设计器组件
- 可视化拖拽设计流程图
- 发布/保存流程定义
- **后端API**：`ProcessDefinitionController`

---

#### 3.2.9 看板 `/kanban`

**布局**：Kanban 拖拽看板

**设计要点**：
- 使用拖拽库（如 vuedraggable 或 @vueuse/gesture）
- 列：待办 → 进行中 → 已完成（可自定义列）
- 卡片展示任务标题、负责人、优先级、截止日期
- 支持跨列拖拽
- 支持新建看板卡片

**后端API**：`KanbanBoardController`

---

#### 3.2.10 报表

包含3个子页面：

##### 自定义报表 `/report/config`
- 配置报表的维度、指标
- 选择资源计划类型、日期范围、公司/项目

##### 报表预览 `/report/preview`
- 展示生成的报表数据
- 表格 + 图表混合展示

##### 导出中心 `/report/export`
- 导出任务列表
- 支持 Excel 导出
- 查看导出历史、下载已完成的导出文件

**后端API**：`CustomReportController`

---

#### 3.2.11 字典管理 `/dict`

**布局**：表格 + 搜索 + 新建/编辑弹窗

**设计要点**：
- 字典类型管理（左侧树或下拉选择）
- 字典值表格展示
- 新建/编辑弹窗
- 支持批量导入

**后端API**：`DictController`, `DictLaborCategoryController`, `DictWorkTypeController`

---

#### 3.2.12 企业微信配置 `/wecom/config`

**布局**：表单配置页

**设计要点**：
- 企业微信应用配置表单
- CorpID、AgentID、Secret 等配置项
- 测试连接按钮
- 启用/禁用开关

**后端API**：`WeComController`

---

## 4. 菜单结构（侧边栏导航）

```
📊 资源计划预警系统
├── 📊 首页（工作台）          /dashboard
├── 🏢 组织架构               /organization
├── 🏛️ 公司管理               /company
├── 📁 项目管理                /project
├── 📋 资源计划                /resource-plan
│   ├── 材料计划              /resource-plan/material
│   ├── 设备计划              /resource-plan/equipment
│   ├── 五金计划              /resource-plan/hardware
│   ├── 周转材计划            /resource-plan/circulation
│   ├── 办公用品计划          /resource-plan/office
│   ├── 安全物资计划          /resource-plan/safety
│   ├── 分包计划              /resource-plan/subcontract
│   └── 劳动力计划            /resource-plan/labor
├── ⚠️ 预警管理                /warning
│   ├── 预警看板              /warning/board
│   ├── 规则配置              /warning/rule
│   ├── 预警历史              /warning/history
│   └── 预警统计              /warning/statistics
├── 🔄 流程审批                /flowable
│   ├── 待办任务              /flowable/task
│   ├── 我发起的              /flowable/my-initiated
│   ├── 审批历史              /flowable/history
│   ├── 流程管理              /flowable/admin
│   └── 流程设计器            /flowable/designer
├── 📌 看板                   /kanban
├── 📊 报表                   /report
│   ├── 自定义报表            /report/config
│   ├── 报表预览              /report/preview
│   └── 导出中心              /report/export
├── 📖 字典管理               /dict
└── 🔗 企业微信               /wecom/config
```

---

## 5. 设计原则与UI规范

### 5.1 整体布局

```
┌──────────────────────────────────────────────────────────────┐
│ 侧边栏 (#001529)        │ 顶部栏 (白色 #fff)                  │
│                         │ ┌─── 折叠按钮 ── 面包屑 ── 用户 ──┤
│   Logo区域              │                                      │
│   ┌─────────────────┐   │ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ │
│   │ 🏢 组织架构      │   │ 内容区域（#f0f2f5 背景）            │
│   │ 🏛️ 公司管理      │   │                                      │
│   │ 📁 项目管理      │   │  ┌──────────────────────────────┐   │
│   │ 📋 资源计划 ▸    │   │  │ 页面标题                      │   │
│   │ ⚠️ 预警管理 ▸    │   │  │                              │   │
│   │ 🔄 流程审批 ▸    │   │  │  搜索筛选区（a-form inline）  │   │
│   │ 📌 看板          │   │  │                              │   │
│   │ 📊 报表 ▸        │   │  │  ┌────────表格────────────┐  │   │
│   │ 🔗 企业微信      │   │  │  │ 斑马纹 a-table         │  │   │
│   └─────────────────┘   │  │  │                        │  │   │
│   (可折叠至图标宽度)     │  │  └────────────────────────┘  │   │
│                         │  └──────────────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
```

### 5.2 设计规范

| 项目 | 规范 | 说明 |
|------|------|------|
| **主色** | `#1890ff` | Ant Design Blue，用于按钮、链接、选中状态 |
| **侧边栏背景** | `#001529` | 深色侧边栏，文字白色，选中项高亮为主色 |
| **顶部栏** | 白色 `#fff` | 顶部面包屑 + 用户信息 + 退出按钮 |
| **内容区背景** | `#f0f2f5` | 浅灰背景，与卡片和表格形成对比 |
| **字体** | `-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif` | 系统默认字体栈 |
| **卡片** | a-card，圆角8px，白色背景，轻微阴影 | 用于仪表盘统计 |
| **表格** | a-table，斑马纹（奇数行浅灰背景），固定表头 | 数据列表展示 |
| **表单** | a-form inline 布局用于搜索区，a-form vertical 布局用于弹窗 | 搜索和编辑 |
| **弹窗** | a-modal，居中，宽度默认 600px（大表单可 800px） | 新建/编辑 |
| **按钮** | a-button type="primary"（主色#1890ff）用于主要操作 | 新建、保存、提交 |
| **面包屑** | a-breadcrumb，放在顶部栏左侧 | 页面导航路径 |
| **分页** | a-pagination，表格自带分页 | 列表分页 |

### 5.3 响应式与交互规范

- 侧边栏支持折叠（展开220px，折叠64px）
- 表格支持列排序和筛选
- 弹窗支持拖拽（如需要）
- 操作成功/失败使用 a-message 提示
- 危险操作（删除）使用 a-modal confirm 二次确认

---

## 6. 技术规范

### 6.1 Requirements Pool

#### P0 — 必须上线

| ID | 模块 | 需求描述 |
|----|------|----------|
| P0-01 | 登录 | 登录页卡片式布局，支持用户名密码认证，token 管理 |
| P0-02 | 布局 | 深色侧边栏+白色顶部栏布局，侧边栏可折叠，面包屑导航，用户信息展示 |
| P0-03 | 首页 | 工作台仪表盘，统计卡片+待办任务+预警概览 |
| P0-04 | 组织架构 | 树形表格展示局-公司-项目三级结构，CRUD |
| P0-05 | 公司管理 | 表格+搜索+新建编辑弹窗 |
| P0-06 | 项目管理 | 表格+搜索+新建编辑弹窗 |
| P0-07 | 资源计划 | 8种资源类型统一CRUD列表+详情+创建+编辑，与对应后端API对接 |
| P0-08 | 预警看板 | 预警概览统计卡片+预警分布 |
| P0-09 | 预警规则 | 规则配置表格+CRUD弹窗 |
| P0-10 | 预警历史 | 预警记录表格+筛选 |
| P0-11 | 预警统计 | 预警趋势统计 |
| P0-12 | 待办任务 | 待办任务表格+审批操作 |
| P0-13 | 我发起的 | 发起流程列表 |
| P0-14 | 审批历史 | 已完成的审批记录 |
| P0-15 | 请求封装 | Axios 请求封装，统一错误处理，token 拦截器 |

#### P1 — 重要功能

| ID | 模块 | 需求描述 |
|----|------|----------|
| P1-01 | 流程管理 | 流程定义管理，部署/挂起/激活 |
| P1-02 | 流程设计器 | Flowable 流程设计器集成 |
| P1-03 | 看板 | Kanban拖拽看板 |
| P1-04 | 报表配置 | 自定义报表配置页面 |
| P1-05 | 报表预览 | 报表数据展示 |
| P1-06 | 导出中心 | 导出任务列表+下载 |
| P1-07 | 字典管理 | 字典值CRUD管理 |

#### P2 — 优化功能

| ID | 模块 | 需求描述 |
|----|------|----------|
| P2-01 | 企业微信 | 企微配置页面 |
| P2-02 | 国际化 | 中英文切换（如需要） |
| P2-03 | 主题切换 | 暗黑模式支持 |
| P2-04 | 权限管理 | 基于角色的按钮级权限控制 |

### 6.2 路由设计

```typescript
// router/index.ts
const routes = [
  // 无需认证
  { path: '/login', component: LoginPage, meta: { requiresAuth: false } },
  
  // 需要认证，使用 Layout 包裹
  {
    path: '/',
    component: Layout,
    meta: { requiresAuth: true },
    children: [
      { path: 'dashboard', name: 'Dashboard', meta: { title: '首页' } },
      { path: 'organization', name: 'Organization', meta: { title: '组织架构' } },
      { path: 'company', name: 'Company', meta: { title: '公司管理' } },
      { path: 'project', name: 'Project', meta: { title: '项目管理' } },
      
      // 资源计划 - 8种类型
      { path: 'resource-plan/material', meta: { title: '材料计划' } },
      { path: 'resource-plan/equipment', meta: { title: '设备计划' } },
      { path: 'resource-plan/hardware', meta: { title: '五金计划' } },
      { path: 'resource-plan/circulation', meta: { title: '周转材计划' } },
      { path: 'resource-plan/office', meta: { title: '办公用品计划' } },
      { path: 'resource-plan/safety', meta: { title: '安全物资计划' } },
      { path: 'resource-plan/subcontract', meta: { title: '分包计划' } },
      { path: 'resource-plan/labor', meta: { title: '劳动力计划' } },
      
      // 预警管理
      { path: 'warning/board', meta: { title: '预警看板' } },
      { path: 'warning/rule', meta: { title: '规则配置' } },
      { path: 'warning/history', meta: { title: '预警历史' } },
      { path: 'warning/statistics', meta: { title: '预警统计' } },
      
      // 流程审批
      { path: 'flowable/task', meta: { title: '待办任务' } },
      { path: 'flowable/my-initiated', meta: { title: '我发起的' } },
      { path: 'flowable/history', meta: { title: '审批历史' } },
      { path: 'flowable/admin', meta: { title: '流程管理' } },
      { path: 'flowable/designer', meta: { title: '流程设计器' } },
      
      // 看板
      { path: 'kanban', meta: { title: '看板' } },
      
      // 报表
      { path: 'report/config', meta: { title: '自定义报表' } },
      { path: 'report/preview', meta: { title: '报表预览' } },
      { path: 'report/export', meta: { title: '导出中心' } },
      
      // 其他
      { path: 'dict', meta: { title: '字典管理' } },
      { path: 'wecom/config', meta: { title: '企业微信配置' } },
    ]
  }
]
```

### 6.3 项目目录结构

```
frontend2/
├── public/
├── src/
│   ├── api/                    # API 接口封装
│   │   ├── request.ts          # Axios 实例 + 拦截器
│   │   ├── auth.ts             # 认证相关接口
│   │   ├── organization.ts     # 组织架构接口
│   │   ├── company.ts          # 公司管理接口
│   │   ├── project.ts          # 项目管理接口
│   │   ├── resource-plan.ts    # 资源计划接口
│   │   ├── warning.ts          # 预警管理接口
│   │   ├── flowable.ts         # 流程审批接口
│   │   ├── kanban.ts           # 看板接口
│   │   ├── report.ts           # 报表接口
│   │   └── dict.ts             # 字典接口
│   │
│   ├── components/             # 通用组件
│   │   ├── AppBreadcrumb.vue   # 面包屑
│   │   └── StatCard.vue        # 统计卡片
│   │
│   ├── layouts/
│   │   └── MainLayout.vue      # 主布局（侧边栏+顶部栏+内容区）
│   │
│   ├── router/
│   │   └── index.ts            # 路由配置
│   │
│   ├── stores/                 # Pinia 状态管理
│   │   ├── user.ts             # 用户状态
│   │   └── app.ts              # 应用状态（侧边栏折叠等）
│   │
│   ├── types/                  # TypeScript 类型定义
│   │   └── index.ts
│   │
│   ├── utils/                  # 工具函数
│   │   └── index.ts
│   │
│   └── views/                  # 页面组件
│       ├── login/
│       │   └── index.vue
│       ├── dashboard/
│       │   └── index.vue
│       ├── organization/
│       │   └── index.vue
│       ├── company/
│       │   └── index.vue
│       ├── project/
│       │   └── index.vue
│       ├── resource-plan/      # 8种子类型
│       │   ├── material/
│       │   ├── equipment/
│       │   ├── hardware/
│       │   ├── circulation/
│       │   ├── office/
│       │   ├── safety/
│       │   ├── subcontract/
│       │   └── labor/
│       ├── warning/
│       │   ├── board/
│       │   ├── rule/
│       │   ├── history/
│       │   └── statistics/
│       ├── flowable/
│       │   ├── task/
│       │   ├── my-initiated/
│       │   ├── history/
│       │   ├── admin/
│       │   └── designer/
│       ├── kanban/
│       │   └── index.vue
│       ├── report/
│       │   ├── config.vue
│       │   ├── preview.vue
│       │   └── export.vue
│       ├── dict/
│       │   └── index.vue
│       └── wecom/
│           └── config/
│               └── index.vue
│
├── index.html
├── vite.config.ts
├── package.json
├── tsconfig.json
└── tailwind.config.js
```

### 6.4 技术选型

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | ^3.4 | 前端框架 |
| TypeScript | ^5.x | 类型安全 |
| Vite | ^5.x | 构建工具 |
| Ant Design Vue | ^4.x | UI 组件库 |
| Tailwind CSS | ^3.x | 原子化 CSS（辅助布局） |
| Pinia | ^2.x | 状态管理 |
| Vue Router | ^4.x | 路由 |
| Axios | ^1.x | HTTP 请求 |
| dayjs | ^1.x | 日期处理（Ant Design Vue 内置依赖） |
| vuedraggable | ^4.x | 拖拽（看板功能） |
| Less/Sass | - | 样式预处理（Ant Design Vue 推荐 Less） |

---

## 7. 后端API兼容性

### 7.1 API 端点映射

| 模块 | 基础路径 | 控制器 |
|------|---------|--------|
| 认证 | `/api/auth` | AuthController |
| 组织架构 | `/api/organization` | OrganizationController |
| 公司管理 | `/api/company` | CompanyController |
| 项目管理 | `/api/project` | ProjectController |
| 资源计划-材料 | `/api/resource-plan/material` | ResourcePlanMaterialController |
| 资源计划-设备 | `/api/resource-plan/equipment` | ResourcePlanEquipmentController |
| 资源计划-五金 | `/api/resource-plan/hardware` | ResourcePlanHardwareController |
| 资源计划-周转材 | `/api/resource-plan/circulation` | ResourcePlanCirculationController |
| 资源计划-办公用品 | `/api/resource-plan/office` | ResourcePlanOfficeController |
| 资源计划-安全物资 | `/api/resource-plan/safety` | ResourcePlanSafetyController |
| 资源计划-分包 | `/api/resource-plan/subcontract` | ResourcePlanSubcontractController |
| 资源计划-劳动力 | `/api/resource-plan/labor` | ResourcePlanLaborController |
| 预警记录 | `/api/warning/record` | WarningRecordController |
| 预警规则 | `/api/warning/rule` | WarningRuleController |
| 流程任务 | `/api/flowable/task` | TaskController |
| 流程管理 | `/api/flowable/admin` | FlowableAdminController |
| 流程定义 | `/api/process-definition` | ProcessDefinitionController |
| 流程实例 | `/api/process-instance` | ProcessInstanceController |
| 审批 | `/api/approval` | ApprovalController |
| 看板 | `/api/kanban` | KanbanBoardController |
| 报表 | `/api/report` | CustomReportController |
| 字典 | `/api/dict` | DictController |
| 企业微信 | `/api/wecom` | WeComController |
| 导入 | `/api/import` | ImportController |
| 财务Mock | `/api/finance` | FinanceMockController |

### 7.2 Axios 请求封装规范

```typescript
// request.ts 核心配置
const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

// 请求拦截器 — 注入 token
service.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截器 — 统一错误处理
service.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response?.status === 401) {
      // token 过期，跳转登录
      localStorage.removeItem('token')
      router.push('/login')
    }
    return Promise.reject(error)
  }
)
```

---

## 8. Open Questions

| # | 问题 | 建议方案 |
|---|------|---------|
| Q1 | 8种资源计划类型是否可共用同一个通用组件？ | 建议先以 material 为模板开发通用CRUD组件，其余7种通过配置差异化字段实现 |
| Q2 | 流程设计器使用 Flowable 官方组件还是第三方封装？ | 建议使用 Flowable 官方提供的 Vue 集成组件或 iframe 嵌入 |
| Q3 | 看板拖拽库选择？ | 推荐 `vuedraggable@next`（基于 SortableJS，Vue 3 兼容） |
| Q4 | 是否需要兼顾 IE 浏览器？ | 建议不做支持，Ant Design Vue 4.x 已放弃 IE |
| Q5 | 路由模式使用 history 还是 hash？ | 继续使用 history 模式，与现有前端保持一致 |

---

## 9. 附录

### 9.1 Ant Design Vue 组件使用清单

| 组件 | 用途 |
|------|------|
| a-layout, a-layout-sider, a-layout-header, a-layout-content | 页面整体布局 |
| a-menu, a-menu-item, a-sub-menu | 侧边栏导航 |
| a-breadcrumb | 面包屑导航 |
| a-table | 数据表格（斑马纹） |
| a-form, a-form-item | 搜索筛选和编辑表单 |
| a-input, a-input-number | 文本和数字输入 |
| a-select | 下拉选择 |
| a-date-picker | 日期选择 |
| a-modal | 弹窗 |
| a-button | 按钮 |
| a-card | 统计卡片 |
| a-tag | 标签（状态标识） |
| a-badge | 徽标（待办计数） |
| a-message, a-notification | 消息提示 |
| a-popconfirm | 删除确认 |
| a-pagination | 分页 |
| a-spin | 加载中 |
| a-empty | 空状态 |
| a-result | 结果页 |
| a-statistic | 统计数值 |
| a-progress | 进度条 |
| a-tabs | 标签页（预警子页面切换） |
| a-collapse | 折叠面板 |
| a-upload | 文件上传 |

### 9.2 与现有 Frontend (Element Plus) 的差异对照

| 维度 | 现有 Frontend (Element Plus) | Frontend2 (Ant Design Vue) |
|------|---------------------------|--------------------------|
| UI 组件库 | Element Plus | Ant Design Vue 4.x |
| 侧边栏色 | `#304156` | `#001529` |
| 主色 | `#409EFF` | `#1890ff` |
| 布局组件 | el-container/el-aside/el-header/el-main | a-layout/a-layout-sider/a-layout-header/a-layout-content |
| 表格 | el-table | a-table（自带斑马纹支持） |
| 弹窗 | el-dialog | a-modal |
| 状态管理 | Vuex (if used) | Pinia |
| 样式方案 | Scoped CSS | Scoped CSS + Tailwind CSS（辅助） |
