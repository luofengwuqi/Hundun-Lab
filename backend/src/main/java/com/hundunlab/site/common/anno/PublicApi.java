package com.hundunlab.site.common.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记「匿名可访问」的接口。
 * <p>
 * 权限模型是**默认拒绝**：没有本注解的接口一律要求登录。
 * 这样新增接口时忘记标注，后果是「多要一次登录」，而不是「意外对全网开放」。
 * <p>
 * 可标注在 Controller 类或方法上，方法上的标注优先（方法标注即可覆盖类级默认）。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PublicApi {
}
