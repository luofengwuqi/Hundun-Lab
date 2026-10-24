package com.hundunlab.site.framework.aspect;

import com.hundunlab.site.common.anno.RequireRole;
import com.hundunlab.site.common.context.BaseContext;
import com.hundunlab.site.common.exception.MyException.InsufficientPermissionsException;
import com.hundunlab.site.module.user.mapper.UserMapper;
import com.hundunlab.site.module.user.pojo.Entity.User;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 统一的角色校验切面
 * 不依赖 URI 前缀判断，直接从数据库读取当前登录用户的真实状态，避免任何路径绕过
 */
@Slf4j
@Component
@Aspect
public class RoleCheckAspect {

    private final UserMapper userMapper;

    public RoleCheckAspect(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Around("@annotation(com.hundunlab.site.common.anno.RequireRole) "
            + "|| @within(com.hundunlab.site.common.anno.RequireRole)")
    public Object checkRole(ProceedingJoinPoint pjp) throws Throwable {
        RequireRole requireRole = resolveAnnotation(pjp);
        if (requireRole == null) {
            return pjp.proceed();
        }

        Long currentId = BaseContext.getCurrentId();
        if (currentId == null || currentId <= 0) {
            throw new InsufficientPermissionsException("未登录或登录已过期");
        }

        // 直接查数据库，避免缓存中的旧状态导致越权
        User currentUser = userMapper.selectUserById(currentId);
        if (currentUser == null) {
            throw new InsufficientPermissionsException("当前用户不存在");
        }

        Integer status = currentUser.getStatus();
        if (status == null) {
            throw new InsufficientPermissionsException("当前用户角色异常");
        }
        boolean allowed = Arrays.stream(requireRole.value()).anyMatch(role -> role == status);
        if (!allowed) {
            log.warn("用户 {} 状态 {} 无权访问 {}", currentUser.getUsername(), status, pjp.getSignature().toShortString());
            throw new InsufficientPermissionsException("权限不足");
        }

        return pjp.proceed();
    }

    /**
     * 方法上的注解优先，其次取 Controller 类上的注解
     */
    private RequireRole resolveAnnotation(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        RequireRole requireRole = AnnotatedElementUtils.findMergedAnnotation(method, RequireRole.class);
        if (requireRole != null) {
            return requireRole;
        }
        Class<?> targetClass = AopUtils.getTargetClass(pjp.getTarget());
        return AnnotatedElementUtils.findMergedAnnotation(targetClass, RequireRole.class);
    }
}
