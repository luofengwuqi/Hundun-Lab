package com.hundunlab.site.framework.aspect;

import com.hundunlab.site.common.anno.RateLimit;
import com.hundunlab.site.common.exception.MyException.RateLimitException;
import com.hundunlab.site.module.auth.controller.LoginController;
import com.hundunlab.site.module.auth.pojo.dto.LoginUsernamePasswordRequestDTO;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 限流切面测试（依赖本机 redis: localhost:6379）
 */
class RateLimitAspectTest {

    private LettuceConnectionFactory factory;
    private StringRedisTemplate template;
    private RateLimitAspect aspect;

    @BeforeEach
    void setUp() {
        factory = new LettuceConnectionFactory(new RedisStandaloneConfiguration("localhost", 6379));
        factory.afterPropertiesSet();
        template = new StringRedisTemplate(factory);
        template.afterPropertiesSet();
        aspect = new RateLimitAspect(template, new com.hundunlab.site.common.util.IpUtil(false));
        cleanKeys();
    }

    @AfterEach
    void tearDown() {
        cleanKeys();
        RequestContextHolder.resetRequestAttributes();
        factory.destroy();
    }

    private void cleanKeys() {
        Set<String> keys = template.keys("ratelimit:*");
        if (keys != null && !keys.isEmpty()) {
            template.delete(keys);
        }
    }

    //模拟一次带请求上下文的调用
    private Object call(String ip, String forwardedFor, Method method, Object... args) throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(ip);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
            when(pjp.getArgs()).thenReturn(args);
            when(pjp.proceed()).thenReturn("OK");
            RateLimit rateLimit = method.getAnnotation(RateLimit.class);
            assertNotNull(rateLimit, "被测方法必须带 @RateLimit");
            return aspect.aroundMethod(pjp, rateLimit);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void loginEndpointBlocksWhenIpExceedsLimit() throws Throwable {
        Method method = LoginController.class.getMethod("loginUsernamePassword", LoginUsernamePasswordRequestDTO.class);
        LoginUsernamePasswordRequestDTO dto = new LoginUsernamePasswordRequestDTO();
        dto.setUsername("attacker");
        dto.setPassword("Sup3rSecret!");

        //注解配置 10 次/60 秒
        for (int i = 0; i < 10; i++) {
            assertEquals("OK", call("1.2.3.4", null, method, dto));
        }
        assertThrows(RateLimitException.class, () -> call("1.2.3.4", null, method, dto));

        //resetOnSuccess: 成功调用后账号维度计数应被清空 IP 维度保留
        assertFalse(template.hasKey("ratelimit:login:username-password:id:attacker"));
        assertTrue(template.hasKey("ratelimit:login:username-password:ip:1.2.3.4"));
    }

    @Test
    void sendCodeBlocksByEmailEvenAcrossDifferentIps() throws Throwable {
        Method method = LoginController.class.getMethod("sendEmailCode", String.class);

        for (int i = 0; i < 3; i++) {
            assertEquals("OK", call("1.1.1.1", null, method, "victim@example.com"));
        }
        //第 4 次同 IP 同邮箱 被拒
        assertThrows(RateLimitException.class, () -> call("1.1.1.1", null, method, "victim@example.com"));

        //换 IP 攻击同一邮箱 依然被拒(账号维度兜底)
        assertThrows(RateLimitException.class, () -> call("2.2.2.2", null, method, "victim@example.com"));

        //同 IP 换别的邮箱 不受影响(维度隔离)
        assertEquals("OK", call("2.2.2.2", null, method, "other@example.com"));
    }

    @Test
    void windowExpiresAndAllowsRequestsAgain() throws Throwable {
        Method method = ShortWindowTarget.class.getMethod("hit", String.class);

        assertEquals("OK", call("3.3.3.3", null, method, "u1"));
        assertEquals("OK", call("3.3.3.3", null, method, "u1"));
        assertThrows(RateLimitException.class, () -> call("3.3.3.3", null, method, "u1"));

        //窗口 2 秒 过期后恢复
        try {
            Thread.sleep(2100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(e);
        }
        assertEquals("OK", call("3.3.3.3", null, method, "u1"));
    }

    @Test
    void passwordNeverStoredInRedisKey() throws Throwable {
        Method method = LoginController.class.getMethod("loginUsernamePassword", LoginUsernamePasswordRequestDTO.class);
        LoginUsernamePasswordRequestDTO dto = new LoginUsernamePasswordRequestDTO();
        dto.setUsername("victim");
        dto.setPassword("Sup3rSecret!Passw0rd");

        call("4.4.4.4", null, method, dto);

        Set<String> keys = template.keys("ratelimit:*");
        assertNotNull(keys);
        assertFalse(keys.isEmpty());
        for (String key : keys) {
            assertFalse(key.contains("Sup3rSecret"), "redis key 中不得出现密码: " + key);
        }
    }

    @Test
    void usesFirstIpFromForwardedForHeader() throws Throwable {
        Method method = LoginController.class.getMethod("loginUsernamePassword", LoginUsernamePasswordRequestDTO.class);
        LoginUsernamePasswordRequestDTO dto = new LoginUsernamePasswordRequestDTO();
        dto.setUsername("u");
        dto.setPassword("password1");

        call("127.0.0.1", "9.9.9.9, 10.0.0.1", method, dto);

        assertTrue(template.hasKey("ratelimit:login:username-password:ip:9.9.9.9"),
                "应取 X-Forwarded-For 链中第一个真实客户端 IP");
    }

    @Test
    void aopProxyEnforcesLimitThroughSpringContext() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(AspectTestConfig.class)) {
            ProxiedTarget proxied = ctx.getBean(ProxiedTarget.class);

            assertEquals("OK", proxied.hit("userA"));
            assertEquals("OK", proxied.hit("userA"));
            //第 3 次超过 2 次/60 秒 说明切点绑定与代理均生效
            RateLimitException e = assertThrows(RateLimitException.class, () -> proxied.hit("userA"));
            assertNotNull(e.getMessage());
        }
    }

    //供窗口过期测试使用的短窗口目标
    static class ShortWindowTarget {
        @RateLimit(count = 2, time = 2, key = "test:short-window")
        public String hit(String user) {
            return "OK";
        }
    }

    //供 AOP 上下文测试使用的代理目标
    static class ProxiedTarget {
        @RateLimit(count = 2, time = 60, key = "test:proxied")
        public String hit(String user) {
            return "OK";
        }
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class AspectTestConfig {
        @Bean
        LettuceConnectionFactory redisConnectionFactory() {
            return new LettuceConnectionFactory(new RedisStandaloneConfiguration("localhost", 6379));
        }

        @Bean
        StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory connectionFactory) {
            return new StringRedisTemplate(connectionFactory);
        }

        @Bean
        RateLimitAspect rateLimitAspect(StringRedisTemplate stringRedisTemplate) {
            return new RateLimitAspect(stringRedisTemplate, new com.hundunlab.site.common.util.IpUtil(false));
        }

        @Bean
        ProxiedTarget proxiedTarget() {
            return new ProxiedTarget();
        }
    }
}
