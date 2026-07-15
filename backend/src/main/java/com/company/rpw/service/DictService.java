package com.company.rpw.service;

import com.company.rpw.mapper.DictMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 字典管理服务
 * 提供通用的字典表操作（支持所有dict_开头的表）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictService {

    private final DictMapper dictMapper;

    /** 允许访问的字典表白名单 */
    private static final String[] ALLOWED_TABLES = {
            "dict_wbs", "dict_resource", "dict_supplier",
            "dict_purchase_source", "dict_purchase_progress",
            "dict_shipping_progress", "dict_work_type", "dict_labor_category"
    };

    /**
     * 验证表名是否合法
     * @param tableName 表名
     * @return 是否合法
     */
    private boolean isValidTable(String tableName) {
        for (String allowed : ALLOWED_TABLES) {
            if (allowed.equals(tableName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 查询字典列表
     * @param tableName 字典表名
     * @return 字典列表
     */
    public List<Map<String, Object>> list(String tableName) {
        if (!isValidTable(tableName)) {
            throw new IllegalArgumentException("非法的字典表名: " + tableName);
        }
        return dictMapper.list(tableName);
    }

    /**
     * 根据ID查询字典
     * @param tableName 字典表名
     * @param id 字典ID
     * @return 字典数据
     */
    public Map<String, Object> getById(String tableName, Long id) {
        if (!isValidTable(tableName)) {
            throw new IllegalArgumentException("非法的字典表名: " + tableName);
        }
        return dictMapper.getById(tableName, id);
    }

    /**
     * 新增字典
     * @param tableName 字典表名
     * @param data 字典数据
     * @return 是否成功
     */
    public boolean create(String tableName, Map<String, Object> data) {
        if (!isValidTable(tableName)) {
            throw new IllegalArgumentException("非法的字典表名: " + tableName);
        }
        int result = dictMapper.insert(tableName, data);
        return result > 0;
    }

    /**
     * 修改字典
     * @param tableName 字典表名
     * @param id 字典ID
     * @param data 字典数据
     * @return 是否成功
     */
    public boolean update(String tableName, Long id, Map<String, Object> data) {
        if (!isValidTable(tableName)) {
            throw new IllegalArgumentException("非法的字典表名: " + tableName);
        }
        int result = dictMapper.update(tableName, id, data);
        return result > 0;
    }

    /**
     * 删除字典（逻辑删除）
     * @param tableName 字典表名
     * @param id 字典ID
     * @return 是否成功
     */
    public boolean delete(String tableName, Long id) {
        if (!isValidTable(tableName)) {
            throw new IllegalArgumentException("非法的字典表名: " + tableName);
        }
        int result = dictMapper.delete(tableName, id);
        return result > 0;
    }

    /**
     * 获取所有字典表名
     * @return 字典表名列表
     */
    public List<String> getDictTables() {
        return List.of(ALLOWED_TABLES);
    }
}
