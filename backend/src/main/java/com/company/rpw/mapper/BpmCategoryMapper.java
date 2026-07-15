package com.company.rpw.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.rpw.entity.bpm.BpmCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * BPM 流程分类 Mapper
 */
@Mapper
public interface BpmCategoryMapper extends BaseMapper<BpmCategory> {
}
