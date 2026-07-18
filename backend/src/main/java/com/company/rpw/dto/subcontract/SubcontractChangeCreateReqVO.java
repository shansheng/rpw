package com.company.rpw.dto.subcontract;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 分包计划变更创建请求
 */
@Data
public class SubcontractChangeCreateReqVO {

    /** 选中的分包计划ID（主表"id"字段） */
    private Long planId;

    /** 项目ID */
    private Long projectId;

    /** 项目名称 */
    private String projectName;

    /** 专业工程 */
    private String specialtyEngineering;

    /** 分包名称 */
    private String subcontractName;

    /** 分包模式 */
    private String subcontractMode;

    /** 队伍来源 */
    private String teamSource;

    /** 备注 */
    private String remark;

    /** 明细列表 */
    private List<DetailReq> details;

    /** 发起人自选审批人：节点ID -> 用户ID列表（与 p_Leave 保持一致） */
    private Map<String, List<Long>> startUserSelectAssignees;

    /**
     * 明细行请求
     */
    @Data
    public static class DetailReq {
        /** 日期类型：1最晚进场 2招标文件 3挂网 4定标 */
        private Integer dateType;
        /** 原日期 */
        private LocalDate originalDate;
        /** 调整后日期 */
        private LocalDate adjustedDate;
    }
}
