package com.company.rpw.enums.bpm;

import lombok.Getter;

/**
 * BPM 节点类型
 */
@Getter
public enum BpmNodeTypeEnum {

    START(1, "开始节点"),
    APPROVE(2, "审批节点"),
    COPY(3, "抄送节点"),
    TRANSACTOR(4, "办理节点"),
    CONDITION(5, "条件分支"),
    DELAY_TIMER(6, "延迟器"),
    TRIGGER(7, "触发器"),
    CHILD_PROCESS(8, "子流程"),
    ROUTE(9, "路由网关节点");

    private final Integer code;
    private final String name;

    BpmNodeTypeEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static BpmNodeTypeEnum of(Integer code) {
        for (BpmNodeTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
