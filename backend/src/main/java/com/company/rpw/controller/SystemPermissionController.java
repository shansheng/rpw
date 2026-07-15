package com.company.rpw.controller;

import com.company.rpw.common.R;
import com.company.rpw.entity.SysRole;
import com.company.rpw.entity.SysRoleMenu;
import com.company.rpw.entity.SysUserRole;
import com.company.rpw.mapper.SysRoleMapper;
import com.company.rpw.mapper.SysRoleMenuMapper;
import com.company.rpw.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统权限分配 Controller
 * 端点对齐 yudao-ui-admin-vben：
 *  - 角色菜单：assign-role-menu / list-role-menus
 *  - 角色数据权限：assign-role-data-scope
 *  - 用户角色：assign-user-role / list-user-roles
 */
@RestController
@RequestMapping("/api/v1/system/permission")
@RequiredArgsConstructor
public class SystemPermissionController {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;

    /** 赋予角色菜单权限（先删后插） */
    @PostMapping("/assign-role-menu")
    public R<Boolean> assignRoleMenu(@RequestBody AssignRoleMenuReq req) {
        if (req.getRoleId() == null) {
            return R.fail(400, "角色ID不能为空");
        }
        roleMenuMapper.deleteByRoleId(req.getRoleId());
        if (req.getMenuIds() != null && !req.getMenuIds().isEmpty()) {
            roleMenuMapper.insertBatch(req.getRoleId(), req.getMenuIds());
        }
        return R.ok(true);
    }

    /** 查询角色拥有的菜单ID列表 */
    @GetMapping("/list-role-menus")
    public R<List<Long>> listRoleMenus(@RequestParam Long roleId) {
        return R.ok(roleMenuMapper.selectMenuIdsByRoleId(roleId));
    }

    /** 赋予角色数据权限（部门范围） */
    @PostMapping("/assign-role-data-scope")
    public R<Boolean> assignRoleDataScope(@RequestBody AssignRoleDataScopeReq req) {
        if (req.getRoleId() == null) {
            return R.fail(400, "角色ID不能为空");
        }
        SysRole role = roleMapper.selectById(req.getRoleId());
        if (role == null) {
            return R.fail(404, "角色不存在");
        }
        role.setDataScope(req.getDataScope() == null ? 1 : req.getDataScope());
        role.setDataScopeDeptIds(joinIds(req.getDataScopeDeptIds()));
        return R.ok(roleMapper.updateById(role) > 0);
    }

    /** 查询用户拥有的角色ID列表 */
    @GetMapping("/list-user-roles")
    public R<List<Long>> listUserRoles(@RequestParam Long userId) {
        return R.ok(userRoleMapper.selectRoleIdsByUserId(userId));
    }

    /** 赋予用户角色（先删后插） */
    @PostMapping("/assign-user-role")
    public R<Boolean> assignUserRole(@RequestBody AssignUserRoleReq req) {
        if (req.getUserId() == null) {
            return R.fail(400, "用户ID不能为空");
        }
        userRoleMapper.deleteByUserId(req.getUserId());
        if (req.getRoleIds() != null && !req.getRoleIds().isEmpty()) {
            userRoleMapper.insertBatch(req.getUserId(), req.getRoleIds());
        }
        return R.ok(true);
    }

    private String joinIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    /** 角色菜单分配请求 */
    public static class AssignRoleMenuReq {
        private Long roleId;
        private List<Long> menuIds;

        public Long getRoleId() {
            return roleId;
        }

        public void setRoleId(Long roleId) {
            this.roleId = roleId;
        }

        public List<Long> getMenuIds() {
            return menuIds;
        }

        public void setMenuIds(List<Long> menuIds) {
            this.menuIds = menuIds;
        }
    }

    /** 角色数据权限分配请求 */
    public static class AssignRoleDataScopeReq {
        private Long roleId;
        private Integer dataScope;
        private List<Long> dataScopeDeptIds;

        public Long getRoleId() {
            return roleId;
        }

        public void setRoleId(Long roleId) {
            this.roleId = roleId;
        }

        public Integer getDataScope() {
            return dataScope;
        }

        public void setDataScope(Integer dataScope) {
            this.dataScope = dataScope;
        }

        public List<Long> getDataScopeDeptIds() {
            return dataScopeDeptIds;
        }

        public void setDataScopeDeptIds(List<Long> dataScopeDeptIds) {
            this.dataScopeDeptIds = dataScopeDeptIds;
        }
    }

    /** 用户角色分配请求 */
    public static class AssignUserRoleReq {
        private Long userId;
        private List<Long> roleIds;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public List<Long> getRoleIds() {
            return roleIds;
        }

        public void setRoleIds(List<Long> roleIds) {
            this.roleIds = roleIds;
        }
    }
}
