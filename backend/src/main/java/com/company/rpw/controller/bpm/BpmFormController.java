package com.company.rpw.controller.bpm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.rpw.common.PageResult;
import com.company.rpw.common.R;
import com.company.rpw.dto.bpm.BpmFormRespVO;
import com.company.rpw.dto.bpm.BpmFormSaveReqVO;
import com.company.rpw.entity.bpm.BpmForm;
import com.company.rpw.service.bpm.BpmFormService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * BPM 流程表单控制器
 */
@RestController
@RequestMapping("/api/v1/bpm/form")
@RequiredArgsConstructor
public class BpmFormController {

    private final BpmFormService bpmFormService;
    private final ObjectMapper objectMapper;

    /**
     * 分页查询流程表单
     */
    @GetMapping("/page")
    public R<PageResult<BpmFormRespVO>> page(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String name) {
        int no = pageNo == null ? 1 : pageNo;
        int size = pageSize == null ? 10 : pageSize;
        Page<BpmForm> page = new Page<>(no, size);
        LambdaQueryWrapper<BpmForm> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(name != null, BpmForm::getName, name);
        Page<BpmForm> result = bpmFormService.page(page, wrapper);
        List<BpmFormRespVO> records = result.getRecords().stream()
                .map(this::toRespVO)
                .collect(Collectors.toList());
        return R.ok(PageResult.of(records, result.getTotal()));
    }

    /**
     * 根据ID查询流程表单
     */
    @GetMapping("/get")
    public R<BpmFormRespVO> get(@RequestParam Long id) {
        return R.ok(toRespVO(bpmFormService.getById(id)));
    }

    /**
     * 新增流程表单
     */
    @PostMapping("/create")
    public R<Long> create(@RequestBody BpmFormSaveReqVO body) {
        BpmForm entity = toEntity(body);
        bpmFormService.save(entity);
        return R.ok(entity.getId());
    }

    /**
     * 修改流程表单
     */
    @PutMapping("/update")
    public R<Boolean> update(@RequestBody BpmFormSaveReqVO body) {
        BpmForm entity = toEntity(body);
        entity.setId(body.getId());
        return R.ok(bpmFormService.updateById(entity));
    }

    /**
     * 删除流程表单
     */
    @DeleteMapping("/delete")
    public R<Boolean> delete(@RequestParam Long id) {
        return R.ok(bpmFormService.removeById(id));
    }

    /**
     * 简易列表
     */
    @GetMapping("/simple-list")
    public R<List<BpmFormRespVO>> simpleList() {
        List<BpmFormRespVO> list = bpmFormService.list().stream()
                .map(this::toRespVO)
                .collect(Collectors.toList());
        return R.ok(list);
    }

    private BpmForm toEntity(BpmFormSaveReqVO body) {
        BpmForm entity = new BpmForm();
        entity.setName(body.getName());
        entity.setConf(body.getConf());
        entity.setFields(toJson(body.getFields()));
        entity.setStatus(body.getStatus());
        entity.setRemark(body.getRemark());
        return entity;
    }

    private BpmFormRespVO toRespVO(BpmForm entity) {
        if (entity == null) {
            return null;
        }
        BpmFormRespVO vo = new BpmFormRespVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setConf(entity.getConf());
        vo.setFields(toList(entity.getFields()));
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private String toJson(List<String> list) {
        if (list == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> toList(String str) {
        if (str == null || str.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(str, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
