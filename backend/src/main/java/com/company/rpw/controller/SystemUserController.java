package com.company.rpw.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.rpw.common.R;
import com.company.rpw.entity.SysUser;
import com.company.rpw.entity.SysUserRole;
import com.company.rpw.mapper.SysUserMapper;
import com.company.rpw.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统用户 Controller
 * - simple-list：供 bpm 流程「谁可以发起」下拉选择
 * - list/create/update/delete/status/reset-password：完整用户管理
 */
@RestController
@RequestMapping("/api/v1/system/user")
@RequiredArgsConstructor
public class SystemUserController {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final SysUserRoleMapper userRoleMapper;

    /** 默认初始口令 */
    private static final String DEFAULT_PASSWORD = "123456";

    /**
     * 用户分页列表（支持按用户名/真实姓名/状态筛选）
     * GET /api/v1/system/user/list
     */
    @GetMapping("/list")
    public R<IPage<SysUser>> list(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long orgId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isEmpty()) {
            wrapper.like(SysUser::getUsername, username);
        }
        if (realName != null && !realName.isEmpty()) {
            wrapper.like(SysUser::getRealName, realName);
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        if (orgId != null) {
            wrapper.eq(SysUser::getOrgId, orgId);
        }
        wrapper.orderByDesc(SysUser::getId);
        IPage<SysUser> page = new Page<>(pageNum, pageSize);
        IPage<SysUser> result = sysUserMapper.selectPage(page, wrapper);
        fillRoleIds(result.getRecords());
        return R.ok(result);
    }

    /**
     * 用户分页（供通用「人员选择」弹窗 /system/user/page 使用）
     * <p>前端 select-modal / select-dialog 组件调用此接口，返回字段对齐
     * SystemUserApi.User：id / username / nickname(=realName) / mobile(=phone) /
     * status / deptId(=orgId)。若缺失本接口，/system/user/page 会落入 /{id}
     * 路由，触发 "Failed to convert value of type 'java.lang.String' to
     * required type 'java.lang.Long'; For input string: 'page'"。</p>
     */
    @GetMapping("/page")
    public R<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) List<Long> userIds) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isEmpty()) {
            wrapper.like(SysUser::getUsername, username);
        }
        if (nickname != null && !nickname.isEmpty()) {
            wrapper.like(SysUser::getRealName, nickname);
        }
        if (mobile != null && !mobile.isEmpty()) {
            wrapper.like(SysUser::getPhone, mobile);
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        if (deptId != null) {
            wrapper.eq(SysUser::getOrgId, deptId);
        }
        if (userIds != null && !userIds.isEmpty()) {
            wrapper.in(SysUser::getId, userIds);
        }
        wrapper.orderByDesc(SysUser::getId);
        IPage<SysUser> page = new Page<>(pageNo, pageSize);
        page = sysUserMapper.selectPage(page, wrapper);
        List<Map<String, Object>> list = page.getRecords().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("nickname", u.getRealName());
            m.put("mobile", u.getPhone());
            m.put("email", u.getEmail());
            m.put("status", u.getStatus());
            m.put("deptId", u.getOrgId() == null ? 0L : u.getOrgId());
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", page.getTotal());
        result.put("pageNo", pageNo);
        result.put("pageSize", pageSize);
        return R.ok(result);
    }

    /**
     * 用户详情（按 id 查询，避免与 /{id} 路径变量歧义）
     * GET /api/v1/system/user/get?id=1
     */
    @GetMapping("/get")
    public R<SysUser> getById(@RequestParam Long id) {
        SysUser user = sysUserMapper.selectById(id);
        user.setRoleIds(userRoleMapper.selectRoleIdsByUserId(id));
        return R.ok(user);
    }

    /**
     * 用户详情
     * GET /api/v1/system/user/{id}
     */
    @GetMapping("/{id}")
    public R<SysUser> get(@PathVariable Long id) {
        SysUser user = sysUserMapper.selectById(id);
        user.setRoleIds(userRoleMapper.selectRoleIdsByUserId(id));
        return R.ok(user);
    }

    /**
     * 新增用户
     * POST /api/v1/system/user/create
     */
    @PostMapping("/create")
    public R<Long> create(@RequestBody UserSaveReq req) {
        if (req.getUsername() == null || req.getUsername().isEmpty()) {
            return R.fail(400, "用户名不能为空");
        }
        if (sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, req.getUsername())) > 0) {
            return R.fail(400, "用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(
                (req.getPassword() == null || req.getPassword().isEmpty())
                        ? DEFAULT_PASSWORD : req.getPassword()));
        user.setRealName(req.getNickname());
        user.setEmail(req.getEmail());
        user.setPhone(req.getMobile());
        user.setOrgId(req.getDeptId());
        user.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        sysUserMapper.insert(user);
        // 同步用户-角色关联（创建时若选了角色）
        syncUserRoles(user.getId(), req.getRoleIds());
        return R.ok(user.getId());
    }

    /**
     * 修改用户
     * PUT /api/v1/system/user/update
     */
    @PutMapping("/update")
    public R<Boolean> update(@RequestBody UserSaveReq req) {
        if (req.getId() == null) {
            return R.fail(400, "用户ID不能为空");
        }
        SysUser existing = sysUserMapper.selectById(req.getId());
        if (existing == null) {
            return R.fail(404, "用户不存在");
        }
        if (req.getPassword() != null && !req.getPassword().isEmpty()) {
            existing.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        if (req.getUsername() != null) {
            existing.setUsername(req.getUsername());
        }
        if (req.getNickname() != null) {
            existing.setRealName(req.getNickname());
        }
        if (req.getEmail() != null) {
            existing.setEmail(req.getEmail());
        }
        if (req.getMobile() != null) {
            existing.setPhone(req.getMobile());
        }
        if (req.getDeptId() != null) {
            existing.setOrgId(req.getDeptId());
        }
        if (req.getStatus() != null) {
            existing.setStatus(req.getStatus());
        }
        boolean ok = sysUserMapper.updateById(existing) > 0;
        // 仅当表单显式传了 roleIds 时才同步（编辑表单未预载角色则保持原有关联）
        if (req.getRoleIds() != null) {
            syncUserRoles(req.getId(), req.getRoleIds());
        }
        return R.ok(ok);
    }

    /** 用户-角色关联同步（先删后插） */
    private void syncUserRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.deleteByUserId(userId);
        if (roleIds != null && !roleIds.isEmpty()) {
            userRoleMapper.insertBatch(userId, roleIds);
        }
    }

    /**
     * 删除用户（逻辑删除由 MyBatis-Plus @TableLogic 处理）
     * DELETE /api/v1/system/user/{id}
     */
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(sysUserMapper.deleteById(id) > 0);
    }

    /**
     * 启用/禁用用户
     * PUT /api/v1/system/user/{id}/status?status=1|0
     */
    @PutMapping("/{id}/status")
    public R<Boolean> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setStatus(status);
        return R.ok(sysUserMapper.updateById(u) > 0);
    }

    /**
     * 重置密码（不传 password 则重置为默认口令）
     * PUT /api/v1/system/user/{id}/reset-password?password=xxx
     */
    @PutMapping("/{id}/reset-password")
    public R<Boolean> resetPassword(@PathVariable Long id,
                                    @RequestParam(required = false) String password) {
        SysUser u = sysUserMapper.selectById(id);
        if (u == null) {
            return R.fail(404, "用户不存在");
        }
        String raw = (password == null || password.isEmpty()) ? DEFAULT_PASSWORD : password;
        u.setPassword(passwordEncoder.encode(raw));
        return R.ok(sysUserMapper.updateById(u) > 0);
    }

    /**
     * 用户精简列表（供流程模型「谁可以发起」下拉选择）
     * 返回字段对齐前端 SystemUserApi.User：id / username / nickname / deptId / status
     */
    @GetMapping("/simple-list")
    public R<List<Map<String, Object>>> simpleList() {
        List<SysUser> users = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>().orderByDesc(SysUser::getId));
        List<Map<String, Object>> list = users.stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("nickname", u.getRealName());
            m.put("deptId", u.getOrgId() == null ? 0L : u.getOrgId());
            m.put("status", u.getStatus());
            return m;
        }).collect(Collectors.toList());
        return R.ok(list);
    }

    /** 批量填充用户角色ID（列表分页用，避免 N+1 查询） */
    private void fillRoleIds(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        List<Long> ids = users.stream().map(SysUser::getId).collect(Collectors.toList());
        List<SysUserRole> urList = userRoleMapper.selectByUserIds(ids);
        Map<Long, List<Long>> map = urList.stream()
                .collect(Collectors.groupingBy(SysUserRole::getUserId,
                        Collectors.mapping(SysUserRole::getRoleId, Collectors.toList())));
        users.forEach(u -> u.setRoleIds(map.getOrDefault(u.getId(), List.of())));
    }

    /**
     * 用户新增/修改请求体（对齐前端 user 表单字段：
     * nickame→realName，deptId→orgId，mobile→phone，roleIds→用户角色关联）
     */
    public static class UserSaveReq {
        private Long id;
        private String username;
        private String password;
        private String nickame;
        private Long deptId;
        private String email;
        private String mobile;
        private Integer status;
        private List<Long> roleIds;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getNickname() {
            return nickame;
        }

        public void setNickname(String nickame) {
            this.nickame = nickame;
        }

        public Long getDeptId() {
            return deptId;
        }

        public void setDeptId(Long deptId) {
            this.deptId = deptId;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public List<Long> getRoleIds() {
            return roleIds;
        }

        public void setRoleIds(List<Long> roleIds) {
            this.roleIds = roleIds;
        }
    }
}
