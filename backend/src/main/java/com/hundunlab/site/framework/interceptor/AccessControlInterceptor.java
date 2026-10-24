package com.hundunlab.site.framework.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hundunlab.site.common.anno.PublicApi;
import com.hundunlab.site.common.anno.RequireRole;
import com.hundunlab.site.common.constant.status.UserStatusConstant;
import com.hundunlab.site.common.context.BaseContext;
import com.hundunlab.site.common.response.ErrorCode;
import com.hundunlab.site.common.response.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Arrays;

/**
 * 访问控制拦截器：全站唯一的「放行 / 拒绝」决策点。
 * <p>
 * 规则按顺序生效：
 * <ol>
 *   <li>{@link PublicApi} 标注的接口：任何访问状态都放行（带无效令牌也按匿名处理）</li>
 *   <li>未标注的接口：必须是已登录状态，否则 401（令牌无效时返回更具体的错误码）</li>
 *   <li>标注 {@link RequireRole} 或路径以 {@code /admin} 开头：再校验角色，不满足则 403</li>
 * </ol>
 * 默认拒绝，新增接口忘记标注的后果是「多要一次登录」，而不是「意外对全网开放」。
 */
@Slf4j
@Component
public class AccessControlInterceptor implements HandlerInterceptor {

    private static final String ADMIN_PATH_PREFIX = "/admin";
    private static final int[] ADMIN_ROLES = {
            UserStatusConstant.STATUS_ADMIN,
            UserStatusConstant.STATUS_SUPER_ADMIN
    };

    private final ObjectMapper objectMapper;

    public AccessControlInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        // 只有落到 Controller 方法上的请求才需要鉴权；静态资源、错误页直接放过
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        // CORS 预检不带业务令牌，交给 CorsFilter 处理
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }

        boolean publicApi = isAnnotated(handlerMethod, PublicApi.class);

        if (!BaseContext.isAuthenticated()) {
            if (publicApi) {
                return true;
            }
            ErrorCode reason = BaseContext.getAuthError();
            reject(response, reason != null ? reason : ErrorCode.AUTH_NOT_LOGGED_IN);
            return false;
        }

        RequireRole requireRole = resolveRole(handlerMethod);
        boolean adminPath = request.getRequestURI().startsWith(ADMIN_PATH_PREFIX);
        if (requireRole == null && !adminPath) {
            return true;
        }

        int[] allowedRoles = requireRole != null ? requireRole.value() : ADMIN_ROLES;
        Integer status = BaseContext.getCurrentStatus();
        boolean allowed = status != null && Arrays.stream(allowedRoles).anyMatch(role -> role == status);
        if (!allowed) {
            log.warn("用户 {} 状态 {} 越权访问 {}", BaseContext.getCurrentUsername(), status, request.getRequestURI());
            reject(response, ErrorCode.AUTH_FORBIDDEN);
            return false;
        }
        return true;
    }

    private boolean isAnnotated(HandlerMethod handlerMethod, Class<? extends java.lang.annotation.Annotation> type) {
        return AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), type)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), type);
    }

    private RequireRole resolveRole(HandlerMethod handlerMethod) {
        RequireRole onMethod = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequireRole.class);
        if (onMethod != null) {
            return onMethod;
        }
        return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequireRole.class);
    }

    private void reject(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(errorCode.getHttpStatus());
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        String json = objectMapper.writeValueAsString(Result.fail(errorCode));
        response.getWriter().write(json);
        response.getWriter().flush();
    }
}
