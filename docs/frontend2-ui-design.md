# Frontend2 — UI 设计方案

## 设计方向：专业企业级 + 克制的高级感

### 设计语言
- **风格关键词**：专业、清晰、高效、企业级
- **主色调**：Ant Design Blue #1890ff，辅助色 #52c41a (成功) #faad14 (警告) #ff4d4f (危险)
- **侧边栏**：#001529 深色（仿 Ant Design Pro）
- **顶部栏**：#fff 纯白，带底部浅灰分割线
- **背景**：主内容区 #f0f2f5 浅灰
- **字体**：-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC', 'Microsoft YaHei'

### 关键页面设计

#### 1. 登录页
- 居中卡片式，宽度420px
- 背景：渐变蓝 #1890ff → #096dd9
- 卡片白色，圆角8px，阴影
- 顶部Logo + 系统名称

#### 2. 首页工作台
- 顶部4个统计卡片（待办、预警、流程、消息）
- 中间左右分栏：左待办任务列表，右预警概览
- 使用a-card+ant-design统计样式

#### 3. 列表页（公司/项目/资源计划）
- 顶部：标题 + 操作按钮（新建）
- 搜索区：a-form inline 布局，水平排列筛选条件
- 表格：a-table 斑马纹，带操作列（编辑/删除/更多）
- 分页：a-pagination 底部

#### 4. 树形表格（组织架构）
- 使用 a-table 的树形数据功能
- 展开/折叠图标

#### 5. 弹窗
- 新建/编辑使用 a-modal
- 表单使用 a-form，label-align="right"

#### 6. 看板
- 使用 vue-draggable-plus
- 5列：草稿/已提交/进行中/已完成/已终止

#### 7. 流程设计器
- 3栏布局：左流程列表 / 中BPMN画布 / 右属性面板
- 顶部工具栏

### 组件规划
- `CommonTable.vue` — 封装搜索+表格+分页
- `CommonModal.vue` — 封装表单弹窗
- `StatCard.vue` — 统计卡片
- `PageHeader.vue` — 页面标题+面包屑
