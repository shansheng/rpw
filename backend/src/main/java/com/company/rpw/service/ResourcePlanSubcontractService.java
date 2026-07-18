package com.company.rpw.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.rpw.entity.Organization;
import com.company.rpw.entity.ResourcePlanSubcontract;
import com.company.rpw.mapper.OrganizationMapper;
import com.company.rpw.mapper.ResourcePlanSubcontractMapper;
import com.company.rpw.dto.subcontract.ChangeRecordVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 分包计划服务类
 */
@Slf4j
@Service
public class ResourcePlanSubcontractService extends ServiceImpl<ResourcePlanSubcontractMapper, ResourcePlanSubcontract> {

    @Autowired
    private OrganizationMapper organizationMapper;

/**
 * 新增分包计划（草稿状态）
 * @param entity 分包计划信息
 * @return 是否成功
 */
public boolean create(ResourcePlanSubcontract entity) {
    entity.setStatus("DRAFT");
    entity.setApprovalStatus("DRAFT");
    // plan_name 与分包名称保持一致（兼容旧表结构）
    if (entity.getSubcontractName() != null) {
        entity.setPlanName(entity.getSubcontractName());
    }
    // 实际招标/挂网/定标日期在新增时保持为空，由后续进展登记填充
    return save(entity);
}

/**
 * 根据条件查询分包计划列表
 * @param projectId 项目ID（可选）
 * @param projectName 项目名称（可选，模糊匹配）
 * @param specialtyEngineering 专业工程（可选，模糊匹配）
 * @param subcontractName 分包名称（可选，模糊匹配）
 * @param subcontractMode 分包模式（可选）
 * @param teamSource 分包队伍来源（可选）
 * @param status 状态（可选）
 * @param wbsCode WBS编码（可选，模糊匹配）
 * @return 分包计划列表
 */
public List<ResourcePlanSubcontract> listByParams(Long projectId, String projectName, String specialtyEngineering,
        String subcontractName, String subcontractMode, String teamSource, String status, String wbsCode) {
    var query = lambdaQuery().eq(ResourcePlanSubcontract::getDeleted, 0);

    if (projectId != null) {
        // 选中“项目部”节点时，也返回同公司下、同一工程家族的“工程”节点的分包计划。
        // 组织树中“项目部”与“工程”是同级节点（均挂在公司下），通过名称归一化识别同一工程家族。
        List<Long> projectIds = resolveProjectFamily(projectId);
        query = query.in(ResourcePlanSubcontract::getProjectId, projectIds);
    }
    if (StringUtils.hasText(projectName)) {
        query = query.like(ResourcePlanSubcontract::getProjectName, projectName);
    }
    if (StringUtils.hasText(specialtyEngineering)) {
        query = query.like(ResourcePlanSubcontract::getSpecialtyEngineering, specialtyEngineering);
    }
    if (StringUtils.hasText(subcontractName)) {
        query = query.like(ResourcePlanSubcontract::getSubcontractName, subcontractName);
    }
    if (StringUtils.hasText(subcontractMode)) {
        query = query.eq(ResourcePlanSubcontract::getSubcontractMode, subcontractMode);
    }
    if (StringUtils.hasText(teamSource)) {
        query = query.eq(ResourcePlanSubcontract::getTeamSource, teamSource);
    }
    if (StringUtils.hasText(status)) {
        query = query.eq(ResourcePlanSubcontract::getStatus, status);
    }
    if (StringUtils.hasText(wbsCode)) {
        query = query.like(ResourcePlanSubcontract::getWbsCode, wbsCode);
    }

    return query.list();
}

    /**
     * 解析“工程家族”的项目ID集合：以所选节点为基准，返回其本身以及同公司下、
     * 归一化名称相同的所有 level-3 节点（即“项目部”与“工程”视为同一项目的不同节点）。
     */
    private List<Long> resolveProjectFamily(Long projectId) {
        List<Long> result = new ArrayList<>();
        result.add(projectId);
        Organization self = organizationMapper.selectById(projectId);
        if (self == null || self.getOrgLevel() == null || self.getOrgLevel() != 3 || self.getParentId() == null) {
            return result;
        }
        // 项目部/工程 的直接父节点即为公司(level=2)
        Long companyId = self.getParentId();
        String base = projectBaseName(self.getOrgName());
        List<Organization> siblings = organizationMapper.selectByParentId(companyId);
        for (Organization s : siblings) {
            if (s.getId() != null && s.getOrgLevel() != null && s.getOrgLevel() == 3
                    && base.equals(projectBaseName(s.getOrgName()))) {
                result.add(s.getId());
            }
        }
        return result;
    }

    /** 名称归一化：去掉“公路”，并剥除末尾的“项目部/工程”，用于识别同一工程家族 */
    private static String projectBaseName(String name) {
        if (name == null) return "";
        return name.replace("公路", "").replaceAll("(项目部|工程)$", "");
    }

    /**
     * 修改分包计划
     * @param id 分包计划ID
     * @param entity 分包计划信息
     * @return 是否成功
     */
    public boolean updatePlan(Long id, ResourcePlanSubcontract entity) {
        entity.setId(id);
        // plan_name 与分包名称保持一致（兼容旧表结构）
        if (entity.getSubcontractName() != null) {
            entity.setPlanName(entity.getSubcontractName());
        }
        return updateById(entity);
    }

    /**
     * 删除分包计划（逻辑删除，仅草稿状态可删）
     * @param id 分包计划ID
     * @return 是否成功
     */
    public boolean delete(Long id) {
        ResourcePlanSubcontract entity = getById(id);
        if (entity == null) {
            return false;
        }
        // 只能删除草稿状态的数据
        if (!"DRAFT".equals(entity.getStatus())) {
            return false;
        }
        return removeById(id);
    }

    /**
     * 提交审批
     * @param id 分包计划ID
     * @return 是否成功
     */
    public boolean submit(Long id) {
        ResourcePlanSubcontract entity = getById(id);
        if (entity == null) {
            return false;
        }
        entity.setStatus("SUBMITTED");
        entity.setApprovalStatus("SUBMITTED");
        return updateById(entity);
    }

    /**
     * 登记进展
     * @param id 分包计划ID
     * @param actualStartDate 实际开始日期
     * @param actualEndDate 实际结束日期
     * @param remark 备注
     * @return 是否成功
     */
    public boolean registerProgress(Long id, java.time.LocalDate actualStartDate, java.time.LocalDate actualEndDate, String remark) {
        ResourcePlanSubcontract entity = getById(id);
        if (entity == null) {
            return false;
        }

        if (actualStartDate != null) {
            entity.setActualStartDate(actualStartDate);
            // 实际开始日期填写后，状态改为进行中
            if (entity.getStatus() == null || "DRAFT".equals(entity.getStatus()) || "SUBMITTED".equals(entity.getStatus())) {
                entity.setStatus("IN_PROGRESS");
            }
        }

        if (actualEndDate != null) {
            entity.setActualEndDate(actualEndDate);
            // 实际结束日期填写后，状态改为已完成
            entity.setStatus("COMPLETED");
        }

        return updateById(entity);
    }

    /**
     * 查询变更历史（暂返回空列表，待集成Flowable后实现）
     * @param id 分包计划ID
     * @return 变更记录列表
     */
    public List<ChangeRecordVO> getChangeHistory(Long id) {
        // TODO: 集成Flowable后实现
        return java.util.Collections.emptyList();
    }

    /**
     * 创建变更申请
     * @param changeDTO 变更申请信息
     * @return 是否成功
     */
    public boolean createChange(com.company.rpw.dto.subcontract.ChangeDTO changeDTO) {
        ResourcePlanSubcontract entity = getById(changeDTO.getPlanId());
        if (entity == null) {
            return false;
        }
        // 设置为已提交状态，Flowable审批流程通过ApprovalController启动
        entity.setStatus("SUBMITTED");
        entity.setApprovalStatus("SUBMITTED");
        return updateById(entity);
    }
}
