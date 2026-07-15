package com.company.rpw.enums.bpm;

import lombok.Getter;

/**
 * 流程表单类型：NORMAL 流程表单（存于 bpm_form）/ CUSTOM 业务表单（自定义 Vue 路径）
 */
@Getter
public enum BpmModelFormTypeEnum {

    NORMAL(1, "流程表单"),
    CUSTOM(2, "业务表单");

    private final Integer code;
    private final String name;

    BpmModelFormTypeEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static BpmModelFormTypeEnum of(Integer code) {
        for (BpmModelFormTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
