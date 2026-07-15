package com.company.rpw.controller.bpm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.rpw.common.PageResult;
import com.company.rpw.common.R;
import com.company.rpw.entity.bpm.BpmCategory;
import com.company.rpw.service.bpm.BpmCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * BPM 流程分类 Controller
 */
@RestController
@RequestMapping("/api/v1/bpm/category")
@RequiredArgsConstructor
public class BpmCategoryController {

    private final BpmCategoryService service;

    /**
     * 分页查询流程分类
     */
    @GetMapping("/page")
    public R<PageResult<BpmCategory>> page(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String createTime) {
        LambdaQueryWrapper<BpmCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(name != null, BpmCategory::getName, name)
                .eq(code != null, BpmCategory::getCode, code)
                .eq(status != null, BpmCategory::getStatus, status);
        Page<BpmCategory> p = new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize);
        IPage<BpmCategory> r = service.page(p, wrapper);
        return R.ok(PageResult.of(r.getRecords(), r.getTotal()));
    }

    /**
     * 根据ID查询流程分类
     */
    @GetMapping("/get")
    public R<BpmCategory> get(@RequestParam Long id) {
        return R.ok(service.getById(id));
    }

    /**
     * 新增流程分类
     */
    @PostMapping("/create")
    public R<Long> create(@RequestBody BpmCategory body) {
        body.setId(null);
        service.save(body);
        return R.ok(body.getId());
    }

    /**
     * 修改流程分类
     */
    @PutMapping("/update")
    public R<Boolean> update(@RequestBody BpmCategory body) {
        return R.ok(service.updateById(body));
    }

    /**
     * 删除流程分类
     */
    @DeleteMapping("/delete")
    public R<Boolean> delete(@RequestParam Long id) {
        return R.ok(service.removeById(id));
    }

    /**
     * 简化列表
     */
    @GetMapping("/simple-list")
    public R<List<BpmCategory>> simpleList() {
        return R.ok(service.list());
    }

    /**
     * 批量更新排序
     */
    @PutMapping("/update-sort-batch")
    public R<Boolean> updateSortBatch(@RequestParam String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
        for (int i = 0; i < idList.size(); i++) {
            BpmCategory entity = new BpmCategory();
            entity.setId(idList.get(i));
            entity.setSort(i + 1);
            service.updateById(entity);
        }
        return R.ok(true);
    }
}
