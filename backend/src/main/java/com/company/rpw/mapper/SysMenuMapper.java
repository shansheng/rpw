package com.company.rpw.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.rpw.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统菜单 Mapper
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 统计某父级下的子菜单数量（删除校验用）
     */
    @Select("SELECT COUNT(*) FROM sys_menu WHERE parent_id = #{parentId} AND deleted = 0")
    long countByParentId(@Param("parentId") Long parentId);
}
