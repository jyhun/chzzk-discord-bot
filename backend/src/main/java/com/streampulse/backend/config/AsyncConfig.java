package com.streampulse.backend.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Configuration
@RequiredArgsConstructor
@EnableAsync(proxyTargetClass = true)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    private final MeterRegistry registry;

    @Bean(name = "taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("task-");

        executor.setRejectedExecutionHandler((r, exec) -> {
            log.warn("비동기 작업 거부됨 (taskExecutor 포화). 작업 정보: {}", r.toString());
            throw new RejectedExecutionException("taskExecutor 포화: " + r);
        });

        // Micrometer에 등록
        ExecutorServiceMetrics.monitor(registry, executor.getThreadPoolExecutor(), "taskExecutor");

        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return taskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) ->
                log.error("Async 작업에서 오류 발생: {} 매개변수: {}", method.getName(), Arrays.toString(params), throwable);
    }
}