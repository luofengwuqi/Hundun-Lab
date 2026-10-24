package com.hundunlab.site.framework.interception;

import com.hundunlab.site.common.context.AuthState;
import com.hundunlab.site.common.context.BaseContext;
import com.hundunlab.site.common.response.ErrorCode;
import com.hundunlab.site.common.util.JwtUtil;
import com.hundunlab.site.module.user.pojo.Entity.User;
import com.hundunlab.site.module.user.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 令牌解析过滤器测试：只验证「上下文里被写入了什么」，不验证放行与否
 * （放行由 {@code AccessControlInterceptor} 决定）。
 */
class TokenResolveFilterTest {

    private static final String TOKEN = "header.payload.signature";

    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOps;
    private UserService userService;
    private JwtUtil jwtUtil;
    private TokenResolveFilter filter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        userService = mock(UserService.class);
        jwtUtil = mock(JwtUtil.class);
        filter = new TokenResolveFilter(redisTemplate, userService, jwtUtil);
    }

    @AfterEach
    void tearDown() {
        BaseContext.clearAll();
    }

    @Test
    @DisplayName("不带令牌 -> 匿名，且不触碰 Redis 与数据库")
    void noTokenMeansAnonymous() throws Exception {
        AtomicReference<AuthState> state = new AtomicReference<>();
        AtomicReference<Boolean> authenticated = new AtomicReference<>();

        filter.doFilter(new MockHttpServletRequest("GET", "/software"), new MockHttpServletResponse(),
                (req, res) -> {
                    state.set(BaseContext.getAuthState());
                    authenticated.set(BaseContext.isAuthenticated());
                });

        assertEquals(AuthState.ANONYMOUS, state.get());
        assertFalse(authenticated.get());
        assertNull(BaseContext.getCurrentId(), "请求结束后上下文必须清空");
    }

    @Test
    @DisplayName("有效令牌 + Redis 一致 + 用户正常 -> 已登录，且写入 id/username/status")
    void validTokenEstablishesIdentity() throws Exception {
        Claims claims = claims(7, "alice");
        when(jwtUtil.solveToken(TOKEN)).thenReturn(claims);
        when(valueOps.get(anyString())).thenReturn(TOKEN);
        when(userService.selectUserById(7L)).thenReturn(user(7L, "alice", 1));

        AtomicReference<Long> id = new AtomicReference<>();
        AtomicReference<String> username = new AtomicReference<>();
        AtomicReference<Integer> status = new AtomicReference<>();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/account");
        request.addHeader("Authorization", "Bearer " + TOKEN);

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            id.set(BaseContext.getCurrentId());
            username.set(BaseContext.getCurrentUsername());
            status.set(BaseContext.getCurrentStatus());
        });

        assertEquals(7L, id.get());
        assertEquals("alice", username.get());
        assertEquals(1, status.get());
        assertNull(BaseContext.getCurrentId(), "请求结束后上下文必须清空");
    }

    @Test
    @DisplayName("过期令牌 -> INVALID + 过期错误码，不抛异常打断链路")
    void expiredTokenMarksInvalid() throws Exception {
        when(jwtUtil.solveToken(TOKEN)).thenThrow(new ExpiredJwtException(null, null, "expired"));

        AtomicReference<AuthState> state = new AtomicReference<>();
        AtomicReference<ErrorCode> error = new AtomicReference<>();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/account");
        request.addHeader("Authorization", "Bearer " + TOKEN);

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            state.set(BaseContext.getAuthState());
            error.set(BaseContext.getAuthError());
        });

        assertEquals(AuthState.INVALID, state.get());
        assertEquals(ErrorCode.AUTH_TOKEN_EXPIRED, error.get());
    }

    @Test
    @DisplayName("令牌被登出吊销（Redis 值不一致）-> INVALID")
    void revokedTokenMarksInvalid() throws Exception {
        Claims claims = claims(7, "alice");
        when(jwtUtil.solveToken(TOKEN)).thenReturn(claims);
        when(valueOps.get(anyString())).thenReturn("another-token");

        AtomicReference<AuthState> state = new AtomicReference<>();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/account");
        request.addHeader("Authorization", "Bearer " + TOKEN);

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> state.set(BaseContext.getAuthState()));

        assertEquals(AuthState.INVALID, state.get());
    }

    @Test
    @DisplayName("账号被封禁 -> INVALID + 账号禁用错误码（403 而非 401）")
    void bannedUserMarksDisabled() throws Exception {
        Claims claims = claims(7, "alice");
        when(jwtUtil.solveToken(TOKEN)).thenReturn(claims);
        when(valueOps.get(anyString())).thenReturn(TOKEN);
        when(userService.selectUserById(7L)).thenReturn(user(7L, "alice", -1));

        AtomicReference<ErrorCode> error = new AtomicReference<>();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/account");
        request.addHeader("Authorization", "Bearer " + TOKEN);

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> error.set(BaseContext.getAuthError()));

        assertEquals(ErrorCode.AUTH_ACCOUNT_DISABLED, error.get());
    }

    @Test
    @DisplayName("Redis 故障 -> 降级为仅验签，登录态仍然成立")
    void redisFailureFallsBackToSignatureOnly() throws Exception {
        Claims claims = claims(7, "alice");
        when(jwtUtil.solveToken(TOKEN)).thenReturn(claims);
        when(valueOps.get(anyString())).thenThrow(new IllegalStateException("redis down"));
        when(userService.selectUserById(7L)).thenReturn(user(7L, "alice", 1));

        AtomicReference<Boolean> authenticated = new AtomicReference<>();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/account");
        request.addHeader("Authorization", "Bearer " + TOKEN);

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> authenticated.set(BaseContext.isAuthenticated()));

        assertTrue(authenticated.get());
    }

    // ==================== 辅助 ====================

    private Claims claims(int id, String username) {
        Claims claims = mock(Claims.class);
        when(claims.get("id", Integer.class)).thenReturn(id);
        when(claims.get("username", String.class)).thenReturn(username);
        return claims;
    }

    private User user(Long id, String username, int status) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setStatus(status);
        return user;
    }
}
