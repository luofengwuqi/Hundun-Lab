package com.hundunlab.site.framework.interception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hundunlab.site.common.response.ErrorCode;
import com.hundunlab.site.common.response.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 上传体积闸门。
 * <p>
 * 为什么需要它：安装包上限是 2GiB（D102），而 Spring 的 multipart 解析发生在
 * 拦截器与 Controller <b>之前</b>——等业务校验跑起来，2GiB 已经落到磁盘临时文件了。
 * 只靠 {@code app.upload.*-max-size-bytes} 等于让人先把硬盘写满再说「不行」。
 * <p>
 * 这里在解析之前按 {@code Content-Length} 判一道，超限直接 413，连临时文件都不产生。
 * 分块传输（无 {@code Content-Length}）拦不住，由 Spring 的 multipart 上限兜底。
 * <p>
 * 顺序排在 {@link TokenResolveFilter} 之前：体积判断不需要身份，越早拒绝越省资源。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class UploadSizeGuardFilter extends OncePerRequestFilter {

    private static final String MULTIPART_PREFIX = "multipart/";

    private final long generalMaxBytes;
    private final long assetMaxBytes;
    private final ObjectMapper objectMapper;

    public UploadSizeGuardFilter(@Value("${app.upload.max-size-bytes:10485760}") long generalMaxBytes,
                                 @Value("${app.upload.asset-max-size-bytes:2147483648}") long assetMaxBytes,
                                 ObjectMapper objectMapper) {
        this.generalMaxBytes = generalMaxBytes;
        this.assetMaxBytes = assetMaxBytes;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String contentType = request.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith(MULTIPART_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        long contentLength = request.getContentLengthLong();
        if (contentLength <= 0) {
            // 分块传输拿不到长度，交给 Spring 的 multipart 上限兜底
            chain.doFilter(request, response);
            return;
        }

        long limit = isReleaseAssetUpload(request) ? assetMaxBytes : generalMaxBytes;
        if (limit > 0 && contentLength > limit) {
            log.warn("上传被体积闸门拦下：{} {}，Content-Length={} 超过上限 {}",
                    request.getMethod(), request.getRequestURI(), contentLength, limit);
            writeTooLarge(response);
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * 只有「后台安装包上传」这一条路径享受大体积额度，其余上传（图片/文档）一律 10MiB。
     * 刻意用完整前缀匹配而不是 {@code endsWith("/assets")}：宁可多匹配一层，也不要有别的
     * 接口碰巧叫 /assets 就白拿 2GiB 配额。
     */
    private boolean isReleaseAssetUpload(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = StringUtils.hasText(contextPath) ? uri.substring(contextPath.length()) : uri;
        return path.startsWith("/admin/software/") && path.endsWith("/assets");
    }

    private void writeTooLarge(HttpServletResponse response) throws IOException {
        response.setStatus(ErrorCode.FILE_TOO_LARGE.getHttpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<Void> body = Result.fail(ErrorCode.FILE_TOO_LARGE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
