package com.hundunlab.site;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling//允许SpringTask运行
@EnableAsync//启用异步支持
public class HundunLabApplication {

	public static void main(String[] args) {

		SpringApplication.run(HundunLabApplication.class, args);
		System.out.println("Hello World!");

	}
}
