package com.company.rpw.dto.report;

import lombok.Data;

import java.util.List;

/**
 * 报表配置JSON结构（解析 config_json 字段）
 */
@Data
public class ReportConfigJson {

    /** 字段配置列表 */
    private List<FieldItem> fields;

    /** 筛选条件列表 */
    private List<FilterItem> filters;

    /** 排序规则列表 */
    private List<SortItem> sorts;

    /**
     * 字段配置项
     */
    @Data
    public static class FieldItem {
        /** 字段名（对应数据库列名） */
        private String field;

        /** 字段标签（显示名称） */
        private String label;

        /** 是否可见 */
        private Boolean visible;

        /** 列宽（可选） */
        private Integer width;
    }

    /**
     * 筛选条件项
     */
    @Data
    public static class FilterItem {
        /** 字段名 */
        private String field;

        /** 操作符：EQ, NE, LIKE, GT, LT, GTE, LTE, BETWEEN, IN */
        private String operator;

        /** 筛选值（BETWEEN时为List，IN时为List） */
        private Object value;
    }

    /**
     * 排序规则项
     */
    @Data
    public static class SortItem {
        /** 字段名 */
        private String field;

        /** 排序方向：ASC, DESC */
        private String order;
    }
}
