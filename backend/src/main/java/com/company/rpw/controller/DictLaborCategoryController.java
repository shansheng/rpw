package com.company.rpw.controller;

import com.company.rpw.common.R;
import com.company.rpw.entity.DictLaborCategory;
import com.company.rpw.service.DictLaborCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 劳务类别字典控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/dict/labor-category")
@RequiredArgsConstructor
public class DictLaborCategoryController {

    private final DictLaborCategoryService service;

    /**
     * 查询劳务类别字典列表
     * GET /api/v1/dict/labor-category/list
     * @param status 状态筛选（可选，1-启用 0-禁用）
     * @return 劳务类别字典列表
     */
    @GetMapping("/list")
    public R<List<DictLaborCategory>> list(
            @RequestParam(required = false) Integer status) {
        return R.ok(service.listAll(status));
    }

    /**
     * 根据ID查询劳务类别字典
     * GET /api/v1/dict/labor-category/{id}
     */
    @GetMapping("/{id}")
    public R<DictLaborCategory> getById(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    /**
     * 根据编码查询劳务类别字典
     * GET /api/v1/dict/labor-category/code/{code}
     */
    @GetMapping("/code/{code}")
    public R<DictLaborCategory> getByCode(@PathVariable("code") String categoryCode) {
        return R.ok(service.getByCode(categoryCode));
    }

    /**
     * 新增劳务类别字典
     * POST /api/v1/dict/labor-category
     */
    @PostMapping
    public R<Boolean> create(@RequestBody DictLaborCategory entity) {
        boolean result = service.create(entity);
        return result ? R.ok(true) : R.fail(500, "新增失败（编码可能已存在）");
    }

    /**
     * 修改劳务类别字典
     * PUT /api/v1/dict/labor-category/{id}
     */
    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody DictLaborCategory entity) {
        boolean result = service.update(id, entity);
        return result ? R.ok(true) : R.fail(500, "修改失败");
    }

    /**
     * 删除劳务类别字典
     * DELETE /api/v1/dict/labor-category/{id}
     */
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        boolean result = service.delete(id);
        return result ? R.ok(true) : R.fail(500, "删除失败");
    }
}
