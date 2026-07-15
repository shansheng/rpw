package com.company.rpw.service;

import com.company.rpw.entity.ResourcePlanOffice;
import com.company.rpw.mapper.ResourcePlanOfficeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 办公用品资源计划服务类
 */
@Slf4j
@Service
public class ResourcePlanOfficeService extends AbstractResourcePlanService<ResourcePlanOffice, ResourcePlanOfficeMapper> {

    /**
     * 根据条件查询办公用品计划列表
     * @param projectId 项目ID（可选）
     * @param wbsCode WBS编码（可选，模糊匹配）
     * @param officeName 办公用品名称（可选，模糊匹配）
     * @return 办公用品计划列表
     */
    public List<ResourcePlanOffice> listByParams(Long projectId, String wbsCode, String officeName) {
        var query = lambdaQuery().eq(ResourcePlanOffice::getDeleted, 0);

        if (projectId != null) {
            query = query.eq(ResourcePlanOffice::getProjectId, projectId);
        }
        if (wbsCode != null && !wbsCode.isEmpty()) {
            query = query.like(ResourcePlanOffice::getWbsCode, wbsCode);
        }
        if (officeName != null && !officeName.isEmpty()) {
            query = query.like(ResourcePlanOffice::getOfficeName, officeName);
        }

        return query.list();
    }

    @Override
    protected void setDefaultStatus(ResourcePlanOffice entity, String status) {
        entity.setStatus(status);
    }

    @Override
    protected void setArrivalDate(ResourcePlanOffice entity, LocalDate actualDate) {
        entity.setActualArrivalDate(actualDate);
    }

    @Override
    protected LocalDate getPlanDate(ResourcePlanOffice entity) {
        return entity.getPlanArrivalDate();
    }

    @Override
    protected LocalDate getActualDate(ResourcePlanOffice entity) {
        return entity.getActualArrivalDate();
    }
}
