package com.company.rpw.bpm;

import lombok.Data;

/**
 * 流程相关用户简要信息（用于 startUsers / assigneeUser 等）
 */
@Data
public class BpmUserInfo {
    private Long id;
    private String nickname;
    private String avatar;
    private String deptName;
}
