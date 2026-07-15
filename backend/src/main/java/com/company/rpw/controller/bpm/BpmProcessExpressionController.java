package com.company.rpw.controller.bpm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.rpw.common.PageResult;
import com.company.rpw.common.R;
import com.company.rpw.entity.bpm.BpmProcessExpression;
import com.company.rpw.service.bpm.BpmProcessExpressionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * BPM 流程表达式控制器
 */
@RestController
@RequestMapping("/api/v1/bpm/process-expression")
@RequiredArgsConstructor
public class BpmProcessExpressionController {

    private final BpmProcessExpressionService service;

    /**
     * 分页查询流程表达式
     */
    @GetMapping("/page")
    public R<PageResult<BpmProcessExpression>> page(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String createTime) {
        int current = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;
        Page<BpmProcessExpression> page = new Page<>(current, size);
        LambdaQueryWrapper<BpmProcessExpression> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(name != null, BpmProcessExpression::getName, name);
        wrapper.eq(status != null, BpmProcessExpression::getStatus, status);
        Page<BpmProcessExpression> result = service.page(page, wrapper);
        return R.ok(PageResult.of(result.getRecords(), result.getTotal()));
    }

    /**
     * 根据ID查询流程表达式
     */
    @GetMapping("/get")
    public R<BpmProcessExpression> get(@RequestParam Long id) {
        return R.ok(service.getById(id));
    }

    /**
     * 新增流程表达式
     */
    @PostMapping("/create")
    public R<Long> create(@RequestBody BpmProcessExpression body) {
        service.save(body);
        return R.ok(body.getId());
    }

    /**
     * 修改流程表达式
     */
    @PutMapping("/update")
    public R<Boolean> update(@RequestBody BpmProcessExpression body) {
        return R.ok(service.updateById(body));
    }

    /**
     * 删除流程表达式
     */
    @DeleteMapping("/delete")
    public R<Boolean> delete(@RequestParam Long id) {
        return R.ok(service.removeById(id));
    }
}
