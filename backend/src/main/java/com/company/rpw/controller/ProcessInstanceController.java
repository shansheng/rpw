package com.company.rpw.controller;

import com.company.rpw.common.R;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.Collections;

/**
 * Flowable 流程实例管理 Controller
 */
@RestController
@RequestMapping("/api/v1/flowable/process-instance")
@Slf4j
public class ProcessInstanceController {

    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final RepositoryService repositoryService;
    private final org.flowable.engine.TaskService taskService;

    public ProcessInstanceController(RuntimeService runtimeService, 
                                   HistoryService historyService,
                                   RepositoryService repositoryService,
                                   org.flowable.engine.TaskService taskService) {
        this.runtimeService = runtimeService;
        this.historyService = historyService;
        this.repositoryService = repositoryService;
        this.taskService = taskService;
    }

    /**
     * 启动流程实例
     */
    @PostMapping("/start")
    public R<String> startProcessInstance(@RequestBody StartProcessDTO dto) {
        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                    dto.getProcessDefinitionKey(),
                    dto.getBusinessKey(),
                    dto.getVariables()
            );

            return R.ok(processInstance.getId());
        } catch (Exception e) {
            return R.fail("启动流程实例失败: " + e.getMessage());
        }
    }

    /**
     * 查询所有流程实例列表（含运行中和已完成的）
     * 对应前端 /v1/flowable/process-instance/list
     */
    @GetMapping("/list")
    public R<List<ProcessInstanceDTO>> getProcessInstanceList(
            @RequestParam(value = "processDefinitionKey", required = false) String processDefinitionKey,
            @RequestParam(value = "finished", required = false) Boolean finished) {
        try {
            // 1. 查询运行中的流程实例
            var runtimeQuery = runtimeService.createProcessInstanceQuery();
            if (processDefinitionKey != null && !processDefinitionKey.isEmpty()) {
                runtimeQuery.processDefinitionKey(processDefinitionKey);
            }
            List<ProcessInstance> runningInstances = runtimeQuery.orderByProcessInstanceId().desc().list();

            // 2. 查询已完成的流程实例（如果不需要已完成，跳过）
            List<ProcessInstanceDTO> result = new java.util.ArrayList<>();

            // 添加运行中的实例
            for (ProcessInstance pi : runningInstances) {
                ProcessInstanceDTO dto = new ProcessInstanceDTO();
                dto.setId(pi.getId());
                dto.setProcessDefinitionId(pi.getProcessDefinitionId());
                dto.setProcessDefinitionKey(pi.getProcessDefinitionKey());
                dto.setBusinessKey(pi.getBusinessKey());
                dto.setStartTime(pi.getStartTime());
                dto.setSuspended(pi.isSuspended());
                dto.setFinished(false);
                dto.setStatus(pi.isSuspended() ? "SUSPENDED" : "RUNNING");
                result.add(dto);
            }

            // 如果没有过滤为仅运行中，也查询已完成的
            if (finished == null || finished) {
                var historyQuery = historyService.createHistoricProcessInstanceQuery().finished();
                if (processDefinitionKey != null && !processDefinitionKey.isEmpty()) {
                    historyQuery.processDefinitionKey(processDefinitionKey);
                }
                // 排除已在运行中的实例（通过ID排除）
                java.util.Set<String> runningIds = runningInstances.stream()
                        .map(ProcessInstance::getId)
                        .collect(java.util.stream.Collectors.toSet());

                List<HistoricProcessInstance> finishedInstances = historyQuery
                        .orderByProcessInstanceEndTime().desc().list();

                for (HistoricProcessInstance hpi : finishedInstances) {
                    if (runningIds.contains(hpi.getId())) continue;
                    ProcessInstanceDTO dto = new ProcessInstanceDTO();
                    dto.setId(hpi.getId());
                    dto.setProcessDefinitionKey(hpi.getProcessDefinitionKey());
                    dto.setBusinessKey(hpi.getBusinessKey());
                    dto.setStartTime(hpi.getStartTime());
                    dto.setEndTime(hpi.getEndTime());
                    dto.setFinished(true);
                    dto.setStatus("COMPLETED");
                    result.add(dto);
                }
            }

            return R.ok(result);
        } catch (Exception e) {
            log.error("查询流程实例列表失败", e);
            return R.fail("查询流程实例列表失败: " + e.getMessage());
        }
    }

    /**
     * 查询我发起的流程实例
     * 对应前端 /v1/flowable/process-instance/my
     */
    @GetMapping("/my")
    public R<List<ProcessInstanceDTO>> getMyProcessInstances(
            @RequestParam(value = "initiator", required = false) String initiator) {
        var query = historyService.createHistoricProcessInstanceQuery();
        if (initiator != null && !initiator.isEmpty()) {
            query.startedBy(initiator);
        }
        List<HistoricProcessInstance> instances = query.orderByProcessInstanceStartTime().desc().list();
        List<ProcessInstanceDTO> dtoList = instances.stream()
                .map(this::convertHistoricToDTO)
                .collect(Collectors.toList());
        return R.ok(dtoList);
    }

    /**
     * 查询流程实例详情（包含历史任务和审批意见）
     */
    @GetMapping("/{id}/detail")
    public R<ProcessInstanceDetailVO> getProcessInstanceDetail(@PathVariable String id) {
        try {
            ProcessInstanceDetailVO detail = new ProcessInstanceDetailVO();
            
            // 1. 基本信息
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(id)
                    .singleResult();
            
            if (processInstance != null) {
                detail.setProcessInstanceId(processInstance.getId());
                detail.setProcessDefinitionKey(processInstance.getProcessDefinitionKey());
                detail.setBusinessKey(processInstance.getBusinessKey());
                detail.setSuspended(processInstance.isSuspended());
                detail.setFinished(false);
            } else {
                // 查询历史流程实例
                HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                        .processInstanceId(id)
                        .singleResult();
                if (historicProcessInstance != null) {
                    detail.setProcessInstanceId(historicProcessInstance.getId());
                    detail.setProcessDefinitionKey(historicProcessInstance.getProcessDefinitionKey());
                    detail.setBusinessKey(historicProcessInstance.getBusinessKey());
                    detail.setSuspended(false);
                    detail.setFinished(historicProcessInstance.getEndTime() != null);
                    detail.setStartTime(historicProcessInstance.getStartTime());
                    detail.setEndTime(historicProcessInstance.getEndTime());
                } else {
                    return R.fail("流程实例不存在");
                }
            }
            
            // 2. 所有历史任务（带审批意见）
            List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(id)
                    .orderByHistoricTaskInstanceStartTime().asc()
                    .list();
            
            List<Map<String, Object>> taskList = new java.util.ArrayList<>();
            for (HistoricTaskInstance task : historicTasks) {
                Map<String, Object> taskMap = new java.util.HashMap<>();
                taskMap.put("taskId", task.getId());
                taskMap.put("name", task.getName());
                taskMap.put("assignee", task.getAssignee());
                taskMap.put("startTime", task.getStartTime());
                taskMap.put("endTime", task.getEndTime());
                taskMap.put("processInstanceId", task.getProcessInstanceId());
                
                // 查询审批意见
                List<org.flowable.engine.task.Comment> comments = taskService.getTaskComments(task.getId());
                if (comments != null && !comments.isEmpty()) {
                    taskMap.put("comment", comments.get(0).getFullMessage());
                }
                
                taskList.add(taskMap);
            }
            detail.setHistoricTasks(taskList);
            
            // 3. 流程定义XML（用于前端渲染流程图）
            try {
                if (repositoryService != null) {
                    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                            .processDefinitionKey(detail.getProcessDefinitionKey())
                            .latestVersion()
                            .singleResult();
                    if (processDefinition != null) {
                        InputStream bpmnStream = repositoryService.getResourceAsStream(
                                processDefinition.getDeploymentId(), 
                                processDefinition.getResourceName());
                        if (bpmnStream != null) {
                            detail.setBpmnXml(new String(bpmnStream.readAllBytes(), StandardCharsets.UTF_8));
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("获取流程定义XML失败: {}", e.getMessage());
            }
            
            return R.ok(detail);
        } catch (Exception e) {
            log.error("查询流程实例详情失败", e);
            return R.fail("查询流程实例详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取流程实例的流程图数据（含高亮信息）
     * GET /api/v1/flowable/process-instance/{instanceId}/diagram
     * 返回：bpmnXml, completedActivityIds, activeActivityIds, completedFlowIds
     */
    @GetMapping("/{instanceId}/diagram")
    public R<Map<String, Object>> getProcessDiagram(@PathVariable String instanceId) {
        try {
            // 1. 查找运行中的流程实例
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(instanceId).singleResult();
            boolean isRunning = processInstance != null;
            String processDefinitionKey;

            if (isRunning) {
                processDefinitionKey = processInstance.getProcessDefinitionKey();
            } else {
                // 2. 尝试查询历史流程实例
                HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                        .processInstanceId(instanceId).singleResult();
                if (historicProcessInstance == null) {
                    return R.fail("流程实例不存在");
                }
                processDefinitionKey = historicProcessInstance.getProcessDefinitionKey();
            }

            // 3. 获取 BPMN XML
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey(processDefinitionKey)
                    .latestVersion()
                    .singleResult();

            String bpmnXml = "";
            if (processDefinition != null) {
                InputStream bpmnStream = repositoryService.getResourceAsStream(
                        processDefinition.getDeploymentId(),
                        processDefinition.getResourceName());
                if (bpmnStream != null) {
                    bpmnXml = new String(bpmnStream.readAllBytes(), StandardCharsets.UTF_8);
                }
            }

            // 4. 获取已完成的活动和序列流
            List<HistoricActivityInstance> finishedActivities = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(instanceId)
                    .finished()
                    .list();

            List<String> completedActivityIds = finishedActivities.stream()
                    .filter(a -> !"sequenceFlow".equals(a.getActivityType()))
                    .map(HistoricActivityInstance::getActivityId)
                    .distinct()
                    .collect(Collectors.toList());

            List<String> completedFlowIds = finishedActivities.stream()
                    .filter(a -> "sequenceFlow".equals(a.getActivityType()))
                    .map(HistoricActivityInstance::getActivityId)
                    .distinct()
                    .collect(Collectors.toList());

            // 5. 获取当前活动节点
            List<String> activeActivityIds = isRunning
                    ? runtimeService.getActiveActivityIds(instanceId)
                    : Collections.emptyList();

            // 6. 组装返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("bpmnXml", bpmnXml);
            result.put("completedActivityIds", completedActivityIds);
            result.put("activeActivityIds", activeActivityIds);
            result.put("completedFlowIds", completedFlowIds);

            return R.ok(result);
        } catch (Exception e) {
            log.error("获取流程图数据失败", e);
            return R.fail("获取流程图数据失败: " + e.getMessage());
        }
    }

    /**
     * 查询流程实例
     */
    @GetMapping("/{processInstanceId}")
    public R<ProcessInstanceDTO> getProcessInstance(@PathVariable String processInstanceId) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (processInstance == null) {
            return R.fail("流程实例不存在");
        }

        ProcessInstanceDTO dto = new ProcessInstanceDTO();
        dto.setProcessInstanceId(processInstance.getId());
        dto.setProcessDefinitionKey(processInstance.getProcessDefinitionKey());
        dto.setBusinessKey(processInstance.getBusinessKey());
        dto.setSuspended(processInstance.isSuspended());

        return R.ok(dto);
    }

    /**
     * 终止流程实例
     */
    @DeleteMapping("/{processInstanceId}")
    public R<Void> deleteProcessInstance(@PathVariable String processInstanceId,
                                              @RequestParam(value = "deleteReason", required = false) String deleteReason) {
        try {
            runtimeService.deleteProcessInstance(processInstanceId, deleteReason);
            return R.ok();
        } catch (Exception e) {
            return R.fail("终止流程实例失败: " + e.getMessage());
        }
    }

    private ProcessInstanceDTO convertHistoricToDTO(HistoricProcessInstance hpi) {
        ProcessInstanceDTO dto = new ProcessInstanceDTO();
        dto.setId(hpi.getId());
        dto.setProcessInstanceId(hpi.getId());
        dto.setProcessDefinitionKey(hpi.getProcessDefinitionKey());
        dto.setBusinessKey(hpi.getBusinessKey());
        dto.setStartTime(hpi.getStartTime());
        dto.setEndTime(hpi.getEndTime());
        dto.setStatus(hpi.getEndTime() != null ? "COMPLETED" : "RUNNING");
        dto.setSuspended(false);
        dto.setFinished(hpi.getEndTime() != null);
        return dto;
    }

    /**
     * 启动流程 DTO
     */
    public static class StartProcessDTO {
        private String processDefinitionKey;
        private String businessKey;
        private Map<String, Object> variables;

        public String getProcessDefinitionKey() { return processDefinitionKey; }
        public void setProcessDefinitionKey(String processDefinitionKey) { this.processDefinitionKey = processDefinitionKey; }

        public String getBusinessKey() { return businessKey; }
        public void setBusinessKey(String businessKey) { this.businessKey = businessKey; }

        public Map<String, Object> getVariables() { return variables; }
        public void setVariables(Map<String, Object> variables) { this.variables = variables; }
    }

    /**
     * 流程实例详情 VO
     */
    public static class ProcessInstanceDetailVO {
        private String processInstanceId;
        private String processDefinitionKey;
        private String businessKey;
        private boolean suspended;
        private boolean finished;
        private Date startTime;
        private Date endTime;
        private List<Map<String, Object>> historicTasks;
        private String bpmnXml;

        public String getProcessInstanceId() { return processInstanceId; }
        public void setProcessInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; }

        public String getProcessDefinitionKey() { return processDefinitionKey; }
        public void setProcessDefinitionKey(String processDefinitionKey) { this.processDefinitionKey = processDefinitionKey; }

        public String getBusinessKey() { return businessKey; }
        public void setBusinessKey(String businessKey) { this.businessKey = businessKey; }

        public boolean isSuspended() { return suspended; }
        public void setSuspended(boolean suspended) { this.suspended = suspended; }

        public boolean isFinished() { return finished; }
        public void setFinished(boolean finished) { this.finished = finished; }

        public Date getStartTime() { return startTime; }
        public void setStartTime(Date startTime) { this.startTime = startTime; }

        public Date getEndTime() { return endTime; }
        public void setEndTime(Date endTime) { this.endTime = endTime; }

        public List<Map<String, Object>> getHistoricTasks() { return historicTasks; }
        public void setHistoricTasks(List<Map<String, Object>> historicTasks) { this.historicTasks = historicTasks; }

        public String getBpmnXml() { return bpmnXml; }
        public void setBpmnXml(String bpmnXml) { this.bpmnXml = bpmnXml; }
    }

    /**
     * 流程实例 DTO
     */
    public static class ProcessInstanceDTO {
        private String id;
        private String processInstanceId;
        private String processDefinitionId;
        private String processDefinitionKey;
        private String businessKey;
        private String status;
        private boolean suspended;
        private boolean finished;
        private java.util.Date startTime;
        private java.util.Date endTime;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getProcessInstanceId() { return processInstanceId; }
        public void setProcessInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; }

        public String getProcessDefinitionId() { return processDefinitionId; }
        public void setProcessDefinitionId(String processDefinitionId) { this.processDefinitionId = processDefinitionId; }

        public String getProcessDefinitionKey() { return processDefinitionKey; }
        public void setProcessDefinitionKey(String processDefinitionKey) { this.processDefinitionKey = processDefinitionKey; }

        public String getBusinessKey() { return businessKey; }
        public void setBusinessKey(String businessKey) { this.businessKey = businessKey; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public boolean isSuspended() { return suspended; }
        public void setSuspended(boolean suspended) { this.suspended = suspended; }

        public boolean isFinished() { return finished; }
        public void setFinished(boolean finished) { this.finished = finished; }

        public java.util.Date getStartTime() { return startTime; }
        public void setStartTime(java.util.Date startTime) { this.startTime = startTime; }

        public java.util.Date getEndTime() { return endTime; }
        public void setEndTime(java.util.Date endTime) { this.endTime = endTime; }
    }
}
