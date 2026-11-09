package com.example.elibrary.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 业务线程池配置：bizExecutor 用于事件总线消费者与并行统计聚合等异步/并行任务。
 * 核心 4、最大 8、有界队列 100、CallerRunsPolicy（饱和时由提交线程执行，形成背压）。
 *
 * @author makejava
 * @since 2026-11-07
 */
@Slf4j
@Configuration
public class ThreadPoolConfig {

    @Bean(name = "bizExecutor", destroyMethod = "shutdown")
    public ThreadPoolExecutor bizExecutor() {
        return new ThreadPoolExecutor(
                4, 8, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),
                new NamedThreadFactory("biz-"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /** 命名线程工厂：线程名带前缀，便于排查与监控 */
    static class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger counter = new AtomicInteger(1);

        NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
}
