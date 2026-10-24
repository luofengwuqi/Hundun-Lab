package com.hundunlab.site;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Hundun Lab 官网后端启动类。
 * 不使用 Spring Cache 抽象（缓存统一由 Redis 手工管理，见各 RedisService）。
 */
@SpringBootApplication
@EnableAsync
public class HundunLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(HundunLabApplication.class, args);
    }
}
