package com.company.rpw.controller;

import com.company.rpw.common.R;
import com.company.rpw.entity.LaborPlanChange;
import com.company.rpw.entity.ResourcePlanLabor;
import com.company.rpw.entity.ResourcePlanMaterial;
import com.company.rpw.entity.ResourcePlanSubcontract;
import com.company.rpw.service.LaborPlanChangeService;
import com.company.rpw.service.ResourcePlanLaborService;
import com.company.rpw.service.ResourcePlanMaterialService;
import com.company.rpw.service.ResourcePlanSubcontractService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统一审批 Controller
 * <p>
 * 所有流程实例 businessKey 统一约定为 {@code planType:planId}：
 * <ul>
 *   <li>material:&lt;id&gt;    物资计划变更</li>
 *   <li>labor:&lt;id&gt;      劳动力计划（通用提交路径）</li>
 *   <li>subcontract:&lt;id&gt; 分包计划变更</li>
 *   <li>laborChange:&lt;id&gt; 劳动力计划变更记录（由 LaborPlanChangeController 发起）</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/v1/approval")
public class ApprovalController {

    private final ResourcePlanMaterialService materialService;
    private final ResourcePlanLaborService laborService;
    private final ResourcePlanSubcontractService subcontractService;
    private final LaborPlanChangeService laborChangeService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final IdentityService identityService;

    public ApprovalController(ResourcePlanMaterialService materialService,
                              ResourcePlanLaborService laborService,
                              ResourcePlanSubcontractService subcontractService,
                              LaborPlanChangeService laborChangeService,
                              RuntimeService runtimeService,
                              TaskService taskService,
                              HistoryService historyService,
                              IdentityService identityService) {
        this.materialService = materialService;
        this.laborService = laborService;
        this.subcontractService = subcontractService;
        this.laborChangeService = laborChangeService;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.historyService = historyService;
        this.identityService = identityService;
    }

    /** planType → Flowable 流程定义 key 映射 */
    private static final Map<String, String> PROCESS_KEY_MAP = Map.of(
            "material", "materialChangeApproval",
            "labor", "laborChangeApproval",
            "subcontract", "subcontractChangeApproval"
    );

    /**
     * 提交审批：根据 planType 启动对应的流程定义
     */
    @PostMapping("/submit")
    public R<Void> submit(@RequestParam("planType") String planType,
                          @RequestParam("planId") Long planId) {
        String processKey = PROCESS_KEY_MAP.get(planType);
        if (processKey == null) {
            return R.fail("不支持的 planType: " + planType);
        }
        Object entity = getPlanEntity(planType, planId);
        if (entity == null) {
            return R.fail("计划不存在");
        }

        try {
            String businessKey = planType + ":" + planId;
            Map<String, Object> variables = Map.of(
                    "planId", planId,
                    "applicant", getCurrentUser()
            );

            identityService.setAuthenticatedUserId(getCurrentUser());
            var processInstance = runtimeService.startProcessInstanceByKey(
                    processKey,
                    businessKey,
                    variables
            );
            identityService.setAuthenticatedUserId(null);

            updatePlanStatus(planType, planId, "SUBMITTED", processInstance.getId());

            return R.ok();
        } catch (Exception e) {
            return R.fail("提交审批失败: " + e.getMessage());
        }
    }

    /**
     * 审批通过
     */
    @PostMapping("/approve")
    public R<Void> approve(@RequestParam("taskId") String taskId,
                           @RequestParam(value = "comment", required = false) String comment) {
        return completeTask(taskId, true, comment);
    }

    /**
     * 审批驳回
     */
    @PostMapping("/reject")
    public R<Void> reject(@RequestParam("taskId") String taskId,
                          @RequestParam("comment") String comment) {
        return completeTask(taskId, false, comment);
    }

    /**
     * 完成任务：同时写入 approvalResult 与 approved 两个变量，兼容两种网关
     */
    private R<Void> completeTask(String taskId, boolean approved, String comment) {
        try {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                return R.fail("审批任务不存在");
            }

            String processInstanceId = task.getProcessInstanceId();
            var processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            Map<String, Object> variables = new java.util.HashMap<>();
            variables.put("approvalResult", approved ? "APPROVED" : "REJECTED");
            variables.put("approved", approved);
            if (comment != null) {
                variables.put("approvalComment", comment);
            }
            taskService.complete(taskId, variables);

            // 判断是否为最终审批节点（无后续任务则流程结束）
            long remainingTasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .count();

            if (remainingTasks == 0 && processInstance != null) {
                String businessKey = processInstance.getBusinessKey();
                if (businessKey != null) {
                    String[] parts = businessKey.split(":", 2);
                    if (parts.length == 2) {
                        String planType = parts[0];
                        Long planId = Long.parseLong(parts[1]);
                        updatePlanStatus(planType, planId,
                                approved ? "APPROVED" : "REJECTED", processInstanceId);
                    }
                }
            }

            return R.ok();
        } catch (Exception e) {
            return R.fail((approved ? "审批通过" : "审批驳回") + "失败: " + e.getMessage());
        }
    }

    /**
     * 查询审批历史（按业务）
     */
    @GetMapping("/history/{planType}/{planId}")
    public R<List<ApprovalHistoryDTO>> getApprovalHistory(@PathVariable("planType") String planType,
                                                          @PathVariable("planId") Long planId) {
        String businessKey = planType + ":" + planId;
        List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .orderByHistoricTaskInstanceEndTime().asc()
                .list();

        List<ApprovalHistoryDTO> historyList = historicTasks.stream()
                .map(this::convertToHistoryDTO)
                .collect(Collectors.toList());
        return R.ok(historyList);
    }

    /**
     * 获取当前用户待审批任务
     */
    @GetMapping("/pending")
    public R<List<PendingTaskDTO>> getPendingTasks() {
        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(getCurrentUser())
                .list();

        List<PendingTaskDTO> result = tasks.stream()
                .map(task -> {
                    var procInst = runtimeService.createProcessInstanceQuery()
                            .processInstanceId(task.getProcessInstanceId())
                            .singleResult();

                    String planType = "";
                    Long planId = null;
                    if (procInst != null && procInst.getBusinessKey() != null) {
                        String[] parts = procInst.getBusinessKey().split(":", 2);
                        if (parts.length == 2) {
                            planType = parts[0];
                            planId = Long.parseLong(parts[1]);
                        }
                    }

                    PendingTaskDTO dto = new PendingTaskDTO();
                    dto.setTaskId(task.getId());
                    dto.setTaskName(task.getName());
                    dto.setAssignee(task.getAssignee());
                    dto.setCreateTime(task.getCreateTime());
                    dto.setPlanType(planType);
                    dto.setPlanId(planId);
                    return dto;
                })
                .collect(Collectors.toList());

        return R.ok(result);
    }

    /**
     * 获取当前用户已办任务（已完成的历史任务）
     */
    @GetMapping("/done")
    public R<List<DoneTaskDTO>> getDoneTasks() {
        List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(getCurrentUser())
                .finished()
                .orderByHistoricTaskInstanceEndTime().desc()
                .list();

        List<DoneTaskDTO> result = historicTasks.stream()
                .map(task -> {
                    String planType = "";
                    Long planId = null;
                    HistoricProcessInstance procInst = historyService.createHistoricProcessInstanceQuery()
                            .processInstanceId(task.getProcessInstanceId())
                            .singleResult();
                    if (procInst != null && procInst.getBusinessKey() != null) {
                        String[] parts = procInst.getBusinessKey().split(":", 2);
                        if (parts.length == 2) {
                            planType = parts[0];
                            planId = Long.parseLong(parts[1]);
                        }
                    }

                    DoneTaskDTO dto = new DoneTaskDTO();
                    dto.setTaskId(task.getId());
                    dto.setTaskName(task.getName());
                    dto.setAssignee(task.getAssignee());
                    dto.setStartTime(task.getStartTime());
                    dto.setEndTime(task.getEndTime());
                    dto.setPlanType(planType);
                    dto.setPlanId(planId);
                    return dto;
                })
                .collect(Collectors.toList());

        return R.ok(result);
    }

    /**
     * 获取当前用户发起的流程实例（含运行中与已结束）
     */
    @GetMapping("/my")
    public R<List<MyProcessDTO>> getMyProcesses() {
        List<HistoricProcessInstance> instances = historyService.createHistoricProcessInstanceQuery()
                .startedBy(getCurrentUser())
                .orderByProcessInstanceStartTime().desc()
                .list();

        List<MyProcessDTO> result = instances.stream().map(inst -> {
            String planType = "";
            Long planId = null;
            if (inst.getBusinessKey() != null) {
                String[] parts = inst.getBusinessKey().split(":", 2);
                if (parts.length == 2) {
                    planType = parts[0];
                    planId = Long.parseLong(parts[1]);
                }
            }

            MyProcessDTO dto = new MyProcessDTO();
            dto.setProcessInstanceId(inst.getId());
            dto.setProcessDefinitionKey(inst.getProcessDefinitionKey());
            dto.setBusinessKey(inst.getBusinessKey());
            dto.setPlanType(planType);
            dto.setPlanId(planId);
            dto.setStatus(inst.getEndTime() == null ? "RUNNING" : "COMPLETED");
            dto.setStartTime(inst.getStartTime());
            dto.setEndTime(inst.getEndTime());
            return dto;
        }).collect(Collectors.toList());

        return R.ok(result);
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

    /**
     * 获取计划实体（通用提交路径：material/labor/subcontract）
     */
    private Object getPlanEntity(String planType, Long planId) {
        return switch (planType) {
            case "material" -> materialService.getById(planId);
            case "labor" -> laborService.getById(planId);
            case "subcontract" -> subcontractService.getById(planId);
            default -> null;
        };
    }

    /**
     * 更新计划状态与流程实例ID；laborChange 走变更记录并回写原劳动力计划
     */
    private void updatePlanStatus(String planType, Long planId, String approvalStatus, String processInstanceId) {
        switch (planType) {
            case "material" -> {
                ResourcePlanMaterial plan = materialService.getById(planId);
                if (plan != null) {
                    plan.setApprovalStatus(approvalStatus);
                    plan.setProcessInstanceId(processInstanceId);
                    materialService.updateById(plan);
                }
            }
            case "labor" -> {
                ResourcePlanLabor plan = laborService.getById(planId);
                if (plan != null) {
                    plan.setApprovalStatus(approvalStatus);
                    plan.setProcessInstanceId(processInstanceId);
                    laborService.updateById(plan);
                }
            }
            case "subcontract" -> {
                ResourcePlanSubcontract plan = subcontractService.getById(planId);
                if (plan != null) {
                    plan.setApprovalStatus(approvalStatus);
                    plan.setProcessInstanceId(processInstanceId);
                    subcontractService.updateById(plan);
                }
            }
            case "laborChange" -> {
                LaborPlanChange change = laborChangeService.getById(planId);
                if (change != null) {
                    change.setApprovalStatus(approvalStatus);
                    change.setProcessInstanceId(processInstanceId);
                    if ("APPROVED".equals(approvalStatus)) {
                        // 最终通过：把变更值回写到原劳动力计划
                        ResourcePlanLabor laborPlan = laborService.getById(change.getLaborPlanId());
                        if (laborPlan != null) {
                            if (change.getNewPlanStartDate() != null) {
                                laborPlan.setPlanStartDate(change.getNewPlanStartDate());
                            }
                            if (change.getNewPlanEndDate() != null) {
                                laborPlan.setPlanEndDate(change.getNewPlanEndDate());
                            }
                            if (change.getNewPlanQuantity() != null) {
                                laborPlan.setPlanQuantity(change.getNewPlanQuantity());
                            }
                            laborService.updateById(laborPlan);
                        }
                    }
                    laborChangeService.updateById(change);
                }
            }
        }
    }

    private ApprovalHistoryDTO convertToHistoryDTO(HistoricTaskInstance task) {
        ApprovalHistoryDTO dto = new ApprovalHistoryDTO();
        dto.setTaskId(task.getId());
        dto.setTaskName(task.getName());
        dto.setAssignee(task.getAssignee());
        dto.setStartTime(task.getStartTime());
        dto.setEndTime(task.getEndTime());
        return dto;
    }

    // ===================== DTO =====================

    public static class ApprovalHistoryDTO {
        private String taskId;
        private String taskName;
        private String assignee;
        private Date startTime;
        private Date endTime;

        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getTaskName() { return taskName; }
        public void setTaskName(String taskName) { this.taskName = taskName; }
        public String getAssignee() { return assignee; }
        public void setAssignee(String assignee) { this.assignee = assignee; }
        public Date getStartTime() { return startTime; }
        public void setStartTime(Date startTime) { this.startTime = startTime; }
        public Date getEndTime() { return endTime; }
        public void setEndTime(Date endTime) { this.endTime = endTime; }
    }

    public static class PendingTaskDTO {
        private String taskId;
        private String taskName;
        private String assignee;
        private Date createTime;
        private String planType;
        private Long planId;

        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getTaskName() { return taskName; }
        public void setTaskName(String taskName) { this.taskName = taskName; }
        public String getAssignee() { return assignee; }
        public void setAssignee(String assignee) { this.assignee = assignee; }
        public Date getCreateTime() { return createTime; }
        public void setCreateTime(Date createTime) { this.createTime = createTime; }
        public String getPlanType() { return planType; }
        public void setPlanType(String planType) { this.planType = planType; }
        public Long getPlanId() { return planId; }
        public void setPlanId(Long planId) { this.planId = planId; }
    }

    public static class DoneTaskDTO {
        private String taskId;
        private String taskName;
        private String assignee;
        private Date startTime;
        private Date endTime;
        private String planType;
        private Long planId;

        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getTaskName() { return taskName; }
        public void setTaskName(String taskName) { this.taskName = taskName; }
        public String getAssignee() { return assignee; }
        public void setAssignee(String assignee) { this.assignee = assignee; }
        public Date getStartTime() { return startTime; }
        public void setStartTime(Date startTime) { this.startTime = startTime; }
        public Date getEndTime() { return endTime; }
        public void setEndTime(Date endTime) { this.endTime = endTime; }
        public String getPlanType() { return planType; }
        public void setPlanType(String planType) { this.planType = planType; }
        public Long getPlanId() { return planId; }
        public void setPlanId(Long planId) { this.planId = planId; }
    }

    public static class MyProcessDTO {
        private String processInstanceId;
        private String processDefinitionKey;
        private String businessKey;
        private String planType;
        private Long planId;
        private String status;
        private Date startTime;
        private Date endTime;

        public String getProcessInstanceId() { return processInstanceId; }
        public void setProcessInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; }
        public String getProcessDefinitionKey() { return processDefinitionKey; }
        public void setProcessDefinitionKey(String processDefinitionKey) { this.processDefinitionKey = processDefinitionKey; }
        public String getBusinessKey() { return businessKey; }
        public void setBusinessKey(String businessKey) { this.businessKey = businessKey; }
        public String getPlanType() { return planType; }
        public void setPlanType(String planType) { this.planType = planType; }
        public Long getPlanId() { return planId; }
        public void setPlanId(Long planId) { this.planId = planId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Date getStartTime() { return startTime; }
        public void setStartTime(Date startTime) { this.startTime = startTime; }
        public Date getEndTime() { return endTime; }
        public void setEndTime(Date endTime) { this.endTime = endTime; }
    }
}
