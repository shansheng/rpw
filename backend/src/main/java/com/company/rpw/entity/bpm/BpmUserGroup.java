package com.company.rpw.entity.bpm;

import com.company.rpw.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程用户分组（审批人分组）
 * user_ids 以逗号分隔的 Long 字符串存储，对外暴露为 List<Long>。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BpmUserGroup extends BaseEntity {

    /** 分组名称 */
    private String name;

    /** 分组描述 */
    private String description;

    /** 成员用户ID列表（逗号分隔存储） */
    private String userIds;

    /** 状态：0 禁用 / 1 启用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
