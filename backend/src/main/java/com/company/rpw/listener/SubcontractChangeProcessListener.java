package com.company.rpw.listener;

import com.company.rpw.service.ResourcePlanSubcontractChangeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.delegate.event.FlowableProcessEngineEvent;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

/**
 * 分包计划变更流程监听器
 *
 * <p>监听 p_fbchange 流程事件：
 * <ul>
 *   <li>{@code PROCESS_COMPLETED}：审批通过，回调写回分包计划日期字段；</li>
 *   <li>{@code TASK_COMPLETED}（审核节点）：将"审批"节点指派给发起人选择的第二位审批人。</li>
 * </ul>
 * 通过 {@code FlowableListenerConfig} 在引擎就绪后注册。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubcontractChangeProcessListener implements FlowableEventListener {

    private static final String PROCESS_KEY = "p_fbchange";
    /** 审核节点 taskDefinitionKey */
    private static final String TASK_REVIEW = "Activity_196iy0w";
    /** 审批节点 taskDefinitionKey */
    private static final String TASK_APPROVE = "Activity_0mc5im0";

    private final ResourcePlanSubcontractChangeService changeService;
    private final HistoryService historyService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;

    @Override
    public void onEvent(FlowableEvent event) {
        try {
            if (FlowableEngineEventType.PROCESS_COMPLETED.equals(event.getType())) {
                handleProcessCompleted(event);
            } else if (FlowableEngineEventType.TASK_COMPLETED.equals(event.getType())) {
                handleTaskCompleted(event);
            }
        } catch (Exception e) {
            log.error("分包计划变更流程监听处理异常", e);
        }
    }

    /** 审批通过：写回分包计划日期字段 */
    private void handleProcessCompleted(FlowableEvent event) {
        String piId = extractProcessInstanceId(event);
        if (piId == null) {
            return;
        }
        HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(piId)
                .singleResult();
        if (hpi == null || !PROCESS_KEY.equals(hpi.getProcessDefinitionKey())) {
            return;
        }
        String businessKey = hpi.getBusinessKey();
        if (businessKey == null) {
            return;
        }
        changeService.applyApprovedChanges(Long.parseLong(businessKey));
    }

    /** 审核节点完成后：指派审批节点审批人 */
    private void handleTaskCompleted(FlowableEvent event) {
        if (!(event instanceof FlowableEntityEvent)) {
            return;
        }
        Object entity = ((FlowableEntityEvent) event).getEntity();
        if (!(entity instanceof Task)) {
            return;
        }
        Task task = (Task) entity;
        if (!TASK_REVIEW.equals(task.getTaskDefinitionKey())) {
            return;
        }
        String piId = task.getProcessInstanceId();
        HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(piId)
                .singleResult();
        if (hpi == null || !PROCESS_KEY.equals(hpi.getProcessDefinitionKey())) {
            return;
        }
        Task approveTask = taskService.createTaskQuery()
                .processInstanceId(piId)
                .taskDefinitionKey(TASK_APPROVE)
                .singleResult();
        if (approveTask == null) {
            return;
        }
        Object approver2 = runtimeService.getVariable(piId, "fbchangeApprover2");
        String assignee = (approver2 instanceof String && !((String) approver2).isEmpty())
                ? (String) approver2
                : (String) runtimeService.getVariable(piId, "fbchangeApprover1");
        if (assignee != null && !assignee.isEmpty()) {
            try {
                taskService.setAssignee(approveTask.getId(), assignee);
            } catch (Exception ignored) {
            }
        }
    }

    private String extractProcessInstanceId(FlowableEvent event) {
        if (event instanceof FlowableProcessEngineEvent) {
            return ((FlowableProcessEngineEvent) event).getProcessInstanceId();
        }
        if (event instanceof FlowableEntityEvent) {
            Object e = ((FlowableEntityEvent) event).getEntity();
            if (e instanceof org.flowable.engine.runtime.ProcessInstance) {
                return ((org.flowable.engine.runtime.ProcessInstance) e).getProcessInstanceId();
            }
        }
        return null;
    }

    @Override
    public boolean isFailOnException() {
        return false;
    }

    @Override
    public String getOnTransaction() {
        return null;
    }

    @Override
    public boolean isFireOnTransactionLifecycleEvent() {
        return false;
    }
}
