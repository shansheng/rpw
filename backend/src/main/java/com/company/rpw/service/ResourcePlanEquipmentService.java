package com.company.rpw.service;

import com.company.rpw.entity.ResourcePlanEquipment;
import com.company.rpw.mapper.ResourcePlanEquipmentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 设备资源计划服务类
 */
@Slf4j
@Service
public class ResourcePlanEquipmentService extends AbstractResourcePlanService<ResourcePlanEquipment, ResourcePlanEquipmentMapper> {

    /**
     * 根据条件查询设备计划列表
     * @param projectId 项目ID（可选）
     * @param wbsCode WBS编码（可选，模糊匹配）
     * @param equipmentName 设备名称（可选，模糊匹配）
     * @return 设备计划列表
     */
    public List<ResourcePlanEquipment> listByParams(Long projectId, String wbsCode, String equipmentName) {
        var query = lambdaQuery().eq(ResourcePlanEquipment::getDeleted, 0);

        if (projectId != null) {
            query = query.eq(ResourcePlanEquipment::getProjectId, projectId);
        }
        if (wbsCode != null && !wbsCode.isEmpty()) {
            query = query.like(ResourcePlanEquipment::getWbsCode, wbsCode);
        }
        if (equipmentName != null && !equipmentName.isEmpty()) {
            query = query.like(ResourcePlanEquipment::getEquipmentName, equipmentName);
        }

        return query.list();
    }

    @Override
    protected void setDefaultStatus(ResourcePlanEquipment entity, String status) {
        entity.setStatus(status);
    }

    @Override
    protected void setArrivalDate(ResourcePlanEquipment entity, LocalDate actualDate) {
        entity.setActualArrivalDate(actualDate);
    }

    @Override
    protected LocalDate getPlanDate(ResourcePlanEquipment entity) {
        return entity.getPlanArrivalDate();
    }

    @Override
    protected LocalDate getActualDate(ResourcePlanEquipment entity) {
        return entity.getActualArrivalDate();
    }
}
