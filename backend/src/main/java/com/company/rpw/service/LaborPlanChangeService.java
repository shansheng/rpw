package com.company.rpw.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.rpw.entity.LaborPlanChange;
import com.company.rpw.mapper.LaborPlanChangeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 劳动力计划变更记录服务类
 */
@Slf4j
@Service
public class LaborPlanChangeService extends ServiceImpl<LaborPlanChangeMapper, LaborPlanChange> {

    /**
     * 根据劳动力计划ID查询变更记录
     * @param laborPlanId 劳动力计划ID
     * @return 变更记录列表
     */
    public java.util.List<LaborPlanChange> listByLaborPlanId(Long laborPlanId) {
        return lambdaQuery()
                .eq(LaborPlanChange::getLaborPlanId, laborPlanId)
                .eq(LaborPlanChange::getDeleted, 0)
                .orderByDesc(LaborPlanChange::getCreateTime)
                .list();
    }

    /**
     * 根据流程实例ID查询变更记录
     * @param processInstanceId 流程实例ID
     * @return 变更记录
     */
    public LaborPlanChange getByProcessInstanceId(String processInstanceId) {
        return lambdaQuery()
                .eq(LaborPlanChange::getProcessInstanceId, processInstanceId)
                .eq(LaborPlanChange::getDeleted, 0)
                .one();
    }
}
