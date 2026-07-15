package com.company.rpw.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.rpw.entity.Project;
import com.company.rpw.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 项目服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService extends ServiceImpl<ProjectMapper, Project> {

    private final ProjectMapper projectMapper;

    /**
     * 根据公司ID查询项目列表
     * @param companyId 公司ID
     * @return 项目列表
     */
    public List<Project> getByCompanyId(Long companyId) {
        return projectMapper.selectByCompanyId(companyId);
    }

    /**
     * 根据状态查询项目列表
     * @param status 状态
     * @return 项目列表
     */
    public List<Project> getByStatus(Integer status) {
        return projectMapper.selectByStatus(status);
    }

    /**
     * 新增项目
     * @param project 项目信息
     * @return 是否成功
     */
    public boolean create(Project project) {
        return save(project);
    }

    /**
     * 修改项目
     * @param id 项目ID
     * @param project 项目信息
     * @return 是否成功
     */
    public boolean updateProject(Long id, Project project) {
        project.setId(id);
        return updateById(project);
    }

    /**
     * 更新项目状态
     * @param id 项目ID
     * @param status 状态：1进行中 2已完工 3已暂停
     * @return 是否成功
     */
    public boolean updateStatus(Long id, Integer status) {
        Project project = new Project();
        project.setId(id);
        project.setStatus(status);
        return updateById(project);
    }

    /**
     * 删除项目
     * @param id 项目ID
     * @return 是否成功
     */
    public boolean delete(Long id) {
        return removeById(id);
    }
}
