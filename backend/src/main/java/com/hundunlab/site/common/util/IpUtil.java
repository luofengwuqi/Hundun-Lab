package com.hundunlab.site.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 客户端 IP 解析
 * 默认只信任 TCP 连接的对端地址；只有显式开启 TRUST_PROXY_HEADERS 时才读取代理头，
 * 避免客户端伪造 X-Forwarded-For 污染审计日志和限流计数
 */
@Component
public class IpUtil {

    private final boolean trustProxyHeaders;

    public IpUtil(@Value("${app.security.trust-proxy-headers:false}") boolean trustProxyHeaders) {
        this.trustProxyHeaders = trustProxyHeaders;
    }

    public String getClientIp(HttpServletRequest request) {
        String ip = null;
        if (trustProxyHeaders) {
            ip = firstNonBlank(
                    request.getHeader("X-Forwarded-For"),
                    request.getHeader("X-Real-IP"),
                    request.getHeader("Proxy-Client-IP"));
            if (ip != null && ip.contains(",")) {
                // 代理链格式为 client, proxy1, proxy2，取第一个
                ip = ip.split(",")[0].trim();
            }
        }

        if (!isIpLiteral(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip == null || ip.isBlank() ? "unknown" : ip;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean isIpLiteral(String value) {
        if (value == null || value.isBlank() || value.length() > 45) {
            return false;
        }
        // IPv4
        String[] parts = value.split("\\.", -1);
        if (parts.length == 4) {
            for (String part : parts) {
                if (part.isEmpty() || part.length() > 3) {
                    return false;
                }
                for (char c : part.toCharArray()) {
                    if (!Character.isDigit(c)) {
                        return false;
                    }
                }
                if (Integer.parseInt(part) > 255) {
                    return false;
                }
            }
            return true;
        }
        // IPv6（只做格式约束，避免 InetAddress 触发 DNS 解析）
        if (value.contains(":")) {
            return value.matches("[0-9a-fA-F:.]+");
        }
        return false;
    }
}
