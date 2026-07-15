package com.company.rpw.service;

import com.company.rpw.entity.SysUser;
import com.company.rpw.mapper.SysUserMapper;
import com.company.rpw.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 认证服务类
 * 处理用户登录、Token生成等业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return JWT Token
     */
    public String login(String username, String password) {
        // 使用 Spring Security 进行身份验证
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        // 生成 JWT Token
        return jwtUtil.generateToken(username, authentication.getAuthorities());
    }

    /**
     * 用户注册（可选功能）
     * @param username 用户名
     * @param password 密码
     * @param realName 真实姓名
     * @return 是否成功
     */
    public boolean register(String username, String password, String realName) {
        // 检查用户是否已存在
        SysUser existUser = sysUserMapper.selectByUsername(username);
        if (existUser != null) {
            log.warn("用户已存在: {}", username);
            return false;
        }

        // 创建新用户
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setStatus(1);

        int result = sysUserMapper.insert(user);
        return result > 0;
    }

    /**
     * 根据用户名获取用户
     * @param username 用户名
     * @return 用户对象
     */
    public SysUser getUserByUsername(String username) {
        return sysUserMapper.selectByUsername(username);
    }
}
