package com.company.rpw.controller;

import com.company.rpw.common.R;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Flowable 后台干预 Controller
 */
@RestController
@RequestMapping("/api/v1/flowable/admin")
public class FlowableAdminController {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;

    public FlowableAdminController(RuntimeService runtimeService,
                                    TaskService taskService,
                                    HistoryService historyService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.historyService = historyService;
    }

    /**
     * 挂起流程实例
     */
    @PostMapping("/suspend/{processInstanceId}")
    public R<Void> suspendProcessInstance(@PathVariable String processInstanceId) {
        try {
            runtimeService.suspendProcessInstanceById(processInstanceId);
            return R.ok();
        } catch (Exception e) {
            return R.fail("挂起流程实例失败: " + e.getMessage());
        }
    }

    /**
     * 激活流程实例
     */
    @PostMapping("/activate/{processInstanceId}")
    public R<Void> activateProcessInstance(@PathVariable String processInstanceId) {
        try {
            runtimeService.activateProcessInstanceById(processInstanceId);
            return R.ok();
        } catch (Exception e) {
            return R.fail("激活流程实例失败: " + e.getMessage());
        }
    }

    /**
     * 跳转到指定任务节点
     */
    @PostMapping("/jump/{taskId}")
    public R<Void> jumpToTask(@PathVariable String taskId,
                                     @RequestParam String targetActivityId) {
        try {
            // 使用 Flowable 的跳转功能（需要 Flowable 6.x+）
            // 这里简化为：完成任务，并指定下一个节点
            taskService.complete(taskId, java.util.Map.of("jumpTo", targetActivityId));
            return R.ok();
        } catch (Exception e) {
            return R.fail("跳转任务失败: " + e.getMessage());
        }
    }

    /**
     * 转派任务
     */
    @PostMapping("/assign/{taskId}")
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
     * 获取所有流程实例
     */
    @GetMapping("/process-instances")
    public R<List<ProcessInstanceDTO>> listProcessInstances() {
        List<ProcessInstance> list = runtimeService.createProcessInstanceQuery()
                .orderByStartTime().desc()
                .list();
        return R.ok(list.stream()
                .map(pi -> new ProcessInstanceDTO(pi.getId(), pi.getProcessDefinitionKey(),
                        pi.getBusinessKey(), pi.getProcessDefinitionName(),
                        pi.getStartTime(), pi.isSuspended()))
                .collect(Collectors.toList()));
    }

    /**
     * 流程实例 DTO
     */
    public static class ProcessInstanceDTO {
        private String processInstanceId;
        private String processDefinitionKey;
        private String businessKey;
        private String processDefinitionName;
        private Date startTime;
        private boolean suspended;

        public ProcessInstanceDTO() {}

        public ProcessInstanceDTO(String processInstanceId, String processDefinitionKey,
                                  String businessKey, String processDefinitionName,
                                  Date startTime, boolean suspended) {
            this.processInstanceId = processInstanceId;
            this.processDefinitionKey = processDefinitionKey;
            this.businessKey = businessKey;
            this.processDefinitionName = processDefinitionName;
            this.startTime = startTime;
            this.suspended = suspended;
        }

        public String getProcessInstanceId() { return processInstanceId; }
        public void setProcessInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; }
        public String getProcessDefinitionKey() { return processDefinitionKey; }
        public void setProcessDefinitionKey(String processDefinitionKey) { this.processDefinitionKey = processDefinitionKey; }
        public String getBusinessKey() { return businessKey; }
        public void setBusinessKey(String businessKey) { this.businessKey = businessKey; }
        public String getProcessDefinitionName() { return processDefinitionName; }
        public void setProcessDefinitionName(String processDefinitionName) { this.processDefinitionName = processDefinitionName; }
        public Date getStartTime() { return startTime; }
        public void setStartTime(Date startTime) { this.startTime = startTime; }
        public boolean isSuspended() { return suspended; }
        public void setSuspended(boolean suspended) { this.suspended = suspended; }
    }

    /**
     * 查询流程实例历史
     */
    @GetMapping("/history/{processInstanceId}")
    public R<List<HistoryDTO>> getProcessInstanceHistory(@PathVariable String processInstanceId) {
        List<HistoricProcessInstance> historicProcessInstances = historyService
                .createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .list();

        List<HistoryDTO> dtoList = historicProcessInstances.stream()
                .map(this::convertToDTO)
                .collect(java.util.stream.Collectors.toList());

        return R.ok(dtoList);
    }

    /**
     * 转换为 DTO
     */
    private HistoryDTO convertToDTO(HistoricProcessInstance instance) {
        HistoryDTO dto = new HistoryDTO();
        dto.setProcessInstanceId(instance.getId());
        dto.setProcessDefinitionKey(instance.getProcessDefinitionKey());
        dto.setStartTime(instance.getStartTime());
        dto.setEndTime(instance.getEndTime());
        dto.setDeleteReason(instance.getDeleteReason());
        return dto;
    }

    /**
     * 历史 DTO
     */
    public static class HistoryDTO {
        private String processInstanceId;
        private String processDefinitionKey;
        private Date startTime;
        private Date endTime;
        private String deleteReason;

        // Getters and Setters
        public String getProcessInstanceId() { return processInstanceId; }
        public void setProcessInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; }

        public String getProcessDefinitionKey() { return processDefinitionKey; }
        public void setProcessDefinitionKey(String processDefinitionKey) { this.processDefinitionKey = processDefinitionKey; }

        public Date getStartTime() { return startTime; }
        public void setStartTime(Date startTime) { this.startTime = startTime; }

        public Date getEndTime() { return endTime; }
        public void setEndTime(Date endTime) { this.endTime = endTime; }

        public String getDeleteReason() { return deleteReason; }
        public void setDeleteReason(String deleteReason) { this.deleteReason = deleteReason; }
    }
}
