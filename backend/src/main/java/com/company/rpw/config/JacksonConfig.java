package com.company.rpw.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Jackson 全局配置：统一 LocalDateTime 与时区无关的「epoch 毫秒」互转。
 *
 * <p>前端所有时间控件（DatePicker valueFormat='x'）均以 epoch 毫秒（Number）与后端通信，
 * 因此后端在序列化/反序列化 LocalDateTime 时统一使用 epoch 毫秒，避免默认的数组/字符串格式
 * 导致前后端时间解析失败。本系统业务时区固定为 Asia/Shanghai，使用绝对时间戳保证一致性。</p>
 */
@Configuration
public class JacksonConfig {

    /** 业务时区（中国标准时间） */
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Bean
    public Module rpwJavaTimeModule() {
        com.fasterxml.jackson.databind.module.SimpleModule module =
                new com.fasterxml.jackson.databind.module.SimpleModule();
        module.addSerializer(LocalDateTime.class, new LocalDateTimeMillisSerializer());
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeMillisDeserializer());
        return module;
    }

    /** LocalDateTime -> epoch 毫秒（数字） */
    static class LocalDateTimeMillisSerializer extends JsonSerializer<LocalDateTime> {
        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            if (value == null) {
                gen.writeNull();
                return;
            }
            gen.writeNumber(value.atZone(ZONE).toInstant().toEpochMilli());
        }
    }

    /** epoch 毫秒（数字或字符串） -> LocalDateTime */
    static class LocalDateTimeMillisDeserializer extends JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {
            JsonNode node = p.readValueAsTree();
            if (node == null || node.isNull()) {
                return null;
            }
            long millis;
            if (node.isNumber()) {
                millis = node.asLong();
            } else if (node.isTextual()) {
                String text = node.asText().trim();
                if (text.isEmpty()) {
                    return null;
                }
                try {
                    millis = Long.parseLong(text);
                } catch (NumberFormatException e) {
                    // 兼容 ISO-8601 字符串
                    return LocalDateTime.parse(text);
                }
            } else {
                return null;
            }
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZONE);
        }
    }
}
