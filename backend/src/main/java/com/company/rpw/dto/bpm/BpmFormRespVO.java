package com.company.rpw.dto.bpm;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * BPM 流程表单响应 VO
 * <p>
 * fields 返回为字段名数组，便于前端 setConfAndFields / decodeFields 直接使用。
 */
@Data
public class BpmFormRespVO {

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

    /** 创建时间（epoch 毫秒，由 JacksonConfig 全局转换） */
    private LocalDateTime createTime;
}
