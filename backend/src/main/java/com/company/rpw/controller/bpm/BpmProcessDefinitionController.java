package com.company.rpw.controller.bpm;

import com.company.rpw.bpm.BpmUserHelper;
import com.company.rpw.bpm.BpmUserInfo;
import com.company.rpw.common.PageResult;
import com.company.rpw.common.R;
import com.company.rpw.entity.bpm.BpmForm;
import com.company.rpw.entity.bpm.BpmModel;
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

import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程定义 Controller（部署态，直接查询 Flowable）
 * 路径: /api/v1/bpm/process-definition
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/bpm/process-definition")
@RequiredArgsConstructor
public class BpmProcessDefinitionController {

    private final RepositoryService repositoryService;
    private final BpmModelService modelService;
    private final BpmFormService formService;
    private final BpmUserHelper userHelper;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Data
    public static class ProcessDefinitionVO {
        private String id;
        private String key;
        private String name;
        private Integer version;
        private Long deploymentTime;
        private Integer suspensionState;
        private String category;
        private String description;
        private Integer modelType;
        private Long modelId;
        private Integer formType;
        private Long formId;
        private String formName;
        private String formCustomCreatePath;
        private String formCustomViewPath;
        private List<String> formFields;
        private String icon;
        private List<BpmUserInfo> startUsers;
        private String bpmnXml;
    }

    private List<String> parseFields(String json) {
        if (!StringUtils.hasText(json)) return new ArrayList<>();
        try {
            return OBJECT_MAPPER.readValue(json, OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private ProcessDefinitionVO toVO(ProcessDefinition pd, Map<String, BpmModel> modelByPdId) {
        ProcessDefinitionVO v = new ProcessDefinitionVO();
        v.setId(pd.getId());
        v.setKey(pd.getKey());
        v.setName(pd.getName());
        v.setVersion(pd.getVersion());
        org.flowable.engine.repository.Deployment dep = repositoryService.createDeploymentQuery()
                .deploymentId(pd.getDeploymentId()).singleResult();
        v.setDeploymentTime(dep != null && dep.getDeploymentTime() != null ? dep.getDeploymentTime().getTime() : null);
        v.setSuspensionState(pd.isSuspended() ? 2 : 1);
        v.setCategory(pd.getCategory());
        v.setDescription(pd.getDescription());
        BpmModel model = modelByPdId.get(pd.getId());
        if (model != null) {
            v.setModelType(model.getType());
            v.setModelId(model.getId());
            v.setFormType(model.getFormType());
            v.setFormId(model.getFormId());
            v.setFormCustomCreatePath(model.getFormCustomCreatePath());
            v.setFormCustomViewPath(model.getFormCustomViewPath());
            v.setIcon(model.getIcon());
            if (model.getFormId() != null) {
                BpmForm form = formService.getById(model.getFormId());
                if (form != null) {
                    v.setFormName(form.getName());
                    v.setFormFields(parseFields(form.getFields()));
                }
            }
            if (StringUtils.hasText(model.getStartUserIds())) {
                List<Long> ids = Arrays.stream(model.getStartUserIds().split(","))
                        .map(s -> { try { return Long.parseLong(s.trim()); } catch (Exception e) { return null; } })
                        .filter(Objects::nonNull).collect(Collectors.toList());
                v.setStartUsers(userHelper.buildInfos(ids));
            }
        } else {
            v.setModelType(1);
        }
        return v;
    }

    @GetMapping("/get")
    public R<ProcessDefinitionVO> get(@RequestParam(required = false) String id,
                                       @RequestParam(required = false) String key) {
        ProcessDefinition pd;
        if (StringUtils.hasText(id)) {
            pd = repositoryService.getProcessDefinition(id);
        } else if (StringUtils.hasText(key)) {
            pd = repositoryService.createProcessDefinitionQuery().processDefinitionKey(key).latestVersion().singleResult();
        } else {
            return R.ok(null);
        }
        if (pd == null) return R.ok(null);
        Map<String, BpmModel> modelMap = modelService.list().stream()
                .filter(m -> StringUtils.hasText(m.getProcessDefinitionId()))
                .collect(Collectors.toMap(BpmModel::getProcessDefinitionId, m -> m, (a, b) -> a));
        ProcessDefinitionVO v = toVO(pd, modelMap);
        BpmModel model = modelMap.get(pd.getId());
        if (model != null) v.setBpmnXml(model.getBpmnXml());
        return R.ok(v);
    }

    @GetMapping("/page")
    public R<PageResult<ProcessDefinitionVO>> page(@RequestParam(required = false) Integer pageNo,
                                                    @RequestParam(required = false) Integer pageSize,
                                                    @RequestParam(required = false) String name,
                                                    @RequestParam(required = false) String key,
                                                    @RequestParam(required = false) String category) {
        var query = repositoryService.createProcessDefinitionQuery();
        if (StringUtils.hasText(name)) query.processDefinitionNameLike("%" + name + "%");
        if (StringUtils.hasText(key)) query.processDefinitionKey(key);
        if (StringUtils.hasText(category)) query.processDefinitionCategory(category);
        query.orderByProcessDefinitionVersion().desc();
        long total = query.count();
        int first = (pageNo == null ? 1 : pageNo) <= 1 ? 0 : (pageNo - 1) * (pageSize == null ? 10 : pageSize);
        int max = pageSize == null ? 10 : pageSize;
        List<ProcessDefinition> list = query.listPage(first, max);
        Map<String, BpmModel> modelMap = modelService.list().stream()
                .filter(m -> StringUtils.hasText(m.getProcessDefinitionId()))
                .collect(Collectors.toMap(BpmModel::getProcessDefinitionId, m -> m, (a, b) -> a));
        List<ProcessDefinitionVO> voList = list.stream().map(pd -> toVO(pd, modelMap)).collect(Collectors.toList());
        return R.ok(PageResult.of(voList, total));
    }

    @GetMapping("/list")
    public R<List<ProcessDefinitionVO>> list(@RequestParam(required = false) String name,
                                              @RequestParam(required = false) String category) {
        var query = repositoryService.createProcessDefinitionQuery();
        if (StringUtils.hasText(name)) query.processDefinitionNameLike("%" + name + "%");
        if (StringUtils.hasText(category)) query.processDefinitionCategory(category);
        query.orderByProcessDefinitionVersion().desc();
        List<ProcessDefinition> list = query.list();
        Map<String, BpmModel> modelMap = modelService.list().stream()
                .filter(m -> StringUtils.hasText(m.getProcessDefinitionId()))
                .collect(Collectors.toMap(BpmModel::getProcessDefinitionId, m -> m, (a, b) -> a));
        return R.ok(list.stream().map(pd -> toVO(pd, modelMap)).collect(Collectors.toList()));
    }

    @GetMapping("/simple-list")
    public R<List<ProcessDefinitionVO>> simpleList() {
        List<ProcessDefinition> list = repositoryService.createProcessDefinitionQuery()
                .orderByProcessDefinitionVersion().desc().list();
        Map<String, BpmModel> modelMap = modelService.list().stream()
                .filter(m -> StringUtils.hasText(m.getProcessDefinitionId()))
                .collect(Collectors.toMap(BpmModel::getProcessDefinitionId, m -> m, (a, b) -> a));
        return R.ok(list.stream().map(pd -> toVO(pd, modelMap)).collect(Collectors.toList()));
    }
}
