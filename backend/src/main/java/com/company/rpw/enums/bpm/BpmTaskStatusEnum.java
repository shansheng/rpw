package com.company.rpw.enums.bpm;

import lombok.Getter;

/**
 * 任务状态
 */
@Getter
public enum BpmTaskStatusEnum {

    RUNNING(1, "审批中"),
    APPROVE(2, "通过"),
    REJECT(3, "不通过"),
    CANCEL(4, "已取消"),
    SKIP(5, "已跳过"),
    WITHDRAW(6, "已撤回");

    private final Integer code;
    private final String name;

    BpmTaskStatusEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static BpmTaskStatusEnum of(Integer code) {
        for (BpmTaskStatusEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
