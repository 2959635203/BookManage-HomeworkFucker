package com.northgod.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        // 使用 Java 21+ 虚拟线程执行器，大幅提升I/O密集型并发性能
        return java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
    }
}