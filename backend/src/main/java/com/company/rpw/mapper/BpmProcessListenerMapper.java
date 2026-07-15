package com.company.rpw.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.rpw.entity.bpm.BpmProcessListener;
import org.apache.ibatis.annotations.Mapper;

/**
 * BPM 流程监听器 Mapper
 */
@Mapper
public interface BpmProcessListenerMapper extends BaseMapper<BpmProcessListener> {
}
