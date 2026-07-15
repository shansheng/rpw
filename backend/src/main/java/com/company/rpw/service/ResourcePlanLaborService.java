package com.company.rpw.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.rpw.entity.ResourcePlanLabor;
import com.company.rpw.mapper.ResourcePlanLaborMapper;
import com.company.rpw.dto.labor.WarningStatusVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 劳动力计划服务类
 */
@Slf4j
@Service
public class ResourcePlanLaborService extends ServiceImpl<ResourcePlanLaborMapper, ResourcePlanLabor> {

/**
 * 新增劳动力计划（草稿状态）
 * @param entity 劳动力计划信息
 * @return 是否成功
 */
public boolean create(ResourcePlanLabor entity) {
    entity.setStatus("DRAFT");
    entity.setApprovalStatus("DRAFT");
    return save(entity);
}

/**
 * 根据条件查询劳动力计划列表
 * @param projectId 项目ID（可选）
 * @param status 状态（可选）
 * @param wbsCode WBS编码（可选，模糊匹配）
 * @param workTypeCode 工种编码（可选）
 * @param laborCategoryCode 劳务类别编码（可选）
 * @return 劳动力计划列表
 */
public List<ResourcePlanLabor> listByParams(Long projectId, String status, String wbsCode, 
                                           String workTypeCode, String laborCategoryCode) {
    var query = lambdaQuery().eq(ResourcePlanLabor::getDeleted, 0);
    
    if (projectId != null) {
        query = query.eq(ResourcePlanLabor::getProjectId, projectId);
    }
    if (status != null && !status.isEmpty()) {
        query = query.eq(ResourcePlanLabor::getStatus, status);
    }
    if (wbsCode != null && !wbsCode.isEmpty()) {
        query = query.like(ResourcePlanLabor::getWbsCode, wbsCode);
    }
    if (workTypeCode != null && !workTypeCode.isEmpty()) {
        query = query.eq(ResourcePlanLabor::getWorkTypeCode, workTypeCode);
    }
    if (laborCategoryCode != null && !laborCategoryCode.isEmpty()) {
        query = query.eq(ResourcePlanLabor::getLaborCategoryCode, laborCategoryCode);
    }
    
    return query.list();
}

    /**
     * 修改劳动力计划
     * @param id 劳动力计划ID
     * @param entity 劳动力计划信息
     * @return 是否成功
     */
    public boolean updatePlan(Long id, ResourcePlanLabor entity) {
        entity.setId(id);
        return updateById(entity);
    }

    /**
     * 删除劳动力计划（逻辑删除，仅草稿状态可删）
     * @param id 劳动力计划ID
     * @return 是否成功
     */
    public boolean delete(Long id) {
        ResourcePlanLabor entity = getById(id);
        if (entity == null) {
            return false;
        }
        // 只能删除草稿状态的数据
        if (!"DRAFT".equals(entity.getStatus())) {
            return false;
        }
        return removeById(id);
    }

    /**
     * 提交审批
     * @param id 劳动力计划ID
     * @return 是否成功
     */
    public boolean submit(Long id) {
        ResourcePlanLabor entity = getById(id);
        if (entity == null) {
            return false;
        }
        entity.setStatus("SUBMITTED");
        entity.setApprovalStatus("SUBMITTED");
        return updateById(entity);
    }

    /**
     * 登记进展
     * @param id 劳动力计划ID
     * @param actualQuantity 实际人数
     * @param actualStartDate 实际开始日期
     * @param actualEndDate 实际结束日期
     * @param attendanceRecords 出勤记录（JSON格式）
     * @return 是否成功
     */
    public boolean registerProgress(Long id, Integer actualQuantity, LocalDate actualStartDate, 
                                     LocalDate actualEndDate, String attendanceRecords) {
        ResourcePlanLabor entity = getById(id);
        if (entity == null) {
            return false;
        }
        
        if (actualQuantity != null) {
            entity.setActualQuantity(actualQuantity);
        }
        
        if (actualStartDate != null) {
            entity.setActualStartDate(actualStartDate);
            // 实际开始日期填写后，状态改为进行中
            if (entity.getStatus() == null || "DRAFT".equals(entity.getStatus()) || "SUBMITTED".equals(entity.getStatus())) {
                entity.setStatus("IN_PROGRESS");
            }
        }
        
        if (actualEndDate != null) {
            entity.setActualEndDate(actualEndDate);
            // 实际结束日期填写后，状态改为已完成
            entity.setStatus("COMPLETED");
        }
        
        if (attendanceRecords != null) {
            entity.setAttendanceRecords(attendanceRecords);
        }
        
        return updateById(entity);
    }

    /**
     * 查询预警状态
     * @param id 劳动力计划ID
     * @return 预警状态VO
     */
    public WarningStatusVO getWarningStatus(Long id) {
        ResourcePlanLabor entity = getById(id);
        if (entity == null) {
            return null;
        }
        
        WarningStatusVO vo = new WarningStatusVO();
        vo.setCheckTime(java.time.LocalDateTime.now());
        
        Integer planQuantity = entity.getPlanQuantity();
        Integer actualQuantity = entity.getActualQuantity();
        
        if (planQuantity != null && actualQuantity != null) {
            if (actualQuantity < planQuantity * 0.8) {
                vo.setWarningLevel("CRITICAL");
                vo.setWarningMessage("实际人数(" + actualQuantity + ") < 计划人数(" + planQuantity + ") 的 80%");
                vo.setSuggestion("建议立即补充人力");
            } else if (actualQuantity < planQuantity) {
                vo.setWarningLevel("WARNING");
                vo.setWarningMessage("实际人数(" + actualQuantity + ") < 计划人数(" + planQuantity + ")");
                vo.setSuggestion("建议尽快补充人力");
            } else {
                vo.setWarningLevel("NORMAL");
                vo.setWarningMessage("人力充足");
                vo.setSuggestion("");
            }
        } else {
            vo.setWarningLevel("NORMAL");
            vo.setWarningMessage("暂无数据");
            vo.setSuggestion("");
        }
        
        return vo;
    }

    /**
     * 查询变更历史（暂返回空列表，待集成Flowable后实现）
     * @param id 劳动力计划ID
     * @return 变更记录列表
     */
    public List<com.company.rpw.dto.subcontract.ChangeRecordVO> getChangeHistory(Long id) {
        return java.util.Collections.emptyList();
    }

    /**
     * 创建变更申请
     * @param changeDTO 变更申请信息
     * @return 是否成功
     */
    public boolean createChange(com.company.rpw.dto.subcontract.ChangeDTO changeDTO) {
        ResourcePlanLabor entity = getById(changeDTO.getPlanId());
        if (entity == null) {
            return false;
        }
        entity.setStatus("SUBMITTED");
        entity.setApprovalStatus("SUBMITTED");
        return updateById(entity);
    }
}
