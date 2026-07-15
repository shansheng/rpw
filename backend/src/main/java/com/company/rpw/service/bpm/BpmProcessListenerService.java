package com.company.rpw.service.bpm;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.rpw.entity.bpm.BpmProcessListener;
import com.company.rpw.mapper.BpmProcessListenerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * BPM 流程监听器服务
 */
@Service
@RequiredArgsConstructor
public class BpmProcessListenerService extends ServiceImpl<BpmProcessListenerMapper, BpmProcessListener> {
}
