package com.hundunlab.site.module.download.controller;

import com.hundunlab.site.common.anno.RateLimit;
import com.hundunlab.site.common.response.Result;
import com.hundunlab.site.module.download.pojo.DTO.DownloadRequestDTO;
import com.hundunlab.site.module.download.pojo.Entity.DownloadRecord;
import com.hundunlab.site.module.download.pojo.VO.DownloadTicketVO;
import com.hundunlab.site.module.download.service.DownloadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 下载接口。
 * <p>
 * 刻意不加 {@code @PublicApi}：匿名点下载会被拦截器挡下并返回 AUTH_NOT_LOGGED_IN，
 * 前端据此弹登录框，登录后拿到票据再从 OSS 直连下载。
 */
@Slf4j
@RestController
@RequestMapping("/download")
public class DownloadController {

    private final DownloadService downloadService;

    public DownloadController(DownloadService downloadService) {
        this.downloadService = downloadService;
    }

    /**
     * 换取下载票据。
     * <p>
     * 限流比页面接口松一些：用户可能连续点多个平台的包，但也不能无限刷地址。
     */
    @RateLimit(count = 30, time = 60, key = "download:ticket")
    @PostMapping("/ticket")
    public Result<DownloadTicketVO> ticket(@Valid @RequestBody DownloadRequestDTO dto,
                                           HttpServletRequest request) {
        return Result.success(downloadService.issueTicket(dto.getAssetId(), request));
    }

    /**
     * 我的下载记录
     */
    @GetMapping("/mine")
    public Result<List<DownloadRecord>> mine(@RequestParam(defaultValue = "20") int limit) {
        return Result.success(downloadService.myDownloads(limit));
    }
}
