package com.company.rpw.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.rpw.common.BizException;
import com.company.rpw.entity.Company;
import com.company.rpw.entity.Project;
import com.company.rpw.mapper.CompanyMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 公司服务类
 */
@Slf4j
@Service
public class CompanyService extends ServiceImpl<CompanyMapper, Company> {

    @Autowired
    private CompanyMapper companyMapper;

    @Autowired
    private ProjectService projectService;

    /**
     * 根据组织ID查询公司列表
     * @param orgId 组织ID
     * @return 公司列表
     */
    public List<Company> getByOrgId(Long orgId) {
        return companyMapper.selectByOrgId(orgId);
    }

    /**
     * 新增公司
     * @param company 公司信息
     * @return 是否成功
     */
    public boolean create(Company company) {
        return save(company);
    }

    /**
     * 修改公司
     * @param id 公司ID
     * @param company 公司信息
     * @return 是否成功
     */
    public boolean updateCompany(Long id, Company company) {
        company.setId(id);
        return updateById(company);
    }

    /**
     * 删除公司
     * @param id 公司ID
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        // 检查是否有项目关联该公司
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Project::getCompanyId, id);
        wrapper.eq(Project::getDeleted, 0);
        long count = projectService.count(wrapper);
        if (count > 0) {
            throw new BizException("该公司下有关联的项目，无法删除");
        }
        return removeById(id);
    }
}
