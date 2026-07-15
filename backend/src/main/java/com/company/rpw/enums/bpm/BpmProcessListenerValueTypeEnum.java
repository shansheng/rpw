package com.company.rpw.enums.bpm;

import lombok.Getter;

/**
 * 流程监听器值类型
 */
@Getter
public enum BpmProcessListenerValueTypeEnum {

    CLASS(1, "类"),
    EXPRESSION(2, "表达式"),
    DELEGATE_EXPRESSION(3, "委托表达式");

    private final Integer code;
    private final String name;

    BpmProcessListenerValueTypeEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static BpmProcessListenerValueTypeEnum of(Integer code) {
        for (BpmProcessListenerValueTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
