package com.company.rpw.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.rpw.dto.report.*;
import com.company.rpw.mapper.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 报表动态查询服务
 * 根据报表配置动态构建查询条件并执行查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportDynamicQueryService {

    private final ResourcePlanSubcontractMapper subcontractMapper;
    private final com.company.rpw.mapper.ResourcePlanLaborMapper laborMapper;
    private final com.company.rpw.mapper.ResourcePlanEquipmentMapper equipmentMapper;
    private final com.company.rpw.mapper.ResourcePlanMaterialMapper materialMapper;
    private final com.company.rpw.mapper.ResourcePlanSafetyMapper safetyMapper;
    private final com.company.rpw.mapper.ResourcePlanOfficeMapper officeMapper;
    private final com.company.rpw.mapper.ResourcePlanHardwareMapper hardwareMapper;
    private final com.company.rpw.mapper.ResourcePlanCirculationMapper circulationMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 根据报表类型和配置JSON执行动态查询
     * @param reportType 报表类型
     * @param configJson 配置JSON
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 查询结果列表（每行为Map）
     */
    public List<Map<String, Object>> dynamicQuery(String reportType, String configJson, int pageNum, int pageSize) {
        ReportConfigJson config = parseConfigJson(configJson);

        return switch (reportType) {
            case "SUBTRACT" -> querySubcontract(config, pageNum, pageSize);
            case "LABOR" -> queryLabor(config, pageNum, pageSize);
            case "EQUIPMENT" -> queryEquipment(config, pageNum, pageSize);
            case "MATERIAL" -> queryMaterial(config, pageNum, pageSize);
            case "SAFETY" -> querySafety(config, pageNum, pageSize);
            case "OFFICE" -> queryOffice(config, pageNum, pageSize);
            case "HARDWARE" -> queryHardware(config, pageNum, pageSize);
            case "CIRCULATION" -> queryCirculation(config, pageNum, pageSize);
            default -> throw new IllegalArgumentException("不支持的报表类型: " + reportType);
        };
    }

    /**
     * 构建动态查询条件
     */
    @SuppressWarnings("unchecked")
    public <T> QueryWrapper<T> buildQueryWrapper(ReportConfigJson config, Class<T> entityClass) {
        QueryWrapper<T> wrapper = new QueryWrapper<>();

        // 添加筛选条件
        if (config.getFilters() != null) {
            for (ReportConfigJson.FilterItem filter : config.getFilters()) {
                String field = camelToSnake(filter.getField());
                switch (filter.getOperator()) {
                    case "EQ" -> wrapper.eq(field, filter.getValue());
                    case "NE" -> wrapper.ne(field, filter.getValue());
                    case "LIKE" -> wrapper.like(field, filter.getValue());
                    case "GT" -> wrapper.gt(field, filter.getValue());
                    case "LT" -> wrapper.lt(field, filter.getValue());
                    case "GTE" -> wrapper.ge(field, filter.getValue());
                    case "LTE" -> wrapper.le(field, filter.getValue());
                    case "BETWEEN" -> {
                        List<Object> values = (List<Object>) filter.getValue();
                        if (values != null && values.size() == 2) {
                            wrapper.between(field, values.get(0), values.get(1));
                        }
                    }
                    case "IN" -> {
                        List<Object> values = (List<Object>) filter.getValue();
                        if (values != null && !values.isEmpty()) {
                            wrapper.in(field, values);
                        }
                    }
                    default -> log.warn("未知的筛选操作符: {}", filter.getOperator());
                }
            }
        }

        // 添加排序规则
        if (config.getSorts() != null) {
            for (ReportConfigJson.SortItem sort : config.getSorts()) {
                String field = camelToSnake(sort.getField());
                if ("ASC".equalsIgnoreCase(sort.getOrder())) {
                    wrapper.orderByAsc(field);
                } else {
                    wrapper.orderByDesc(field);
                }
            }
        }

        return wrapper;
    }

    /**
     * 解析配置JSON
     */
    public ReportConfigJson parseConfigJson(String configJson) {
        try {
            return objectMapper.readValue(configJson, ReportConfigJson.class);
        } catch (Exception e) {
            log.error("解析报表配置JSON失败: {}", configJson, e);
            throw new RuntimeException("报表配置JSON格式错误");
        }
    }

    /**
     * 驼峰转下划线
     */
    private String camelToSnake(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        StringBuilder sb = new StringBuilder();
        for (char c : camelCase.toCharArray()) {
            if (Character.isUpperCase(c)) {
                if (sb.length() > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private List<Map<String, Object>> querySubcontract(ReportConfigJson config, int pageNum, int pageSize) {
        QueryWrapper<com.company.rpw.entity.ResourcePlanSubcontract> wrapper = buildQueryWrapper(config, com.company.rpw.entity.ResourcePlanSubcontract.class);
        Page<com.company.rpw.entity.ResourcePlanSubcontract> page = new Page<>(pageNum, pageSize);
        List<com.company.rpw.entity.ResourcePlanSubcontract> list = subcontractMapper.selectPage(page, wrapper).getRecords();
        return convertToMapList(list);
    }

    private List<Map<String, Object>> queryLabor(ReportConfigJson config, int pageNum, int pageSize) {
        var wrapper = buildQueryWrapper(config, com.company.rpw.entity.ResourcePlanLabor.class);
        Page<com.company.rpw.entity.ResourcePlanLabor> page = new Page<>(pageNum, pageSize);
        var list = laborMapper.selectPage(page, wrapper).getRecords();
        return convertToMapList(list);
    }

    private List<Map<String, Object>> queryEquipment(ReportConfigJson config, int pageNum, int pageSize) {
        var wrapper = buildQueryWrapper(config, com.company.rpw.entity.ResourcePlanEquipment.class);
        Page<com.company.rpw.entity.ResourcePlanEquipment> page = new Page<>(pageNum, pageSize);
        var list = equipmentMapper.selectPage(page, wrapper).getRecords();
        return convertToMapList(list);
    }

    private List<Map<String, Object>> queryMaterial(ReportConfigJson config, int pageNum, int pageSize) {
        var wrapper = buildQueryWrapper(config, com.company.rpw.entity.ResourcePlanMaterial.class);
        Page<com.company.rpw.entity.ResourcePlanMaterial> page = new Page<>(pageNum, pageSize);
        var list = materialMapper.selectPage(page, wrapper).getRecords();
        return convertToMapList(list);
    }

    private List<Map<String, Object>> querySafety(ReportConfigJson config, int pageNum, int pageSize) {
        var wrapper = buildQueryWrapper(config, com.company.rpw.entity.ResourcePlanSafety.class);
        Page<com.company.rpw.entity.ResourcePlanSafety> page = new Page<>(pageNum, pageSize);
        var list = safetyMapper.selectPage(page, wrapper).getRecords();
        return convertToMapList(list);
    }

    private List<Map<String, Object>> queryOffice(ReportConfigJson config, int pageNum, int pageSize) {
        var wrapper = buildQueryWrapper(config, com.company.rpw.entity.ResourcePlanOffice.class);
        Page<com.company.rpw.entity.ResourcePlanOffice> page = new Page<>(pageNum, pageSize);
        var list = officeMapper.selectPage(page, wrapper).getRecords();
        return convertToMapList(list);
    }

    private List<Map<String, Object>> queryHardware(ReportConfigJson config, int pageNum, int pageSize) {
        var wrapper = buildQueryWrapper(config, com.company.rpw.entity.ResourcePlanHardware.class);
        Page<com.company.rpw.entity.ResourcePlanHardware> page = new Page<>(pageNum, pageSize);
        var list = hardwareMapper.selectPage(page, wrapper).getRecords();
        return convertToMapList(list);
    }

    private List<Map<String, Object>> queryCirculation(ReportConfigJson config, int pageNum, int pageSize) {
        var wrapper = buildQueryWrapper(config, com.company.rpw.entity.ResourcePlanCirculation.class);
        Page<com.company.rpw.entity.ResourcePlanCirculation> page = new Page<>(pageNum, pageSize);
        var list = circulationMapper.selectPage(page, wrapper).getRecords();
        return convertToMapList(list);
    }

    /**
     * 将实体列表转为Map列表（用于动态字段映射）
     */
    private List<Map<String, Object>> convertToMapList(List<?> list) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> map = objectMapper.convertValue(item, new TypeReference<>() {});
            result.add(map);
        }
        return result;
    }
}
