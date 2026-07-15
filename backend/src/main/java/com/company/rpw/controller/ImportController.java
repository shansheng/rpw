package com.company.rpw.controller;

import com.company.rpw.common.R;
import com.company.rpw.dto.common.ImportResultVO;
import com.company.rpw.service.ImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Excel 导入控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/import")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;

    /**
     * 导入 WBS
     * POST /api/v1/import/wbs
     *
     * @param file Excel文件
     * @return 导入结果
     */
    @PostMapping("/wbs")
    public R<ImportResultVO> importWbs(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.fail("上传文件不能为空");
        }
        log.info("导入WBS开始, fileName={}, size={}", file.getOriginalFilename(), file.getSize());
        ImportResultVO result = importService.importWbs(file);
        return R.ok(result);
    }

    /**
     * 导入资源字典
     * POST /api/v1/import/resource
     *
     * @param file Excel文件
     * @return 导入结果
     */
    @PostMapping("/resource")
    public R<ImportResultVO> importResource(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.fail("上传文件不能为空");
        }
        log.info("导入资源字典开始, fileName={}, size={}", file.getOriginalFilename(), file.getSize());
        ImportResultVO result = importService.importResource(file);
        return R.ok(result);
    }

    /**
     * 导入供应商字典
     * POST /api/v1/import/supplier
     *
     * @param file Excel文件
     * @return 导入结果
     */
    @PostMapping("/supplier")
    public R<ImportResultVO> importSupplier(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.fail("上传文件不能为空");
        }
        log.info("导入供应商字典开始, fileName={}, size={}", file.getOriginalFilename(), file.getSize());
        ImportResultVO result = importService.importSupplier(file);
        return R.ok(result);
    }

    /**
     * 导入材料计划
     * POST /api/v1/import/material-plan
     *
     * @param file Excel文件
     * @return 导入结果
     */
    @PostMapping("/material-plan")
    public R<ImportResultVO> importMaterialPlan(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.fail("上传文件不能为空");
        }
        log.info("导入材料计划开始, fileName={}, size={}", file.getOriginalFilename(), file.getSize());
        ImportResultVO result = importService.importMaterialPlan(file);
        return R.ok(result);
    }
}
