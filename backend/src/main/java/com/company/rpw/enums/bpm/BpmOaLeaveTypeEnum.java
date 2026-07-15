package com.company.rpw.enums.bpm;

import lombok.Getter;

/**
 * OA 请假类型（演示业务表单）
 */
@Getter
public enum BpmOaLeaveTypeEnum {

    DAY_OFF(1, "事假"),
    SICK_LEAVE(2, "病假"),
    ANNUAL_LEAVE(3, "年假"),
    MARRIAGE_LEAVE(4, "婚假"),
    MATERNITY_LEAVE(5, "产假");

    private final Integer code;
    private final String name;

    BpmOaLeaveTypeEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static BpmOaLeaveTypeEnum of(Integer code) {
        for (BpmOaLeaveTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
