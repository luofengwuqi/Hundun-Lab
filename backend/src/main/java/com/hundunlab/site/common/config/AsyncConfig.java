package com.hundunlab.site.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 邮件通知专用线程池
     */
    @Bean("emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：常驻线程数量
        executor.setCorePoolSize(2);

        // 最大线程数：高峰期最多创建的线程数
        executor.setMaxPoolSize(5);

        // 队列容量：核心线程满后，任务进入队列等待
        executor.setQueueCapacity(100);

        // 线程空闲时间：超过核心线程数的线程，空闲多久后销毁（秒）
        executor.setKeepAliveSeconds(60);

        // 线程名前缀：方便日志排查
        executor.setThreadNamePrefix("email-notify-");

        // 拒绝策略：队列满且达到最大线程数时的处理方式
        // CallerRunsPolicy：由调用线程（主线程）执行，起到降级作用
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间（秒）
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }

    /**
     * 通用异步任务线程池
     */
    @Bean("generalExecutor")
    public Executor generalExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("async-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}