package com.company.rpw.controller.bpm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.rpw.common.PageResult;
import com.company.rpw.common.R;
import com.company.rpw.entity.bpm.BpmProcessListener;
import com.company.rpw.service.bpm.BpmProcessListenerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * BPM 流程监听器 Controller
 */
@RestController
@RequestMapping("/api/v1/bpm/process-listener")
@RequiredArgsConstructor
public class BpmProcessListenerController {

    private final BpmProcessListenerService processListenerService;

    /**
     * 分页查询流程监听器
     */
    @GetMapping("/page")
    public R<PageResult<BpmProcessListener>> page(@RequestParam(required = false) Integer pageNo,
                                                  @RequestParam(required = false) Integer pageSize,
                                                  @RequestParam(required = false) String name,
                                                  @RequestParam(required = false) String type) {
        int current = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;

        LambdaQueryWrapper<BpmProcessListener> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(name != null && !name.isEmpty(), BpmProcessListener::getName, name)
               .eq(type != null && !type.isEmpty(), BpmProcessListener::getType, type)
               .orderByDesc(BpmProcessListener::getId);

        Page<BpmProcessListener> page = processListenerService.page(new Page<>(current, size), wrapper);
        List<BpmProcessListener> list = page.getRecords();
        return R.ok(PageResult.of(list, page.getTotal()));
    }

    /**
     * 获取流程监听器详情
     */
    @GetMapping("/get")
    public R<BpmProcessListener> get(@RequestParam("id") Long id) {
        return R.ok(processListenerService.getById(id));
    }

    /**
     * 新增流程监听器
     */
    @PostMapping("/create")
    public R<Long> create(@RequestBody BpmProcessListener body) {
        processListenerService.save(body);
        return R.ok(body.getId());
    }

    /**
     * 更新流程监听器
     */
    @PutMapping("/update")
    public R<Boolean> update(@RequestBody BpmProcessListener body) {
        return R.ok(processListenerService.updateById(body));
    }

    /**
     * 删除流程监听器
     */
    @DeleteMapping("/delete")
    public R<Boolean> delete(@RequestParam("id") Long id) {
        return R.ok(processListenerService.removeById(id));
    }
}
