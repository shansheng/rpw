package com.company.rpw.service;

import com.company.rpw.dto.subcontract.ChangeDTO;
import com.company.rpw.dto.subcontract.ChangeRecordVO;
import com.company.rpw.entity.ResourcePlanMaterial;
import com.company.rpw.mapper.ResourcePlanMaterialMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * 材料资源计划服务类
 */
@Slf4j
@Service
public class ResourcePlanMaterialService extends AbstractResourcePlanService<ResourcePlanMaterial, ResourcePlanMaterialMapper> {

    /**
     * 根据条件查询材料计划列表
     * @param projectId 项目ID（可选）
     * @param approvalStatus 审批状态（可选）
     * @param wbsCode WBS编码（可选，模糊匹配）
     * @param resourceName 材料名称（可选，模糊匹配）
     * @return 材料计划列表
     */
    public List<ResourcePlanMaterial> listByParams(Long projectId, String approvalStatus, String wbsCode, String resourceName) {
        var query = lambdaQuery().eq(ResourcePlanMaterial::getDeleted, 0);

        if (projectId != null) {
            query = query.eq(ResourcePlanMaterial::getProjectId, projectId);
        }
        if (approvalStatus != null && !approvalStatus.isEmpty()) {
            query = query.eq(ResourcePlanMaterial::getApprovalStatus, approvalStatus);
        }
        if (wbsCode != null && !wbsCode.isEmpty()) {
            query = query.like(ResourcePlanMaterial::getWbsCode, wbsCode);
        }
        if (resourceName != null && !resourceName.isEmpty()) {
            query = query.like(ResourcePlanMaterial::getResourceName, resourceName);
        }

        return query.list();
    }

    /**
     * 提交审批
     * @param id 材料计划ID
     * @return 是否成功
     */
    public boolean submit(Long id) {
        ResourcePlanMaterial entity = getById(id);
        if (entity == null) {
            return false;
        }
        entity.setApprovalStatus("SUBMITTED");
        return updateById(entity);
    }

    /**
     * 查询变更历史（暂返回空列表，待集成Flowable后实现）
     * @param id 材料计划ID
     * @return 变更记录列表
     */
    public List<ChangeRecordVO> getChangeHistory(Long id) {
        // TODO: 集成Flowable后实现
        return Collections.emptyList();
    }

    /**
     * 创建变更申请
     * @param changeDTO 变更申请信息
     * @return 是否成功
     */
    public boolean createChange(ChangeDTO changeDTO) {
        ResourcePlanMaterial entity = getById(changeDTO.getPlanId());
        if (entity == null) {
            return false;
        }
        entity.setStatus("SUBMITTED");
        entity.setApprovalStatus("SUBMITTED");
        return updateById(entity);
    }

    @Override
    protected void setDefaultStatus(ResourcePlanMaterial entity, String status) {
        entity.setStatus(status);
        entity.setApprovalStatus(status);
    }

    @Override
    protected void setArrivalDate(ResourcePlanMaterial entity, LocalDate actualDate) {
        entity.setActualArrivalDate(actualDate);
    }

    @Override
    protected LocalDate getPlanDate(ResourcePlanMaterial entity) {
        return entity.getPlanArrivalDate();
    }

    @Override
    protected LocalDate getActualDate(ResourcePlanMaterial entity) {
        return entity.getActualArrivalDate();
    }
}
