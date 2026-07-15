package com.company.rpw.controller;

import com.company.rpw.common.R;
import com.company.rpw.entity.SysMenu;
import com.company.rpw.entity.SysRole;
import com.company.rpw.entity.SysUser;
import com.company.rpw.mapper.SysMenuMapper;
import com.company.rpw.mapper.SysRoleMapper;
import com.company.rpw.mapper.SysRoleMenuMapper;
import com.company.rpw.mapper.SysUserRoleMapper;
import com.company.rpw.service.AuthService;
import com.company.rpw.utils.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 适配 yudao-ui-admin-vben 脚手架的认证/权限端点。
 * 路径刻意放在 /api/v1/system/auth/* 下，与脚手架默认请求路径一致，
 * 因此前端 auth.ts 无需改动，只需把请求成功码配置为 200（见 frontend3 的 request.ts）。
 *
 * 提供：
 *  - POST /api/v1/system/auth/login              登录（返回 yudao 形态 {accessToken,refreshToken,userId,expiresTime}）
 *  - GET  /api/v1/system/auth/get-permission-info 权限信息（user/roles/permissions/menus）
 *  - POST /api/v1/system/auth/logout             登出
 *  - POST /api/v1/system/auth/refresh-token      刷新令牌
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/system/auth")
@RequiredArgsConstructor
public class SystemAuthAdapterController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final SysMenuMapper menuMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    /** 登录 */
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        try {
            String token = authService.login(username, password);
            SysUser user = authService.getUserByUsername(
                    jwtUtil.getUsernameFromToken(token));
            Map<String, Object> data = new HashMap<>();
            data.put("accessToken", token);
            data.put("refreshToken", token);
            data.put("userId", user != null ? user.getId() : 0L);
            data.put("expiresTime", System.currentTimeMillis() + 86400L * 1000L);
            return R.ok(data);
        } catch (Exception e) {
            return R.fail(401, "用户名或密码错误");
        }
    }

    /** 权限信息（菜单树 + 权限码 + 用户信息），由 DB 驱动 */
    @GetMapping("/get-permission-info")
    public R<Map<String, Object>> getPermissionInfo(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            return R.fail(401, "未登录或Token无效");
        }
        String username = jwtUtil.getUsernameFromToken(token);
        SysUser user = authService.getUserByUsername(username);
        if (user == null) {
            return R.fail(401, "用户不存在");
        }

        // 1. 角色
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(user.getId());
        boolean isAdmin = roleIds.stream().anyMatch(rid -> {
            SysRole r = roleMapper.selectById(rid);
            return r != null && "admin".equals(r.getCode());
        });
        List<String> roleCodes = new ArrayList<>();
        for (Long rid : roleIds) {
            SysRole r = roleMapper.selectById(rid);
            if (r != null) {
                roleCodes.add(r.getCode());
            }
        }

        // 2. 菜单ID 集合（admin 取全部；非 admin 取角色菜单并补全祖先目录）
        Set<Long> menuIdSet = new HashSet<>();
        if (isAdmin) {
            menuMapper.selectList(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getStatus, 1))
                    .forEach(m -> menuIdSet.add(m.getId()));
        } else {
            for (Long rid : roleIds) {
                menuIdSet.addAll(roleMenuMapper.selectMenuIdsByRoleId(rid));
            }
            // 补全祖先目录，保证前端树完整
            Map<Long, SysMenu> allMap = menuMapper.selectList(null).stream()
                    .collect(Collectors.toMap(SysMenu::getId, m -> m, (a, b) -> a));
            Set<Long> closure = new HashSet<>(menuIdSet);
            for (Long id : new ArrayList<>(menuIdSet)) {
                SysMenu m = allMap.get(id);
                Long pid = m != null ? m.getParentId() : null;
                while (pid != null && pid != 0L && allMap.get(pid) != null) {
                    if (!closure.add(pid)) {
                        break;
                    }
                    pid = allMap.get(pid).getParentId();
                }
            }
            menuIdSet.clear();
            menuIdSet.addAll(closure);
        }

        // 3. 查菜单并组装树（menuIdSet 为空时直接返回空，避免 id IN () 非法 SQL）
        List<SysMenu> menus = new ArrayList<>();
        if (!menuIdSet.isEmpty()) {
            menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                    .in(SysMenu::getId, menuIdSet).eq(SysMenu::getStatus, 1)
                    .orderByAsc(SysMenu::getParentId).orderByAsc(SysMenu::getSort));
        }
        Map<Long, List<Map<String, Object>>> childrenMap = new HashMap<>();
        Map<Long, Map<String, Object>> nodeMap = new HashMap<>();
        for (SysMenu m : menus) {
            Map<String, Object> node = toNode(m);
            nodeMap.put(m.getId(), node);
            Long pid = (m.getParentId() == null || m.getParentId() == 0L) ? 0L : m.getParentId();
            childrenMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(node);
        }
        for (Map.Entry<Long, Map<String, Object>> e : nodeMap.entrySet()) {
            List<Map<String, Object>> ch = childrenMap.get(e.getKey());
            if (ch != null && !ch.isEmpty()) {
                e.getValue().put("children", ch);
            }
        }
        List<Map<String, Object>> tree = childrenMap.getOrDefault(0L, new ArrayList<>());

        // 4. 权限串（admin 给 *；否则取菜单 permission）
        List<String> permissions = new ArrayList<>();
        if (isAdmin) {
            permissions.add("*");
        } else {
            for (SysMenu m : menus) {
                if (m.getPermission() != null && !m.getPermission().isEmpty()) {
                    permissions.add(m.getPermission());
                }
            }
        }

        // 5. 组装返回
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("username", username);
        userMap.put("nickname", user.getRealName());
        userMap.put("realName", user.getRealName());
        userMap.put("email", user.getEmail());
        userMap.put("homePath", "/dashboard");
        userMap.put("roles", roleCodes);
        userMap.put("permissions", permissions);

        Map<String, Object> data = new HashMap<>();
        data.put("user", userMap);
        data.put("roles", roleCodes);
        data.put("permissions", permissions);
        data.put("menus", tree);
        return R.ok(data);
    }

    /** 登出（无状态 JWT，直接返回成功） */
    @PostMapping("/logout")
    public R<Boolean> logout() {
        return R.ok(true);
    }

    /** 刷新令牌 */
    @PostMapping("/refresh-token")
    public R<Map<String, Object>> refreshToken(@RequestParam("refreshToken") String refreshToken) {
        if (refreshToken == null || !jwtUtil.validateToken(refreshToken)) {
            return R.fail(401, "refreshToken 无效");
        }
        try {
            String username = jwtUtil.getUsernameFromToken(refreshToken);
            SysUser user = authService.getUserByUsername(username);
            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
            String newToken = jwtUtil.generateToken(username, authorities);
            Map<String, Object> data = new HashMap<>();
            data.put("accessToken", newToken);
            data.put("refreshToken", newToken);
            data.put("userId", user != null ? user.getId() : 0L);
            data.put("expiresTime", System.currentTimeMillis() + 86400L * 1000L);
            return R.ok(data);
        } catch (Exception e) {
            return R.fail(401, "刷新令牌失败");
        }
    }

    // ===================== 菜单树（驱动前端路由） =====================

    /**
     * 构建菜单树（遵循 vben-admin / yudao 后端菜单规范）：
     *  - 父级目录：path 带前导 "/"（如 /rpw/basic），component 设为 "Layout"（框架自动转 BasicLayout）。
     *  - 子级菜单：path 使用【相对父级】的路径（不带前导 "/"），由
     *    convertServerMenuToRouteRecordStringComponent 的 `${parent}/${path}` 自动拼接，
     *    避免产生 "//" 双斜杠导致 vue-router 无法匹配而 404。
     *  - component：使用 "views/{module}/{page}/index.vue" 标准格式（带 views/ 前缀与 .vue 后缀），
     *    与前端 src/views 下的文件一一对应，由 pageMap 按 normalizeViewPath 解析。
     */
    /**
     * 将 DB 菜单实体转为前端路由节点（字段对齐 vben-admin / yudao 规范）。
     * 与旧 buildMenus() 的 node() 输出结构一致：
     * id / name / path / component / componentName / parentId / icon / visible / keepAlive / sort。
     */
    private Map<String, Object> toNode(SysMenu m) {
        Map<String, Object> n = new HashMap<>();
        n.put("id", m.getId());
        n.put("name", m.getName());
        n.put("path", m.getPath());
        n.put("component", m.getComponent());
        n.put("componentName", m.getComponentName());
        n.put("parentId", m.getParentId() == null ? 0L : m.getParentId());
        n.put("icon", m.getIcon());
        n.put("visible", m.getVisible() != null && m.getVisible() == 1);
        n.put("keepAlive", m.getKeepAlive() != null && m.getKeepAlive() == 1);
        n.put("sort", m.getSort());
        return n;
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
