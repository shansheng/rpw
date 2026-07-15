package com.company.rpw.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.company.rpw.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 组织架构实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("organization")
public class Organization extends BaseEntity {

    /** 组织名称 */
    private String orgName;

    /** 组织级别：1局 2公司 3项目（部门节点继承父级级别） */
    private Integer orgLevel;

    /** 上级组织ID */
    private Long parentId;

    /** 节点类型：1组织节点(局/公司/项目) 2部门节点 */
    private Integer nodeType;

    /** 同级排序（越小越靠前；部门默认排在组织之前） */
    private Integer sort;

    /** 项目编码（仅项目节点使用） */
    private String projectCode;

    /** 状态（仅项目节点：1进行中 2已完工 3已暂停） */
    private Integer status;

    /** 计划开始日期（仅项目节点） */
    private LocalDate planStartDate;

    /** 计划结束日期（仅项目节点） */
    private LocalDate planEndDate;

    /** 部门（历史冗余字段，部门节点改用 orgName，保留兼容） */
    private String department;

    /** 处室（历史冗余字段，保留兼容） */
    private String section;

    /** 子节点列表（用于树形结构展示，不在数据库中映射） */
    @TableField(exist = false)
    private List<Organization> children;
}
