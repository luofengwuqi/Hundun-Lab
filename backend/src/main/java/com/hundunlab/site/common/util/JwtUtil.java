package com.hundunlab.site.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类
 * 密钥只从外部配置读取（环境变量 JWT_SECRET 或本地 application-secret.yml），不硬编码
 */
@Component
public class JwtUtil {

    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey secretKey;
    private final long expirationMillis;

    public JwtUtil(@Value("${app.jwt.secret:}") String secret,
                   @Value("${app.jwt.expiration-millis:86400000}") long expirationMillis) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT 密钥未配置，请设置环境变量 JWT_SECRET（至少32字节）");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT 密钥强度不足，至少需要 " + MIN_SECRET_BYTES + " 字节");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMillis = expirationMillis;
    }

    /**
     * 生成令牌
     */
    public String makeToken(Map<String, Object> claims) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMillis))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public String makeToken(Integer id, String username) {
        return makeToken(Map.of("id", id, "username", username));
    }

    /**
     * 解析令牌，签名错误/格式错误/过期均抛 io.jsonwebtoken.JwtException 子类
     */
    public Claims solveToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
