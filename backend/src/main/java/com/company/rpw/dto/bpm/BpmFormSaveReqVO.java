package com.company.rpw.dto.bpm;

import lombok.Data;

import java.util.List;

/**
 * BPM 流程表单保存请求 VO
 * <p>
 * fields 在 API 层为字段名数组（与前端 form-create 约定一致），
 * 持久化时由 controller 转为 JSON 字符串存入 bpm_form.fields(longtext)。
 */
@Data
public class BpmFormSaveReqVO {

    /** 主键（更新时必填，新增时忽略） */
    private Long id;

    /** 表单名称 */
    private String name;

    /** form-create 的 JSON 配置字符串 */
    private String conf;

    /** 字段名数组 */
    private List<String> fields;

    /** 状态：0 禁用 / 1 启用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
