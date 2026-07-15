package com.company.rpw.enums.bpm;

import lombok.Getter;

/**
 * 流程监听器类型
 */
@Getter
public enum BpmProcessListenerTypeEnum {

    EXECUTION(1, "执行监听器"),
    TASK(2, "任务监听器");

    private final Integer code;
    private final String name;

    BpmProcessListenerTypeEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static BpmProcessListenerTypeEnum of(Integer code) {
        for (BpmProcessListenerTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
