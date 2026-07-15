package com.company.rpw.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 字典通用 Mapper
 * 使用动态SQL操作所有字典表（表结构相同）
 */
@Mapper
public interface DictMapper {

    /**
     * 查询字典列表（排除已删除的）
     * @param tableName 表名
     * @return 字典列表
     */
    @Select("""
            <script>
            SELECT * FROM ${tableName}
            WHERE deleted = 0
            ORDER BY sort_order ASC, id ASC
            </script>
            """)
    List<Map<String, Object>> list(@Param("tableName") String tableName);

    /**
     * 根据ID查询字典
     * @param tableName 表名
     * @param id ID
     * @return 字典数据
     */
    @Select("""
            <script>
            SELECT * FROM ${tableName}
            WHERE id = #{id} AND deleted = 0
            </script>
            """)
    Map<String, Object> getById(@Param("tableName") String tableName, @Param("id") Long id);

    /**
     * 新增字典
     * @param tableName 表名
     * @param data 数据
     * @return 影响行数
     */
    int insert(@Param("tableName") String tableName, @Param("data") Map<String, Object> data);

    /**
     * 修改字典
     * @param tableName 表名
     * @param id ID
     * @param data 数据
     * @return 影响行数
     */
    int update(@Param("tableName") String tableName, @Param("id") Long id, @Param("data") Map<String, Object> data);

    /**
     * 逻辑删除字典
     * @param tableName 表名
     * @param id ID
     * @return 影响行数
     */
    @Update("""
            <script>
            UPDATE ${tableName}
            SET deleted = 1, updated_at = NOW()
            WHERE id = #{id}
            </script>
            """)
    int delete(@Param("tableName") String tableName, @Param("id") Long id);
}
