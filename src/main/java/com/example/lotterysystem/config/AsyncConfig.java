package com.example.lotterysystem.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置
 * <p>
 * 用于邮件发送、抽奖计数更新等非关键路径操作，避免阻塞主线程响应。
 * 拒绝策略为 CallerRunsPolicy —— 队列满时由调用线程执行，保证任务不丢失。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：常驻线程，应对日常低负载
        executor.setCorePoolSize(2);
        // 最大线程数：高峰期扩容上限
        executor.setMaxPoolSize(4);
        // 队列容量：缓冲突发流量
        executor.setQueueCapacity(100);
        // 线程名前缀：方便日志排查
        executor.setThreadNamePrefix("lottery-async-");
        // 拒绝策略：队列满时回退到调用线程执行（保证任务不丢失）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 线程空闲回收时间（秒）
        executor.setKeepAliveSeconds(60);
        // 允许核心线程超时回收
        executor.setAllowCoreThreadTimeOut(true);
        executor.initialize();
        log.info("异步线程池初始化完成: core=2, max=4, queue=100");
        return executor;
    }
}
