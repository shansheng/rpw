package com.company.rpw.controller;

import com.company.rpw.common.R;
import com.company.rpw.service.DictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 字典管理控制器
 * 提供通用的字典表CRUD接口
 * URL格式：/api/v1/dict/{tableName}
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    /**
     * 查询字典列表
     * GET /api/v1/dict/{tableName}
     * @param tableName 字典表名（如：dict_wbs, dict_resource等）
     * @return 字典列表
     */
    @GetMapping("/{tableName}")
    public R<List<Map<String, Object>>> list(@PathVariable String tableName) {
        log.info("查询字典列表: {}", tableName);
        List<Map<String, Object>> list = dictService.list(tableName);
        return R.ok(list);
    }

    /**
     * 根据ID查询字典详情
     * GET /api/v1/dict/{tableName}/{id}
     * @param tableName 字典表名
     * @param id 字典ID
     * @return 字典详情
     */
    @GetMapping("/{tableName}/{id}")
    public R<Map<String, Object>> getById(
            @PathVariable String tableName,
            @PathVariable Long id) {
        log.info("查询字典详情: {}/{}", tableName, id);
        Map<String, Object> dict = dictService.getById(tableName, id);
        return R.ok(dict);
    }

    /**
     * 新增字典
     * POST /api/v1/dict/{tableName}
     * @param tableName 字典表名
     * @param data 字典数据
     * @return 是否成功
     */
    @PostMapping("/{tableName}")
    public R<Boolean> create(
            @PathVariable String tableName,
            @RequestBody Map<String, Object> data) {
        log.info("新增字典: {}", tableName);
        boolean result = dictService.create(tableName, data);
        return result ? R.ok(true) : R.fail(500, "新增失败");
    }

    /**
     * 修改字典
     * PUT /api/v1/dict/{tableName}/{id}
     * @param tableName 字典表名
     * @param id 字典ID
     * @param data 字典数据
     * @return 是否成功
     */
    @PutMapping("/{tableName}/{id}")
    public R<Boolean> update(
            @PathVariable String tableName,
            @PathVariable Long id,
            @RequestBody Map<String, Object> data) {
        log.info("修改字典: {}/{}", tableName, id);
        boolean result = dictService.update(tableName, id, data);
        return result ? R.ok(true) : R.fail(500, "修改失败");
    }

    /**
     * 删除字典（逻辑删除）
     * DELETE /api/v1/dict/{tableName}/{id}
     * @param tableName 字典表名
     * @param id 字典ID
     * @return 是否成功
     */
    @DeleteMapping("/{tableName}/{id}")
    public R<Boolean> delete(
            @PathVariable String tableName,
            @PathVariable Long id) {
        log.info("删除字典: {}/{}", tableName, id);
        boolean result = dictService.delete(tableName, id);
        return result ? R.ok(true) : R.fail(500, "删除失败");
    }

    /**
     * 获取所有可用的字典表名
     * GET /api/v1/dict/tables
     * @return 字典表名列表
     */
    @GetMapping("/tables")
    public R<List<String>> getDictTables() {
        List<String> tables = dictService.getDictTables();
        return R.ok(tables);
    }
}
