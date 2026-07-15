package com.company.rpw.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.rpw.common.R;
import com.company.rpw.entity.Organization;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.company.rpw.entity.SysUser;
import com.company.rpw.entity.SysUserRole;
import com.company.rpw.mapper.SysUserMapper;
import com.company.rpw.mapper.SysUserRoleMapper;
import com.company.rpw.service.OrganizationService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
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
    private final OrganizationService organizationService;

    /** 默认初始口令 */
    private static final String DEFAULT_PASSWORD = "123456";

    /**
     * 收集某组织及其全部子孙组织的 ID（含自身）。
     * 用于按组织筛选用户时，递归包含下级（公司/项目/部门）人员，
     * 而非仅精确匹配该组织自身。组织树可能含已删除节点，需跳过。
     */
    private Set<Long> collectOrgAndDescendants(Long orgId) {
        Set<Long> result = new LinkedHashSet<>();
        if (orgId == null) {
            return result;
        }
        List<Organization> all = organizationService.list();
        Map<Long, List<Long>> parentToChildren = new HashMap<>();
        for (Organization o : all) {
            if (o.getParentId() == null
                    || (o.getDeleted() != null && o.getDeleted() == 1)) {
                continue;
            }
            parentToChildren
                    .computeIfAbsent(o.getParentId(), k -> new ArrayList<>())
                    .add(o.getId());
        }
        Queue<Long> queue = new LinkedList<>();
        queue.add(orgId);
        result.add(orgId);
        while (!queue.isEmpty()) {
            Long cur = queue.poll();
            List<Long> children = parentToChildren.get(cur);
            if (children != null) {
                for (Long child : children) {
                    if (result.add(child)) {
                        queue.add(child);
                    }
                }
            }
        }
        return result;
    }

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
            wrapper.in(SysUser::getOrgId, collectOrgAndDescendants(deptId));
        }
        if (userIds != null && !userIds.isEmpty()) {
            wrapper.in(SysUser::getId, userIds);
        }
        wrapper.orderByDesc(SysUser::getId);
        IPage<SysUser> page = new Page<>(pageNo, pageSize);
        page = sysUserMapper.selectPage(page, wrapper);
        fillRoleIds(page.getRecords());
        List<Map<String, Object>> list = page.getRecords().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("nickname", u.getRealName());
            m.put("mobile", u.getPhone());
            m.put("email", u.getEmail());
            m.put("status", u.getStatus());
            m.put("deptId", u.getOrgId() == null ? 0L : u.getOrgId());
            m.put("roleIds", u.getRoleIds());
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
    public R<Map<String, Object>> getById(@RequestParam Long id) {
        SysUser user = sysUserMapper.selectById(id);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("nickname", user.getRealName());
        m.put("email", user.getEmail());
        m.put("mobile", user.getPhone());
        m.put("deptId", user.getOrgId() == null ? 0L : user.getOrgId());
        m.put("status", user.getStatus());
        m.put("sex", user.getSex() == null ? 1 : user.getSex());
        m.put("roleIds", userRoleMapper.selectRoleIdsByUserId(id));
        return R.ok(m);
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
        user.setSex(req.getSex() == null ? 1 : req.getSex());
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
        if (req.getSex() != null) {
            existing.setSex(req.getSex());
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
     * 删除用户（与前端 deleteUser 对齐：
     * DELETE /api/v1/system/user/delete?id=1）
     * <p>显式映射，避免被 /{id} 兜底把 "delete" 当 Long 解析报 500。</p>
     */
    @DeleteMapping("/delete")
    public R<Boolean> deleteById(@RequestParam Long id) {
        return R.ok(sysUserMapper.deleteById(id) > 0);
    }

    /**
     * 批量删除用户（与前端 deleteUserList 对齐：
     * DELETE /api/v1/system/user/delete-list?ids=1,2,3）
     */
    @DeleteMapping("/delete-list")
    public R<Boolean> deleteByIds(@RequestParam List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return R.ok(true);
        }
        return R.ok(sysUserMapper.deleteBatchIds(ids) > 0);
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
        private Integer sex;
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

        public Integer getSex() {
            return sex;
        }

        public void setSex(Integer sex) {
            this.sex = sex;
        }

        public List<Long> getRoleIds() {
            return roleIds;
        }

        public void setRoleIds(List<Long> roleIds) {
            this.roleIds = roleIds;
        }
    }

    /**
     * 导出用户（Excel）
     * GET /api/v1/system/user/export-excel
     */
    @GetMapping("/export-excel")
    public void exportExcel(HttpServletResponse response,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) Integer status) throws IOException {
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
        wrapper.orderByDesc(SysUser::getId);
        List<SysUser> users = sysUserMapper.selectList(wrapper);
        List<UserExcelVO> list = users.stream().map(u -> {
            UserExcelVO v = new UserExcelVO();
            v.setUsername(u.getUsername());
            v.setNickname(u.getRealName());
            v.setMobile(u.getPhone());
            v.setEmail(u.getEmail());
            v.setStatusText(u.getStatus() != null && u.getStatus() == 1 ? "启用" : "禁用");
            return v;
        }).collect(Collectors.toList());
        writeExcel(response, "用户数据", list);
    }

    /**
     * 下载用户导入模板（仅表头）
     * GET /api/v1/system/user/get-import-template
     */
    @GetMapping("/get-import-template")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        writeExcel(response, "用户导入模板", new ArrayList<>());
    }

    /**
     * 导入用户（Excel）
     * POST /api/v1/system/user/import  (multipart/form-data)
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Map<String, Object>> importUsers(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "updateSupport", required = false) Boolean updateSupport) {
        try {
            List<UserExcelVO> data = EasyExcel.read(file.getInputStream())
                    .head(UserExcelVO.class)
                    .sheet()
                    .doReadSync();
            int success = 0;
            int failure = 0;
            List<String> failureUsernames = new ArrayList<>();
            boolean doUpdate = Boolean.TRUE.equals(updateSupport);
            for (UserExcelVO v : data) {
                if (v.getUsername() == null || v.getUsername().trim().isEmpty()) {
                    failure++;
                    failureUsernames.add("(空用户名)");
                    continue;
                }
                String uname = v.getUsername().trim();
                SysUser existing = sysUserMapper.selectOne(
                        new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, uname));
                if (existing != null) {
                    if (!doUpdate) {
                        failure++;
                        failureUsernames.add(uname);
                        continue;
                    }
                    if (v.getNickname() != null) {
                        existing.setRealName(v.getNickname());
                    }
                    if (v.getMobile() != null) {
                        existing.setPhone(v.getMobile());
                    }
                    if (v.getEmail() != null) {
                        existing.setEmail(v.getEmail());
                    }
                    if (v.getStatusText() != null) {
                        existing.setStatus("禁用".equals(v.getStatusText()) ? 0 : 1);
                    }
                    sysUserMapper.updateById(existing);
                    success++;
                } else {
                    SysUser u = new SysUser();
                    u.setUsername(uname);
                    u.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
                    u.setRealName(v.getNickname());
                    u.setPhone(v.getMobile());
                    u.setEmail(v.getEmail());
                    u.setStatus(v.getStatusText() != null && "禁用".equals(v.getStatusText()) ? 0 : 1);
                    u.setSex(1);
                    sysUserMapper.insert(u);
                    success++;
                }
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("successCount", success);
            result.put("failureCount", failure);
            result.put("failureUsernames", failureUsernames);
            return R.ok(result);
        } catch (Exception e) {
            return R.fail("导入失败：" + e.getMessage());
        }
    }

    /** 通用：把 Excel 数据写入响应流（浏览器下载） */
    private void writeExcel(HttpServletResponse response, String fileName, List<UserExcelVO> list)
            throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encoded = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encoded + ".xlsx");
        EasyExcel.write(response.getOutputStream(), UserExcelVO.class)
                .sheet("用户数据")
                .doWrite(list);
    }

    /** 用户导入/导出 Excel 行模型 */
    @Data
    public static class UserExcelVO {
        @ExcelProperty("用户名称")
        private String username;
        @ExcelProperty("用户昵称")
        private String nickname;
        @ExcelProperty("手机号码")
        private String mobile;
        @ExcelProperty("用户邮箱")
        private String email;
        @ExcelProperty("账号状态")
        private String statusText;
    }
}
