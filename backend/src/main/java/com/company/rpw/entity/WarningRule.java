package com.company.rpw.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.company.rpw.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 预警规则实体（表达式驱动）
 *
 * <p>每条规则绑定一个预警对象（如分包计划/材料计划），并通过 {@link #conditionExpr}
 * 条件表达式判断该对象是否触发预警；触发时生成 {@link #warningLevel} 对应等级的预警。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("warning_rule")
public class WarningRule extends BaseEntity {

    /** 规则名称 */
    private String name;

    /**
     * 预警对象类型
     * MATERIAL/EQUIPMENT/HARDWARE/CIRCULATION/OFFICE/SAFETY/SUBCONTRACT/LABOR
     */
    private String objectType;

    /** 条件表达式（如：isnull(实际招标日期) and 3 < 开始编制招标文件日期 - 当前日期 and 开始编制招标文件日期 - 当前日期 < 7） */
    private String conditionExpr;

    /** 预警类型（RED-红色/ORANGE-橙色/YELLOW-黄色） */
    private String warningLevel;

    /** 优先级（数字越小越优先；用于排序与展示） */
    private Integer priority;

    /** 是否启用（0-禁用，1-启用） */
    private Integer enabled;

    /** 规则说明 */
    private String remark;
}
