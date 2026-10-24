package com.hundunlab.site.common.anno;

import com.hundunlab.site.common.constant.status.UserStatusConstant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色校验注解
 * 标注在 Controller 类或方法上，由 RoleCheckAspect 统一校验当前登录用户的角色
 * 方法上的注解优先于类上的注解
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /**
     * 允许访问的角色状态值，用户状态必须与其中之一完全相等
     */
    int[] value() default {UserStatusConstant.STATUS_ADMIN, UserStatusConstant.STATUS_SUPER_ADMIN};
}
