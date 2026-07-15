package com.company.rpw.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.rpw.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统部门 Mapper
 */
@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /**
     * 统计某父级下的子部门数量（删除校验用）
     */
    @Select("SELECT COUNT(*) FROM sys_dept WHERE parent_id = #{parentId} AND deleted = 0")
    long countByParentId(@Param("parentId") Long parentId);
}
