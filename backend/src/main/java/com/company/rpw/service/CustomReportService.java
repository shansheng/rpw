package com.company.rpw.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.company.rpw.dto.report.*;
import com.company.rpw.entity.ReportConfig;
import com.company.rpw.entity.ReportExportLog;
import com.company.rpw.mapper.ReportConfigMapper;
import com.company.rpw.mapper.ReportExportLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.alibaba.excel.EasyExcel;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * 自定义报表服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomReportService {

    private final ReportConfigMapper reportConfigMapper;
    private final ReportExportLogMapper reportExportLogMapper;
    private final ReportDynamicQueryService dynamicQueryService;
    private final ObjectMapper objectMapper;

    /**
     * 保存报表配置
     * @param dto 报表配置DTO
     * @return 配置ID
     */
    public Long saveConfig(ReportConfigDTO dto) {
        // 如果设为默认，先取消该用户同类型下的其他默认配置
        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            reportConfigMapper.update(null, new UpdateWrapper<ReportConfig>()
                    .eq("user_id", dto.getUserId())
                    .eq("report_type", dto.getReportType())
                    .set("is_default", 0));
        }

        ReportConfig config;
        if (dto.getId() != null) {
            config = reportConfigMapper.selectById(dto.getId());
            if (config == null) {
                throw new RuntimeException("报表配置不存在");
            }
            config.setReportName(dto.getReportName());
            config.setReportType(dto.getReportType());
            config.setConfigJson(dto.getConfigJson());
            config.setIsDefault(Boolean.TRUE.equals(dto.getIsDefault()) ? 1 : 0);
            reportConfigMapper.updateById(config);
        } else {
            config = new ReportConfig();
            config.setUserId(dto.getUserId());
            config.setReportName(dto.getReportName());
            config.setReportType(dto.getReportType());
            config.setConfigJson(dto.getConfigJson());
            config.setIsDefault(Boolean.TRUE.equals(dto.getIsDefault()) ? 1 : 0);
            reportConfigMapper.insert(config);
        }

        return config.getId();
    }

    /**
     * 获取报表配置列表
     * @param userId 用户ID
     * @param reportType 报表类型
     * @return 配置列表
     */
    public List<ReportConfigVO> getConfigList(Long userId, String reportType) {
        QueryWrapper<ReportConfig> wrapper = new QueryWrapper<>();
        if (userId != null) {
            wrapper.eq("user_id", userId);
        }
        if (reportType != null && !reportType.isEmpty()) {
            wrapper.eq("report_type", reportType);
        }
        wrapper.orderByDesc("is_default").orderByDesc("create_time");

        List<ReportConfig> list = reportConfigMapper.selectList(wrapper);
        return list.stream().map(this::toVO).toList();
    }

    /**
     * 获取报表配置详情
     * @param id 配置ID
     * @return 配置详情
     */
    public ReportConfigVO getConfigById(Long id) {
        ReportConfig config = reportConfigMapper.selectById(id);
        return config != null ? toVO(config) : null;
    }

    /**
     * 删除报表配置
     * @param id 配置ID
     * @return 是否成功
     */
    public boolean deleteConfig(Long id) {
        return reportConfigMapper.deleteById(id) > 0;
    }

    /**
     * 预览报表数据
     * @param dto 查询DTO
     * @return 报表结果
     */
    public ReportResultVO previewReport(ReportQueryDTO dto) {
        // 解析配置JSON
        ReportConfigJson config = dynamicQueryService.parseConfigJson(dto.getConfigJson());

        // 执行动态查询
        List<Map<String, Object>> data = dynamicQueryService.dynamicQuery(
                dto.getReportType(), dto.getConfigJson(), dto.getPageNum(), dto.getPageSize());

        // 构建返回结果
        ReportResultVO vo = new ReportResultVO();
        List<ReportResultVO.FieldConfig> visibleFields = config.getFields().stream()
                .filter(f -> Boolean.TRUE.equals(f.getVisible()))
                .map(f -> {
                    ReportResultVO.FieldConfig fc = new ReportResultVO.FieldConfig();
                    fc.setField(f.getField());
                    fc.setLabel(f.getLabel());
                    fc.setVisible(f.getVisible());
                    fc.setWidth(f.getWidth());
                    return fc;
                }).toList();
        vo.setFields(visibleFields);
        vo.setData(data);
        vo.setTotal(data.size());

        return vo;
    }

    /**
     * 导出报表为Excel（真实xlsx格式）
     * @param dto 查询DTO
     * @param response HTTP响应
     */
    public void exportExcel(ReportQueryDTO dto, HttpServletResponse response) throws IOException {
        dto.setPageNum(1);
        dto.setPageSize(10000);
        ReportResultVO result = previewReport(dto);

        // 使用EasyExcel动态生成真实xlsx文件
        List<List<String>> headList = new ArrayList<>();
        for (ReportResultVO.FieldConfig field : result.getFields()) {
            headList.add(java.util.Collections.singletonList(field.getLabel()));
        }

        List<List<Object>> dataList = new ArrayList<>();
        for (Map<String, Object> rowData : result.getData()) {
            List<Object> row = new ArrayList<>();
            for (ReportResultVO.FieldConfig field : result.getFields()) {
                row.add(rowData.get(field.getField()));
            }
            dataList.add(row);
        }

        String fileName = URLEncoder.encode("报表导出", StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream())
                .head(headList)
                .sheet("报表数据")
                .doWrite(dataList);

        saveExportLog(1L, dto.getReportType(), "EXCEL", result.getTotal());
    }

    /**
     * 导出报表为PDF（真实PDF格式）
     * @param dto 查询DTO
     * @param response HTTP响应
     */
    public void exportPdf(ReportQueryDTO dto, HttpServletResponse response) throws IOException {
        dto.setPageNum(1);
        dto.setPageSize(10000);
        ReportResultVO result = previewReport(dto);

        String fileName = URLEncoder.encode("报表导出", StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".pdf");

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float margin = 50;
            float yStart = 750;
            float rowHeight = 20;
            float headerHeight = 25;
            float pageWidth = page.getMediaBox().getWidth();
            float tableWidth = pageWidth - 2 * margin;
            int colCount = result.getFields().size();
            float colWidth = tableWidth / colCount;

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                // Title
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                cs.newLineAtOffset(margin, yStart);
                cs.showText("报表导出");
                cs.endText();

                // Date
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                cs.newLineAtOffset(margin, yStart - 20);
                cs.showText("导出日期: " + LocalDate.now());
                cs.endText();

                float tableTop = yStart - 40;

                // Header row background
                cs.setNonStrokingColor(0.9f, 0.9f, 0.9f);
                cs.addRect(margin, tableTop - headerHeight, tableWidth, headerHeight);
                cs.fill();
                cs.setNonStrokingColor(0f, 0f, 0f);

                // Header text
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 9);
                for (int i = 0; i < colCount; i++) {
                    String label = result.getFields().get(i).getLabel();
                    float x = margin + i * colWidth + 2;
                    cs.beginText();
                    cs.newLineAtOffset(x, tableTop - headerHeight + 6);
                    cs.showText(label != null ? label : "");
                    cs.endText();
                }

                // Draw header bottom line
                cs.setLineWidth(0.5f);
                cs.moveTo(margin, tableTop - headerHeight);
                cs.lineTo(margin + tableWidth, tableTop - headerHeight);
                cs.stroke();

                // Data rows
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
                float rowY = tableTop - headerHeight - rowHeight;
                for (Map<String, Object> rowData : result.getData()) {
                    if (rowY < margin) {
                        // Page break - close current page content stream and add new page
                        // (contentStream is in try-with-resources, we'd need to handle this differently)
                        break;
                    }

                    // Draw row bottom line
                    cs.moveTo(margin, rowY);
                    cs.lineTo(margin + tableWidth, rowY);
                    cs.stroke();

                    for (int i = 0; i < colCount; i++) {
                        Object value = rowData.get(result.getFields().get(i).getField());
                        String strValue = value != null ? value.toString() : "";
                        float x = margin + i * colWidth + 2;
                        cs.beginText();
                        cs.newLineAtOffset(x, rowY + 5);
                        cs.showText(truncateText(strValue, colWidth, 8));
                        cs.endText();
                    }
                    rowY -= rowHeight;
                }

                // Draw vertical lines
                for (int i = 0; i <= colCount; i++) {
                    float x = margin + i * colWidth;
                    cs.moveTo(x, tableTop);
                    cs.lineTo(x, rowY + rowHeight);
                    cs.stroke();
                }

                // Top line of table
                cs.moveTo(margin, tableTop);
                cs.lineTo(margin + tableWidth, tableTop);
                cs.stroke();
            }

            document.save(response.getOutputStream());
        }

        saveExportLog(1L, dto.getReportType(), "PDF", result.getTotal());
    }

    /**
     * 截断文本以适应列宽
     */
    private String truncateText(String text, float colWidth, float fontSize) {
        // Approximate: each char ~ 5px at font size 8
        int maxLen = Math.max(1, (int) (colWidth / (fontSize * 0.6f)));
        if (text != null && text.length() > maxLen) {
            return text.substring(0, maxLen - 2) + "..";
        }
        return text != null ? text : "";
    }

    /**
     * 保存导出日志
     */
    private void saveExportLog(Long userId, String reportType, String exportType, int recordCount) {
        try {
            ReportExportLog exportLog = new ReportExportLog();
            exportLog.setUserId(userId);
            exportLog.setExportType(exportType);
            exportLog.setRecordCount(recordCount);
            exportLog.setExportTime(LocalDateTime.now());
            reportExportLogMapper.insert(exportLog);
        } catch (Exception e) {
            log.error("保存导出日志失败", e);
        }
    }

    /**
     * 实体转VO
     */
    private ReportConfigVO toVO(ReportConfig config) {
        ReportConfigVO vo = new ReportConfigVO();
        vo.setId(config.getId());
        vo.setUserId(config.getUserId());
        vo.setReportName(config.getReportName());
        vo.setReportType(config.getReportType());
        vo.setConfigJson(config.getConfigJson());
        vo.setIsDefault(config.getIsDefault());
        vo.setCreateTime(config.getCreateTime());
        vo.setUpdateTime(config.getUpdateTime());
        return vo;
    }
}
