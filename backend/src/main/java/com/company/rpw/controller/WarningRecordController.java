package com.company.rpw.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.rpw.common.R;
import com.company.rpw.entity.WarningRecord;
import com.company.rpw.service.WarningRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 预警记录管理控制器
 */
@RestController
@RequestMapping("/api/v1/warning/record")
@RequiredArgsConstructor
public class WarningRecordController {

    private final WarningRecordService warningRecordService;

    @GetMapping("/list")
    public R<IPage<WarningRecord>> list(
            @RequestParam(required = false) String objectType,
            @RequestParam(required = false) String warningLevel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String planName,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        LambdaQueryWrapper<WarningRecord> wrapper = new LambdaQueryWrapper<>();
        if (objectType != null && !objectType.isEmpty()) {
            wrapper.eq(WarningRecord::getObjectType, objectType);
        }
        if (warningLevel != null && !warningLevel.isEmpty()) {
            wrapper.eq(WarningRecord::getWarningLevel, warningLevel);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(WarningRecord::getStatus, status);
        }
        if (planName != null && !planName.isEmpty()) {
            wrapper.like(WarningRecord::getPlanName, planName);
        }
        if (startTime != null) {
            wrapper.ge(WarningRecord::getTriggeredTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(WarningRecord::getTriggeredTime, endTime);
        }
        wrapper.orderByDesc(WarningRecord::getTriggeredTime);

        IPage<WarningRecord> page = new Page<>(pageNum, pageSize);
        return R.ok(warningRecordService.page(page, wrapper));
    }

    @GetMapping("/{id}")
    public R<WarningRecord> getById(@PathVariable Long id) {
        return R.ok(warningRecordService.getById(id));
    }

    @PutMapping("/{id}/handle")
    public R<Boolean> handle(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String handleRemark) {
        WarningRecord entity = warningRecordService.getById(id);
        if (entity == null) {
            return R.fail(404, "记录不存在");
        }
        entity.setStatus(status);
        entity.setHandleTime(LocalDateTime.now());
        if (handleRemark != null) {
            entity.setHandleRemark(handleRemark);
        }
        boolean result = warningRecordService.updateById(entity);
        return result ? R.ok(true) : R.fail(500, "处理失败");
    }

    @PutMapping("/batch-handle")
    public R<Boolean> batchHandle(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) params.get("ids");
        String status = (String) params.get("status");
        String handleRemark = (String) params.get("handleRemark");
        if (ids == null || ids.isEmpty()) {
            return R.fail(400, "请选择要处理的记录");
        }
        for (Long id : ids) {
            WarningRecord entity = warningRecordService.getById(id);
            if (entity != null) {
                entity.setStatus(status);
                entity.setHandleTime(LocalDateTime.now());
                if (handleRemark != null) {
                    entity.setHandleRemark(handleRemark);
                }
                warningRecordService.updateById(entity);
            }
        }
        return R.ok(true);
    }

    @GetMapping("/statistics")
    public R<Map<String, Object>> statistics() {
        LambdaQueryWrapper<WarningRecord> base = new LambdaQueryWrapper<>();

        long totalCount = warningRecordService.count(base.clone());
        long pendingCount = warningRecordService.count(base.clone().eq(WarningRecord::getStatus, "PENDING"));
        long resolvedCount = warningRecordService.count(base.clone().eq(WarningRecord::getStatus, "RESOLVED"));
        long ignoredCount = warningRecordService.count(base.clone().eq(WarningRecord::getStatus, "IGNORED"));
        long redCount = warningRecordService.count(base.clone().eq(WarningRecord::getWarningLevel, "RED"));
        long orangeCount = warningRecordService.count(base.clone().eq(WarningRecord::getWarningLevel, "ORANGE"));
        long yellowCount = warningRecordService.count(base.clone().eq(WarningRecord::getWarningLevel, "YELLOW"));

        LambdaQueryWrapper<WarningRecord> todayWrapper = base.clone();
        todayWrapper.apply("DATE(triggered_time) = CURDATE()");
        long todayTriggeredCount = warningRecordService.count(todayWrapper);

        Map<String, Object> statistics = new LinkedHashMap<>();
        statistics.put("totalCount", totalCount);
        statistics.put("pendingCount", pendingCount);
        statistics.put("resolvedCount", resolvedCount);
        statistics.put("ignoredCount", ignoredCount);
        statistics.put("redCount", redCount);
        statistics.put("orangeCount", orangeCount);
        statistics.put("yellowCount", yellowCount);
        statistics.put("todayTriggeredCount", todayTriggeredCount);
        return R.ok(statistics);
    }
}
