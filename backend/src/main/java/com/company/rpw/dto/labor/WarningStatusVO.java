package com.company.rpw.dto.labor;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预警状态 VO
 */
@Data
public class WarningStatusVO {

    /** 预警级别（NORMAL-正常/WARNING-黄色/CRITICAL-红色） */
    private String warningLevel;

    /** 预警消息 */
    private String warningMessage;

    /** 建议 */
    private String suggestion;

    /** 检查时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkTime;
}
