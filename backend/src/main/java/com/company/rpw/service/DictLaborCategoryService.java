package com.company.rpw.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.rpw.entity.DictLaborCategory;
import com.company.rpw.mapper.DictLaborCategoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 劳务类别字典服务类
 */
@Slf4j
@Service
public class DictLaborCategoryService extends ServiceImpl<DictLaborCategoryMapper, DictLaborCategory> {

    /**
     * 查询劳务类别字典列表
     * @param status 状态筛选（可选）
     * @return 劳务类别字典列表
     */
    public List<DictLaborCategory> listAll(Integer status) {
        if (status != null) {
            return lambdaQuery()
                    .eq(DictLaborCategory::getStatus, status)
                    .orderByAsc(DictLaborCategory::getSortOrder)
                    .list();
        }
        return lambdaQuery()
                .orderByAsc(DictLaborCategory::getSortOrder)
                .list();
    }

    /**
     * 根据编码查询
     * @param categoryCode 类别编码
     * @return 劳务类别字典
     */
    public DictLaborCategory getByCode(String categoryCode) {
        return lambdaQuery()
                .eq(DictLaborCategory::getCategoryCode, categoryCode)
                .one();
    }

    /**
     * 新增劳务类别字典
     * @param entity 劳务类别字典信息
     * @return 是否成功
     */
    public boolean create(DictLaborCategory entity) {
        // 检查编码是否已存在
        if (getByCode(entity.getCategoryCode()) != null) {
            return false;
        }
        return save(entity);
    }

    /**
     * 修改劳务类别字典
     * @param id 劳务类别字典ID
     * @param entity 劳务类别字典信息
     * @return 是否成功
     */
    public boolean update(Long id, DictLaborCategory entity) {
        entity.setId(id);
        return updateById(entity);
    }

    /**
     * 删除劳务类别字典（逻辑删除）
     * @param id 劳务类别字典ID
     * @return 是否成功
     */
    public boolean delete(Long id) {
        return removeById(id);
    }
}
