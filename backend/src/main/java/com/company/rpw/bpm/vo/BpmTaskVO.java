package com.company.rpw.bpm.vo;

import com.company.rpw.bpm.BpmUserInfo;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 流程任务视图对象（待办/已办/管理/审批详情共用）。
 * 字段尽量对齐前端 BpmTaskApi.Task；未使用的复杂字段置 null，前端可容错。
 */
@Data
public class BpmTaskVO {
    private String id;
    private String name;
    private Integer status;
    private Long createTime;
    private Long endTime;
    private Long durationInMillis;
    private String reason;
    private String signPicUrl;
    private BpmUserInfo ownerUser;
    private BpmUserInfo assigneeUser;
    private String taskDefinitionKey;
    private String processInstanceId;
    /** 流程实例简要信息（id/name/summary/startUser/createTime） */
    private Map<String, Object> processInstance;
    private Object parentTaskId;
    private Object children;
    private Long formId;
    private String formName;
    private String formConf;
    private Object formFields;
    private Object formVariables;
    private Integer formType;
    private String formCustomViewPath;
    private Object buttonsSetting;
    private Object signEnable;
    private Object reasonRequire;
    private Object nodeType;
    private List<BpmTaskVO> listByParentTaskId;
}
