package com.hundunlab.site.framework.aspect;

import com.hundunlab.site.common.anno.RateLimit;
import com.hundunlab.site.common.exception.MyException.RateLimitException;
import com.hundunlab.site.common.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static com.hundunlab.site.common.constant.redis.RedisConstant.RATE_LIMIT_PATH;

/**
 * 接口限流切面：基于 redis 固定窗口计数器
 * 同时按 IP 和账号标识(用户名/邮箱/手机号)两个维度计数 任一维度超限即拒绝
 */
@Slf4j
@Component
@Aspect
public class RateLimitAspect {

    private final StringRedisTemplate stringRedisTemplate;
    private final IpUtil ipUtil;

    public RateLimitAspect(StringRedisTemplate stringRedisTemplate, IpUtil ipUtil) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.ipUtil = ipUtil;
    }

    @Pointcut("@annotation(com.hundunlab.site.common.anno.RateLimit)")
    public void rateLimitPointcut() {
    }

    //注解通过切面参数绑定拿到 不走反射查找 免去代理类/接口方法查不到注解的坑
    @Around(value = "rateLimitPointcut() && @annotation(rateLimit)", argNames = "pjp,rateLimit")
    public Object aroundMethod(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String clientIp = getClientIp();
        String identity = resolveIdentity(pjp.getArgs());

        String ipKey = buildKey(rateLimit, "ip", clientIp);
        String identityKey = identity == null ? null : buildKey(rateLimit, "id", identity);

        //两个维度都要过闸 超限直接抛异常 由全局异常处理器转成 429
        checkLimit(ipKey, rateLimit);
        if (identityKey != null) {
            checkLimit(identityKey, rateLimit);
        }

        Object result = pjp.proceed();

        //登录类接口校验通过后清空账号维度计数 防止正常用户输错几次被误锁
        //IP 维度计数保留 攻击者无法靠反复"登录成功自己的小号"洗白 IP
        if (rateLimit.resetOnSuccess() && identityKey != null) {
            stringRedisTemplate.delete(identityKey);
            log.info("限流计数已重置:{}", identityKey);
        }
        return result;
    }

    //固定窗口计数:第一次进入窗口时 INCR + EXPIRE 之后只 INCR
    private void checkLimit(String key, RateLimit annotation) {
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, annotation.time(), TimeUnit.SECONDS);
        }
        if (count != null && count > annotation.count()) {
            throw new RateLimitException("操作太频繁了,请稍后再试...");
        }
    }

    //拼 redis key: ratelimit:${接口标识}:${维度}:${值}
    private String buildKey(RateLimit annotation, String dimension, String value) {
        return RATE_LIMIT_PATH + annotation.key() + ":" + dimension + ":" + value;
    }

    //从入参中提取账号标识(用户名/邮箱/手机号) 只取第一个参数
    //注意:绝不使用 toString/含密码的字段做 key 密码不能进 redis
    private String resolveIdentity(Object[] args) {
        if (args == null || args.length == 0 || args[0] == null) {
            return null;
        }
        Object arg = args[0];
        if (arg instanceof String s) {
            return s.isBlank() ? null : s;
        }
        for (String getter : new String[]{"getUsername", "getEmail", "getPhone"}) {
            try {
                Method method = arg.getClass().getMethod(getter);
                Object value = method.invoke(arg);
                if (value instanceof String s && !s.isBlank()) {
                    return s;
                }
            } catch (NoSuchMethodException e) {
                //该 DTO 没有这个 getter 换下一个
            } catch (ReflectiveOperationException e) {
                log.warn("解析限流账号标识失败:{}", e.getMessage());
            }
        }
        return null;
    }

    //获取真实客户端 IP（默认不信任代理头，防止伪造）
    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        return ipUtil.getClientIp(attributes.getRequest());
    }
}
