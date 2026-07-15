package com.company.rpw.entity.bpm;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.rpw.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * BPM 流程表达式实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bpm_process_expression")
public class BpmProcessExpression extends BaseEntity {

    /** 名称 */
    private String name;

    /** 状态（0-禁用，1-启用） */
    private Integer status;

    /** 表达式（例如 ${xxx}） */
    private String expression;

    /** 备注 */
    private String remark;
}
