package com.company.rpw.entity.bpm;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.rpw.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * BPM 流程表单实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bpm_form")
public class BpmForm extends BaseEntity {

    /** 表单名称 */
    private String name;

    /** form-create 的 JSON 配置字符串 (longtext) */
    private String conf;

    /** 字段名数组的 JSON 字符串 (longtext)，例如 "[\"field1\",\"field2\"]" */
    private String fields;

    /** 状态：0 禁用 / 1 启用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
