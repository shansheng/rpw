package com.company.rpw.common;

import lombok.Data;

/**
 * 统一响应结果封装类
 * @param <T> 响应数据类型
 */
@Data
public class R<T> {

    /** 状态码：200成功，其他为失败 */
    private int code;

    /** 响应消息 */
    private String message;

    /** 响应数据 */
    private T data;

    /** 时间戳 */
    private long timestamp;

    public R() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 成功响应（带数据）
     * @param data 响应数据
     * @return R对象
     */
    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(200);
        r.setMessage("success");
        r.setData(data);
        return r;
    }

    /**
     * 成功响应（无数据）
     * @return R对象
     */
    public static <T> R<T> ok() {
        return ok(null);
    }

    /**
     * 失败响应
     * @param code 错误码
     * @param message 错误消息
     * @return R对象
     */
    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    /**
     * 失败响应（使用默认错误码500）
     * @param message 错误消息
     * @return R对象
     */
    public static <T> R<T> fail(String message) {
        return fail(500, message);
    }
}
