package com.company.rpw.controller;

import com.company.rpw.common.R;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Flowable 流程定义管理 Controller
 */
@RestController
@RequestMapping("/api/v1/flowable/process-definition")
public class ProcessDefinitionController {

    private final RepositoryService repositoryService;

    public ProcessDefinitionController(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    /**
     * 部署流程定义（文件上传）
     */
    @PostMapping("/deploy")
    public R<String> deployProcessDefinition(@RequestParam("file") MultipartFile file) {
        try {
            Deployment deployment = repositoryService.createDeployment()
                    .addInputStream(file.getOriginalFilename(), file.getInputStream())
                    .deploy();

            return R.ok(deployment.getId());
        } catch (IOException e) {
            return R.fail("部署失败: " + e.getMessage());
        }
    }

    /**
     * 部署流程定义（XML 文本方式）
     */
    @PostMapping("/deploy/xml")
    public R<String> deployByXml(@RequestBody XmlDeployDTO dto) {
        try {
            String xml = dto.getXml();
            String name = dto.getName() != null ? dto.getName() : "process.bpmn20.xml";
            byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);

            Deployment deployment = repositoryService.createDeployment()
                    .addInputStream(name, new java.io.ByteArrayInputStream(bytes))
                    .name(dto.getDeploymentName() != null ? dto.getDeploymentName() : "XML部署")
                    .deploy();

            return R.ok(deployment.getId());
        } catch (Exception e) {
            return R.fail("部署失败: " + e.getMessage());
        }
    }

    /**
     * 查询流程定义列表
     */
    @GetMapping("/list")
    public R<List<ProcessDefinitionDTO>> listProcessDefinitions(
            @RequestParam(value = "key", required = false) String key) {

        var query = repositoryService.createProcessDefinitionQuery();
        if (key != null) {
            query.processDefinitionKey(key);
        }

        List<ProcessDefinition> processDefinitions = query.latestVersion()
                .list();

        List<ProcessDefinitionDTO> dtoList = processDefinitions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return R.ok(dtoList);
    }

    /**
     * 删除流程定义（级联删除流程实例）
     */
    @DeleteMapping("/{deploymentId}")
    public R<Void> deleteProcessDefinition(@PathVariable String deploymentId,
                                              @RequestParam(value = "cascade", defaultValue = "false") boolean cascade) {
        try {
            repositoryService.deleteDeployment(deploymentId, cascade);
            return R.ok();
        } catch (Exception e) {
            return R.fail("删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取流程定义 BPMN XML
     */
    @GetMapping("/{processDefinitionId}/xml")
    public R<String> getProcessDefinitionXml(@PathVariable String processDefinitionId) {
        try {
            ProcessDefinition processDefinition = repositoryService.getProcessDefinition(processDefinitionId);
            if (processDefinition == null) {
                return R.fail("流程定义不存在");
            }
            InputStream is = repositoryService.getResourceAsStream(
                    processDefinition.getDeploymentId(),
                    processDefinition.getResourceName()
            );
            if (is == null) {
                return R.fail("流程资源不存在");
            }
            String xml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return R.ok(xml);
        } catch (Exception e) {
            return R.fail("获取XML失败: " + e.getMessage());
        }
    }

    /**
     * 转换为 DTO
     */
    private ProcessDefinitionDTO convertToDTO(ProcessDefinition processDefinition) {
        ProcessDefinitionDTO dto = new ProcessDefinitionDTO();
        dto.setId(processDefinition.getId());
        dto.setKey(processDefinition.getKey());
        dto.setName(processDefinition.getName());
        dto.setVersion(processDefinition.getVersion());
        dto.setDeploymentId(processDefinition.getDeploymentId());
        return dto;
    }

    /**
     * 流程定义 DTO
     */
    public static class ProcessDefinitionDTO {
        private String id;
        private String key;
        private String name;
        private Integer version;
        private String deploymentId;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Integer getVersion() { return version; }
        public void setVersion(Integer version) { this.version = version; }

        public String getDeploymentId() { return deploymentId; }
        public void setDeploymentId(String deploymentId) { this.deploymentId = deploymentId; }
    }

    /**
     * XML 部署 DTO
     */
    public static class XmlDeployDTO {
        private String xml;
        private String name;
        private String deploymentName;

        public String getXml() { return xml; }
        public void setXml(String xml) { this.xml = xml; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDeploymentName() { return deploymentName; }
        public void setDeploymentName(String deploymentName) { this.deploymentName = deploymentName; }
    }
}
