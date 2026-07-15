package com.company.rpw.service;

import com.company.rpw.entity.ResourcePlanSafety;
import com.company.rpw.mapper.ResourcePlanSafetyMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 安全物资资源计划服务类
 */
@Slf4j
@Service
public class ResourcePlanSafetyService extends AbstractResourcePlanService<ResourcePlanSafety, ResourcePlanSafetyMapper> {

    /**
     * 根据条件查询安全物资计划列表
     * @param projectId 项目ID（可选）
     * @param wbsCode WBS编码（可选，模糊匹配）
     * @param safetyName 安全物资名称（可选，模糊匹配）
     * @return 安全物资计划列表
     */
    public List<ResourcePlanSafety> listByParams(Long projectId, String wbsCode, String safetyName) {
        var query = lambdaQuery().eq(ResourcePlanSafety::getDeleted, 0);

        if (projectId != null) {
            query = query.eq(ResourcePlanSafety::getProjectId, projectId);
        }
        if (wbsCode != null && !wbsCode.isEmpty()) {
            query = query.like(ResourcePlanSafety::getWbsCode, wbsCode);
        }
        if (safetyName != null && !safetyName.isEmpty()) {
            query = query.like(ResourcePlanSafety::getSafetyName, safetyName);
        }

        return query.list();
    }

    @Override
    protected void setDefaultStatus(ResourcePlanSafety entity, String status) {
        entity.setStatus(status);
    }

    @Override
    protected void setArrivalDate(ResourcePlanSafety entity, LocalDate actualDate) {
        entity.setActualArrivalDate(actualDate);
    }

    @Override
    protected LocalDate getPlanDate(ResourcePlanSafety entity) {
        return entity.getPlanArrivalDate();
    }

    @Override
    protected LocalDate getActualDate(ResourcePlanSafety entity) {
        return entity.getActualArrivalDate();
    }
}
