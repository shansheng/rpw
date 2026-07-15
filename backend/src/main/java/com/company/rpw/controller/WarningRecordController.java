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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 预警记录管理控制器
 */
@RestController
@RequestMapping("/api/v1/warning/record")
@RequiredArgsConstructor
public class WarningRecordController {

    private final WarningRecordService warningRecordService;

    /**
     * 查询预警记录列表（支持分页和筛选）
     * GET /api/v1/warning/record/list
     * @param resourceType 资源类型（可选）
     * @param projectId 项目ID（可选）
     * @param warningLevel 预警等级（可选）
     * @param status 处理状态（可选）
     * @param startTime 触发开始时间（可选）
     * @param endTime 触发结束时间（可选）
     * @param pageNum 页码（默认1）
     * @param pageSize 每页数量（默认10）
     * @return 预警记录列表
     */
    @GetMapping("/list")
    public R<IPage<WarningRecord>> list(
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String warningLevel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        LambdaQueryWrapper<WarningRecord> wrapper = new LambdaQueryWrapper<>();
        if (resourceType != null && !resourceType.isEmpty()) {
            wrapper.eq(WarningRecord::getResourceType, resourceType);
        }
        if (projectId != null) {
            wrapper.eq(WarningRecord::getProjectId, projectId);
        }
        if (warningLevel != null && !warningLevel.isEmpty()) {
            wrapper.eq(WarningRecord::getWarningLevel, warningLevel);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(WarningRecord::getStatus, status);
        }
        if (startTime != null) {
            wrapper.ge(WarningRecord::getTriggeredTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(WarningRecord::getTriggeredTime, endTime);
        }
        wrapper.orderByDesc(WarningRecord::getTriggeredTime);
        
        IPage<WarningRecord> page = new Page<>(pageNum, pageSize);
        IPage<WarningRecord> result = warningRecordService.page(page, wrapper);
        return R.ok(result);
    }

    /**
     * 根据ID查询预警记录
     * GET /api/v1/warning/record/{id}
     * @param id 预警记录ID
     * @return 预警记录详情
     */
    @GetMapping("/{id}")
    public R<WarningRecord> getById(@PathVariable Long id) {
        WarningRecord entity = warningRecordService.getById(id);
        return R.ok(entity);
    }

    /**
     * 处理预警记录
     * PUT /api/v1/warning/record/{id}/handle
     * @param id 预警记录ID
     * @param status 处理状态（PROCESSING/RESOLVED/IGNORED）
     * @param handleRemark 处理备注（可选）
     * @return 是否成功
     */
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

    /**
     * 批量处理预警记录
     * PUT /api/v1/warning/record/batch-handle
     * @param ids 预警记录ID列表
     * @param status 处理状态（PROCESSING/RESOLVED/IGNORED）
     * @param handleRemark 处理备注（可选）
     * @return 是否成功
     */
    @PutMapping("/batch-handle")
    public R<Boolean> batchHandle(
            @RequestBody Map<String, Object> params) {
        
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

    /**
     * 获取预警统计信息
     * GET /api/v1/warning/record/statistics
     * @param projectId 项目ID（可选）
     * @return 统计信息
     */
    @GetMapping("/statistics")
    public R<Map<String, Object>> statistics(@RequestParam(required = false) Long projectId) {
        LambdaQueryWrapper<WarningRecord> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(WarningRecord::getProjectId, projectId);
        }
        
        // 统计总数
        long totalCount = warningRecordService.count(wrapper);
        
        // 统计待处理
        LambdaQueryWrapper<WarningRecord> pendingWrapper = wrapper.clone();
        pendingWrapper.eq(WarningRecord::getStatus, "PENDING");
        long pendingCount = warningRecordService.count(pendingWrapper);
        
        // 统计处理中
        LambdaQueryWrapper<WarningRecord> processingWrapper = wrapper.clone();
        processingWrapper.eq(WarningRecord::getStatus, "PROCESSING");
        long processingCount = warningRecordService.count(processingWrapper);
        
        // 统计已解决
        LambdaQueryWrapper<WarningRecord> resolvedWrapper = wrapper.clone();
        resolvedWrapper.eq(WarningRecord::getStatus, "RESOLVED");
        long resolvedCount = warningRecordService.count(resolvedWrapper);
        
        // 统计已忽略
        LambdaQueryWrapper<WarningRecord> ignoredWrapper = wrapper.clone();
        ignoredWrapper.eq(WarningRecord::getStatus, "IGNORED");
        long ignoredCount = warningRecordService.count(ignoredWrapper);
        
        // 统计紧急
        LambdaQueryWrapper<WarningRecord> urgentWrapper = wrapper.clone();
        urgentWrapper.eq(WarningRecord::getWarningLevel, "URGENT");
        long urgentCount = warningRecordService.count(urgentWrapper);
        
        // 统计重要
        LambdaQueryWrapper<WarningRecord> importantWrapper = wrapper.clone();
        importantWrapper.eq(WarningRecord::getWarningLevel, "IMPORTANT");
        long importantCount = warningRecordService.count(importantWrapper);
        
        // 统计一般
        LambdaQueryWrapper<WarningRecord> generalWrapper = wrapper.clone();
        generalWrapper.eq(WarningRecord::getWarningLevel, "GENERAL");
        long generalCount = warningRecordService.count(generalWrapper);
        
        // 统计今日触发
        LambdaQueryWrapper<WarningRecord> todayWrapper = wrapper.clone();
        todayWrapper.apply("DATE(triggered_time) = CURDATE()");
        long todayTriggeredCount = warningRecordService.count(todayWrapper);
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalCount", totalCount);
        statistics.put("pendingCount", pendingCount);
        statistics.put("processingCount", processingCount);
        statistics.put("resolvedCount", resolvedCount);
        statistics.put("ignoredCount", ignoredCount);
        statistics.put("urgentCount", urgentCount);
        statistics.put("importantCount", importantCount);
        statistics.put("generalCount", generalCount);
        statistics.put("todayTriggeredCount", todayTriggeredCount);
        
        return R.ok(statistics);
    }
}
