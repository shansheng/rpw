package com.company.rpw.controller;

import com.company.rpw.common.R;
import com.company.rpw.entity.LaborPlanChange;
import com.company.rpw.entity.ResourcePlanLabor;
import com.company.rpw.service.LaborPlanChangeService;
import com.company.rpw.service.ResourcePlanLaborService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.RuntimeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 劳动力计划变更 Controller
 * 前端调用路径: /api/v1/resource-plan/labor-change
 *
 * <p>说明：创建变更即启动 laborChangeApproval 流程（businessKey = laborChange:&lt;id&gt;）。
 * 审批统一在工作流「待办任务」通过 /approval/approve|reject 完成，
 * 本控制器不再提供独立的 approve/reject，避免产生孤儿 Flowable 任务。</p>
 */
@RestController
@RequestMapping("/api/v1/resource-plan/labor-change")
public class LaborPlanChangeController {

    private final LaborPlanChangeService laborPlanChangeService;
    private final ResourcePlanLaborService resourcePlanLaborService;
    private final RuntimeService runtimeService;
    private final IdentityService identityService;

    public LaborPlanChangeController(LaborPlanChangeService laborPlanChangeService,
                                      ResourcePlanLaborService resourcePlanLaborService,
                                      RuntimeService runtimeService,
                                      IdentityService identityService) {
        this.laborPlanChangeService = laborPlanChangeService;
        this.resourcePlanLaborService = resourcePlanLaborService;
        this.runtimeService = runtimeService;
        this.identityService = identityService;
    }

    /**
     * 创建变更申请
     * POST /api/v1/resource-plan/labor-change
     */
    @PostMapping
    public R<LaborPlanChange> create(@RequestBody LaborPlanChange laborPlanChange) {
        // 查找原劳动力计划，记录旧值
        ResourcePlanLabor laborPlan = resourcePlanLaborService.getById(laborPlanChange.getLaborPlanId());
        if (laborPlan == null) {
            return R.fail("劳动力计划不存在");
        }

        laborPlanChange.setOldPlanStartDate(laborPlan.getPlanStartDate());
        laborPlanChange.setOldPlanEndDate(laborPlan.getPlanEndDate());
        laborPlanChange.setOldPlanQuantity(laborPlan.getPlanQuantity());
        laborPlanChange.setApprovalStatus("PENDING");
        laborPlanChange.setApplicant(getCurrentUser());

        laborPlanChangeService.save(laborPlanChange);

        // 启动 Flowable 流程: laborChangeApproval（businessKey 统一为 laborChange:<id>）
        String businessKey = "laborChange:" + laborPlanChange.getId();
        Map<String, Object> variables = new java.util.HashMap<>();
        variables.put("changeType", laborPlanChange.getChangeType());
        variables.put("planId", laborPlanChange.getLaborPlanId());
        variables.put("applicant", getCurrentUser());

        identityService.setAuthenticatedUserId(getCurrentUser());
        var processInstance = runtimeService.startProcessInstanceByKey(
                "laborChangeApproval", businessKey, variables);
        identityService.setAuthenticatedUserId(null);

        laborPlanChange.setProcessInstanceId(processInstance.getId());
        laborPlanChange.setApprovalStatus("SUBMITTED");
        laborPlanChangeService.updateById(laborPlanChange);

        return R.ok(laborPlanChange);
    }

    /**
     * 查询变更列表
     * GET /api/v1/resource-plan/labor-change/list?laborPlanId=xxx
     */
    @GetMapping("/list")
    public R<List<LaborPlanChange>> list(@RequestParam Long laborPlanId) {
        return R.ok(laborPlanChangeService.listByLaborPlanId(laborPlanId));
    }

    /**
     * 重新提交（被驳回后再次发起审批流程）
     * POST /api/v1/resource-plan/labor-change/{id}/resubmit
     */
    @PostMapping("/{id}/resubmit")
    public R<Void> resubmit(@PathVariable Long id) {
        LaborPlanChange change = laborPlanChangeService.getById(id);
        if (change == null) {
            return R.fail("变更记录不存在");
        }

        String businessKey = "laborChange:" + change.getId();
        Map<String, Object> variables = new java.util.HashMap<>();
        variables.put("changeType", change.getChangeType());
        variables.put("planId", change.getLaborPlanId());
        variables.put("applicant", getCurrentUser());

        identityService.setAuthenticatedUserId(getCurrentUser());
        var processInstance = runtimeService.startProcessInstanceByKey(
                "laborChangeApproval", businessKey, variables);
        identityService.setAuthenticatedUserId(null);

        change.setProcessInstanceId(processInstance.getId());
        change.setApprovalStatus("SUBMITTED");
        laborPlanChangeService.updateById(change);
        return R.ok();
    }

    /**
     * 获取当前用户
     */
    private String getCurrentUser() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "anonymous";
    }
}
