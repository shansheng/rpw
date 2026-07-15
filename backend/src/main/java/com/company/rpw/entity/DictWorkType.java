package com.company.rpw.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.company.rpw.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工种字典实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dict_work_type")
public class DictWorkType extends BaseEntity {

    /** 工种编码（唯一） */
    @TableField("work_type_code")
    private String workTypeCode;

    /** 工种名称 */
    @TableField("work_type_name")
    private String workTypeName;

    /** 排序 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 状态（1-启用 0-禁用） */
    @TableField("status")
    private Integer status;
}
