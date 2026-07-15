package com.company.rpw.controller.bpm;

import com.company.rpw.bpm.BpmUserHelper;
import com.company.rpw.bpm.BpmUserInfo;
import com.company.rpw.common.R;
import com.company.rpw.bpm.BpmSimpleModelConvert;
import com.company.rpw.entity.bpm.BpmCategory;
import com.company.rpw.entity.bpm.BpmForm;
import com.company.rpw.entity.bpm.BpmModel;
import com.company.rpw.service.bpm.BpmCategoryService;
import com.company.rpw.service.bpm.BpmFormService;
import com.company.rpw.service.bpm.BpmModelService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程模型 Controller（设计态）
 * 路径: /api/v1/bpm/model
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/bpm/model")
@RequiredArgsConstructor
public class BpmModelController {

    private final BpmModelService modelService;
    private final BpmCategoryService categoryService;
    private final BpmFormService formService;
    private final BpmUserHelper userHelper;
    private final RepositoryService repositoryService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Data
    public static class ModelRespVO {
        private Long id;
        private String key;
        private String name;
        private String icon;
        private String description;
        private String category;
        private String categoryName;
        private Integer type;
        private Integer status;
        private Integer formType;
        private Long formId;
        private String formName;
        private String formCustomCreatePath;
        private String formCustomViewPath;
        private String processDefinitionId;
        private String processDefinitionKey;
        private Integer processDefinitionVersion;
        private Long deploymentTime;
        private String bpmnXml;
        private Object simpleModel;
        private List<Long> startUserIds;
        private List<Long> startDeptIds;
        private List<Long> managerUserIds;
        private Boolean visible;
        private String remark;
        private Long createTime;
        private ProcessDefinitionNested processDefinition;
    }

    @Data
    public static class ProcessDefinitionNested {
        private String id;
        private String key;
        private Integer version;
        private Long deploymentTime;
        private Integer suspensionState;
        private Integer formType;
        private String formCustomCreatePath;
        private String formCustomViewPath;
        private List<String> formFields;
    }

    @Data
    public static class ModelSaveReqVO {
        private Long id;
        private String key;
        private String name;
        private String icon;
        private String category;
        private Integer type;
        private Integer formType;
        private Long formId;
        private String formCustomCreatePath;
        private String formCustomViewPath;
        private String description;
        private String remark;
        private String bpmnXml;
        private Object simpleModel;
        private List<Long> startUserIds;
        private List<Long> startDeptIds;
        private List<Long> managerUserIds;
        private Boolean visible;
    }

    private String joinIds(List<Long> ids) {
        return ids == null ? "" : ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private Long toEpoch(java.time.LocalDateTime t) {
        return t == null ? null : t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private List<Long> parseUserIds(String csv) {
        List<Long> list = new ArrayList<>();
        if (!StringUtils.hasText(csv)) return list;
        for (String s : csv.split(",")) {
            try { list.add(Long.parseLong(s.trim())); } catch (NumberFormatException ignored) {}
        }
        return list;
    }

    /** simpleModel 入参（前端传 JSON 对象）转为存储字符串 */
    private String simpleModelToStr(Object obj) {
        if (obj == null) return null;
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    /** 存储的 simpleModel 字符串解析回对象，便于前端直接作为节点树使用 */
    private Object simpleModelToObj(String str) {
        if (!StringUtils.hasText(str)) return null;
        try {
            return OBJECT_MAPPER.readValue(str, Object.class);
        } catch (Exception e) {
            return str;
        }
    }

    private List<String> parseFields(String json) {
        if (!StringUtils.hasText(json)) return new ArrayList<>();
        try {
            return OBJECT_MAPPER.readValue(json, OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private ModelRespVO toResp(BpmModel m, Map<String, String> catMap) {
        ModelRespVO v = new ModelRespVO();
        v.setId(m.getId());
        v.setKey(m.getKey());
        v.setName(m.getName());
        v.setIcon(m.getIcon());
        v.setDescription(m.getDescription());
        v.setCategory(m.getCategory());
        v.setCategoryName(m.getCategory() != null ? catMap.getOrDefault(m.getCategory(), m.getCategory()) : null);
        v.setType(m.getType());
        v.setStatus(m.getStatus());
        v.setFormType(m.getFormType());
        v.setFormId(m.getFormId());
        v.setFormCustomCreatePath(m.getFormCustomCreatePath());
        v.setFormCustomViewPath(m.getFormCustomViewPath());
        v.setProcessDefinitionId(m.getProcessDefinitionId());
        v.setProcessDefinitionKey(m.getProcessDefinitionKey());
        v.setProcessDefinitionVersion(m.getProcessDefinitionVersion());
        v.setDeploymentTime(toEpoch(m.getDeploymentTime()));
        v.setBpmnXml(m.getBpmnXml());
        v.setSimpleModel(simpleModelToObj(m.getSimpleModel()));
        v.setStartUserIds(parseUserIds(m.getStartUserIds()));
        v.setStartDeptIds(parseUserIds(m.getStartDeptIds()));
        v.setManagerUserIds(parseUserIds(m.getManagerUserIds()));
        v.setVisible(m.getVisible() == null ? Boolean.TRUE : m.getVisible());
        v.setRemark(m.getRemark());
        v.setCreateTime(toEpoch(m.getCreateTime()));
        // 表单名称
        if (m.getFormId() != null) {
            BpmForm form = formService.getById(m.getFormId());
            if (form != null) {
                v.setFormName(form.getName());
            }
        }
        // 已部署则补充嵌套流程定义
        if (StringUtils.hasText(m.getProcessDefinitionId())) {
            try {
                ProcessDefinition pd = repositoryService.getProcessDefinition(m.getProcessDefinitionId());
                ProcessDefinitionNested n = new ProcessDefinitionNested();
                n.setId(pd.getId());
                n.setKey(pd.getKey());
                n.setVersion(pd.getVersion());
                org.flowable.engine.repository.Deployment dep = repositoryService.createDeploymentQuery()
                        .deploymentId(pd.getDeploymentId()).singleResult();
                n.setDeploymentTime(dep != null && dep.getDeploymentTime() != null ? dep.getDeploymentTime().getTime() : null);
                n.setSuspensionState(pd.isSuspended() ? 2 : 1);
                n.setFormType(m.getFormType());
                n.setFormCustomCreatePath(m.getFormCustomCreatePath());
                n.setFormCustomViewPath(m.getFormCustomViewPath());
                if (m.getFormId() != null) {
                    BpmForm form = formService.getById(m.getFormId());
                    if (form != null) n.setFormFields(parseFields(form.getFields()));
                }
                v.setProcessDefinition(n);
            } catch (Exception e) {
                log.warn("读取流程定义失败: {}", m.getProcessDefinitionId(), e);
            }
        }
        return v;
    }

    @GetMapping("/list")
    public R<List<ModelRespVO>> list(@RequestParam(required = false) String name) {
        List<BpmModel> models = modelService.lambdaQuery()
                .like(StringUtils.hasText(name), BpmModel::getName, name)
                .orderByDesc(BpmModel::getSort)
                .orderByDesc(BpmModel::getId)
                .list();
        Map<String, String> catMap = categoryService.list().stream()
                .collect(Collectors.toMap(BpmCategory::getCode, BpmCategory::getName, (a, b) -> a));
        List<ModelRespVO> result = models.stream().map(m -> toResp(m, catMap)).collect(Collectors.toList());
        return R.ok(result);
    }

    @GetMapping("/get")
    public R<ModelRespVO> get(@RequestParam Long id) {
        BpmModel m = modelService.getById(id);
        if (m == null) return R.ok(null);
        Map<String, String> catMap = categoryService.list().stream()
                .collect(Collectors.toMap(BpmCategory::getCode, BpmCategory::getName, (a, b) -> a));
        return R.ok(toResp(m, catMap));
    }

    @PostMapping("/create")
    public R<Long> create(@RequestBody ModelSaveReqVO req) {
        BpmModel m = new BpmModel();
        m.setName(req.getName());
        m.setIcon(req.getIcon());
        m.setCategory(req.getCategory());
        m.setType(req.getType() == null ? 1 : req.getType());
        m.setFormType(req.getFormType());
        m.setFormId(req.getFormId());
        m.setFormCustomCreatePath(req.getFormCustomCreatePath());
        m.setFormCustomViewPath(req.getFormCustomViewPath());
        m.setDescription(req.getDescription());
        m.setRemark(req.getRemark());
        m.setBpmnXml(req.getBpmnXml());
        m.setSimpleModel(simpleModelToStr(req.getSimpleModel()));
        m.setStartUserIds(joinIds(req.getStartUserIds()));
        m.setStartDeptIds(joinIds(req.getStartDeptIds()));
        m.setManagerUserIds(joinIds(req.getManagerUserIds()));
        m.setVisible(req.getVisible() == null ? Boolean.TRUE : req.getVisible());
        m.setStatus(0);
        m.setSort(0);
        if (!StringUtils.hasText(req.getKey())) {
            m.setKey("process_" + System.currentTimeMillis());
        } else {
            m.setKey(req.getKey());
        }
        modelService.save(m);
        return R.ok(m.getId());
    }

    @PutMapping("/update")
    public R<Void> update(@RequestBody ModelSaveReqVO req) {
        BpmModel m = new BpmModel();
        m.setId(req.getId());
        m.setName(req.getName());
        m.setKey(req.getKey());
        m.setIcon(req.getIcon());
        m.setCategory(req.getCategory());
        m.setType(req.getType());
        m.setFormType(req.getFormType());
        m.setFormId(req.getFormId());
        m.setFormCustomCreatePath(req.getFormCustomCreatePath());
        m.setFormCustomViewPath(req.getFormCustomViewPath());
        m.setDescription(req.getDescription());
        m.setRemark(req.getRemark());
        // 关键修复：更新时必须持久化 bpmnXml。此前该方法漏设此字段，导致 BPMN 类型模型在
        // 编辑（update）模式下点“保存”后 bpmn_xml 列始终为空，发布时后端读到空即报“模型 BPMN 为空，无法部署”。
        m.setBpmnXml(req.getBpmnXml());
        m.setSimpleModel(simpleModelToStr(req.getSimpleModel()));
        m.setStartUserIds(joinIds(req.getStartUserIds()));
        m.setStartDeptIds(joinIds(req.getStartDeptIds()));
        m.setManagerUserIds(joinIds(req.getManagerUserIds()));
        m.setVisible(req.getVisible() == null ? Boolean.TRUE : req.getVisible());
        modelService.updateById(m);
        return R.ok();
    }

    @PutMapping("/update-bpmn")
    public R<Void> updateBpmn(@RequestBody ModelSaveReqVO req) {
        modelService.lambdaUpdate()
                .eq(BpmModel::getId, req.getId())
                .set(BpmModel::getBpmnXml, req.getBpmnXml())
                .update();
        return R.ok();
    }

    @PutMapping("/update-state")
    public R<Void> updateState(@RequestBody Map<String, Object> body) {
        Long id = ((Number) body.get("id")).longValue();
        Integer state = ((Number) body.get("state")).intValue();
        modelService.lambdaUpdate().eq(BpmModel::getId, id).set(BpmModel::getStatus, state).update();
        return R.ok();
    }

    @PostMapping("/deploy")
    public R<Void> deploy(@RequestParam Long id) {
        BpmModel m = modelService.getById(id);
        if (m == null) return R.fail("模型不存在");
        // SIMPLE（简易流程）类型：前端不生成 BPMN XML，而是由 simpleModel 节点树描述流程，
        // 发布时将其转换为 BPMN XML 后再部署。
        if (!StringUtils.hasText(m.getBpmnXml()) && m.getType() != null && m.getType() == 20) {
            String generated = BpmSimpleModelConvert.convertBpmnXml(m.getKey(), m.getName(), m.getSimpleModel());
            if (StringUtils.hasText(generated)) {
                m.setBpmnXml(generated);
                modelService.updateById(m);
            }
        }
        if (!StringUtils.hasText(m.getBpmnXml())) {
            if (m.getType() != null && m.getType() == 20) {
                return R.fail("简易流程未配置审批节点，无法部署");
            }
            return R.fail("模型 BPMN 为空，无法部署（请先在「流程设计」中绘制完整流程图）");
        }
        try {
            String resourceName = (m.getName() == null ? "process" : m.getName()) + ".bpmn20.xml";
            Deployment deployment = repositoryService.createDeployment()
                    .addBytes(resourceName, m.getBpmnXml().getBytes(StandardCharsets.UTF_8))
                    .name(m.getName())
                    .deploy();
            ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId()).latestVersion().singleResult();
            if (pd == null) {
                pd = repositoryService.createProcessDefinitionQuery()
                        .processDefinitionKey(m.getKey()).latestVersion().singleResult();
            }
            if (pd == null) return R.fail("部署成功但未找到流程定义");
            m.setProcessDefinitionId(pd.getId());
            m.setProcessDefinitionKey(pd.getKey());
            m.setProcessDefinitionVersion(pd.getVersion());
            m.setDeploymentTime(java.time.LocalDateTime.now());
            m.setStatus(1);
            modelService.updateById(m);
            return R.ok();
        } catch (Exception e) {
            log.error("部署流程模型失败", e);
            return R.fail("部署失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/clean")
    public R<Void> clean(@RequestParam Long id) {
        modelService.lambdaUpdate().eq(BpmModel::getId, id)
                .set(BpmModel::getProcessDefinitionId, null)
                .set(BpmModel::getProcessDefinitionKey, null)
                .set(BpmModel::getProcessDefinitionVersion, null)
                .set(BpmModel::getDeploymentTime, null)
                .set(BpmModel::getStatus, 0)
                .update();
        return R.ok();
    }

    @DeleteMapping("/delete")
    public R<Void> delete(@RequestParam Long id) {
        modelService.removeById(id);
        return R.ok();
    }

    @PutMapping("/update-sort-batch")
    public R<Boolean> updateSortBatch(@RequestParam String ids) {
        String[] arr = ids.split(",");
        for (int i = 0; i < arr.length; i++) {
            try {
                Long mid = Long.parseLong(arr[i].trim());
                modelService.lambdaUpdate().eq(BpmModel::getId, mid).set(BpmModel::getSort, i + 1).update();
            } catch (NumberFormatException ignored) {
            }
        }
        return R.ok(true);
    }
}
