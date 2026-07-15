package com.company.rpw.entity.bpm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.rpw.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 流程模型（设计态）。部署后关联 Flowable 的 processDefinition。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bpm_model")
public class BpmModel extends BaseEntity {

    /** 模型名称 */
    private String name;

    /** 模型标识（流程 key），对应 Flowable processDefinitionKey */
    @TableField("`key`")
    private String key;

    /** 图标 */
    private String icon;

    /** 分类 code（关联 bpm_category.code） */
    private String category;

    /** 模型类型：1 BPMN / 2 SIMPLE（BpmModelTypeEnum） */
    private Integer type;

    /** 表单类型：1 流程表单 / 2 业务表单（BpmModelFormTypeEnum） */
    private Integer formType;

    /** 表单ID（bpm_form.id） */
    private Long formId;

    /** 业务表单创建路径（自定义 Vue 组件路径） */
    private String formCustomCreatePath;

    /** 业务表单查看路径 */
    private String formCustomViewPath;

    /** 关联的流程定义 key */
    private String processDefinitionKey;

    /** 关联的流程定义ID */
    private String processDefinitionId;

    /** 关联的流程定义版本 */
    private Integer processDefinitionVersion;

    /** 部署时间 */
    private LocalDateTime deploymentTime;

    /** 状态：0 未部署（草稿） / 1 已部署 */
    private Integer status;

    /** BPMN XML */
    private String bpmnXml;

    /** 简易流程（SIMPLE）节点树 JSON，仅 type=2(SIMPLE) 时使用 */
    private String simpleModel;

    /** 发起人ID列表（逗号分隔） */
    private String startUserIds;

    /** 可发起部门ID列表（逗号分隔） */
    private String startDeptIds;

    /** 流程管理员ID列表（逗号分隔） */
    private String managerUserIds;

    /** 是否可见 */
    private Boolean visible;

    /** 排序 */
    private Integer sort;

    /** 备注 */
    private String remark;

    /** 描述 */
    private String description;
}
