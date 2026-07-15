package com.company.rpw.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.rpw.entity.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 项目 Mapper 接口
 */
@Mapper
public interface ProjectMapper extends BaseMapper<Project> {

    /**
     * 根据公司ID查询项目列表
     * @param companyId 公司ID
     * @return 项目列表
     */
    List<Project> selectByCompanyId(@Param("companyId") Long companyId);

    /**
     * 根据状态查询项目列表
     * @param status 状态
     * @return 项目列表
     */
    List<Project> selectByStatus(@Param("status") Integer status);
}
