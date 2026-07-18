package com.company.rpw.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.rpw.bpm.BpmCurrentUser;
import com.company.rpw.common.PageResult;
import com.company.rpw.dto.subcontract.SubcontractChangeCreateReqVO;
import com.company.rpw.entity.ResourcePlanSubcontract;
import com.company.rpw.entity.ResourcePlanSubcontractChange;
import com.company.rpw.entity.ResourcePlanSubcontractChangeDetail;
import com.company.rpw.entity.SysUser;
import com.company.rpw.mapper.ResourcePlanSubcontractChangeDetailMapper;
import com.company.rpw.mapper.ResourcePlanSubcontractChangeMapper;
import com.company.rpw.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分包计划变更服务类（主从结构 + p_fbchange 流程驱动）
 *
 * <p>创建：保存主表 + 明细 -> 启动 p_fbchange 流程实例（businessKey=变更ID）
 * -> 自动完成"提交"节点 -> 将"审核"节点指派给发起人选择的审批人。
 * 审批通过后由 {@code SubcontractChangeProcessListener} 回调
 * {@link #applyApprovedChanges(Long)} 将明细中的调整后日期写回对应分包计划。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourcePlanSubcontractChangeService
        extends ServiceImpl<ResourcePlanSubcontractChangeMapper, ResourcePlanSubcontractChange> {

    private static final String PROCESS_KEY = "p_fbchange";
    /** 审核节点（ candidateStrategy=35，发起人自选审批人） */
    private static final String TASK_REVIEW = "Activity_196iy0w";
    /** 审批节点 */
    private static final String TASK_APPROVE = "Activity_0mc5im0";

    private final ResourcePlanSubcontractChangeDetailMapper detailMapper;
    private final ResourcePlanSubcontractService subcontractService;
    private final SysUserMapper sysUserMapper;
    private final RuntimeService runtimeService;
    private final TaskService taskService;

    /**
     * 创建分包计划变更并启动流程
     */
    public Long create(SubcontractChangeCreateReqVO req) {
        String username = BpmCurrentUser.getUsername();

        ResourcePlanSubcontractChange change = new ResourcePlanSubcontractChange();
        change.setPlanId(req.getPlanId());
        change.setProjectId(req.getProjectId());
        change.setProjectName(req.getProjectName());
        change.setSpecialtyEngineering(req.getSpecialtyEngineering());
        change.setSubcontractName(req.getSubcontractName());
        change.setSubcontractMode(req.getSubcontractMode());
        change.setTeamSource(req.getTeamSource());
        change.setRemark(req.getRemark());
        change.setCreator(username);
        change.setStatus("RUNNING");
        change.setProcessInstanceId("");
        save(change);

        // 保存明细（序号由后端统一编排）
        if (req.getDetails() != null) {
            int seq = 1;
            for (SubcontractChangeCreateReqVO.DetailReq d : req.getDetails()) {
                ResourcePlanSubcontractChangeDetail detail = new ResourcePlanSubcontractChangeDetail();
                detail.setChangeId(change.getId());
                detail.setSeq(seq++);
                detail.setDateType(d.getDateType());
                detail.setOriginalDate(d.getOriginalDate());
                detail.setAdjustedDate(d.getAdjustedDate());
                detailMapper.insert(detail);
            }
        }

        // 汇总发起人自选审批人 -> 用户名列表（按顺序：审核、审批）
        List<String> usernames = collectUsernames(req.getStartUserSelectAssignees());
        String approver1 = usernames.isEmpty() ? username : usernames.get(0);
        String approver2 = usernames.size() > 1 ? usernames.get(1) : approver1;

        Map<String, Object> variables = new HashMap<>();
        variables.put("fbchangeApprover1", approver1);
        variables.put("fbchangeApprover2", approver2);

        // 以当前用户身份启动流程实例（businessKey 绑定变更ID）
        Authentication.setAuthenticatedUserId(username);
        ProcessInstance pi;
        try {
            pi = runtimeService.startProcessInstanceByKey(
                    PROCESS_KEY, String.valueOf(change.getId()), variables);
        } finally {
            Authentication.setAuthenticatedUserId(null);
        }

        // 自动完成"提交"任务，推进到审核节点
        List<Task> submitTasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        for (Task t : submitTasks) {
            try {
                taskService.setAssignee(t.getId(), username);
            } catch (Exception ignored) {
            }
            taskService.complete(t.getId());
        }

        // 为"审核"节点指派发起人选择的审批人
        List<Task> reviewTasks = taskService.createTaskQuery()
                .processInstanceId(pi.getId())
                .taskDefinitionKey(TASK_REVIEW)
                .list();
        for (Task t : reviewTasks) {
            try {
                taskService.setAssignee(t.getId(), approver1);
            } catch (Exception ignored) {
            }
        }

        // 回写流程实例ID
        change.setProcessInstanceId(pi.getId());
        updateById(change);
        return change.getId();
    }

    /**
     * 取消变更（删除流程实例 + 业务状态置为已取消）
     */
    public boolean cancel(Long id, String reason) {
        ResourcePlanSubcontractChange change = getById(id);
        if (change == null) {
            return false;
        }
        if (!"RUNNING".equals(change.getStatus())) {
            return false;
        }
        if (StringUtils.hasText(change.getProcessInstanceId())) {
            try {
                runtimeService.deleteProcessInstance(change.getProcessInstanceId(),
                        "用户取消: " + (reason == null ? "" : reason));
            } catch (Exception ignored) {
            }
        }
        change.setStatus("CANCEL");
        updateById(change);
        return true;
    }

    /**
     * 查询变更明细
     */
    public List<ResourcePlanSubcontractChangeDetail> getDetails(Long changeId) {
        return detailMapper.selectList(Wrappers
                .<ResourcePlanSubcontractChangeDetail>lambdaQuery()
                .eq(ResourcePlanSubcontractChangeDetail::getChangeId, changeId)
                .orderByAsc(ResourcePlanSubcontractChangeDetail::getSeq));
    }

    /**
     * 分页查询变更列表
     */
    public PageResult<ResourcePlanSubcontractChange> page(int pageNo, int pageSize,
                                                          Long projectId, String subcontractName, String status) {
        long current = pageNo <= 0 ? 1 : pageNo;
        long size = pageSize <= 0 ? 10 : pageSize;

        var wrapper = Wrappers.<ResourcePlanSubcontractChange>lambdaQuery();
        wrapper.eq(projectId != null, ResourcePlanSubcontractChange::getProjectId, projectId);
        wrapper.like(StringUtils.hasText(subcontractName),
                ResourcePlanSubcontractChange::getSubcontractName, subcontractName);
        wrapper.eq(StringUtils.hasText(status), ResourcePlanSubcontractChange::getStatus, status);
        wrapper.orderByDesc(ResourcePlanSubcontractChange::getCreateTime);

        IPage<ResourcePlanSubcontractChange> page = page(new Page<>(current, size), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    /**
     * 审批通过后，根据明细调整内容写回对应分包计划日期字段。
     * 幂等：已审批通过则跳过。
     */
    @Transactional(rollbackFor = Exception.class)
    public void applyApprovedChanges(Long changeId) {
        ResourcePlanSubcontractChange change = getById(changeId);
        if (change == null) {
            return;
        }
        if ("APPROVED".equals(change.getStatus())) {
            return;
        }
        List<ResourcePlanSubcontractChangeDetail> details = getDetails(changeId);
        if (change.getPlanId() != null) {
            ResourcePlanSubcontract plan = subcontractService.getById(change.getPlanId());
            if (plan != null) {
                for (ResourcePlanSubcontractChangeDetail d : details) {
                    applyDate(plan, d.getDateType(), d.getAdjustedDate());
                }
                subcontractService.updateById(plan);
            }
        }
        change.setStatus("APPROVED");
        updateById(change);
        log.info("分包计划变更[{}]审批通过，已写回分包计划[{}]日期字段", changeId, change.getPlanId());
    }

    /**
     * 按日期类型将调整后日期写回分包计划对应字段
     */
    private void applyDate(ResourcePlanSubcontract plan, Integer dateType, LocalDate date) {
        if (dateType == null) {
            return;
        }
        switch (dateType) {
            case 1:
                plan.setLatestEntryDate(date);
                break;
            case 2:
                plan.setStartPrepareBidDate(date);
                break;
            case 3:
                plan.setPlannedOnlineBidDate(date);
                break;
            case 4:
                plan.setPlannedAwardDate(date);
                break;
            default:
                break;
        }
    }

    /**
     * 将发起人自选审批人（节点ID -> 用户ID列表）扁平化为有序用户名列表
     */
    private List<String> collectUsernames(Map<String, List<Long>> approvers) {
        List<String> names = new ArrayList<>();
        if (approvers == null || approvers.isEmpty()) {
            return names;
        }
        List<String> keys = new ArrayList<>(approvers.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            List<Long> ids = approvers.get(key);
            if (ids == null) {
                continue;
            }
            for (Long uid : ids) {
                SysUser u = sysUserMapper.selectById(uid);
                if (u != null && StringUtils.hasText(u.getUsername())) {
                    names.add(u.getUsername());
                }
            }
        }
        return names;
    }
}
