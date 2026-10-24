package com.hundunlab.site.framework.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hundunlab.site.common.anno.PublicApi;
import com.hundunlab.site.common.anno.RequireRole;
import com.hundunlab.site.common.context.AuthState;
import com.hundunlab.site.common.context.BaseContext;
import com.hundunlab.site.common.response.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.nio.charset.StandardCharsets;

import static com.hundunlab.site.common.constant.status.UserStatusConstant.STATUS_ADMIN;
import static com.hundunlab.site.common.constant.status.UserStatusConstant.STATUS_SUPER_ADMIN;
import static com.hundunlab.site.common.constant.status.UserStatusConstant.STATUS_USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 访问控制矩阵测试：三种身份 × 三类接口。
 * 这是「匿名可浏览 + 登录才能下载」两项验收的直接证据。
 */
class AccessControlInterceptorTest {

    // 与生产一致：Spring 提供的 ObjectMapper 已注册 JavaTimeModule，Result 里含 LocalDateTime
    private final ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    private AccessControlInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new AccessControlInterceptor(objectMapper);
    }

    @AfterEach
    void tearDown() {
        BaseContext.clearAll();
    }

    // ==================== 匿名 ====================

    @Test
    @DisplayName("匿名 + 公开接口 -> 放行")
    void anonymousCanAccessPublicApi() throws Exception {
        anonymous();

        boolean allowed = interceptor.preHandle(request("/software"), new MockHttpServletResponse(), handler("publicApi"));

        assertTrue(allowed);
    }

    @Test
    @DisplayName("匿名 + 需登录接口 -> 401 未登录")
    void anonymousBlockedOnProtectedApi() throws Exception {
        anonymous();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request("/download/ticket"), response, handler("protectedApi"));

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
        assertEquals(ErrorCode.AUTH_NOT_LOGGED_IN.name(), errorCodeOf(response));
    }

    @Test
    @DisplayName("令牌过期 + 公开接口 -> 仍放行（不把公开页一起锁死）")
    void expiredTokenStillReadsPublicApi() throws Exception {
        BaseContext.setAuthState(AuthState.INVALID);
        BaseContext.setAuthError(ErrorCode.AUTH_TOKEN_EXPIRED);

        boolean allowed = interceptor.preHandle(request("/software"), new MockHttpServletResponse(), handler("publicApi"));

        assertTrue(allowed);
    }

    @Test
    @DisplayName("令牌过期 + 需登录接口 -> 401 且错误码为过期而非未登录")
    void expiredTokenReportsExpired() throws Exception {
        BaseContext.setAuthState(AuthState.INVALID);
        BaseContext.setAuthError(ErrorCode.AUTH_TOKEN_EXPIRED);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request("/download/ticket"), response, handler("protectedApi"));

        assertFalse(allowed);
        assertEquals(ErrorCode.AUTH_TOKEN_EXPIRED.name(), errorCodeOf(response));
    }

    // ==================== 普通用户 ====================

    @Test
    @DisplayName("普通用户 + 需登录接口 -> 放行")
    void userCanAccessProtectedApi() throws Exception {
        login(2L, "alice", STATUS_USER);

        boolean allowed = interceptor.preHandle(request("/download/ticket"), new MockHttpServletResponse(), handler("protectedApi"));

        assertTrue(allowed);
    }

    @Test
    @DisplayName("普通用户 + /admin 路径 -> 403")
    void userBlockedOnAdminPath() throws Exception {
        login(2L, "alice", STATUS_USER);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request("/admin/software"), response, handler("protectedApi"));

        assertFalse(allowed);
        assertEquals(403, response.getStatus());
        assertEquals(ErrorCode.AUTH_FORBIDDEN.name(), errorCodeOf(response));
    }

    @Test
    @DisplayName("普通用户 + @RequireRole 接口 -> 403")
    void userBlockedByRequireRole() throws Exception {
        login(2L, "alice", STATUS_USER);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request("/feedback"), response, handler("adminOnly"));

        assertFalse(allowed);
        assertEquals(ErrorCode.AUTH_FORBIDDEN.name(), errorCodeOf(response));
    }

    // ==================== 管理员 ====================

    @Test
    @DisplayName("管理员 + /admin 路径 -> 放行")
    void adminCanAccessAdminPath() throws Exception {
        login(1L, "root", STATUS_ADMIN);

        boolean allowed = interceptor.preHandle(request("/admin/software"), new MockHttpServletResponse(), handler("protectedApi"));

        assertTrue(allowed);
    }

    @Test
    @DisplayName("超级管理员 + @RequireRole 接口 -> 放行")
    void superAdminPassesRequireRole() throws Exception {
        login(1L, "root", STATUS_SUPER_ADMIN);

        boolean allowed = interceptor.preHandle(request("/feedback"), new MockHttpServletResponse(), handler("adminOnly"));

        assertTrue(allowed);
    }

    // ==================== 边界 ====================

    @Test
    @DisplayName("非 Controller 处理器（静态资源/错误页）-> 直接放行")
    void nonHandlerMethodPassesThrough() throws Exception {
        anonymous();

        assertTrue(interceptor.preHandle(request("/error"), new MockHttpServletResponse(), new Object()));
    }

    // ==================== 辅助 ====================

    private void anonymous() {
        BaseContext.setAuthState(AuthState.ANONYMOUS);
    }

    private void login(Long id, String username, int status) {
        BaseContext.setCurrentId(id);
        BaseContext.setCurrentUsername(username);
        BaseContext.setCurrentStatus(status);
        BaseContext.setAuthState(AuthState.AUTHENTICATED);
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRequestURI(uri);
        return request;
    }

    private HandlerMethod handler(String methodName) throws NoSuchMethodException {
        SampleController controller = new SampleController();
        return new HandlerMethod(controller, SampleController.class.getMethod(methodName));
    }

    private String errorCodeOf(MockHttpServletResponse response) throws Exception {
        String body = response.getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).path("errorCode").asText();
    }

    /**
     * 用于构造 HandlerMethod 的样例控制器，不会被真正调用
     */
    @SuppressWarnings("unused")
    static class SampleController {

        @PublicApi
        public void publicApi() {
        }

        public void protectedApi() {
        }

        @RequireRole({STATUS_ADMIN, STATUS_SUPER_ADMIN})
        public void adminOnly() {
        }
    }
}
