package com.company.rpw.controller;

import com.company.rpw.common.R;
import com.company.rpw.dto.report.ReportConfigDTO;
import com.company.rpw.dto.report.ReportConfigVO;
import com.company.rpw.dto.report.ReportQueryDTO;
import com.company.rpw.dto.report.ReportResultVO;
import com.company.rpw.service.CustomReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 自定义报表控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
public class CustomReportController {

    private final CustomReportService reportService;

    /**
     * 保存报表配置
     * POST /api/v1/report/config
     */
    @PostMapping("/config")
    public R<Long> saveConfig(@RequestBody @Valid ReportConfigDTO dto) {
        Long configId = reportService.saveConfig(dto);
        return R.ok(configId);
    }

    /**
     * 获取报表配置列表
     * GET /api/v1/report/config/list
     */
    @GetMapping("/config/list")
    public R<List<ReportConfigVO>> getConfigList(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String reportType) {
        return R.ok(reportService.getConfigList(userId, reportType));
    }

    /**
     * 获取报表配置详情
     * GET /api/v1/report/config/{id}
     */
    @GetMapping("/config/{id}")
    public R<ReportConfigVO> getConfigById(@PathVariable Long id) {
        return R.ok(reportService.getConfigById(id));
    }

    /**
     * 删除报表配置
     * DELETE /api/v1/report/config/{id}
     */
    @DeleteMapping("/config/{id}")
    public R<Boolean> deleteConfig(@PathVariable Long id) {
        boolean result = reportService.deleteConfig(id);
        return result ? R.ok(true) : R.fail(500, "删除失败");
    }

    /**
     * 预览报表数据
     * POST /api/v1/report/preview
     */
    @PostMapping("/preview")
    public R<ReportResultVO> previewReport(@RequestBody @Valid ReportQueryDTO dto) {
        return R.ok(reportService.previewReport(dto));
    }

    /**
     * 导出报表为 Excel
     * POST /api/v1/report/export/excel
     */
    @PostMapping("/export/excel")
    public void exportExcel(@RequestBody @Valid ReportQueryDTO dto, jakarta.servlet.http.HttpServletResponse response) {
        try {
            reportService.exportExcel(dto, response);
        } catch (Exception e) {
            log.error("导出Excel失败", e);
        }
    }

    /**
     * 导出报表为 CSV
     * POST /api/v1/report/export/pdf
     */
    @PostMapping("/export/pdf")
    public void exportPdf(@RequestBody @Valid ReportQueryDTO dto, jakarta.servlet.http.HttpServletResponse response) {
        try {
            reportService.exportPdf(dto, response);
        } catch (Exception e) {
            log.error("导出PDF失败", e);
        }
    }
}
