package com.company.rpw.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.rpw.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统菜单实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    /** 菜单名称 */
    private String name;

    /** 权限标识（按钮类型必填） */
    private String permission;

    /** 菜单类型：1目录 2菜单 3按钮 */
    private Integer type;

    /** 显示顺序 */
    private Integer sort;

    /** 父菜单ID（0顶级） */
    private Long parentId;

    /** 路由地址 */
    private String path;

    /** 菜单图标 */
    private String icon;

    /** 组件地址 */
    private String component;

    /** 组件名称（路由 name，单页 push 命中用） */
    private String componentName;

    /** 状态：1启用 0禁用 */
    private Integer status;

    /** 显示状态：1显示 0隐藏 */
    private Integer visible;

    /** 缓存状态：1缓存 0不缓存 */
    private Integer keepAlive;

    /** 总是显示：1总是 0不是 */
    private Integer alwaysShow;
}
