package com.company.rpw.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.rpw.entity.SysUserRole;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 用户角色关联 Mapper
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /** 查询用户拥有的角色ID列表 */
    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    /** 删除用户的全部角色关联 */
    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    /** 删除角色的全部用户关联（删角色时级联） */
    @Delete("DELETE FROM sys_user_role WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /** 批量插入用户角色关联 */
    @Insert("<script>INSERT INTO sys_user_role (user_id, role_id) VALUES " +
            "<foreach collection='roleIds' item='rid' separator=','>(#{userId}, #{rid})</foreach></script>")
    void insertBatch(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);

    /** 批量查询用户-角色关联（列表/批量回填用） */
    @Select("<script>SELECT user_id, role_id FROM sys_user_role WHERE user_id IN " +
            "<foreach collection='userIds' item='uid' open='(' separator=',' close=')'>#{uid}</foreach></script>")
    List<SysUserRole> selectByUserIds(@Param("userIds") List<Long> userIds);
}
