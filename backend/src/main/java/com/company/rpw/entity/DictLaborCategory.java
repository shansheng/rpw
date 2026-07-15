package com.company.rpw.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.company.rpw.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 劳务类别字典实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dict_labor_category")
public class DictLaborCategory extends BaseEntity {

    /** 类别编码（唯一） */
    @TableField("category_code")
    private String categoryCode;

    /** 类别名称 */
    @TableField("category_name")
    private String categoryName;

    /** 排序 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 状态（1-启用 0-禁用） */
    @TableField("status")
    private Integer status;
}
