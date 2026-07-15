package com.company.rpw.controller;

import com.company.rpw.common.R;
import com.company.rpw.entity.SysUser;
import com.company.rpw.service.AuthService;
import com.company.rpw.service.NotifyMessageService;
import com.company.rpw.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 适配 yudao-ui-admin-vben 站内信消息模块的系统端点。
 * 路径刻意放在 /api/v1/system/notify-message/* 下，与脚手架默认请求路径一致，
 * 因此前端 api/system/notify/message 无需改动。
 *
 * 提供：
 *  - GET /api/v1/system/notify-message/get-unread-count  当前用户未读站内信数量
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/system/notify-message")
@RequiredArgsConstructor
public class SystemNotifyMessageAdapterController {

    private final NotifyMessageService notifyMessageService;
    private final AuthService authService;
    private final JwtUtil jwtUtil;

    /**
     * 获得当前用户的未读站内信数量
     */
    @GetMapping("/get-unread-count")
    public R<Long> getUnreadCount(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            return R.fail(401, "未登录或Token无效");
        }
        String username = jwtUtil.getUsernameFromToken(token);
        SysUser user = authService.getUserByUsername(username);
        long count = user != null ? notifyMessageService.getUnreadCount(user.getId()) : 0L;
        return R.ok(count);
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
