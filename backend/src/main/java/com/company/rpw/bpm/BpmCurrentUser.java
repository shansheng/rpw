package com.company.rpw.bpm;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 获取当前登录用户名的工具类（与 yudao 的 SecurityFrameworkUtils 对齐）。
 * 在单机 admin 演示环境下，未登录或匿名时回退为 "admin"。
 */
public class BpmCurrentUser {

    /**
     * 获取当前登录用户名
     */
    public static String getUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            return authentication.getName();
        }
        return "admin";
    }
}
