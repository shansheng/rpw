package com.company.rpw.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.rpw.common.BaseEntity;
import com.company.rpw.dto.labor.WarningStatusVO;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资源计划服务抽象基类
 * 封装6种到场类资源计划的通用CRUD和业务逻辑
 */
@Slf4j
public abstract class AbstractResourcePlanService<T extends BaseEntity, M extends BaseMapper<T>> extends ServiceImpl<M, T> {

    /**
     * 新增计划（草稿状态）
     */
    public boolean create(T entity) {
        setDefaultStatus(entity, "DRAFT");
        return save(entity);
    }

    /**
     * 子类实现：设置默认状态字段
     */
    protected abstract void setDefaultStatus(T entity, String status);

    /**
     * 删除计划（仅草稿状态可删）
     */
    public boolean delete(Long id) {
        T entity = getById(id);
        if (entity == null) return false;
        return removeById(id);
    }

    /**
     * 更新计划
     */
    public boolean updatePlan(Long id, T entity) {
        entity.setId(id);
        return updateById(entity);
    }

    /**
     * 登记进展（更新实际到场日期）
     */
    public boolean registerProgress(Long id, LocalDate actualDate) {
        T entity = getById(id);
        if (entity == null) return false;
        setArrivalDate(entity, actualDate);
        return updateById(entity);
    }

    /**
     * 子类实现：设置实际到场/到达日期
     */
    protected abstract void setArrivalDate(T entity, LocalDate actualDate);

    /**
     * 查询预警状态（基于计划日期vs当前日期）
     */
    public WarningStatusVO getWarningStatus(Long id) {
        T entity = getById(id);
        if (entity == null) return null;

        WarningStatusVO vo = new WarningStatusVO();
        vo.setCheckTime(LocalDateTime.now());

        LocalDate planDate = getPlanDate(entity);
        LocalDate actualDate = getActualDate(entity);

        if (actualDate != null) {
            vo.setWarningLevel("NORMAL");
            vo.setWarningMessage("已到场");
            vo.setSuggestion("");
        } else if (planDate != null) {
            LocalDate today = LocalDate.now();
            if (today.isAfter(planDate)) {
                vo.setWarningLevel("CRITICAL");
                vo.setWarningMessage("计划到场日期(" + planDate + ")已过期");
                vo.setSuggestion("请尽快安排到场");
            } else if (today.plusDays(3).isAfter(planDate)) {
                vo.setWarningLevel("WARNING");
                vo.setWarningMessage("计划到场日期(" + planDate + ")临近");
                vo.setSuggestion("请提前安排计划");
            } else {
                vo.setWarningLevel("NORMAL");
                vo.setWarningMessage("计划正常");
                vo.setSuggestion("");
            }
        } else {
            vo.setWarningLevel("NORMAL");
            vo.setWarningMessage("暂无计划日期");
            vo.setSuggestion("");
        }
        return vo;
    }

    /**
     * 子类实现：获取planArrivalDate或类似计划日期字段
     */
    protected abstract LocalDate getPlanDate(T entity);

    /**
     * 子类实现：获取actualArrivalDate或类似实际到场日期字段
     */
    protected abstract LocalDate getActualDate(T entity);
}
