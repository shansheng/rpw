package com.company.rpw.controller.vo;

import lombok.Data;
import java.util.List;

/**
 * 适配 yudao-ui-admin-vben 的菜单/路由树节点。
 * 字段命名与前端 convertServerMenuToRouteRecordStringComponent 对齐：
 * component 为 src/views 下的相对路径（不含 .vue、不含 views 前缀），
 * 或 'Layout'/'BasicLayout'/'IFrameView'。
 */
@Data
public class MenuAdapter {
    private Long id;
    private String name;
    private String path;
    private String component;
    private String componentName;
    private Long parentId;
    private String icon;
    private Boolean visible = true;
    private Boolean keepAlive = false;
    private Integer sort;
    private List<MenuAdapter> children;
}
