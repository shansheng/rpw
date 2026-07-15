package com.company.rpw.entity.bpm;

import com.company.rpw.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程评论 / 抄送记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BpmComment extends BaseEntity {

    /** 任务ID */
    private String taskId;

    /** 流程实例ID */
    private String processInstanceId;

    /** 类型：1 评论 / 2 审批意见 / 3 抄送 */
    private Integer type;

    /** 内容 */
    private String message;

    /** 用户ID */
    private Long userId;
}
