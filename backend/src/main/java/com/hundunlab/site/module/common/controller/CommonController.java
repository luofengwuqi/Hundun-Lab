package com.hundunlab.site.module.common.controller;

import com.hundunlab.site.common.anno.RateLimit;
import com.hundunlab.site.common.anno.RequireRole;
import com.hundunlab.site.common.response.Result;
import com.hundunlab.site.module.common.service.CommonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 通用上传。挂在 common 模块下，与账号业务无关。
 * <p>
 * 只对管理员开放（{@code status ≥ 2}）：公开站没有任何让普通用户上传文件的场景，
 * 留着就是白送一块存储。安装包不走这里——那是 {@code /admin/software/**} 的专用通道（D102）。
 */
@Slf4j
@RequireRole
@RestController
@RequestMapping
public class CommonController {

    private final CommonService commonService;

    public CommonController(CommonService commonService) {
        this.commonService = commonService;
    }

    /**
     * 上传图片/附件（限流：防止 OSS 存储轰炸直接产生账单费用）
     */
    @RateLimit(count = 10, time = 60, key = "files:upload")
    @PostMapping("/files")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file) {
        return Result.success("文件上传成功", commonService.addFile(file));
    }
}
