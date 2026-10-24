package com.hundunlab.site.framework.aspect;

import com.hundunlab.site.common.anno.AutoFillTime;
import com.hundunlab.site.common.enums.AutoFillTimeType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Slf4j
@Component
@Aspect
public class AutoFillAspect {

    @Pointcut("execution(* com.hundunlab.site.module.*.mapper.*.*(..)) && @annotation(com.hundunlab.site.common.anno.AutoFillTime)")
    public void autoFillTimePointCut() {
    }

    @Before("autoFillTimePointCut()")
    public void autoFillTime(JoinPoint joinPoint) {
        AutoFillTime annotation = ((MethodSignature) joinPoint.getSignature())
                .getMethod().getAnnotation(AutoFillTime.class);
        if (annotation == null) {
            return;
        }

        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            log.warn("AutoFillTime 未找到方法参数: {}", joinPoint.getSignature().toShortString());
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        AutoFillTimeType type = annotation.value();
        boolean filled = false;
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            boolean current = false;
            if (type == AutoFillTimeType.INSERT) {
                current = invokeSetter(arg, "setCreatedAt", now);
            }
            boolean updated = invokeSetter(arg, "setUpdatedAt", now);
            if (current || updated) {
                filled = true;
                break;
            }
        }

        if (!filled) {
            log.warn("AutoFillTime 未找到可填充的时间字段: {}", joinPoint.getSignature().toShortString());
        }
    }

    /**
     * 找不到 setter 时只记录告警，不打断原业务
     */
    private boolean invokeSetter(Object target, String setterName, LocalDateTime value) {
        try {
            Method setter = target.getClass().getMethod(setterName, LocalDateTime.class);
            setter.invoke(target, value);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        } catch (ReflectiveOperationException e) {
            log.warn("AutoFillTime 调用 {} 失败: {}", setterName, e.getMessage());
            return false;
        }
    }
}
