package com.company.rpw.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.rpw.entity.SubcontractorTeam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 分包队伍统计 Mapper 接口
 */
@Mapper
public interface SubcontractorTeamMapper extends BaseMapper<SubcontractorTeam> {

    /**
     * 根据项目ID查询
     */
    List<SubcontractorTeam> selectByProjectId(@Param("projectId") Long projectId);

    /**
     * 根据专业工程模糊查询
     */
    List<SubcontractorTeam> selectByProfessionalEngineering(@Param("professionalEngineering") String professionalEngineering);
}
