package com.company.rpw.controller;

import com.company.rpw.common.R;
import com.company.rpw.service.WeComMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 企业微信控制器
 * 提供企业微信配置测试和消息发送接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/wecom")
@RequiredArgsConstructor
public class WeComController {

    private final WeComMessageService weComMessageService;

    /**
     * 测试企业微信连接
     * GET /api/v1/wecom/test
     * @return 测试结果
     */
    @GetMapping("/test")
    public R<String> testConnection() {
        log.info("测试企业微信连接");
        try {
            String token = weComMessageService.getAccessToken();
            if (token != null) {
                return R.ok("连接成功，access_token已获取");
            } else {
                return R.fail(500, "连接失败：无法获取access_token");
            }
        } catch (Exception e) {
            log.error("测试连接异常", e);
            return R.fail(500, "连接异常: " + e.getMessage());
        }
    }

    /**
     * 发送文本消息测试
     * POST /api/v1/wecom/send-text
     * @param request 请求参数（touser, content）
     * @return 发送结果
     */
    @PostMapping("/send-text")
    public R<Boolean> sendTextMessage(@RequestBody Map<String, String> request) {
        String touser = request.get("touser");
        String content = request.get("content");

        log.info("发送企业微信文本消息给: {}", touser);

        boolean result = weComMessageService.sendTextMessage(touser, content);
        return result ? R.ok(true) : R.fail(500, "消息发送失败");
    }

    /**
     * 发送Markdown消息测试
     * POST /api/v1/wecom/send-markdown
     * @param request 请求参数（touser, content）
     * @return 发送结果
     */
    @PostMapping("/send-markdown")
    public R<Boolean> sendMarkdownMessage(@RequestBody Map<String, String> request) {
        String touser = request.get("touser");
        String content = request.get("content");

        log.info("发送企业微信Markdown消息给: {}", touser);

        boolean result = weComMessageService.sendMarkdownMessage(touser, content);
        return result ? R.ok(true) : R.fail(500, "消息发送失败");
    }

    /**
     * 获取企业微信配置信息（脱敏）
     * GET /api/v1/wecom/config
     * @return 配置信息（脱敏处理）
     */
    @GetMapping("/config")
    public R<Map<String, Object>> getConfig() {
        // TODO: 从数据库读取配置并返回（脱敏处理）
        log.info("获取企业微信配置");
        return R.ok(Map.of(
                "corpId", "***",
                "agentId", "***",
                "configured", false
        ));
    }

    /**
     * 保存企业微信配置
     * POST /api/v1/wecom/config
     * @param config 配置参数
     * @return 保存结果
     */
    @PostMapping("/config")
    public R<Boolean> saveConfig(@RequestBody Map<String, Object> config) {
        // TODO: 将配置保存到数据库
        log.info("保存企业微信配置");
        return R.ok(true);
    }
}
