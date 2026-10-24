package com.hundunlab.site.framework.config;

import com.hundunlab.site.framework.interceptor.AccessControlInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册访问控制拦截器。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AccessControlInterceptor accessControlInterceptor;

    public WebMvcConfig(AccessControlInterceptor accessControlInterceptor) {
        this.accessControlInterceptor = accessControlInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accessControlInterceptor)
                .addPathPatterns("/**")
                // /error 是容器转发过来的错误页，拦它会把真实错误掩盖成 401
                .excludePathPatterns("/error", "/favicon.ico");
    }
}
