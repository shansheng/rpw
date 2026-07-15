package com.company.rpw.controller;

import com.company.rpw.common.R;
import com.company.rpw.entity.SysUser;
import com.company.rpw.service.AuthService;
import com.company.rpw.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 处理用户登录、登出等认证相关请求
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    /**
     * 用户登录接口
     * POST /api/v1/auth/login
     * @param loginRequest 登录请求（包含username和password）
     * @return 包含JWT Token的响应
     */
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody LoginRequest loginRequest) {
        log.info("用户登录请求: {}", loginRequest.getUsername());

        try {
            // 执行登录，获取Token
            String token = authService.login(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
            );

            // 构造返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("tokenType", "Bearer");
            data.put("expiresIn", 86400);

            return R.ok(data);
        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage());
            return R.fail(401, "用户名或密码错误");
        }
    }

    /**
     * 用户注册接口
     * POST /api/v1/auth/register
     * @param registerRequest 注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    public R<Boolean> register(@RequestBody RegisterRequest registerRequest) {
        log.info("用户注册请求: {}", registerRequest.getUsername());

        boolean result = authService.register(
                registerRequest.getUsername(),
                registerRequest.getPassword(),
                registerRequest.getRealName()
        );

        if (result) {
            return R.ok(true);
        } else {
            return R.fail(400, "用户已存在");
        }
    }

    /**
     * 登录请求DTO（内部类）
     */
    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    /**
     * 注册请求DTO（内部类）
     */
    public static class RegisterRequest {
        private String username;
        private String password;
        private String realName;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getRealName() { return realName; }
        public void setRealName(String realName) { this.realName = realName; }
    }

    /**
     * 调试端点：检查Token提取和验证
     */
    @GetMapping("/debug")
    public R<Map<String, Object>> debug(HttpServletRequest request) {
        Map<String, Object> data = new HashMap<>();
        String token = extractTokenFromHeader(request);
        data.put("tokenExtracted", token != null);
        if (token != null) {
            data.put("tokenLength", token.length());
            data.put("tokenPrefix", token.substring(0, Math.min(20, token.length())) + "...");
            data.put("valid", jwtUtil.validateToken(token));
        }
        return R.ok(data);
    }

    /**
     * 获取当前登录用户信息
     * GET /api/v1/auth/userinfo
     * @return 用户信息
     */
    @GetMapping("/userinfo")
    public R<Map<String, Object>> getUserInfo(HttpServletRequest request) {
        // 从请求头中获取Token
        String token = extractTokenFromHeader(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            return R.fail(401, "未登录或Token无效");
        }

        // 从Token中解析用户名
        String username = jwtUtil.getUsernameFromToken(token);

        // 查询用户信息
        SysUser user = authService.getUserByUsername(username);
        if (user == null) {
            return R.fail(404, "用户不存在");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("email", user.getEmail());

        return R.ok(data);
    }

    /**
     * 从请求头中提取Token
     */
    private String extractTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
