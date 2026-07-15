package com.company.rpw.enums.bpm;

import lombok.Getter;

/**
 * 流程实例状态
 */
@Getter
public enum BpmProcessInstanceStatusEnum {

    RUNNING(1, "审批中"),
    FINISH(2, "审批通过"),
    CANCEL(3, "已取消"),
    INVALID(4, "审批不通过");

    private final Integer code;
    private final String name;

    BpmProcessInstanceStatusEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static BpmProcessInstanceStatusEnum of(Integer code) {
        for (BpmProcessInstanceStatusEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
