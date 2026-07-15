package com.company.rpw.service;

import com.company.rpw.entity.ResourcePlanCirculation;
import com.company.rpw.mapper.ResourcePlanCirculationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 周转材计划服务类
 */
@Slf4j
@Service
public class ResourcePlanCirculationService extends AbstractResourcePlanService<ResourcePlanCirculation, ResourcePlanCirculationMapper> {

    /**
     * 根据条件查询周转材计划列表
     * @param projectId 项目ID（可选）
     * @param wbsCode WBS编码（可选，模糊匹配）
     * @param circulationName 周转材名称（可选，模糊匹配）
     * @return 周转材计划列表
     */
    public List<ResourcePlanCirculation> listByParams(Long projectId, String wbsCode, String circulationName) {
        var query = lambdaQuery().eq(ResourcePlanCirculation::getDeleted, 0);

        if (projectId != null) {
            query = query.eq(ResourcePlanCirculation::getProjectId, projectId);
        }
        if (wbsCode != null && !wbsCode.isEmpty()) {
            query = query.like(ResourcePlanCirculation::getWbsCode, wbsCode);
        }
        if (circulationName != null && !circulationName.isEmpty()) {
            query = query.like(ResourcePlanCirculation::getCirculationName, circulationName);
        }

        return query.list();
    }

    /**
     * 保留原有方法名兼容性
     */
    public boolean updateCirculation(Long id, ResourcePlanCirculation entity) {
        return updatePlan(id, entity);
    }

    @Override
    protected void setDefaultStatus(ResourcePlanCirculation entity, String status) {
        entity.setStatus(status);
    }

    @Override
    protected void setArrivalDate(ResourcePlanCirculation entity, LocalDate actualDate) {
        entity.setActualArrivalDate(actualDate);
    }

    @Override
    protected LocalDate getPlanDate(ResourcePlanCirculation entity) {
        return entity.getPlanArrivalDate();
    }

    @Override
    protected LocalDate getActualDate(ResourcePlanCirculation entity) {
        return entity.getActualArrivalDate();
    }
}
