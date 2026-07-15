package com.company.rpw.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.rpw.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统部门实体（数据权限范围）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDept extends BaseEntity {

    /** 部门名称 */
    private String name;

    /** 父部门ID（0顶级） */
    private Long parentId;

    /** 显示顺序 */
    private Integer sort;

    /** 状态：1启用 0禁用 */
    private Integer status;

    /** 负责人用户ID */
    private Long leaderUserId;

    /** 联系电话 */
    private String phone;

    /** 邮箱 */
    private String email;
}
