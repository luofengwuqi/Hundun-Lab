package com.hundunlab.site.module.extension.controller;

import com.hundunlab.site.common.anno.PublicApi;
import com.hundunlab.site.common.anno.RateLimit;
import com.hundunlab.site.common.response.Result;
import com.hundunlab.site.module.extension.pojo.VO.ExtensionVO;
import com.hundunlab.site.module.extension.service.ExtensionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * 扩展站点侧接口（F07/F08/F10）。
 * <p>
 * 路由挂在所属软件下（D075），整个控制器匿名可访问：
 * 扩展市场是公开页面内容，下载短链也是匿名可下（extension_download 表注释口径）。
 * 注意 {@code @PublicApi} 是 OR 语义，类级标注后方法级无法收回；
 * 将来提交接口（F09 的 POST）必须单独放在别的控制器里走登录鉴权。
 */
@RestController
@RequestMapping("/software/{slug}/extensions")
@PublicApi
public class ExtensionController {

    private final ExtensionService extensionService;

    public ExtensionController(ExtensionService extensionService) {
        this.extensionService = extensionService;
    }

    /**
     * 扩展列表（F07）：featured DESC、download_count DESC；无扩展返回空数组
     */
    @GetMapping
    public Result<List<ExtensionVO>> list(@PathVariable String slug) {
        return Result.success(extensionService.listPublished(slug));
    }

    /**
     * 扩展详情（F08）：id 接受数字主键或 URL slug；跨软件返回 EXTENSION_NOT_FOUND（D072）
     */
    @GetMapping("/{idOrSlug}")
    public Result<ExtensionVO> detail(@PathVariable String slug, @PathVariable String idOrSlug) {
        return Result.success(extensionService.getDetail(slug, idOrSlug));
    }

    /**
     * 下载短链（F10）：302 到预签名 URL，浏览器/桌面端直接从 OSS 拉取，不经过应用服务器中转
     * <p>
     * 限流（仅 IP 维度，60 次/分钟）：这个接口匿名可下，且每次请求都会写一条
     * {@code extension_download} 记录并自增下载计数，不设闸的话可以被人拿脚本把表灌爆。
     * <p>
     * 参数顺序是有意的：限流切面用 args[0] 提取账号维度，HttpServletRequest 没有账号
     * getter，身份维度自然为空，只走 IP。若把 slug 放到第一个参数，切面会把「软件
     * slug」当账号标识，变成同一软件全部下载共享一个全局计数器，会误伤真实流量。
     * 调整本方法签名前先看 {@code RateLimitAspect#resolveIdentity}。
     */
    @RateLimit(count = 60, time = 60, key = "ext:download")
    @GetMapping("/{idOrSlug}/download")
    public ResponseEntity<Void> download(HttpServletRequest request,
                                         @PathVariable String slug,
                                         @PathVariable String idOrSlug) {
        String url = extensionService.issueDownloadUrl(slug, idOrSlug, request);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }
}
