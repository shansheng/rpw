# RPW 系统权限管理（RBAC）详细设计

> 范围：**单系统、非 SaaS（无租户/多租户）** 的 RBAC 权限体系。
> 目标：让前端已有的「角色 / 菜单 / 菜单权限分配 / 数据权限分配 / 用户角色分配」等权限配置 UI 真正可用；导航菜单由 `sys_menu` 表驱动，而非硬编码。

---

## 1. 现状与差距

### 1.1 前端（已就绪，无需大改）
`frontend3/apps/web-antd/src/views/system/` 下已存在完整 RBAC UI，调用的端点契约已固定：

| 模块 | 调用的端点 |
|---|---|
| 角色 `system/role` | `GET /system/role/page`、`/simple-list`、`/get?id=`、`POST /create`、`PUT /update`、`DELETE /delete?id=`、`/delete-list?ids=`、`GET /export-excel` |
| 菜单 `system/menu` | `GET /system/menu/list`、`/simple-list`、`/get?id=`、`POST /create`、`PUT /update`、`DELETE /delete?id=`、`/delete-list?ids=` |
| 权限分配 `system/permission` | `POST /assign-role-menu`、`GET /list-role-menus?roleId=`、`POST /assign-role-data-scope`、`GET /list-user-roles?userId=`、`POST /assign-user-role` |
| 用户角色 `system/user`（assign-role-form） | `POST /assign-user-role`、`GET /list-user-roles?userId=` |
| 部门 `system/dept`（数据权限范围树） | `GET /system/dept/list`、`/simple-list`、`/get?id=`、`POST /create`、`PUT /update`、`DELETE /delete?id=`、`/delete-list?ids=` |

### 1.2 后端（空壳，需补齐）
- `SystemRoleController` / `SystemDeptController` / `SystemPostController`：仅 `/simple-list` 返回**空列表** stub。
- `SystemMenuController` / `SystemPermissionController`：**不存在**。
- `SystemAuthAdapterController.getPermissionInfo()`：导航菜单由 **硬编码 `buildMenus()`** 生成，与数据库无关。
- `sys_user` 实体**无角色关联字段**；库里 `sys_%` 表只有 `sys_user`，缺 `sys_role` / `sys_menu` / `sys_role_menu` / `sys_user_role` / `sys_dept`。
- 编码风格：Controller 内直接使用 MyBatis-Plus `XxxMapper`（见 `SystemUserController`），本设计沿用。

---

## 2. 数据模型（ER）

```
┌──────────┐       ┌──────────────┐       ┌──────────┐
│ sys_user  │──1   N│ sys_user_role│   N   1│ sys_role │
└──────────┘       └──────────────┘       └────┬─────┘
                                                   │ 1   N
                                                   ▼
                                            ┌──────────────┐
                                            │ sys_role_menu│
                                            └────┬─────┘
                                                 │ N   1
                                                 ▼
                                            ┌──────────┐
                                            │ sys_menu  │◀── parent_id 自引用树
                                            └──────────┘

┌──────────┐   parent_id 自引用树（数据权限部门范围）
│ sys_dept  │
└──────────┘
```

### 2.1 表结构
| 表 | 关键字段 |
|---|---|
| `sys_role` | id, name, code(唯一), sort, status(1启用/0禁用), **type**(1系统内置/2自定义), **data_scope**(1全部/2仅本人/3所在部门/4所在部门及子部门/5自定义), **data_scope_dept_ids**(逗号串), remark, create_time, update_time, deleted |
| `sys_menu` | id, name, **permission**(按钮权限标识), **type**(1目录/2菜单/3按钮), sort, **parent_id**(0=顶级), **path**, **icon**, **component**, **component_name**, **status**, **visible**(0隐藏/1显示), **keep_alive**, **always_show**, create_time, update_time, deleted |
| `sys_role_menu` | id, role_id, menu_id（联合唯一） |
| `sys_user_role` | id, user_id, role_id（联合唯一） |
| `sys_dept` | id, name, parent_id(0=顶级), sort, status, leader_user_id, phone, email, create_time, update_time, deleted |
| `sys_user`（扩展） | 新增 `role_ids` 冗余列**非必须**；采用 `sys_user_role` 关联表范式化 |

> 说明：`sys_post`（岗位）前端 `system/post` 页存在，但权限核心不依赖；本轮将 `SystemPostController` 的 stub 升级为可管理（可选，见 §6 风险）。

---

## 3. 后端实现（端点契约，严格对齐前端）

### 3.1 `SystemRoleController` `@RequestMapping("/api/v1/system/role")`
- `GET /page`：分页（name/code/status/createTime 范围过滤）。
- `GET /simple-list`：全部角色精简列表（id/name/code），供流程「候选人-角色」等下拉。
- `GET /get?id=`：详情。
- `POST /create`：校验 `code` 唯一、`name` 非空；type 默认 1、dataScope 默认 1。
- `PUT /update`：同校验；**更新时同步 `setBpmnXml` 式一致——所有字段都要 set**（避免再出现「保存成功但字段丢失」类回归）。
- `DELETE /delete?id=`：删除角色，级联删 `sys_role_menu` + `sys_user_role`。
- `DELETE /delete-list?ids=`：批量。
- `GET /export-excel`：导出（可先返回空流，后续补）。

### 3.2 `SystemMenuController` `@RequestMapping("/api/v1/system/menu")`
- `GET /list`：全部菜单（前端按 `parentId` 自行 `handleTree` 成树）。
- `GET /simple-list`：精简列表（建菜单/分配权限时选父级、画权限树用）。
- `GET /get?id=`：详情。
- `POST /create` / `PUT /update`：校验——目录/菜单 `parentId=0` 时 `path` 以 `/` 开头、反之不以 `/` 开头；目录/菜单需 `icon`；按钮需 `permission`。
- `DELETE /delete?id=`：校验无子节点方可删；级联删 `sys_role_menu`。
- `DELETE /delete-list?ids=`：批量（同样先校验子节点）。

### 3.3 `SystemPermissionController` `@RequestMapping("/api/v1/system/permission")`
- `POST /assign-role-menu` `{roleId, menuIds[]}`：先删该角色全部 `sys_role_menu`，再批量插入。
- `GET /list-role-menus?roleId=`：返回该角色拥有的 `menu_id` 数组（供「菜单权限」弹窗回填）。
- `POST /assign-role-data-scope` `{roleId, dataScope, dataScopeDeptIds[]}`：更新 `sys_role.data_scope` 与 `data_scope_dept_ids`。
- `GET /list-user-roles?userId=`：返回该用户角色 `role_id` 数组。
- `POST /assign-user-role` `{userId, roleIds[]}`：先删后插 `sys_user_role`。

### 3.4 `SystemDeptController` `@RequestMapping("/api/v1/system/dept")`
- 升级 stub → `GET /list`、`/simple-list`、`/get?id=`、`POST /create`、`PUT /update`、`DELETE /delete?id=`、`/delete-list?ids=`。
- `list` 返回扁平列表（前端 `handleTree` 成树）；`simple-list` 精简。

### 3.5 `SystemUserController` `@RequestMapping("/api/v1/system/user")`
- 现有 CRUD 保留。
- 用户 `create`/`update` 时若请求体含 `roleIds`，同步 `sys_user_role`（先删后插）。
- `simple-list` 已存在（用户选择下拉）。

### 3.6 `SystemAuthAdapterController.getPermissionInfo()`（核心改造）
由硬编码 `buildMenus()` 改为 **DB 驱动**：
1. 解析 token → `username` → `sys_user` → 其角色（`sys_user_role`）→ 角色菜单（`sys_role_menu`）→ 去重 `menu_id` 集合。
2. 查 `sys_menu` 中这些 id 的菜单，按 `parent_id` 组装树，结构沿用现有 `node()`：`id/name/path/component/componentName/parentId/icon/visible/keepAlive/sort/children`。
3. `permissions` = 这些菜单中 `type=按钮(3)` 的 `permission` 字符串集合；**admin 角色直接给 `"*"`**（保证内置管理员全可见）。
4. `user.roles` = 角色 `code` 列表；`user.permissions` = 同上。
5. 删除硬编码 `buildMenus()`（或置空），菜单来源切到 `sys_menu`。

> 关键约束（历史踩坑）：列表页若被 `router.push({name:'XxxYyy'})` 引用，**菜单必须带英文 `componentName`**（如流程模型 `componentName="BpmModel"`）。种子数据严格沿用原 `buildMenus()` 的 `componentName`，避免 No match / 404。

---

## 4. 种子数据（SQL）
将现有 `buildMenus()` 的菜单结构**原样落库到 `sys_menu`**（id 保持不变，保证前端路由按 `componentName` 命名单页）：

- 工作台(1) → 前端首页
- 基础数据(10,Layout) → 组织管理(11)
- 资源计划(20,Layout) → 人力(21)/材料(22)/设备(23)/五金(24)/办公(25)/安全(26)/周转(27)/分包(28)
- 预警管理(30,Layout) → 规则(31)/记录(32)
- BPM 工作流(40,Layout) → 人力计划变更(71)/流程模型(72,componentName=BpmModel)/流程定义(73)/表单(74)/分类(75)/用户分组(76)/表达式(77)/监听器(78)/我的流程(79)/流程管理(80)/数据报表(81)/待办(82)/已办(83)/任务管理(84)/抄送(85)/请假(86)
- 报表与看板(50,Layout) → 资源看板(51)/自定义报表(52)
- 系统管理(60,Layout) → 字典(61)/用户(62)

角色与关联：
- 角色 `admin`（code=`admin`，type=1，data_scope=1）→ `sys_role_menu` 全量插入所有菜单 id。
- 用户 `admin`（已存在）→ `sys_user_role` 关联 admin 角色。
- 部门：种子一个根部门「总公司」(id=1, parent_id=0)，供数据权限演示。

---

## 5. 前端改造

1. **导航去 SaaS**：租户 `system/tenant`、租户套餐 `system/tenantPackage` 等多租户页**不种入菜单种子**，前端按后端菜单自动不渲染（无需改路由）。`oauth2`/`social` 等如非必需，同样不种菜单、并可从 `src/views/system/` 移除目录或置顶禁用，避免误用 SaaS 能力。
2. **权限 UI 已对齐**：角色/菜单/权限分配/用户角色分配页调用的端点由本轮后端实现后**无需改 `.vue`**（字段已对齐；仅上线后回归验证）。
3. **生成路由**：`generate-menus.ts` 用 `componentName` 映射已有逻辑，种子菜单已带 `componentName`，列表页 `router.push({name})` 可命中。
4. **可移除/隐藏**：`system/tenant`、`system/tenantPackage` 目录（及 `tenant-package` api）建议从前端代码清理，使「SaaS 不需要」在代码层也成立。

---

## 6. 实施步骤（建议顺序）

1. **建表 + 种子 SQL** → 执行到 `rpw_db`（`sys_role`/`sys_menu`/`sys_role_menu`/`sys_user_role`/`sys_dept` + 菜单/角色/关联/根部门种子）。
2. **实体**：`SysRole`/`SysMenu`/`SysRoleMenu`/`SysUserRole`/`SysDept`（MyBatis-Plus `@TableName` + `BaseEntity`）。
3. **Mapper**：对应 `*Mapper extends BaseMapper<Xxx>`。
4. **Controller 实现**：§3 五个 Controller，遵循现有 Mapper 直用风格。
5. **改写 `getPermissionInfo`** 为 DB 驱动（保留 admin 全量菜单兜底）。
6. **clean 编译 + 固定 8080 重启**（`mvn -o clean spring-boot:run -Dserver.port=8080`）。
7. **验证**：
   - `curl` 登录 admin → `GET /system/auth/get-permission-info` 返回菜单树；
   - 角色页增删改查、菜单页树、分配角色菜单、用户分配角色均通；
   - 前端 `localhost:5666` 硬刷新后导航正常、权限按钮按角色显隐。
8. **前端清理**：移除/隐藏 tenant 等 SaaS 目录。

> 风险：① `getPermissionInfo` 改坏 → 全站无菜单（空白）。缓解：admin 全量菜单种子 + 登录后先 `curl` 验证结构。② 菜单 `componentName` 错 → 列表页 404/No match。缓解：种子沿用原 `buildMenus()` 的 `componentName`。③ `permission` 字符串决定按钮可见性：admin 给 `"*"` 保证全可见；非 admin 按菜单 `permission` 控制。

---

## 7. 本轮交付范围（建议）

**必做（核心权限闭环）**：§2 表 + §3.1/3.2/3.3/3.6 + §4 种子 + §6 验证。
**建议同做**：§3.4 部门（数据权限范围树需要）+ §3.5 用户角色级联（用户页「分配角色」需要）。
**可选**：`sys_post` 岗位管理（前端 post 页）、`oauth2`/`social` 清理。
