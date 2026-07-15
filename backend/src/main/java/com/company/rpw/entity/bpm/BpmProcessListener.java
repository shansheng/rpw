package com.company.rpw.entity.bpm;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.rpw.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * BPM 流程监听器
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bpm_process_listener")
public class BpmProcessListener extends BaseEntity {

    /** 监听器名称 */
    private String name;

    /** 监听器类型：execution / task（对应 BpmProcessListenerTypeEnum） */
    private String type;

    /** 状态：0 禁用 / 1 启用 */
    private Integer status;

    /** 事件名，如 create / complete */
    private String event;

    /** 值类型：class / expression / delegateExpression */
    private String valueType;

    /** 类全名或表达式 */
    private String value;

    /** 备注 */
    private String remark;
}
