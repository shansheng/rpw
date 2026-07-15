package com.company.rpw.service.bpm;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.rpw.entity.bpm.BpmCategory;
import com.company.rpw.mapper.BpmCategoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * BPM 流程分类 Service
 */
@Slf4j
@Service
public class BpmCategoryService extends ServiceImpl<BpmCategoryMapper, BpmCategory> {
}
