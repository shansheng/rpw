package com.company.rpw.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.rpw.entity.Company;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 公司 Mapper 接口
 */
@Mapper
public interface CompanyMapper extends BaseMapper<Company> {

    /**
     * 根据组织ID查询公司列表
     * @param orgId 组织ID
     * @return 公司列表
     */
    List<Company> selectByOrgId(@Param("orgId") Long orgId);
}
