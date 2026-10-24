package com.hundunlab.site.module.software.controller;

import com.hundunlab.site.common.anno.PublicApi;
import com.hundunlab.site.common.response.Result;
import com.hundunlab.site.module.software.pojo.VO.SoftwareDetailVO;
import com.hundunlab.site.module.software.pojo.VO.SoftwareSummaryVO;
import com.hundunlab.site.module.software.service.SoftwareService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 软件展示接口。
 * <p>
 * 整类标注 {@link PublicApi}：匿名用户可以看首页与软件详情，
 * 只有点「下载」那一步才需要登录（见 DownloadController）。
 */
@Slf4j
@PublicApi
@RestController
@RequestMapping("/software")
public class SoftwareController {

    private final SoftwareService softwareService;

    public SoftwareController(SoftwareService softwareService) {
        this.softwareService = softwareService;
    }

    /**
     * 已发布软件列表
     */
    @GetMapping
    public Result<List<SoftwareSummaryVO>> list() {
        return Result.success(softwareService.listPublished());
    }

    /**
     * 软件详情（含功能特性、截图、平台、版本与安装包）
     */
    @GetMapping("/{slug}")
    public Result<SoftwareDetailVO> detail(@PathVariable String slug) {
        return Result.success(softwareService.getDetail(slug));
    }
}
