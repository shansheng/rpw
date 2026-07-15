package com.company.rpw.enums.bpm;

import lombok.Getter;

/**
 * 流程候选人策略（审批人来源）
 */
@Getter
public enum BpmCandidateStrategyEnum {

    START_USER_SELECT(10, "发起人自选"),
    APPROVE_USER_SELECT(20, "审批人自选"),
    ROLE(30, "角色"),
    DEPT_LEADER(40, "部门领导"),
    POST(50, "岗位"),
    USER(60, "用户"),
    USER_GROUP(70, "用户组"),
    EXPRESSION(80, "表达式"),
    EMPTY(90, "为空"),
    SUPER_LEADER(100, "连续多级部门领导");

    private final Integer code;
    private final String name;

    BpmCandidateStrategyEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static BpmCandidateStrategyEnum of(Integer code) {
        for (BpmCandidateStrategyEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
