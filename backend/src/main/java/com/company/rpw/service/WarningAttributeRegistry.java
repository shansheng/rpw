package com.company.rpw.service;

import com.company.rpw.entity.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 预警对象属性注册表。
 *
 * <p>按 {@code objectType} 反射对应计划实体的字段，生成可被规则表达式引用的属性元数据
 * （中文名称 / 字段名 / 数据类型）。同时维护对象类型显示名与计划名称字段。</p>
 */
@Slf4j
@Component
public class WarningAttributeRegistry {

    /** 属性元数据类型 */
    public enum AttrType {
        DATE, NUMBER, STRING, BOOLEAN
    }

    @Getter
    public static class AttributeMeta {
        /** 中文显示名（表达式中使用） */
        public final String label;
        /** 实体字段名 */
        public final String field;
        /** 数据类型 */
        public final AttrType type;

        public AttributeMeta(String label, String field, AttrType type) {
            this.label = label;
            this.field = field;
            this.type = type;
        }
    }

    /** objectType -> 实体类 */
    private static final Map<String, Class<?>> ENTITY_CLASS = new LinkedHashMap<>();
    static {
        ENTITY_CLASS.put("SUBCONTRACT", ResourcePlanSubcontract.class);
        ENTITY_CLASS.put("MATERIAL", ResourcePlanMaterial.class);
        ENTITY_CLASS.put("EQUIPMENT", ResourcePlanEquipment.class);
        ENTITY_CLASS.put("LABOR", ResourcePlanLabor.class);
        ENTITY_CLASS.put("HARDWARE", ResourcePlanHardware.class);
        ENTITY_CLASS.put("CIRCULATION", ResourcePlanCirculation.class);
        ENTITY_CLASS.put("OFFICE", ResourcePlanOffice.class);
        ENTITY_CLASS.put("SAFETY", ResourcePlanSafety.class);
    }

    /** objectType -> 显示名 */
    public static final Map<String, String> OBJECT_TYPE_LABELS = new LinkedHashMap<>();
    static {
        OBJECT_TYPE_LABELS.put("SUBCONTRACT", "分包计划");
        OBJECT_TYPE_LABELS.put("MATERIAL", "材料计划");
        OBJECT_TYPE_LABELS.put("EQUIPMENT", "设备计划");
        OBJECT_TYPE_LABELS.put("LABOR", "劳务计划");
        OBJECT_TYPE_LABELS.put("HARDWARE", "五金计划");
        OBJECT_TYPE_LABELS.put("CIRCULATION", "周转材计划");
        OBJECT_TYPE_LABELS.put("OFFICE", "办公用品计划");
        OBJECT_TYPE_LABELS.put("SAFETY", "安全物资计划");
    }

    /** objectType -> 计划名称字段（用于预警记录展示） */
    private static final Map<String, String> NAME_FIELD = new LinkedHashMap<>();
    static {
        NAME_FIELD.put("SUBCONTRACT", "subcontractName");
        NAME_FIELD.put("MATERIAL", "resourceName");
        NAME_FIELD.put("EQUIPMENT", "equipmentName");
        NAME_FIELD.put("LABOR", "workTypeName");
        NAME_FIELD.put("HARDWARE", "hardwareName");
        NAME_FIELD.put("CIRCULATION", "circulationName");
        NAME_FIELD.put("OFFICE", "officeName");
        NAME_FIELD.put("SAFETY", "safetyName");
    }

    /** 字段名 -> 中文显示名（未命中的字段直接使用英文原名） */
    private static final Map<String, String> FIELD_LABELS = new HashMap<>();
    static {
        FIELD_LABELS.put("latestEntryDate", "最晚进场日期");
        FIELD_LABELS.put("actualEntryDate", "实际进场日期");
        FIELD_LABELS.put("startPrepareBidDate", "开始编制招标文件日期");
        FIELD_LABELS.put("actualBidDate", "实际招标日期");
        FIELD_LABELS.put("plannedOnlineBidDate", "计划挂网日期");
        FIELD_LABELS.put("actualOnlineBidDate", "实际挂网日期");
        FIELD_LABELS.put("plannedAwardDate", "计划定标日期");
        FIELD_LABELS.put("actualAwardDate", "实际定标日期");
        FIELD_LABELS.put("planArrivalDate", "计划到场日期");
        FIELD_LABELS.put("actualArrivalDate", "实际到场日期");
        FIELD_LABELS.put("planStartDate", "计划开始日期");
        FIELD_LABELS.put("actualStartDate", "实际开始日期");
        FIELD_LABELS.put("planEndDate", "计划结束日期");
        FIELD_LABELS.put("actualEndDate", "实际结束日期");
        FIELD_LABELS.put("planQuantity", "计划数量");
        FIELD_LABELS.put("actualQuantity", "实际数量");
        FIELD_LABELS.put("budgetQuantity", "预算数量");
        FIELD_LABELS.put("mobilizationPeriod", "动员周期");
        FIELD_LABELS.put("specification", "规格型号");
        FIELD_LABELS.put("unit", "单位");
        FIELD_LABELS.put("supplierName", "供应商名称");
        FIELD_LABELS.put("supplierCode", "供应商编码");
        FIELD_LABELS.put("wbsName", "WBS名称");
        FIELD_LABELS.put("wbsCode", "WBS编码");
        FIELD_LABELS.put("workTypeName", "工种名称");
        FIELD_LABELS.put("workTypeCode", "工种编码");
        FIELD_LABELS.put("specialtyEngineering", "专业工程");
        FIELD_LABELS.put("subcontractName", "分包名称");
        FIELD_LABELS.put("subcontractMode", "分包模式");
        FIELD_LABELS.put("teamSource", "队伍来源");
    }

    /** 不暴露给规则编辑器的系统/无关字段 */
    private static final Set<String> EXCLUDED = new HashSet<>(Arrays.asList(
            "id", "projectId", "createTime", "updateTime", "deleted",
            "remark", "status", "approvalStatus", "processInstanceId",
            "attendanceRecords", "tenantId", "creator", "updater"
    ));

    /** 系统属性（表达式可直接使用） */
    public static final List<AttributeMeta> SYSTEM_ATTRIBUTES = Arrays.asList(
            new AttributeMeta("当前日期", "当前日期", AttrType.DATE),
            new AttributeMeta("当前时间", "当前时间", AttrType.DATE),
            new AttributeMeta("今天", "今天", AttrType.DATE)
    );

    private final Map<String, List<AttributeMeta>> cache = new HashMap<>();

    /** 获取对象类型的属性列表（含缓存） */
    public List<AttributeMeta> getAttributes(String objectType) {
        return cache.computeIfAbsent(objectType, this::buildAttributes);
    }

    private List<AttributeMeta> buildAttributes(String objectType) {
        Class<?> clazz = ENTITY_CLASS.get(objectType);
        if (clazz == null) {
            return new ArrayList<>();
        }
        List<AttributeMeta> attrs = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            String name = field.getName();
            if (EXCLUDED.contains(name)) {
                continue;
            }
            AttrType type = toAttrType(field.getType());
            if (type == null) {
                continue;
            }
            String label = FIELD_LABELS.getOrDefault(name, name);
            attrs.add(new AttributeMeta(label, name, type));
        }
        return attrs;
    }

    private AttrType toAttrType(Class<?> type) {
        if (LocalDate.class.isAssignableFrom(type) || LocalDateTime.class.isAssignableFrom(type)) {
            return AttrType.DATE;
        }
        if (Number.class.isAssignableFrom(type) || type.isPrimitive() && type != boolean.class) {
            return AttrType.NUMBER;
        }
        if (boolean.class.isAssignableFrom(type) || Boolean.class.isAssignableFrom(type)) {
            return AttrType.BOOLEAN;
        }
        if (CharSequence.class.isAssignableFrom(type)) {
            return AttrType.STRING;
        }
        return null;
    }

    /** 读取计划对象某字段的值（反射，字段可能为 null） */
    public Object getFieldValue(Object plan, String field) {
        try {
            Field f = findField(plan.getClass(), field);
            if (f == null) {
                return null;
            }
            f.setAccessible(true);
            return f.get(plan);
        } catch (Exception e) {
            log.warn("读取字段失败: {} -> {}", plan.getClass().getSimpleName(), field, e);
            return null;
        }
    }

    private Field findField(Class<?> clazz, String field) {
        Class<?> c = clazz;
        while (c != null && c != Object.class) {
            try {
                return c.getDeclaredField(field);
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    /** 获取计划展示名称 */
    public String getPlanName(String objectType, Object plan) {
        String nameField = NAME_FIELD.get(objectType);
        if (nameField == null) {
            return String.valueOf(getFieldValue(plan, "id"));
        }
        Object v = getFieldValue(plan, nameField);
        return v == null ? "" : String.valueOf(v);
    }

    /** 是否支持该对象类型 */
    public boolean isSupported(String objectType) {
        return ENTITY_CLASS.containsKey(objectType);
    }
}
