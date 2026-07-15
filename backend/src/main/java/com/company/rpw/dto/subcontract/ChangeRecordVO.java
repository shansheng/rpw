package com.company.rpw.dto.subcontract;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 变更记录 VO
 */
@Data
public class ChangeRecordVO {

    /** 记录ID */
    private Long id;

    /** 变更类型 */
    private String changeType;

    /** 变更内容 */
    private String changeContent;

    /** 申请人 */
    private String applicantName;

    /** 申请时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime applyTime;

    /** 审批状态 */
    private String approvalStatus;
}
