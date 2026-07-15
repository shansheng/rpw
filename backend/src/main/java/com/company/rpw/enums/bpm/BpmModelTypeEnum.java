package com.company.rpw.enums.bpm;

import lombok.Getter;

/**
 * BPM 模型类型：BPMN 普通流程 / SIMPLE 简易（低代码）流程
 */
@Getter
public enum BpmModelTypeEnum {

    BPMN(10, "BPMN"),
    SIMPLE(20, "SIMPLE");

    private final Integer code;
    private final String name;

    BpmModelTypeEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static BpmModelTypeEnum of(Integer code) {
        for (BpmModelTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
