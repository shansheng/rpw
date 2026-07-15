package com.company.rpw.controller.bpm;

import com.company.rpw.bpm.BpmCurrentUser;
import com.company.rpw.bpm.BpmUserHelper;
import com.company.rpw.bpm.vo.BpmTaskVO;
import com.company.rpw.common.PageResult;
import com.company.rpw.common.R;
import com.company.rpw.entity.bpm.BpmComment;
import com.company.rpw.entity.bpm.BpmForm;
import com.company.rpw.entity.bpm.BpmModel;
import com.company.rpw.entity.bpm.BpmOaLeave;
import com.company.rpw.enums.bpm.BpmProcessInstanceStatusEnum;
import com.company.rpw.entity.bpm.BpmOaLeave;
import com.company.rpw.service.bpm.BpmCommentService;
import com.company.rpw.service.bpm.BpmFormService;
import com.company.rpw.service.bpm.BpmModelService;
import com.company.rpw.service.bpm.BpmOaLeaveService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.Task;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程任务 Controller
 * 路径: /api/v1/bpm/task
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/bpm/task")
@RequiredArgsConstructor
public class BpmTaskController {

    private final TaskService taskService;
    private final HistoryService historyService;
    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final BpmModelService modelService;
    private final BpmFormService formService;
    private final BpmUserHelper userHelper;
    private final BpmCommentService commentService;
    private final BpmOaLeaveService leaveService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private void enrichForm(BpmTaskVO v, String processInstanceId) {
        if (processInstanceId == null) return;
        try {
            org.flowable.engine.repository.ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(historyService.createHistoricProcessInstanceQuery()
                            .processInstanceId(processInstanceId).singleResult().getProcessDefinitionId())
                    .singleResult();
            if (pd == null) return;
            BpmModel model = modelService.lambdaQuery().eq(BpmModel::getProcessDefinitionId, pd.getId()).one();
            if (model != null && model.getFormId() != null) {
                BpmForm form = formService.getById(model.getFormId());
                if (form != null) {
                    v.setFormId(form.getId());
                    v.setFormName(form.getName());
                    v.setFormConf(form.getConf());
                    // 流程表单（NORMAL）：回填字段规则与表单值，供前端 form-create 渲染审批详情
                    List<String> fieldNames = parseFieldNames(form.getFields());
                    v.setFormFields(parseFields(form.getFields()));
                    v.setFormVariables(buildLeaveFormVariables(processInstanceId, fieldNames));
                }
            }
            // 业务表单（CUSTOM）信息：供前端流转记录「查看表单」判断是否渲染业务组件
            if (model != null) {
                v.setFormType(model.getFormType());
                v.setFormCustomViewPath(model.getFormCustomViewPath());
            }
        } catch (Exception e) {
            log.warn("任务表单信息补全失败", e);
        }
    }

    /** 解析 bpm_form.fields（JSON 字符串数组）为 List<String> 规则串 */
    private List<String> parseFields(String json) {
        if (!StringUtils.hasText(json)) return new ArrayList<>();
        try {
            return OBJECT_MAPPER.readValue(json,
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /** 从 bpm_form.fields 提取每个规则的 field 名 */
    private List<String> parseFieldNames(String json) {
        List<String> names = new ArrayList<>();
        if (!StringUtils.hasText(json)) return names;
        try {
            List<String> rules = OBJECT_MAPPER.readValue(json,
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, String.class));
            for (String r : rules) {
                try {
                    Map<String, Object> m = OBJECT_MAPPER.readValue(r, Map.class);
                    Object f = m.get("field");
                    if (f != null) names.add(String.valueOf(f));
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return names;
    }

    /**
     * 构建流程表单（NORMAL）的字段值。本 fork 请假业务数据存于 bpm_oa_leave，
     * 按流程实例ID回查业务记录，并将业务列按字段名映射到表单值（对旧/新实例均生效）。
     */
    private Map<String, Object> buildLeaveFormVariables(String processInstanceId, List<String> fieldNames) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (fieldNames == null || fieldNames.isEmpty()) return result;
        try {
            BpmOaLeave leave = leaveService.lambdaQuery()
                    .eq(BpmOaLeave::getProcessInstanceId, processInstanceId).last("LIMIT 1").one();
            if (leave == null) return result;
            Map<String, Object> cand = new LinkedHashMap<>();
            cand.put("leaveType", leave.getType());
            cand.put("type", leave.getType());
            cand.put("startTime", leave.getStartTime() == null ? null : leave.getStartTime().format(DT_FMT));
            cand.put("endTime", leave.getEndTime() == null ? null : leave.getEndTime().format(DT_FMT));
            cand.put("reason", leave.getReason());
            for (String fn : fieldNames) {
                if (cand.containsKey(fn) && cand.get(fn) != null) {
                    result.put(fn, cand.get(fn));
                }
            }
        } catch (Exception e) {
            log.warn("请假业务表单值构建失败", e);
        }
        return result;
    }

    private BpmTaskVO buildTaskVO(Task t) {
        BpmTaskVO v = new BpmTaskVO();
        v.setId(t.getId());
        v.setName(t.getName());
        v.setStatus(1);
        v.setCreateTime(t.getCreateTime() == null ? null : t.getCreateTime().getTime());
        v.setTaskDefinitionKey(t.getTaskDefinitionKey());
        v.setProcessInstanceId(t.getProcessInstanceId());
        v.setAssigneeUser(userHelper.buildInfoByUsername(t.getAssignee()));
        v.setProcessInstance(buildProcessInstanceBrief(t.getProcessInstanceId()));
        enrichForm(v, t.getProcessInstanceId());
        return v;
    }

    private BpmTaskVO buildTaskVO(HistoricTaskInstance t) {
        BpmTaskVO v = new BpmTaskVO();
        v.setId(t.getId());
        v.setName(t.getName());
        v.setStatus(t.getEndTime() != null ? 2 : 1);
        v.setCreateTime(t.getCreateTime() == null ? null : t.getCreateTime().getTime());
        v.setEndTime(t.getEndTime() == null ? null : t.getEndTime().getTime());
        v.setDurationInMillis(t.getDurationInMillis());
        v.setTaskDefinitionKey(t.getTaskDefinitionKey());
        v.setProcessInstanceId(t.getProcessInstanceId());
        v.setAssigneeUser(userHelper.buildInfoByUsername(t.getAssignee()));
        v.setProcessInstance(buildProcessInstanceBrief(t.getProcessInstanceId()));
        enrichForm(v, t.getProcessInstanceId());
        return v;
    }

    private Map<String, Object> buildProcessInstanceBrief(String processInstanceId) {
        if (processInstanceId == null) return null;
        HistoricProcessInstance h = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        Map<String, Object> m = new HashMap<>();
        if (h != null) {
            m.put("id", h.getId());
            m.put("name", h.getName());
            m.put("startUser", userHelper.buildInfoByUsername(h.getStartUserId()));
            m.put("createTime", h.getStartTime() == null ? null : h.getStartTime().getTime());
        }
        return m;
    }

    @GetMapping("/todo-page")
    public R<PageResult<BpmTaskVO>> todoPage(@RequestParam(required = false) Integer pageNo,
                                              @RequestParam(required = false) Integer pageSize,
                                              @RequestParam(required = false) String name,
                                              @RequestParam(required = false) String processDefinitionKey,
                                              @RequestParam(required = false) Integer status) {
        String user = BpmCurrentUser.getUsername();
        var q = taskService.createTaskQuery().taskAssignee(user).active();
        if (StringUtils.hasText(name)) q.taskNameLike("%" + name + "%");
        if (StringUtils.hasText(processDefinitionKey)) q.processDefinitionKey(processDefinitionKey);
        // status 过滤：待办固定 RUNNING，非 1 直接返回空
        if (status != null && status != 1) {
            return R.ok(PageResult.of(new ArrayList<>(), 0L));
        }
        long total = q.count();
        int first = (pageNo == null || pageNo <= 1) ? 0 : (pageNo - 1) * (pageSize == null ? 10 : pageSize);
        List<Task> list = q.orderByTaskCreateTime().desc().listPage(first, pageSize == null ? 10 : pageSize);
        List<BpmTaskVO> voList = list.stream().map(this::buildTaskVO).collect(Collectors.toList());
        return R.ok(PageResult.of(voList, total));
    }

    @GetMapping("/done-page")
    public R<PageResult<BpmTaskVO>> donePage(@RequestParam(required = false) Integer pageNo,
                                              @RequestParam(required = false) Integer pageSize,
                                              @RequestParam(required = false) String name,
                                              @RequestParam(required = false) Integer status) {
        String user = BpmCurrentUser.getUsername();
        var q = historyService.createHistoricTaskInstanceQuery().taskAssignee(user).finished();
        if (StringUtils.hasText(name)) q.taskNameLike("%" + name + "%");
        if (status != null && status != 2) {
            return R.ok(PageResult.of(new ArrayList<>(), 0L));
        }
        long total = q.count();
        int first = (pageNo == null || pageNo <= 1) ? 0 : (pageNo - 1) * (pageSize == null ? 10 : pageSize);
        List<HistoricTaskInstance> list = q.orderByHistoricTaskInstanceEndTime().desc()
                .listPage(first, pageSize == null ? 10 : pageSize);
        List<BpmTaskVO> voList = list.stream().map(this::buildTaskVO).collect(Collectors.toList());
        return R.ok(PageResult.of(voList, total));
    }

    @GetMapping("/manager-page")
    public R<PageResult<BpmTaskVO>> managerPage(@RequestParam(required = false) Integer pageNo,
                                                 @RequestParam(required = false) Integer pageSize,
                                                 @RequestParam(required = false) String name) {
        var q = taskService.createTaskQuery().active();
        if (StringUtils.hasText(name)) q.taskNameLike("%" + name + "%");
        long total = q.count();
        int first = (pageNo == null || pageNo <= 1) ? 0 : (pageNo - 1) * (pageSize == null ? 10 : pageSize);
        List<Task> list = q.orderByTaskCreateTime().desc().listPage(first, pageSize == null ? 10 : pageSize);
        List<BpmTaskVO> voList = list.stream().map(this::buildTaskVO).collect(Collectors.toList());
        return R.ok(PageResult.of(voList, total));
    }

    @PutMapping("/approve")
    public R<Void> approve(@RequestBody Map<String, Object> body) {
        return completeTask(body, true, null);
    }

    @PutMapping("/reject")
    public R<Void> reject(@RequestBody Map<String, Object> body) {
        String reason = (String) body.get("reason");
        return completeTask(body, false, reason);
    }

    /**
     * 完成审批任务（通过 / 拒绝共用）。
     * <p>关键不变量：业务状态完全由 approved 决定——通过=审批通过(FINISH)、拒绝=审批不通过(INVALID)。
     * 两个入口仅 approved 取值不同，从结构上杜绝「拒绝被误标为审批通过」这类逻辑分叉错误。</p>
     */
    private R<Void> completeTask(Map<String, Object> body, boolean approved, String reason) {
        String taskId = String.valueOf(body.get("id"));
        // 预判断：任务可能已被处理（如重复点击、页面未刷新），避免把 Flowable 原始异常抛给用户
        Task t = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (t == null) {
            return R.fail("任务不存在或已处理");
        }
        String piId = t.getProcessInstanceId();
        Map<String, Object> variables = body.get("variables") == null
                ? new HashMap<>() : (Map<String, Object>) body.get("variables");
        // 同时写入 approved 与 approvalResult 两个变量，兼容两种网关写法
        variables.put("approved", approved);
        variables.put("approvalResult", approved ? 1 : 2);
        try {
            taskService.complete(taskId, variables);
            // 拒绝且流程仍在运行：终止流程（驳回）
            if (!approved) {
                try {
                    if (runtimeService.createProcessInstanceQuery().processInstanceId(piId).count() > 0) {
                        runtimeService.deleteProcessInstance(piId, "驳回: " + (reason == null ? "" : reason));
                    }
                } catch (Exception ignored) {
                    // 实例已结束则忽略
                }
            }
            // 回写请假业务状态：仅当流程真正结束（endTime 非空）才更新，避免多实例中途误改
            syncLeaveStatus(piId, approved ? BpmProcessInstanceStatusEnum.FINISH
                    : BpmProcessInstanceStatusEnum.INVALID);
            return R.ok();
        } catch (Exception e) {
            log.error(approved ? "审批通过失败" : "驳回失败", e);
            return R.fail((approved ? "审批通过" : "驳回") + "失败: " + e.getMessage());
        }
    }

    /** 流程实例结束后，回写请假业务表状态（仅当实例真正结束才更新，避免多实例中途误改） */
    private void syncLeaveStatus(String processInstanceId, BpmProcessInstanceStatusEnum target) {
        if (processInstanceId == null) return;
        try {
            HistoricProcessInstance h = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId).singleResult();
            if (h == null || h.getEndTime() == null) return; // 流程尚未真正结束
            BpmOaLeave leave = leaveService.lambdaQuery()
                    .eq(BpmOaLeave::getProcessInstanceId, processInstanceId).one();
            if (leave != null && !target.getCode().equals(leave.getStatus())) {
                leave.setStatus(target.getCode());
                leaveService.updateById(leave);
            }
        } catch (Exception e) {
            log.warn("回写请假状态失败: {}", processInstanceId, e);
        }
    }

    @GetMapping("/list-by-process-instance-id")
    public R<List<BpmTaskVO>> listByProcessInstanceId(@RequestParam String processInstanceId) {
        // 按 id 去重：Flowable 中「进行中」的任务同时存在于运行表与历史表，
        // id 相同会导致 vxe-table 报「主键重复」，故合并为一条（优先运行态，保留实时 assignee/候选信息）。
        Map<String, BpmTaskVO> result = new LinkedHashMap<>();
        taskService.createTaskQuery().processInstanceId(processInstanceId).list()
                .forEach(t -> result.put(t.getId(), buildTaskVO(t)));
        historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstanceId).list()
                .forEach(t -> result.putIfAbsent(t.getId(), buildTaskVO(t)));
        return R.ok(new ArrayList<>(result.values()));
    }

    @GetMapping("/list-by-return")
    public R<List<Map<String, Object>>> listByReturn(@RequestParam String id) {
        Task t = taskService.createTaskQuery().taskId(id).singleResult();
        if (t == null) return R.ok(new ArrayList<>());
        List<HistoricActivityInstance> acts = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(t.getProcessInstanceId()).activityType("userTask").list();
        List<Map<String, Object>> result = acts.stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getActivityId());
            m.put("name", a.getActivityName());
            m.put("taskDefinitionKey", a.getActivityId());
            return m;
        }).collect(Collectors.toList());
        return R.ok(result);
    }

    @PutMapping("/return")
    public R<Void> returnTask(@RequestBody Map<String, Object> body) {
        String taskId = String.valueOf(body.get("id"));
        String target = (String) body.get("targetTaskDefinitionKey");
        Task t = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (t == null) return R.fail("任务不存在或已处理");
        try {
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(t.getProcessInstanceId())
                    .moveExecutionToActivityId(t.getExecutionId(), target)
                    .changeState();
            return R.ok();
        } catch (Exception e) {
            log.error("退回失败", e);
            return R.fail("退回失败: " + e.getMessage());
        }
    }

    @PutMapping("/delegate")
    public R<Void> delegate(@RequestBody Map<String, Object> body) {
        String taskId = String.valueOf(body.get("id"));
        Object delegateUserId = body.get("delegateUserId");
        String username = userHelper.getUsernameById(toLong(delegateUserId));
        if (username == null) return R.fail("委派用户不存在");
        taskService.delegateTask(taskId, username);
        return R.ok();
    }

    @PutMapping("/transfer")
    public R<Void> transfer(@RequestBody Map<String, Object> body) {
        String taskId = String.valueOf(body.get("id"));
        Object assigneeUserId = body.get("assigneeUserId");
        String username = userHelper.getUsernameById(toLong(assigneeUserId));
        if (username == null) return R.fail("转派用户不存在");
        taskService.setAssignee(taskId, username);
        return R.ok();
    }

    @PutMapping("/create-sign")
    public R<Void> createSign(@RequestBody Map<String, Object> body) {
        String taskId = String.valueOf(body.get("id"));
        List<Integer> userIds = (List<Integer>) body.get("userIds");
        Task parent = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (parent == null) return R.fail("任务不存在");
        if (userIds != null) {
            for (Integer uid : userIds) {
                String username = userHelper.getUsernameById(uid.longValue());
                if (username == null) continue;
                Task sub = taskService.newTask();
                sub.setParentTaskId(taskId);
                sub.setName(parent.getName() + "-加签");
                sub.setAssignee(username);
                taskService.saveTask(sub);
            }
        }
        return R.ok();
    }

    @DeleteMapping("/delete-sign")
    public R<Void> deleteSign(@RequestBody Map<String, Object> body) {
        String taskId = String.valueOf(body.get("id"));
        List<Task> subs = taskService.getSubTasks(taskId);
        for (Task sub : subs) {
            taskService.deleteTask(sub.getId(), true);
        }
        return R.ok();
    }

    @PutMapping("/copy")
    public R<Void> copy(@RequestBody Map<String, Object> body) {
        String taskId = String.valueOf(body.get("id"));
        String reason = (String) body.get("reason");
        List<Integer> copyUserIds = (List<Integer>) body.get("copyUserIds");
        Task t = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (t != null) {
            BpmComment c = new BpmComment();
            c.setTaskId(taskId);
            c.setProcessInstanceId(t.getProcessInstanceId());
            c.setType(3);
            c.setMessage(reason);
            c.setUserId(toLong(body.get("userId")));
            commentService.save(c);
        }
        return R.ok();
    }

    @GetMapping("/list-by-parent-task-id")
    public R<List<BpmTaskVO>> listByParentTaskId(@RequestParam String parentTaskId) {
        List<Task> subs = taskService.getSubTasks(parentTaskId);
        return R.ok(subs.stream().map(this::buildTaskVO).collect(Collectors.toList()));
    }

    @PutMapping("/withdraw")
    public R<Void> withdraw(@RequestParam String taskId) {
        Task t = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (t == null) return R.fail("任务不存在或已处理");
        runtimeService.deleteProcessInstance(t.getProcessInstanceId(), "撤回");
        return R.ok();
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return null; }
    }
}
