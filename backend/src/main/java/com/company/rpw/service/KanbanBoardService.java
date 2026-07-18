package com.company.rpw.service;

import com.company.rpw.dto.kanban.KanbanBoardVO;
import com.company.rpw.dto.kanban.KanbanCardDTO;
import com.company.rpw.dto.kanban.KanbanCardVO;
import com.company.rpw.dto.kanban.KanbanColumnVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 看板服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KanbanBoardService {

    private final ResourcePlanSubcontractService subcontractService;
    private final ResourcePlanLaborService laborService;
    private final ResourcePlanEquipmentService equipmentService;
    private final ResourcePlanMaterialService materialService;
    private final ResourcePlanSafetyService safetyService;
    private final ResourcePlanOfficeService officeService;
    private final ResourcePlanHardwareService hardwareService;
    private final ResourcePlanCirculationService circulationService;

    /**
     * 获取看板数据
     * @param projectId 项目ID（可选）
     * @param resourceType 资源类型（可选：SUBTRACT, LABOR, EQUIPMENT, etc.）
     * @return 看板数据（按状态分组的卡片列表）
     */
    public KanbanBoardVO getBoardData(Long projectId, String resourceType) {
        KanbanBoardVO vo = new KanbanBoardVO();
        List<KanbanColumnVO> columns = new ArrayList<>();

        // 定义看板列配置
        List<KanbanColumnVO> columnConfigs = getColumnConfig();

        // 如果未指定资源类型，返回所有类型的卡片
        if (resourceType == null || "SUBTRACT".equals(resourceType)) {
            buildSubcontractCards(projectId, columnConfigs);
        }
        if (resourceType == null || "LABOR".equals(resourceType)) {
            buildLaborCards(projectId, columnConfigs);
        }
        if (resourceType == null || "EQUIPMENT".equals(resourceType)) {
            buildEquipmentCards(projectId, columnConfigs);
        }
        if (resourceType == null || "MATERIAL".equals(resourceType)) {
            buildMaterialCards(projectId, columnConfigs);
        }
        if (resourceType == null || "SAFETY".equals(resourceType)) {
            buildSafetyCards(projectId, columnConfigs);
        }
        if (resourceType == null || "OFFICE".equals(resourceType)) {
            buildOfficeCards(projectId, columnConfigs);
        }
        if (resourceType == null || "HARDWARE".equals(resourceType)) {
            buildHardwareCards(projectId, columnConfigs);
        }
        if (resourceType == null || "CIRCULATION".equals(resourceType)) {
            buildCirculationCards(projectId, columnConfigs);
        }

        // 只返回有卡片的列
        columns.addAll(columnConfigs.stream()
                .filter(col -> col.getCards() != null && !col.getCards().isEmpty())
                .toList());

        // 计算卡片总数
        int totalCards = columns.stream()
                .mapToInt(col -> col.getCards() != null ? col.getCards().size() : 0)
                .sum();

        vo.setColumns(columns);
        vo.setTotalCards(totalCards);

        return vo;
    }

    /**
     * 更新卡片状态（拖拽后调用）
     * @param dto 卡片状态更新DTO
     * @return 是否成功
     */
    public boolean updateCardStatus(KanbanCardDTO dto) {
        if (dto == null || dto.getId() == null || dto.getResourceType() == null || dto.getNewStatus() == null) {
            log.warn("更新卡片状态参数不完整");
            return false;
        }

        // 校验状态流转合法性
        if (!isValidStatusTransition(dto.getResourceType(), dto.getNewStatus())) {
            log.warn("无效的状态值: {}", dto.getNewStatus());
            return false;
        }

        // 根据资源类型调用对应的服务更新状态
        return switch (dto.getResourceType()) {
            case "SUBTRACT" -> updateSubcontractStatus(dto);
            case "LABOR" -> updateLaborStatus(dto);
            case "EQUIPMENT" -> updateEquipmentStatus(dto);
            case "MATERIAL" -> updateMaterialStatus(dto);
            case "SAFETY" -> updateSafetyStatus(dto);
            case "OFFICE" -> updateOfficeStatus(dto);
            case "HARDWARE" -> updateHardwareStatus(dto);
            case "CIRCULATION" -> updateCirculationStatus(dto);
            default -> {
                log.warn("未知的资源类型: {}", dto.getResourceType());
                yield false;
            }
        };
    }

    /**
     * 获取看板列配置
     * @return 列配置列表
     */
    public List<KanbanColumnVO> getColumnConfig() {
        List<KanbanColumnVO> columns = new ArrayList<>();
        columns.add(createColumnVO("DRAFT", "草稿", 1));
        columns.add(createColumnVO("SUBMITTED", "已提交", 2));
        columns.add(createColumnVO("IN_PROGRESS", "进行中", 3));
        columns.add(createColumnVO("COMPLETED", "已完成", 4));
        columns.add(createColumnVO("TERMINATED", "已终止", 5));
        return columns;
    }

    /**
     * 创建列VO对象
     */
    private KanbanColumnVO createColumnVO(String statusKey, String statusName, int order) {
        KanbanColumnVO column = new KanbanColumnVO();
        column.setStatusKey(statusKey);
        column.setStatusName(statusName);
        column.setColumnOrder(order);
        column.setCards(new ArrayList<>());
        return column;
    }

    /**
     * 校验状态流转是否合法
     */
    private boolean isValidStatusTransition(String resourceType, String newStatus) {
        // 允许的状态值
        List<String> validStatuses = List.of("DRAFT", "SUBMITTED", "IN_PROGRESS", "COMPLETED", "TERMINATED");
        return validStatuses.contains(newStatus);
    }

    /**
     * 构建分包计划卡片
     */
    private void buildSubcontractCards(Long projectId, List<KanbanColumnVO> columns) {
        var subcontracts = subcontractService.listByParams(projectId, null, null, null, null, null, null, null);
        for (var subcontract : subcontracts) {
            KanbanCardVO card = new KanbanCardVO();
            card.setId(subcontract.getId());
            card.setResourceType("SUBTRACT");
            card.setWbsCode(subcontract.getWbsCode());
            card.setResourceName(subcontract.getSubcontractName());
            card.setStatus(subcontract.getStatus());
            card.setPlanStartDate(subcontract.getPlanStartDate());
            card.setPlanEndDate(subcontract.getPlanEndDate());
            addCardToColumn(columns, subcontract.getStatus(), card);
        }
    }

    /**
     * 构建劳动力计划卡片
     */
    private void buildLaborCards(Long projectId, List<KanbanColumnVO> columns) {
        var labors = laborService.listByParams(projectId, null, null, null, null);
        for (var labor : labors) {
            KanbanCardVO card = new KanbanCardVO();
            card.setId(labor.getId());
            card.setResourceType("LABOR");
            card.setWbsCode(labor.getWbsCode());
            card.setResourceName(labor.getWorkTypeCode());
            card.setStatus(labor.getStatus());
            card.setPlanStartDate(labor.getPlanStartDate());
            card.setPlanEndDate(labor.getPlanEndDate());
            addCardToColumn(columns, labor.getStatus(), card);
        }
    }

    /**
     * 构建设备计划卡片
     */
    private void buildEquipmentCards(Long projectId, List<KanbanColumnVO> columns) {
        var all = equipmentService.list();
        for (var entity : all) {
            if (projectId != null && !projectId.equals(entity.getProjectId())) continue;
            KanbanCardVO card = new KanbanCardVO();
            card.setId(entity.getId());
            card.setResourceType("EQUIPMENT");
            card.setWbsCode(entity.getWbsCode());
            card.setResourceName(entity.getEquipmentName());
            card.setStatus(entity.getStatus());
            card.setPlanStartDate(null);
            card.setPlanEndDate(entity.getPlanArrivalDate());
            addCardToColumn(columns, entity.getStatus(), card);
        }
    }

    /**
     * 构建材料计划卡片
     */
    private void buildMaterialCards(Long projectId, List<KanbanColumnVO> columns) {
        var list = materialService.list();
        for (var entity : list) {
            if (projectId != null && !projectId.equals(entity.getProjectId())) continue;
            KanbanCardVO card = new KanbanCardVO();
            card.setId(entity.getId());
            card.setResourceType("MATERIAL");
            card.setWbsCode(entity.getWbsCode());
            card.setResourceName(entity.getResourceName());
            card.setStatus(entity.getStatus());
            card.setPlanStartDate(null);
            card.setPlanEndDate(entity.getPlanArrivalDate());
            addCardToColumn(columns, entity.getStatus(), card);
        }
    }

    /**
     * 构建安全资源计划卡片
     */
    private void buildSafetyCards(Long projectId, List<KanbanColumnVO> columns) {
        var list = safetyService.list();
        for (var entity : list) {
            if (projectId != null && !projectId.equals(entity.getProjectId())) continue;
            KanbanCardVO card = new KanbanCardVO();
            card.setId(entity.getId());
            card.setResourceType("SAFETY");
            card.setWbsCode(entity.getWbsCode());
            card.setResourceName(entity.getSafetyName());
            card.setStatus(entity.getStatus());
            card.setPlanStartDate(null);
            card.setPlanEndDate(entity.getPlanArrivalDate());
            addCardToColumn(columns, entity.getStatus(), card);
        }
    }

    /**
     * 构建办公资源计划卡片
     */
    private void buildOfficeCards(Long projectId, List<KanbanColumnVO> columns) {
        var list = officeService.list();
        for (var entity : list) {
            if (projectId != null && !projectId.equals(entity.getProjectId())) continue;
            KanbanCardVO card = new KanbanCardVO();
            card.setId(entity.getId());
            card.setResourceType("OFFICE");
            card.setWbsCode(entity.getWbsCode());
            card.setResourceName(entity.getOfficeName());
            card.setStatus(entity.getStatus());
            card.setPlanStartDate(null);
            card.setPlanEndDate(entity.getPlanArrivalDate());
            addCardToColumn(columns, entity.getStatus(), card);
        }
    }

    /**
     * 构建硬件计划卡片
     */
    private void buildHardwareCards(Long projectId, List<KanbanColumnVO> columns) {
        var list = hardwareService.list();
        for (var entity : list) {
            if (projectId != null && !projectId.equals(entity.getProjectId())) continue;
            KanbanCardVO card = new KanbanCardVO();
            card.setId(entity.getId());
            card.setResourceType("HARDWARE");
            card.setWbsCode(entity.getWbsCode());
            card.setResourceName(entity.getHardwareName());
            card.setStatus(entity.getStatus());
            card.setPlanStartDate(null);
            card.setPlanEndDate(entity.getPlanArrivalDate());
            addCardToColumn(columns, entity.getStatus(), card);
        }
    }

    /**
     * 构建流通资源计划卡片
     */
    private void buildCirculationCards(Long projectId, List<KanbanColumnVO> columns) {
        var list = circulationService.list();
        for (var entity : list) {
            if (projectId != null && !projectId.equals(entity.getProjectId())) continue;
            KanbanCardVO card = new KanbanCardVO();
            card.setId(entity.getId());
            card.setResourceType("CIRCULATION");
            card.setWbsCode(entity.getWbsCode());
            card.setResourceName(entity.getCirculationName());
            card.setStatus(entity.getStatus());
            card.setPlanStartDate(null);
            card.setPlanEndDate(entity.getPlanArrivalDate());
            addCardToColumn(columns, entity.getStatus(), card);
        }
    }

    /**
     * 将卡片添加到对应的列中
     */
    private void addCardToColumn(List<KanbanColumnVO> columns, String status, KanbanCardVO card) {
        for (var column : columns) {
            if (column.getStatusKey().equals(status)) {
                if (column.getCards() == null) {
                    column.setCards(new ArrayList<>());
                }
                column.getCards().add(card);
                break;
            }
        }
    }

    /**
     * 更新分包计划状态
     */
    private boolean updateSubcontractStatus(KanbanCardDTO dto) {
        var entity = subcontractService.getById(dto.getId());
        if (entity == null) {
            log.warn("分包计划不存在: {}", dto.getId());
            return false;
        }
        entity.setStatus(dto.getNewStatus());
        return subcontractService.updateById(entity);
    }

    /**
     * 更新劳动力计划状态
     */
    private boolean updateLaborStatus(KanbanCardDTO dto) {
        var entity = laborService.getById(dto.getId());
        if (entity == null) {
            log.warn("劳动力计划不存在: {}", dto.getId());
            return false;
        }
        entity.setStatus(dto.getNewStatus());
        return laborService.updateById(entity);
    }

    /**
     * 更新设备计划状态
     */
    private boolean updateEquipmentStatus(KanbanCardDTO dto) {
        var entity = equipmentService.getById(dto.getId());
        if (entity == null) {
            log.warn("设备计划不存在: {}", dto.getId());
            return false;
        }
        entity.setStatus(dto.getNewStatus());
        return equipmentService.updateById(entity);
    }

    /**
     * 更新材料计划状态
     */
    private boolean updateMaterialStatus(KanbanCardDTO dto) {
        var entity = materialService.getById(dto.getId());
        if (entity == null) {
            log.warn("材料计划不存在: {}", dto.getId());
            return false;
        }
        entity.setStatus(dto.getNewStatus());
        return materialService.updateById(entity);
    }

    /**
     * 更新安全资源计划状态
     */
    private boolean updateSafetyStatus(KanbanCardDTO dto) {
        var entity = safetyService.getById(dto.getId());
        if (entity == null) {
            log.warn("安全资源计划不存在: {}", dto.getId());
            return false;
        }
        entity.setStatus(dto.getNewStatus());
        return safetyService.updateById(entity);
    }

    /**
     * 更新办公资源计划状态
     */
    private boolean updateOfficeStatus(KanbanCardDTO dto) {
        var entity = officeService.getById(dto.getId());
        if (entity == null) {
            log.warn("办公资源计划不存在: {}", dto.getId());
            return false;
        }
        entity.setStatus(dto.getNewStatus());
        return officeService.updateById(entity);
    }

    /**
     * 更新硬件计划状态
     */
    private boolean updateHardwareStatus(KanbanCardDTO dto) {
        var entity = hardwareService.getById(dto.getId());
        if (entity == null) {
            log.warn("硬件计划不存在: {}", dto.getId());
            return false;
        }
        entity.setStatus(dto.getNewStatus());
        return hardwareService.updateById(entity);
    }

    /**
     * 更新流通资源计划状态
     */
    private boolean updateCirculationStatus(KanbanCardDTO dto) {
        var entity = circulationService.getById(dto.getId());
        if (entity == null) {
            log.warn("流通资源计划不存在: {}", dto.getId());
            return false;
        }
        entity.setStatus(dto.getNewStatus());
        return circulationService.updateById(entity);
    }
}
