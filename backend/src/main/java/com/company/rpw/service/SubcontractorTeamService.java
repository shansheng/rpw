package com.company.rpw.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.rpw.entity.SubcontractorTeam;
import com.company.rpw.mapper.SubcontractorTeamMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 分包队伍统计服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubcontractorTeamService extends ServiceImpl<SubcontractorTeamMapper, SubcontractorTeam> {

    private final SubcontractorTeamMapper subcontractorTeamMapper;

    /**
     * 查询列表（支持项目ID、专业工程筛选）
     */
    public List<SubcontractorTeam> list(Long projectId, String professionalEngineering) {
        if (projectId != null) {
            return subcontractorTeamMapper.selectByProjectId(projectId);
        }
        if (professionalEngineering != null && !professionalEngineering.isBlank()) {
            return subcontractorTeamMapper.selectByProfessionalEngineering(professionalEngineering);
        }
        return list(Wrappers.<SubcontractorTeam>lambdaQuery().orderByAsc(SubcontractorTeam::getId));
    }

    /**
     * 新增
     */
    public boolean create(SubcontractorTeam entity) {
        return save(entity);
    }

    /**
     * 修改
     */
    public boolean update(Long id, SubcontractorTeam entity) {
        entity.setId(id);
        return updateById(entity);
    }

    /**
     * 删除
     */
    public boolean delete(Long id) {
        return removeById(id);
    }
}
