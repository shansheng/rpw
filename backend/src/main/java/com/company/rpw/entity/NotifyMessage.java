package com.company.rpw.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.rpw.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 站内信消息实体类
 * 对应 yudao-ui-admin-vben 的 system/notify-message 模块。
 * 本后端为精简业务后端，仅实现未读计数所需字段，其余模板字段保留以便后续扩展。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notify_message")
public class NotifyMessage extends BaseEntity {

    /** 用户编号 */
    private Long userId;

    /** 用户类型：1=后台管理员 */
    private Integer userType;

    /** 模板编号 */
    private Long templateId;

    /** 模板编码 */
    private String templateCode;

    /** 模板名称 */
    private String templateNickname;

    /** 模板内容 */
    private String templateContent;

    /** 模板类型 */
    private Integer templateType;

    /** 模板参数（JSON 字符串） */
    private String templateParams;

    /** 是否已读：0=未读 1=已读 */
    private Integer readStatus;

    /** 阅读时间 */
    private LocalDateTime readTime;
}
