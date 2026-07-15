package com.company.rpw.dto.bpm;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * OA 请假创建请求 VO
 *
 * <p>前端提交包含：请假类型、事由、起止时间（epoch 毫秒，由 Jackson 模块转为 LocalDateTime），
 * 以及「发起人自选审批人」映射（key=用户任务节点ID，value=审批人用户ID列表）。</p>
 */
@Data
public class BpmOaLeaveCreateReqVO {

    /** 请假类型编码，对应字典 bpm_oa_leave_type 的值（如 "1" 事假） */
    private String type;

    /** 请假事由 */
    private String reason;

    /** 请假开始时间（epoch 毫秒） */
    private LocalDateTime startTime;

    /** 请假结束时间（epoch 毫秒） */
    private LocalDateTime endTime;

    /** 发起人自选审批人：节点ID -> 审批人用户ID列表（用于 START_USER_SELECT 策略节点） */
    private Map<String, List<Long>> startUserSelectAssignees;
}
