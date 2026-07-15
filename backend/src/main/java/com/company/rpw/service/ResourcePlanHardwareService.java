package com.company.rpw.service;

import com.company.rpw.entity.ResourcePlanHardware;
import com.company.rpw.mapper.ResourcePlanHardwareMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 五金计划服务类
 */
@Slf4j
@Service
public class ResourcePlanHardwareService extends AbstractResourcePlanService<ResourcePlanHardware, ResourcePlanHardwareMapper> {

    /**
     * 根据条件查询五金计划列表
     * @param projectId 项目ID（可选）
     * @param wbsCode WBS编码（可选，模糊匹配）
     * @param hardwareName 五金名称（可选，模糊匹配）
     * @return 五金计划列表
     */
    public List<ResourcePlanHardware> listByParams(Long projectId, String wbsCode, String hardwareName) {
        var query = lambdaQuery().eq(ResourcePlanHardware::getDeleted, 0);

        if (projectId != null) {
            query = query.eq(ResourcePlanHardware::getProjectId, projectId);
        }
        if (wbsCode != null && !wbsCode.isEmpty()) {
            query = query.like(ResourcePlanHardware::getWbsCode, wbsCode);
        }
        if (hardwareName != null && !hardwareName.isEmpty()) {
            query = query.like(ResourcePlanHardware::getHardwareName, hardwareName);
        }

        return query.list();
    }

    /**
     * 保留原有方法名兼容性
     */
    public boolean updateHardware(Long id, ResourcePlanHardware entity) {
        return updatePlan(id, entity);
    }

    @Override
    protected void setDefaultStatus(ResourcePlanHardware entity, String status) {
        entity.setStatus(status);
    }

    @Override
    protected void setArrivalDate(ResourcePlanHardware entity, LocalDate actualDate) {
        entity.setActualArrivalDate(actualDate);
    }

    @Override
    protected LocalDate getPlanDate(ResourcePlanHardware entity) {
        return entity.getPlanArrivalDate();
    }

    @Override
    protected LocalDate getActualDate(ResourcePlanHardware entity) {
        return entity.getActualArrivalDate();
    }
}
