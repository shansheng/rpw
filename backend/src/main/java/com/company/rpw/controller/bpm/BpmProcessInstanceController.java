package com.company.rpw.controller.bpm;

import com.company.rpw.bpm.BpmCurrentUser;
import com.company.rpw.bpm.BpmUserHelper;
import com.company.rpw.bpm.BpmUserInfo;
import com.company.rpw.bpm.vo.BpmTaskVO;
import com.company.rpw.common.PageResult;
import com.company.rpw.common.R;
import com.company.rpw.entity.bpm.BpmCategory;
import com.company.rpw.entity.bpm.BpmForm;
import com.company.rpw.entity.bpm.BpmModel;
import com.company.rpw.entity.bpm.BpmOaLeave;
import com.company.rpw.service.bpm.BpmCategoryService;
import com.company.rpw.service.bpm.BpmFormService;
import com.company.rpw.service.bpm.BpmModelService;
import com.company.rpw.service.bpm.BpmOaLeaveService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.task.api.Task;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.parsers.*;
import org.w3c.dom.*;

/**
 * 流程实例 Controller（运行态）
 * 路径: /api/v1/bpm/process-instance
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/bpm/process-instance")
@RequiredArgsConstructor
public class BpmProcessInstanceController {

    private final HistoryService historyService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final RepositoryService repositoryService;
    private final BpmModelService modelService;
    private final BpmFormService formService;
    private final BpmCategoryService categoryService;
    private final BpmUserHelper userHelper;
    private final BpmOaLeaveService leaveService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Data
    public static class ProcessInstanceVO {
        private String id;
        private String name;
        private String businessKey;
        private String category;
        private String categoryName;
        private Integer status;
        private Integer result;
        private Long startTime;
        private Long endTime;
        private Long createTime;
        private Long durationInMillis;
        private BpmUserInfo startUser;
        private Map<String, Object> processDefinition;
        private String processDefinitionId;
        private String remark;
        private Map<String, Object> formVariables;
        private List<String> fields;
        private List<Map<String, Object>> summary;
        private List<Map<String, Object>> tasks;
        private List<String> activityIds;
        private String bpmnXml;
        // ↓↓↓ 流程图高亮所需的活动节点/连线 id 列表（前端 bpmn-js 着色用）
        private List<String> finishedTaskActivityIds; // 已完成的用户任务活动 id
        private List<String> unfinishedTaskActivityIds; // 当前进行中的任务活动 id（=运行任务定义 key）
        private List<String> finishedSequenceFlowActivityIds; // 已流转的连线 id
        private List<String> rejectedTaskActivityIds; // 被驳回的任务活动 id
    }

    @Data
    public static class ApprovalNodeInfo {
        private String id;
        private String name;
        private Integer nodeType;
        private Integer status;
        private Long startTime;
        private Long endTime;
        private Integer candidateStrategy;
        private List<BpmUserInfo> candidateUsers;
        private List<ApprovalTaskInfo> tasks;
    }

    @Data
    public static class ApprovalTaskInfo {
        private String id;
        private BpmUserInfo assigneeUser;
        private BpmUserInfo ownerUser;
        private String reason;
        private String signPicUrl;
        private Integer status;
        private List<String> attachments;
    }

    @Data
    public static class ApprovalDetailRespVO {
        private ProcessInstanceVO processInstance;
        private Map<String, Object> processDefinition;
        private List<ApprovalNodeInfo> activityNodes;
        private Object formFieldsPermission;
        private Integer status;
        private BpmTaskVO todoTask;
    }

    @Data
    public static class ProcessPrintDataRespVO {
        private Boolean printTemplateEnable;
        private String printTemplateHtml;
        private ProcessInstanceVO processInstance;
        private List<Map<String, Object>> tasks;
    }

    private Long safeParseLong(String s) {
        if (s == null) return null;
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
    }

    private int determineStatus(HistoricProcessInstance hpi) {
        if (hpi.getEndTime() == null) return 1;
        String reason = hpi.getDeleteReason();
        if (reason != null) {
            String r = reason.toLowerCase();
            if (r.contains("取消") || r.contains("cancel")) return 3;
            if (r.contains("驳回") || r.contains("拒绝") || r.contains("reject")) return 4;
        }
        return 2;
    }

    private Map<String, Object> buildProcessDefinitionNested(String processDefinitionId) {
        Map<String, Object> m = new HashMap<>();
        if (!StringUtils.hasText(processDefinitionId)) return m;
        try {
            org.flowable.engine.repository.ProcessDefinition pd = repositoryService.getProcessDefinition(processDefinitionId);
            m.put("id", pd.getId());
            m.put("key", pd.getKey());
            m.put("name", pd.getName());
            BpmModel model = modelService.lambdaQuery().eq(BpmModel::getProcessDefinitionId, processDefinitionId).one();
            // 取 BPMN：优先模型表；模型行缺失或字段为空时，回退 Flowable 已部署资源。
            // 关键：已结束 / 旧版本的流程实例，其 processDefinitionId 可能匹配不上 bpm_model 的最新行
            // （如实例是 p_Leave:2:...、而 bpm_model 存的是 p_Leave:3:...），
            // 但 Flowable 部署资源里一定存有该流程定义的 BPMN，故回退逻辑必须独立于 model 是否为空。
            String bpmnXml = model != null ? model.getBpmnXml() : null;
            if (model != null) {
                m.put("modelType", model.getType());
                m.put("formType", model.getFormType());
                m.put("formCustomCreatePath", model.getFormCustomCreatePath());
                m.put("formCustomViewPath", model.getFormCustomViewPath());
            }
            if (!StringUtils.hasText(bpmnXml)
                    && pd.getDeploymentId() != null
                    && pd.getResourceName() != null) {
                try (InputStream is = repositoryService.getResourceAsStream(
                        pd.getDeploymentId(), pd.getResourceName())) {
                    bpmnXml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                } catch (Exception ex) {
                    log.warn("从 Flowable 部署资源回退 BPMN 失败: {}", processDefinitionId, ex);
                }
            }
            m.put("bpmnXml", bpmnXml);
            if (model != null && model.getFormId() != null) {
                    BpmForm form = formService.getById(model.getFormId());
                    if (form != null) {
                        m.put("formConf", form.getConf());
                        try {
                            m.put("formFields", OBJECT_MAPPER.readValue(form.getFields() == null ? "[]" : form.getFields(),
                                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, String.class)));
                        } catch (Exception e) { m.put("formFields", new ArrayList<>()); }
                    }
                }
            } catch (Exception e) {
            log.warn("构建流程定义嵌套失败: {}", processDefinitionId, e);
        }
        return m;
    }

    /** 从 bpm_form.fields（规则串列表）提取每个规则的 field 名 */
    private List<String> parseFieldNames(List<String> rules) {
        List<String> names = new ArrayList<>();
        if (rules == null) return names;
        for (String r : rules) {
            try {
                Map<String, Object> m = OBJECT_MAPPER.readValue(r, Map.class);
                Object f = m.get("field");
                if (f != null) names.add(String.valueOf(f));
            } catch (Exception ignored) {
            }
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

    private Map<String, Object> buildProcessInstanceBrief(HistoricProcessInstance hpi) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", hpi.getId());
        m.put("name", hpi.getName());
        m.put("startUser", userHelper.buildInfo(safeParseLong(hpi.getStartUserId())));
        m.put("createTime", hpi.getStartTime() == null ? null : hpi.getStartTime().getTime());
        return m;
    }

    private ProcessInstanceVO buildProcessInstanceVO(HistoricProcessInstance hpi, Map<String, String> catMap) {
        ProcessInstanceVO v = new ProcessInstanceVO();
        v.setId(hpi.getId());
        v.setName(hpi.getName());
        v.setProcessDefinitionId(hpi.getProcessDefinitionId());
        v.setBusinessKey(hpi.getBusinessKey());
        v.setStatus(determineStatus(hpi));
        v.setResult(v.getStatus());
        v.setStartTime(hpi.getStartTime() == null ? null : hpi.getStartTime().getTime());
        v.setEndTime(hpi.getEndTime() == null ? null : hpi.getEndTime().getTime());
        v.setDurationInMillis(hpi.getDurationInMillis());
        v.setStartUser(userHelper.buildInfo(safeParseLong(hpi.getStartUserId())));
        v.setRemark(hpi.getDeleteReason());
        try {
            org.flowable.engine.repository.ProcessDefinition pd = repositoryService.getProcessDefinition(hpi.getProcessDefinitionId());
            v.setCategory(pd.getCategory());
            v.setCategoryName(pd.getCategory() != null ? catMap.getOrDefault(pd.getCategory(), pd.getCategory()) : null);
        } catch (Exception ignored) {}
        Map<String, Object> vars = hpi.getProcessVariables();
        v.setFormVariables(vars);
        if (vars != null && !vars.isEmpty()) {
            v.setSummary(vars.entrySet().stream()
                    .map(e -> { Map<String, Object> s = new HashMap<>(); s.put("key", e.getKey()); s.put("value", String.valueOf(e.getValue())); return s; })
                    .collect(Collectors.toList()));
        }
        v.setProcessDefinition(buildProcessDefinitionNested(hpi.getProcessDefinitionId()));
        // 流程表单（NORMAL）值补充：将请假业务数据按表单字段名合并进 formVariables（旧/新实例均生效）
        try {
            Object pdObj = v.getProcessDefinition();
            if (pdObj instanceof Map) {
                Object ffObj = ((Map<?, ?>) pdObj).get("formFields");
                if (ffObj instanceof List) {
                    List<String> fieldNames = parseFieldNames((List<String>) ffObj);
                    Map<String, Object> leaveVars = buildLeaveFormVariables(hpi.getId(), fieldNames);
                    Map<String, Object> merged = new LinkedHashMap<>(vars == null ? Collections.emptyMap() : vars);
                    merged.putAll(leaveVars);
                    v.setFormVariables(merged);
                }
            }
        } catch (Exception e) {
            log.warn("流程表单值补充失败", e);
        }
        List<Task> cur = taskService.createTaskQuery().processInstanceId(hpi.getId()).list();
        v.setTasks(cur.stream().map(t -> {
            Map<String, Object> tm = new HashMap<>();
            tm.put("id", t.getId());
            tm.put("name", t.getName());
            tm.put("assigneeUser", userHelper.buildInfoByUsername(t.getAssignee()));
            return tm;
        }).collect(Collectors.toList()));
        return v;
    }

    /**
     * 计算流程高亮所需的活动节点/连线 id 列表（供前端 bpmn-js 着色）。
     * - finishedTaskActivityIds: 已结束的用户任务活动 id
     * - finishedSequenceFlowActivityIds: 已流转的 sequenceFlow 连线 id
     * - unfinishedTaskActivityIds: 当前进行中的任务活动 id（=运行态任务定义 key）
     * - rejectedTaskActivityIds: 被驳回（deleteReason 含"驳回/拒绝/reject"）的任务活动 id
     */
    private void fillActivityHighlight(ProcessInstanceVO v, String processInstanceId, List<Task> curTasks) {
        List<HistoricActivityInstance> acts = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId).list();
        Set<String> finishedTasks = new LinkedHashSet<>();
        Set<String> finishedFlows = new LinkedHashSet<>();
        for (HistoricActivityInstance a : acts) {
            if (a.getEndTime() == null) {
                continue;
            }
            String type = a.getActivityType();
            if ("sequenceFlow".equals(type)) {
                finishedFlows.add(a.getActivityId());
            } else if ("userTask".equals(type)) {
                finishedTasks.add(a.getActivityId());
            }
        }
        Set<String> unfinished = curTasks.stream()
                .map(Task::getTaskDefinitionKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> rejected = new LinkedHashSet<>();
        List<HistoricTaskInstance> hts = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId).list();
        for (HistoricTaskInstance ht : hts) {
            String dr = ht.getDeleteReason();
            if (dr != null && (dr.contains("驳回") || dr.contains("拒绝")
                    || dr.toLowerCase().contains("reject"))) {
                rejected.add(ht.getTaskDefinitionKey());
            }
        }
        v.setFinishedTaskActivityIds(new ArrayList<>(finishedTasks));
        v.setFinishedSequenceFlowActivityIds(new ArrayList<>(finishedFlows));
        v.setUnfinishedTaskActivityIds(new ArrayList<>(unfinished));
        v.setRejectedTaskActivityIds(new ArrayList<>(rejected));
    }

    private List<ApprovalNodeInfo> buildActivityNodes(String processInstanceId) {
        List<HistoricActivityInstance> acts = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId).orderByHistoricActivityInstanceStartTime().asc().list();
        List<ApprovalNodeInfo> nodes = new ArrayList<>();
        for (HistoricActivityInstance act : acts) {
            if (!"userTask".equals(act.getActivityType())) continue;
            ApprovalNodeInfo n = new ApprovalNodeInfo();
            n.setId(act.getActivityId());
            n.setName(act.getActivityName());
            n.setNodeType(2);
            n.setStatus(act.getEndTime() != null ? 2 : 1);
            n.setStartTime(act.getStartTime() == null ? null : act.getStartTime().getTime());
            n.setEndTime(act.getEndTime() == null ? null : act.getEndTime().getTime());
            n.setCandidateUsers(new ArrayList<>());
        List<ApprovalTaskInfo> tis = new ArrayList<>();
        Set<String> histIds = new HashSet<>();
        List<HistoricTaskInstance> hts = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId).taskDefinitionKey(act.getActivityId())
                .orderByHistoricTaskInstanceStartTime().asc().list();
        for (HistoricTaskInstance ht : hts) {
            histIds.add(ht.getId());
            ApprovalTaskInfo ti = new ApprovalTaskInfo();
            ti.setId(ht.getId());
            ti.setAssigneeUser(userHelper.buildInfoByUsername(ht.getAssignee()));
            ti.setOwnerUser(userHelper.buildInfoByUsername(ht.getOwner()));
            ti.setReason(ht.getDeleteReason());
            ti.setStatus(ht.getEndTime() != null ? 2 : 1);
            tis.add(ti);
        }
        if (act.getEndTime() == null) {
            List<Task> rts = taskService.createTaskQuery().processInstanceId(processInstanceId)
                    .taskDefinitionKey(act.getActivityId()).list();
            for (Task rt : rts) {
                if (histIds.contains(rt.getId())) {
                    continue; // 运行中的任务已包含于历史任务实例，避免重复展示
                }
                ApprovalTaskInfo ti = new ApprovalTaskInfo();
                ti.setId(rt.getId());
                ti.setAssigneeUser(userHelper.buildInfoByUsername(rt.getAssignee()));
                ti.setStatus(1);
                tis.add(ti);
            }
        }
        n.setTasks(tis);
            nodes.add(n);
        }
        return nodes;
    }

    @GetMapping("/my-page")
    public R<PageResult<ProcessInstanceVO>> myPage(@RequestParam(required = false) Integer pageNo,
                                                    @RequestParam(required = false) Integer pageSize,
                                                    @RequestParam(required = false) String name,
                                                    @RequestParam(required = false) String processDefinitionId,
                                                    @RequestParam(required = false) String category,
                                                    @RequestParam(required = false) Integer status) {
        var q = historyService.createHistoricProcessInstanceQuery()
                .startedBy(BpmCurrentUser.getUsername()).includeProcessVariables();
        if (StringUtils.hasText(name)) q.processInstanceNameLike("%" + name + "%");
        if (StringUtils.hasText(processDefinitionId)) q.processDefinitionId(processDefinitionId);
        long total = q.count();
        int first = (pageNo == null || pageNo <= 1) ? 0 : (pageNo - 1) * (pageSize == null ? 10 : pageSize);
        List<HistoricProcessInstance> list = q.orderByProcessInstanceStartTime().desc()
                .listPage(first, pageSize == null ? 10 : pageSize);
        Map<String, String> catMap = categoryService.list().stream()
                .collect(Collectors.toMap(BpmCategory::getCode, BpmCategory::getName, (a, b) -> a));
        List<ProcessInstanceVO> voList = list.stream().map(h -> buildProcessInstanceVO(h, catMap)).collect(Collectors.toList());
        // 可选状态过滤（历史表无直接 status 字段）
        if (status != null) voList = voList.stream().filter(v -> status.equals(v.getStatus())).collect(Collectors.toList());
        return R.ok(PageResult.of(voList, (long) (status != null ? voList.size() : total)));
    }

    @GetMapping("/manager-page")
    public R<PageResult<ProcessInstanceVO>> managerPage(@RequestParam(required = false) Integer pageNo,
                                                         @RequestParam(required = false) Integer pageSize,
                                                         @RequestParam(required = false) String name,
                                                         @RequestParam(required = false) String processDefinitionId,
                                                         @RequestParam(required = false) String category,
                                                         @RequestParam(required = false) String startUserId,
                                                         @RequestParam(required = false) Integer status) {
        var q = historyService.createHistoricProcessInstanceQuery().includeProcessVariables();
        if (StringUtils.hasText(name)) q.processInstanceNameLike("%" + name + "%");
        if (StringUtils.hasText(processDefinitionId)) q.processDefinitionId(processDefinitionId);
        if (StringUtils.hasText(startUserId)) q.startedBy(startUserId);
        long total = q.count();
        int first = (pageNo == null || pageNo <= 1) ? 0 : (pageNo - 1) * (pageSize == null ? 10 : pageSize);
        List<HistoricProcessInstance> list = q.orderByProcessInstanceStartTime().desc()
                .listPage(first, pageSize == null ? 10 : pageSize);
        Map<String, String> catMap = categoryService.list().stream()
                .collect(Collectors.toMap(BpmCategory::getCode, BpmCategory::getName, (a, b) -> a));
        List<ProcessInstanceVO> voList = list.stream().map(h -> buildProcessInstanceVO(h, catMap)).collect(Collectors.toList());
        if (status != null) voList = voList.stream().filter(v -> status.equals(v.getStatus())).collect(Collectors.toList());
        return R.ok(PageResult.of(voList, (long) (status != null ? voList.size() : total)));
    }

    @PostMapping("/create")
    public R<ProcessInstanceVO> create(@RequestBody Map<String, Object> body) {
        String processDefinitionId = (String) body.get("processDefinitionId");
        if (!StringUtils.hasText(processDefinitionId)) return R.fail("流程定义ID不能为空");
        Map<String, Object> variables = body.get("variables") == null
                ? new HashMap<>() : (Map<String, Object>) body.get("variables");
        String user = BpmCurrentUser.getUsername();
        Authentication.setAuthenticatedUserId(user);
        org.flowable.engine.runtime.ProcessInstance pi;
        try {
            pi = runtimeService.startProcessInstanceById(processDefinitionId, variables);
        } finally {
            Authentication.setAuthenticatedUserId(null);
        }
        HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(pi.getId()).includeProcessVariables().singleResult();
        Map<String, String> catMap = categoryService.list().stream()
                .collect(Collectors.toMap(BpmCategory::getCode, BpmCategory::getName, (a, b) -> a));
        return R.ok(buildProcessInstanceVO(hpi, catMap));
    }

    @DeleteMapping("/cancel-by-start-user")
    public R<Boolean> cancelByStartUser(@RequestBody Map<String, Object> body) {
        String id = String.valueOf(body.get("id"));
        String reason = (String) body.get("reason");
        runtimeService.deleteProcessInstance(id, "用户取消: " + (reason == null ? "" : reason));
        return R.ok(true);
    }

    @DeleteMapping("/cancel-by-admin")
    public R<Boolean> cancelByAdmin(@RequestBody Map<String, Object> body) {
        String id = String.valueOf(body.get("id"));
        String reason = (String) body.get("reason");
        runtimeService.deleteProcessInstance(id, "管理员取消: " + (reason == null ? "" : reason));
        return R.ok(true);
    }

    @GetMapping("/get")
    public R<ProcessInstanceVO> get(@RequestParam String id) {
        HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(id).includeProcessVariables().singleResult();
        if (hpi == null) return R.ok(null);
        Map<String, String> catMap = categoryService.list().stream()
                .collect(Collectors.toMap(BpmCategory::getCode, BpmCategory::getName, (a, b) -> a));
        return R.ok(buildProcessInstanceVO(hpi, catMap));
    }

    @GetMapping("/copy/page")
    public R<PageResult<ProcessInstanceVO>> copyPage(@RequestParam(required = false) Integer pageNo,
                                                      @RequestParam(required = false) Integer pageSize) {
        // 抄送数据依赖独立抄送表，当前返回空（功能占位）
        return R.ok(PageResult.of(new ArrayList<>(), 0L));
    }

    @PutMapping("/update")
    public R<Void> update(@RequestBody Map<String, Object> body) {
        // 历史流程实例不可变，预留接口
        return R.ok();
    }

    @GetMapping("/get-approval-detail")
    public R<ApprovalDetailRespVO> getApprovalDetail(
            @RequestParam(required = false) String processInstanceId,
            @RequestParam(required = false) String processDefinitionId,
            @RequestParam(required = false) String activityId,
            @RequestParam(required = false) String processVariablesStr) {
        ApprovalDetailRespVO resp = new ApprovalDetailRespVO();
        if (StringUtils.hasText(processInstanceId)) {
            HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId).includeProcessVariables().singleResult();
            if (hpi == null) return R.fail("流程实例不存在");
            Map<String, String> catMap = categoryService.list().stream()
                    .collect(Collectors.toMap(BpmCategory::getCode, BpmCategory::getName, (a, b) -> a));
            ProcessInstanceVO piVO = buildProcessInstanceVO(hpi, catMap);
            resp.setProcessInstance(piVO);
            resp.setProcessDefinition(piVO.getProcessDefinition());
            resp.setActivityNodes(buildActivityNodes(processInstanceId));
            resp.setFormFieldsPermission(new HashMap<>());
            resp.setStatus(piVO.getStatus());
            // 当前待办任务
            List<Task> cur = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
            if (!cur.isEmpty()) {
                resp.setTodoTask(buildTaskVO(cur.get(0)));
            }
            return R.ok(resp);
        }
        // 预测模式：基于流程定义 + BPMN，供业务表单「创建页」预判审批节点（此时尚无流程实例）
        if (!StringUtils.hasText(processDefinitionId)) {
            resp.setActivityNodes(new ArrayList<>());
            resp.setProcessDefinition(new HashMap<>());
            resp.setFormFieldsPermission(new HashMap<>());
            resp.setStatus(1);
            return R.ok(resp);
        }
        Map<String, Object> pd = buildProcessDefinitionNested(processDefinitionId);
        resp.setProcessDefinition(pd);
        resp.setActivityNodes(buildPredictActivityNodes(pd));
        resp.setFormFieldsPermission(new HashMap<>());
        resp.setStatus(1);
        return R.ok(resp);
    }

    /**
     * 基于 BPMN XML 预测用户任务节点（含 START_USER_SELECT 候选策略），用于创建页审批人选择预判。
     */
    private List<ApprovalNodeInfo> buildPredictActivityNodes(Map<String, Object> pd) {
        List<ApprovalNodeInfo> nodes = new ArrayList<>();
        if (pd == null) return nodes;
        Object xmlObj = pd.get("bpmnXml");
        if (!(xmlObj instanceof String) || !StringUtils.hasText((String) xmlObj)) {
            return nodes;
        }
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(((String) xmlObj).getBytes(StandardCharsets.UTF_8)));
            NodeList tasks = doc.getElementsByTagNameNS("*", "userTask");
            for (int i = 0; i < tasks.getLength(); i++) {
                Element t = (Element) tasks.item(i);
                ApprovalNodeInfo n = new ApprovalNodeInfo();
                n.setId(t.getAttribute("id"));
                n.setName(t.getAttribute("name"));
                n.setNodeType(2);
                n.setStatus(1);
                n.setCandidateUsers(new ArrayList<>());
                Element ext = getChildElementByLocalName(t, "extensionElements");
                if (ext != null) {
                    Element cs = getChildElementByLocalName(ext, "candidateStrategy");
                    if (cs != null && StringUtils.hasText(cs.getTextContent())) {
                        try {
                            n.setCandidateStrategy(Integer.parseInt(cs.getTextContent().trim()));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                nodes.add(n);
            }
        } catch (Exception e) {
            log.warn("预测审批节点失败: {}", e.getMessage());
        }
        return nodes;
    }

    private Element getChildElementByLocalName(Element parent, String localName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node c = children.item(i);
            if (c.getNodeType() == Node.ELEMENT_NODE && localName.equals(c.getLocalName())) {
                return (Element) c;
            }
        }
        return null;
    }

    @GetMapping("/get-next-approval-nodes")
    public R<List<ApprovalNodeInfo>> getNextApprovalNodes(@RequestParam String processInstanceId) {
        return R.ok(buildActivityNodes(processInstanceId));
    }

    @GetMapping("/get-bpmn-model-view")
    public R<ProcessInstanceVO> getBpmnModelView(@RequestParam String id) {
        HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(id).includeProcessVariables().singleResult();
        if (hpi == null) return R.ok(null);
        Map<String, String> catMap = categoryService.list().stream()
                .collect(Collectors.toMap(BpmCategory::getCode, BpmCategory::getName, (a, b) -> a));
        ProcessInstanceVO v = buildProcessInstanceVO(hpi, catMap);
        v.setBpmnXml((String) v.getProcessDefinition().get("bpmnXml"));
        List<Task> cur = taskService.createTaskQuery().processInstanceId(id).list();
        v.setActivityIds(cur.stream().map(Task::getTaskDefinitionKey).collect(Collectors.toList()));
        fillActivityHighlight(v, id, cur);
        return R.ok(v);
    }

    @GetMapping("/get-print-data")
    public R<ProcessPrintDataRespVO> getPrintData(@RequestParam String processInstanceId) {
        HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId).includeProcessVariables().singleResult();
        Map<String, String> catMap = categoryService.list().stream()
                .collect(Collectors.toMap(BpmCategory::getCode, BpmCategory::getName, (a, b) -> a));
        ProcessInstanceVO v = hpi == null ? null : buildProcessInstanceVO(hpi, catMap);
        ProcessPrintDataRespVO r = new ProcessPrintDataRespVO();
        r.setPrintTemplateEnable(false);
        r.setProcessInstance(v);
        List<Task> cur = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
        r.setTasks(cur.stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("signPicUrl", "");
            return m;
        }).collect(Collectors.toList()));
        return R.ok(r);
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
        v.setProcessInstance(buildProcessInstanceBrief(
                historyService.createHistoricProcessInstanceQuery().processInstanceId(t.getProcessInstanceId()).singleResult()));
        return v;
    }
}
