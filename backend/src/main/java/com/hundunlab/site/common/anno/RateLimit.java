package com.hundunlab.site.common.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)        // 这个注解只能贴在方法上
@Retention(RetentionPolicy.RUNTIME) // 运行时还能读到它（反射能获取）
public @interface RateLimit {

    //时间窗口内允许的最大请求数
    int count();

    //时间窗口长度 单位:秒
    int time();

    //限流标识 会拼进 redis key 建议填接口名 保证各接口计数互不干扰
    String key();

    //调用成功后是否清空账号维度计数(登录成功时重置 避免正常用户输错几次就被锁)
    boolean resetOnSuccess() default false;
}
