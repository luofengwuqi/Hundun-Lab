package com.hundunlab.site.common.anno;

import com.hundunlab.site.common.constant.status.UserStatusConstant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色校验注解。
 * 标注在 Controller 类或方法上，由 AccessControlInterceptor 统一校验当前登录用户的角色。
 * 方法上的注解优先于类上的注解。
 * <p>
 * 只应在 Controller 层使用：写在 Service 实现类上会依赖 AOP 代理类型，切到 JDK 动态代理时会静默失效。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /**
     * 允许访问的角色状态值，用户状态必须与其中之一完全相等
     */
    int[] value() default {UserStatusConstant.STATUS_ADMIN, UserStatusConstant.STATUS_SUPER_ADMIN};
}
