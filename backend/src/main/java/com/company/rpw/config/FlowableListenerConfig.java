package com.company.rpw.config;

import com.company.rpw.listener.SubcontractChangeProcessListener;
import lombok.RequiredArgsConstructor;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.RuntimeService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Flowable 事件监听器注册配置
 *
 * <p>在应用上下文完全刷新（引擎就绪）后，将
 * {@link SubcontractChangeProcessListener} 注册到运行时服务，
 * 仅监听 {@code PROCESS_COMPLETED} 与 {@code TASK_COMPLETED} 事件。</p>
 */
@Configuration
@RequiredArgsConstructor
public class FlowableListenerConfig implements ApplicationRunner {

    private final RuntimeService runtimeService;
    private final SubcontractChangeProcessListener listener;

    @Override
    public void run(ApplicationArguments args) {
        runtimeService.addEventListener(listener,
                FlowableEngineEventType.PROCESS_COMPLETED,
                FlowableEngineEventType.TASK_COMPLETED);
    }
}
