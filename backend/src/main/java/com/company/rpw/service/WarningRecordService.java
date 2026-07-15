package com.company.rpw.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.rpw.entity.WarningRecord;
import com.company.rpw.mapper.WarningRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 预警记录服务类
 */
@Slf4j
@Service
public class WarningRecordService extends ServiceImpl<WarningRecordMapper, WarningRecord> {
}
