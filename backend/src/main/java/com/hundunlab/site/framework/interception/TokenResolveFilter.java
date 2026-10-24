package com.hundunlab.site.framework.interception;

import com.hundunlab.site.common.context.AuthState;
import com.hundunlab.site.common.context.BaseContext;
import com.hundunlab.site.common.response.ErrorCode;
import com.hundunlab.site.common.util.JwtUtil;
import com.hundunlab.site.module.user.pojo.Entity.User;
import com.hundunlab.site.module.user.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.hundunlab.site.common.constant.redis.RedisConstant.TOKEN_EXPIRE_TIME;
import static com.hundunlab.site.common.constant.redis.RedisConstant.TOKEN_PATH;

/**
 * 令牌解析过滤器。
 * <p>
 * 职责只有「解析并写入上下文」，**不做放行/拒绝决策**：
 * <ul>
 *   <li>没带令牌 → 标记匿名，继续往下走（首页、软件页、插件页要能匿名访问）</li>
 *   <li>令牌无效 → 标记 INVALID 并记录原因，继续往下走；由 {@code AccessControlInterceptor}
 *       在真正需要登录的接口上返回 401/403，公开接口不受影响</li>
 * </ul>
 * 这样「令牌过期后连首页都打不开」和「误把公开接口拦死」两类问题同时消失。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TokenResolveFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtUtil jwtUtil;

    public TokenResolveFilter(RedisTemplate<String, Object> redisTemplate,
                              UserService userService,
                              JwtUtil jwtUtil) {
        this.redisTemplate = redisTemplate;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            resolve(request);
            chain.doFilter(request, response);
        } finally {
            // 线程池复用，必须清理，否则下一个请求会继承上一个请求的身份
            BaseContext.clearAll();
        }
    }

    private void resolve(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            BaseContext.setAuthState(AuthState.ANONYMOUS);
            return;
        }

        Claims claims;
        try {
            claims = jwtUtil.solveToken(token);
        } catch (ExpiredJwtException e) {
            markInvalid(ErrorCode.AUTH_TOKEN_EXPIRED, e.getMessage());
            return;
        } catch (JwtException | IllegalArgumentException e) {
            markInvalid(ErrorCode.AUTH_TOKEN_INVALID, e.getMessage());
            return;
        }

        Integer id = claims.get("id", Integer.class);
        String username = claims.get("username", String.class);
        if (id == null || username == null) {
            markInvalid(ErrorCode.AUTH_TOKEN_INVALID, "令牌缺少 id/username");
            return;
        }

        // Redis 吊销校验：登出、改密、封禁都会删除该 key，使旧令牌立即失效
        boolean redisAvailable = true;
        try {
            String cachedToken = (String) redisTemplate.opsForValue().get(TOKEN_PATH + id);
            if (cachedToken == null || !cachedToken.equals(token)) {
                markInvalid(ErrorCode.AUTH_TOKEN_EXPIRED, "令牌已被吊销");
                return;
            }
        } catch (Exception e) {
            // Redis 故障时降级为仅验签：可用性优先，代价是这段时间内无法主动吊销令牌
            redisAvailable = false;
            log.error("Redis 不可用，降级为仅校验 JWT 签名：{}", e.getMessage());
        }

        // 第二道防线：用户被删除或封禁后，旧令牌不能继续使用
        User user = userService.selectUserById(id.longValue());
        if (user == null) {
            markInvalid(ErrorCode.AUTH_TOKEN_INVALID, "令牌中的用户已不存在");
            return;
        }
        Integer status = user.getStatus();
        if (status == null || status < 0) {
            markInvalid(ErrorCode.AUTH_ACCOUNT_DISABLED, "账号已被禁用");
            return;
        }

        BaseContext.setCurrentId(id.longValue());
        BaseContext.setCurrentUsername(username);
        BaseContext.setCurrentStatus(status);
        BaseContext.setAuthState(AuthState.AUTHENTICATED);

        // 滑动续期：活跃请求不断延后过期时间
        if (redisAvailable) {
            try {
                redisTemplate.expire(TOKEN_PATH + id, TOKEN_EXPIRE_TIME, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.warn("刷新令牌过期时间失败：{}", e.getMessage());
            }
        }
    }

    private void markInvalid(ErrorCode errorCode, String reason) {
        BaseContext.setAuthState(AuthState.INVALID);
        BaseContext.setAuthError(errorCode);
        log.debug("令牌校验失败：[{}] {}", errorCode, reason);
    }

    /**
     * 优先 Authorization: Bearer &lt;token&gt;，兼容旧前端的 token 头
     */
    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            String token = authorization.substring(BEARER_PREFIX.length()).trim();
            return token.isEmpty() ? null : token;
        }
        String legacy = request.getHeader("token");
        return (legacy == null || legacy.isBlank()) ? null : legacy.trim();
    }
}
