package com.company.rpw.controller;

import com.company.rpw.common.R;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Flowable 任务管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/flowable/task")
public class TaskController {

    private final TaskService taskService;
    private final HistoryService historyService;
    private final RuntimeService runtimeService;
    
    // 用于查询业务信息
    private org.springframework.context.ApplicationContext applicationContext;

    public TaskController(TaskService taskService, 
                         HistoryService historyService, 
                         RuntimeService runtimeService,
                         org.springframework.context.ApplicationContext applicationContext) {
        this.taskService = taskService;
        this.historyService = historyService;
        this.runtimeService = runtimeService;
        this.applicationContext = applicationContext;
    }

    /**
     * 查询待办任务（当前用户的待办 = assignee 或 candidateUser）
     * 对应前端 /v1/flowable/task/todo
     */
    @GetMapping("/todo")
    public R<List<TaskDTO>> getTodoTasks(@RequestParam(value = "userId", required = false) String userId) {
        var query = taskService.createTaskQuery();

        if (userId != null && !userId.isEmpty()) {
            // 查询已分配给该用户的任务 + 候选用户包含该用户的任务
            List<Task> assignedTasks = query.taskAssignee(userId).list();
            List<Task> candidateTasks = taskService.createTaskQuery()
                    .taskCandidateUser(userId)
                    .list();

            // 合并去重
            Set<String> taskIds = new HashSet<>();
            List<TaskDTO> result = new ArrayList<>();
            for (Task t : assignedTasks) {
                if (taskIds.add(t.getId())) result.add(convertToDTO(t));
            }
            for (Task t : candidateTasks) {
                if (taskIds.add(t.getId())) result.add(convertToDTO(t));
            }
            result.sort((a, b) -> {
                if (b.createTime == null) return -1;
                if (a.createTime == null) return 1;
                return b.createTime.compareTo(a.createTime);
            });
            return R.ok(result);
        } else {
            // 无 userId 时返回所有待办
            List<Task> tasks = query.orderByTaskCreateTime().desc().list();
            return R.ok(tasks.stream().map(this::convertToDTO).collect(Collectors.toList()));
        }
    }

    /**
     * 查询任务列表（通用，支持 assignee/candidateUser/candidateGroup 过滤）
     */
    @GetMapping("/list")
    public R<List<TaskDTO>> getTaskList(
            @RequestParam(value = "assignee", required = false) String assignee,
            @RequestParam(value = "candidateUser", required = false) String candidateUser,
            @RequestParam(value = "candidateGroup", required = false) String candidateGroup) {

        var query = taskService.createTaskQuery();

        if (assignee != null) {
            query.taskAssignee(assignee);
        }
        if (candidateUser != null) {
            query.taskCandidateUser(candidateUser);
        }
        if (candidateGroup != null) {
            query.taskCandidateGroup(candidateGroup);
        }

        List<Task> tasks = query.orderByTaskCreateTime().desc().list();

        List<TaskDTO> dtoList = tasks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return R.ok(dtoList);
    }

    /**
     * 查询历史任务
     */
    @GetMapping("/history")
    public R<List<HistoricTaskDTO>> getHistoricTasks(
            @RequestParam(value = "assignee", required = false) String assignee) {

        var query = historyService.createHistoricTaskInstanceQuery();

        if (assignee != null) {
            query.taskAssignee(assignee);
        }

        List<HistoricTaskInstance> historicTasks = query.orderByHistoricTaskInstanceEndTime().desc().list();
        List<HistoricTaskDTO> dtoList = historicTasks.stream()
                .map(this::convertToHistoricDTO)
                .collect(Collectors.toList());

        return R.ok(dtoList);
    }

    /**
     * 认领任务
     */
    @PostMapping("/{taskId}/claim")
    public R<Void> claimTask(@PathVariable String taskId,
                                  @RequestParam(required = false) String userId) {
        try {
            taskService.claim(taskId, userId != null ? userId : "admin");
            return R.ok();
        } catch (Exception e) {
            return R.fail("认领任务失败: " + e.getMessage());
        }
    }

    /**
     * 完成任务
     */
    @PostMapping("/{taskId}/complete")
    public R<Void> completeTask(@PathVariable String taskId,
                                     @RequestBody(required = false) Map<String, Object> variables) {
        try {
            // 处理审批意见
            String comment = variables != null ? (String) variables.remove("comment") : null;
            if (comment != null && !comment.isEmpty()) {
                Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
                if (task != null) {
                    taskService.addComment(taskId, task.getProcessInstanceId(), "审批意见", comment);
                }
            }
            
            if (variables != null && !variables.isEmpty()) {
                taskService.complete(taskId, variables);
            } else {
                taskService.complete(taskId);
            }
            return R.ok();
        } catch (Exception e) {
            return R.fail("完成任务失败: " + e.getMessage());
        }
    }

    /**
     * 查询任务的审批意见历史
     */
    @GetMapping("/{taskId}/approval-history")
    public R<List<Map<String, Object>>> getApprovalHistory(@PathVariable String taskId) {
        try {
            List<org.flowable.task.api.history.HistoricTaskInstance> historicTask = historyService.createHistoricTaskInstanceQuery()
                    .taskId(taskId)
                    .list();
            
            if (historicTask == null || historicTask.isEmpty()) {
                // 如果是待办任务，从taskService获取
                org.flowable.task.api.Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
                if (task != null) {
                    List<org.flowable.task.api.history.HistoricTaskInstance> histTasks = historyService.createHistoricTaskInstanceQuery()
                            .processInstanceId(task.getProcessInstanceId())
                            .orderByHistoricTaskInstanceEndTime().asc()
                            .list();
                    
                    List<Map<String, Object>> comments = new java.util.ArrayList<>();
                    for (org.flowable.task.api.history.HistoricTaskInstance histTask : histTasks) {
                        List<org.flowable.engine.task.Comment> taskComments = taskService.getTaskComments(histTask.getId());
                        for (org.flowable.engine.task.Comment comment : taskComments) {
                            Map<String, Object> commentMap = new java.util.HashMap<>();
                            commentMap.put("taskId", histTask.getId());
                            commentMap.put("taskName", histTask.getName());
                            commentMap.put("userId", comment.getUserId());
                            commentMap.put("userName", comment.getUserId()); // TODO: 从用户表查询用户名
                            commentMap.put("comment", comment.getFullMessage());
                            commentMap.put("time", comment.getTime());
                            comments.add(commentMap);
                        }
                    }
                    return R.ok(comments);
                }
                return R.ok(java.util.Collections.emptyList());
            }
            
            // 查询该任务的评论
            List<org.flowable.engine.task.Comment> comments = taskService.getTaskComments(taskId);
            List<Map<String, Object>> result = new java.util.ArrayList<>();
            
            for (org.flowable.engine.task.Comment comment : comments) {
                Map<String, Object> commentMap = new java.util.HashMap<>();
                commentMap.put("userId", comment.getUserId());
                commentMap.put("userName", comment.getUserId()); // TODO: 从用户表查询用户名
                commentMap.put("comment", comment.getFullMessage());
                commentMap.put("time", comment.getTime());
                result.add(commentMap);
            }
            
            return R.ok(result);
        } catch (Exception e) {
            log.error("查询审批意见失败", e);
            return R.fail("查询审批意见失败: " + e.getMessage());
        }
    }

    /**
     * 查询流程图高亮数据
     * GET /api/v1/flowable/task/{taskId}/diagram
     */
    @GetMapping("/{taskId}/diagram")
    public R<Map<String, Object>> getDiagramData(@PathVariable String taskId) {
        try {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                // Try historic task
                HistoricTaskInstance historicTask = historyService.createHistoricTaskInstanceQuery()
                        .taskId(taskId).singleResult();
                if (historicTask == null) {
                    return R.fail("任务不存在");
                }
                Map<String, Object> result = new HashMap<>();
                result.put("processDefinitionId", historicTask.getProcessDefinitionId());
                result.put("processInstanceId", historicTask.getProcessInstanceId());

                // 已完成节点
                List<HistoricActivityInstance> completedActivities = historyService
                        .createHistoricActivityInstanceQuery()
                        .processInstanceId(historicTask.getProcessInstanceId())
                        .finished()
                        .list();
                List<String> activityIds = completedActivities.stream()
                        .map(HistoricActivityInstance::getActivityId)
                        .distinct()
                        .collect(Collectors.toList());
                result.put("activityIds", activityIds);

                // 当前活跃节点（历史任务无活跃节点）
                result.put("activeActivityIds", Collections.emptyList());

                return R.ok(result);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("processDefinitionId", task.getProcessDefinitionId());
            result.put("processInstanceId", task.getProcessInstanceId());

            // 已完成节点
            List<HistoricActivityInstance> completedActivities = historyService
                    .createHistoricActivityInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .finished()
                    .list();
            List<String> activityIds = completedActivities.stream()
                    .map(HistoricActivityInstance::getActivityId)
                    .distinct()
                    .collect(Collectors.toList());
            result.put("activityIds", activityIds);

            // 当前活跃节点
            List<String> activeActivityIds = runtimeService.getActiveActivityIds(task.getProcessInstanceId());
            result.put("activeActivityIds", activeActivityIds);

            return R.ok(result);
        } catch (Exception e) {
            log.error("获取流程图数据失败", e);
            return R.fail("获取流程图数据失败: " + e.getMessage());
        }
    }

    /**
     * 转派任务
     */
    @PostMapping("/{taskId}/assign")
    public R<Void> assignTask(@PathVariable String taskId,
                                    @RequestParam String userId) {
        try {
            taskService.setAssignee(taskId, userId);
            return R.ok();
        } catch (Exception e) {
            return R.fail("转派任务失败: " + e.getMessage());
        }
    }

    /**
     * 转换为 DTO
     */
    private TaskDTO convertToDTO(Task task) {
        TaskDTO dto = new TaskDTO();
        dto.setTaskId(task.getId());
        dto.setName(task.getName());
        dto.setAssignee(task.getAssignee());
        dto.setCreateTime(task.getCreateTime());
        dto.setProcessInstanceId(task.getProcessInstanceId());
        dto.setProcessDefinitionId(task.getProcessDefinitionId());
        
        try {
            ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId()).singleResult();
            if (pi != null) {
                dto.setBusinessKey(pi.getBusinessKey());
                
                // 解析businessKey，查询业务信息
                Map<String, Object> businessInfo = new java.util.HashMap<>();
                String businessKey = pi.getBusinessKey();
                
                if (businessKey != null && applicationContext != null) {
                    try {
                        if (businessKey.startsWith("LABOR_CHANGE_")) {
                            String changeId = businessKey.replace("LABOR_CHANGE_", "");
                            com.company.rpw.service.LaborPlanChangeService changeService = 
                                    applicationContext.getBean(com.company.rpw.service.LaborPlanChangeService.class);
                            com.company.rpw.entity.LaborPlanChange change = changeService.getById(Long.parseLong(changeId));
                            if (change != null) {
                                businessInfo.put("type", "LABOR_CHANGE");
                                businessInfo.put("changeId", change.getId());
                                businessInfo.put("laborPlanId", change.getLaborPlanId());
                                businessInfo.put("changeType", change.getChangeType());
                                businessInfo.put("approvalStatus", change.getApprovalStatus());
                            }
                        } else if (businessKey.startsWith("LABOR_SUBMIT_")) {
                            String laborId = businessKey.replace("LABOR_SUBMIT_", "");
                            com.company.rpw.service.ResourcePlanLaborService laborService = 
                                    applicationContext.getBean(com.company.rpw.service.ResourcePlanLaborService.class);
                            com.company.rpw.entity.ResourcePlanLabor labor = laborService.getById(Long.parseLong(laborId));
                            if (labor != null) {
                                businessInfo.put("type", "LABOR_SUBMIT");
                                businessInfo.put("laborPlanId", labor.getId());
                                businessInfo.put("status", labor.getStatus());
                                businessInfo.put("approvalStatus", labor.getApprovalStatus());
                            }
                        }
                    } catch (Exception e) {
                        log.warn("解析businessKey失败: {}", e.getMessage());
                    }
                }
                
                dto.setBusinessInfo(businessInfo);
            }
        } catch (Exception ignored) {}
        return dto;
    }

    /**
     * 转换为历史任务 DTO
     */
    private HistoricTaskDTO convertToHistoricDTO(HistoricTaskInstance task) {
        HistoricTaskDTO dto = new HistoricTaskDTO();
        dto.setTaskId(task.getId());
        dto.setName(task.getName());
        dto.setAssignee(task.getAssignee());
        dto.setStartTime(task.getStartTime());
        dto.setEndTime(task.getEndTime());
        dto.setProcessInstanceId(task.getProcessInstanceId());
        return dto;
    }

    /**
     * 任务 DTO
     */
    public static class TaskDTO {
        private String taskId;
        private String name;
        private String assignee;
        private Date createTime;
        private String processInstanceId;
        private String processDefinitionId;
        private String businessKey;
        private Map<String, Object> businessInfo;

        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getAssignee() { return assignee; }
        public void setAssignee(String assignee) { this.assignee = assignee; }

        public Date getCreateTime() { return createTime; }
        public void setCreateTime(Date createTime) { this.createTime = createTime; }

        public String getProcessInstanceId() { return processInstanceId; }
        public void setProcessInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; }

        public String getProcessDefinitionId() { return processDefinitionId; }
        public void setProcessDefinitionId(String processDefinitionId) { this.processDefinitionId = processDefinitionId; }

        public String getBusinessKey() { return businessKey; }
        public void setBusinessKey(String businessKey) { this.businessKey = businessKey; }

        public Map<String, Object> getBusinessInfo() { return businessInfo; }
        public void setBusinessInfo(Map<String, Object> businessInfo) { this.businessInfo = businessInfo; }
    }

    /**
     * 历史任务 DTO
     */
    public static class HistoricTaskDTO {
        private String taskId;
        private String name;
        private String assignee;
        private Date startTime;
        private Date endTime;
        private String processInstanceId;
        private String comment;

        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getAssignee() { return assignee; }
        public void setAssignee(String assignee) { this.assignee = assignee; }

        public Date getStartTime() { return startTime; }
        public void setStartTime(Date startTime) { this.startTime = startTime; }

        public Date getEndTime() { return endTime; }
        public void setEndTime(Date endTime) { this.endTime = endTime; }

        public String getProcessInstanceId() { return processInstanceId; }
        public void setProcessInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; }

        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }
}
