package com.company.rpw.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.rpw.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统角色实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    /** 角色名称 */
    private String name;

    /** 角色标识（唯一） */
    private String code;

    /** 显示顺序 */
    private Integer sort;

    /** 状态：1启用 0禁用 */
    private Integer status;

    /** 类型：1系统内置 2自定义 */
    private Integer type;

    /** 数据范围：1全部 2仅本人 3所在部门 4部门及子部门 5自定义 */
    private Integer dataScope;

    /** 自定义部门数据范围（逗号分隔） */
    private String dataScopeDeptIds;

    /** 备注 */
    private String remark;
}
