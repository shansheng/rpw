package com.company.rpw.controller;

import com.company.rpw.common.R;
import com.company.rpw.entity.DictWorkType;
import com.company.rpw.service.DictWorkTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工种字典控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/dict/work-type")
@RequiredArgsConstructor
public class DictWorkTypeController {

    private final DictWorkTypeService service;

    /**
     * 查询工种字典列表
     * GET /api/v1/dict/work-type/list
     * @param status 状态筛选（可选，1-启用 0-禁用）
     * @return 工种字典列表
     */
    @GetMapping("/list")
    public R<List<DictWorkType>> list(
            @RequestParam(required = false) Integer status) {
        return R.ok(service.listAll(status));
    }

    /**
     * 根据ID查询工种字典
     * GET /api/v1/dict/work-type/{id}
     */
    @GetMapping("/{id}")
    public R<DictWorkType> getById(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    /**
     * 根据编码查询工种字典
     * GET /api/v1/dict/work-type/code/{code}
     */
    @GetMapping("/code/{code}")
    public R<DictWorkType> getByCode(@PathVariable String code) {
        return R.ok(service.getByCode(code));
    }

    /**
     * 新增工种字典
     * POST /api/v1/dict/work-type
     */
    @PostMapping
    public R<Boolean> create(@RequestBody DictWorkType entity) {
        boolean result = service.create(entity);
        return result ? R.ok(true) : R.fail(500, "新增失败（编码可能已存在）");
    }

    /**
     * 修改工种字典
     * PUT /api/v1/dict/work-type/{id}
     */
    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody DictWorkType entity) {
        boolean result = service.update(id, entity);
        return result ? R.ok(true) : R.fail(500, "修改失败");
    }

    /**
     * 删除工种字典
     * DELETE /api/v1/dict/work-type/{id}
     */
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        boolean result = service.delete(id);
        return result ? R.ok(true) : R.fail(500, "删除失败");
    }
}
