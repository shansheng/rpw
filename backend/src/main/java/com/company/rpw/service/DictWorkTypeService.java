package com.company.rpw.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.rpw.entity.DictWorkType;
import com.company.rpw.mapper.DictWorkTypeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 工种字典服务类
 */
@Slf4j
@Service
public class DictWorkTypeService extends ServiceImpl<DictWorkTypeMapper, DictWorkType> {

    /**
     * 查询工种字典列表
     * @param status 状态筛选（可选）
     * @return 工种字典列表
     */
    public List<DictWorkType> listAll(Integer status) {
        if (status != null) {
            return lambdaQuery()
                    .eq(DictWorkType::getStatus, status)
                    .orderByAsc(DictWorkType::getSortOrder)
                    .list();
        }
        return lambdaQuery()
                .orderByAsc(DictWorkType::getSortOrder)
                .list();
    }

    /**
     * 根据编码查询
     * @param workTypeCode 工种编码
     * @return 工种字典
     */
    public DictWorkType getByCode(String workTypeCode) {
        return lambdaQuery()
                .eq(DictWorkType::getWorkTypeCode, workTypeCode)
                .one();
    }

    /**
     * 新增工种字典
     * @param entity 工种字典信息
     * @return 是否成功
     */
    public boolean create(DictWorkType entity) {
        // 检查编码是否已存在
        if (getByCode(entity.getWorkTypeCode()) != null) {
            return false;
        }
        return save(entity);
    }

    /**
     * 修改工种字典
     * @param id 工种字典ID
     * @param entity 工种字典信息
     * @return 是否成功
     */
    public boolean update(Long id, DictWorkType entity) {
        entity.setId(id);
        return updateById(entity);
    }

    /**
     * 删除工种字典（逻辑删除）
     * @param id 工种字典ID
     * @return 是否成功
     */
    public boolean delete(Long id) {
        return removeById(id);
    }
}
