package com.company.rpw.entity.bpm;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.rpw.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * OA 请假业务表单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bpm_oa_leave")
public class BpmOaLeave extends BaseEntity {

    /** 申请人用户ID */
    private Long userId;

    /** 请假类型编码，如 '1' 对应事假 */
    private String type;

    /** 请假事由 */
    private String reason;

    /** 请假开始时间 */
    private LocalDateTime startTime;

    /** 请假结束时间 */
    private LocalDateTime endTime;

    /** 审批状态：0 审批中 / 1 通过 / 2 不通过 */
    private Integer status;

    /** 关联流程实例ID */
    private String processInstanceId;
}
