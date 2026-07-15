package com.company.rpw.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.company.rpw.config.WeComConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 企业微信消息推送服务
 * 负责获取access_token和发送消息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeComMessageService {

    private final WeComConfig weComConfig;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String ACCESS_TOKEN_KEY = "wecom:access_token";

    /**
     * 获取企业微信 access_token
     * 优先从Redis缓存获取，缓存未命中则重新获取
     * @return access_token
     */
    public String getAccessToken() {
        // 先从Redis缓存获取
        String token = redisTemplate.opsForValue().get(ACCESS_TOKEN_KEY);
        if (token != null) {
            return token;
        }

        // 重新获取token
        String url = weComConfig.getApiUrl() + "/cgi-bin/gettoken"
                + "?corpid=" + weComConfig.getCorpId()
                + "&corpsecret=" + weComConfig.getSecret();

        try (HttpResponse response = HttpRequest.get(url).execute()) {
            String body = response.body();
            JSONObject json = JSONUtil.parseObj(body);

            if (json.getInt("errcode") == 0) {
                token = json.getStr("access_token");
                int expiresIn = json.getInt("expires_in");

                // 缓存到Redis（提前5分钟过期）
                redisTemplate.opsForValue().set(
                        ACCESS_TOKEN_KEY,
                        token,
                        expiresIn - 300,
                        TimeUnit.SECONDS
                );

                log.info("获取企业微信access_token成功");
                return token;
            } else {
                log.error("获取企业微信access_token失败: {}", body);
                throw new RuntimeException("获取access_token失败: " + json.getStr("errmsg"));
            }
        } catch (Exception e) {
            log.error("获取企业微信access_token异常", e);
            throw new RuntimeException("获取access_token异常", e);
        }
    }

    /**
     * 发送文本消息
     * @param touser 接收人用户ID列表（逗号分隔，@all表示所有人）
     * @param content 消息内容
     * @return 是否发送成功
     */
    public boolean sendTextMessage(String touser, String content) {
        String accessToken = getAccessToken();
        String url = weComConfig.getApiUrl() + "/cgi-bin/message/send?access_token=" + accessToken;

        JSONObject message = new JSONObject();
        message.set("touser", touser);
        message.set("msgtype", "text");
        message.set("agentid", weComConfig.getAgentId());

        JSONObject text = new JSONObject();
        text.set("content", content);
        message.set("text", text);

        try (HttpResponse response = HttpRequest.post(url)
                .body(JSONUtil.toJsonStr(message))
                .execute()) {
            String body = response.body();
            JSONObject json = JSONUtil.parseObj(body);

            if (json.getInt("errcode") == 0) {
                log.info("企业微信消息发送成功: {}", content);
                return true;
            } else {
                log.error("企业微信消息发送失败: {}", body);
                return false;
            }
        } catch (Exception e) {
            log.error("企业微信消息发送异常", e);
            return false;
        }
    }

    /**
     * 发送Markdown消息
     * @param touser 接收人用户ID
     * @param content Markdown内容
     * @return 是否发送成功
     */
    public boolean sendMarkdownMessage(String touser, String content) {
        String accessToken = getAccessToken();
        String url = weComConfig.getApiUrl() + "/cgi-bin/message/send?access_token=" + accessToken;

        JSONObject message = new JSONObject();
        message.set("touser", touser);
        message.set("msgtype", "markdown");
        message.set("agentid", weComConfig.getAgentId());

        JSONObject markdown = new JSONObject();
        markdown.set("content", content);
        message.set("markdown", markdown);

        try (HttpResponse response = HttpRequest.post(url)
                .body(JSONUtil.toJsonStr(message))
                .execute()) {
            String body = response.body();
            JSONObject json = JSONUtil.parseObj(body);

            if (json.getInt("errcode") == 0) {
                log.info("企业微信Markdown消息发送成功");
                return true;
            } else {
                log.error("企业微信Markdown消息发送失败: {}", body);
                return false;
            }
        } catch (Exception e) {
            log.error("企业微信Markdown消息发送异常", e);
            return false;
        }
    }
}
