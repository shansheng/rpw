package com.company.rpw.entity.bpm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.rpw.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * BPM 流程分类实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bpm_category")
public class BpmCategory extends BaseEntity {

    /** 分类名称 */
    @TableField("name")
    private String name;

    /** 分类编码 */
    @TableField("code")
    private String code;

    /** 状态：0 禁用 / 1 启用 */
    @TableField("status")
    private Integer status;

    /** 描述 */
    @TableField("description")
    private String description;

    /** 排序 */
    @TableField("sort")
    private Integer sort;
}
