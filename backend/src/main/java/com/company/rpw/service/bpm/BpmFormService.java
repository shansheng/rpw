package com.company.rpw.service.bpm;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.rpw.entity.bpm.BpmForm;
import com.company.rpw.mapper.BpmFormMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * BPM 流程表单服务
 */
@Service
@RequiredArgsConstructor
public class BpmFormService extends ServiceImpl<BpmFormMapper, BpmForm> {
}
