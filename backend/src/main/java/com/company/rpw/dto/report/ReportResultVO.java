package com.company.rpw.dto.report;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 报表查询结果 VO
 */
@Data
public class ReportResultVO {

    /** 字段列表 */
    private List<FieldConfig> fields;

    /** 查询数据 */
    private List<Map<String, Object>> data;

    /** 总记录数 */
    private Integer total;

    /**
     * 字段配置
     */
    @Data
    public static class FieldConfig {
        /** 字段名 */
        private String field;

        /** 字段标签 */
        private String label;

        /** 是否可见 */
        private Boolean visible;

        /** 列宽 */
        private Integer width;
    }
}
