package com.company.rpw.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.rpw.entity.Organization;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 组织架构 Mapper 接口
 */
@Mapper
public interface OrganizationMapper extends BaseMapper<Organization> {

    /**
     * 根据父ID查询子节点
     * @param parentId 父节点ID
     * @return 子节点列表
     */
    List<Organization> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 根据级别查询
     * @param orgLevel 组织级别
     * @return 组织列表
     */
    List<Organization> selectByLevel(@Param("orgLevel") Integer orgLevel);

    /**
     * 查询所有子节点ID（包含嵌套子节点）
     * @param id 父节点ID
     * @return 所有子节点ID列表
     */
    List<Long> selectAllChildrenIds(@Param("id") Long id);
}
