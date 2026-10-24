package com.hundunlab.site.module.user.controller;

import com.hundunlab.site.common.anno.RateLimit;
import com.hundunlab.site.common.context.BaseContext;
import com.hundunlab.site.common.response.Result;
import com.hundunlab.site.module.user.service.CommonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping
public class CommonController {

//    @Autowired
//    private com.hundunlab.site.common.util.AliOSSUtil aliOssUtil;

    @Autowired
    private CommonService cs;
    /**
     * 上传图片/附件（限流：防止OSS存储轰炸直接产生账单费用）
     */
    @RateLimit(count = 10, time = 60, key = "files:upload")
    @PostMapping("/files")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileUrl =  cs.addFile(file);
        if(fileUrl == null)
        {
            return Result.fail(500, "文件上传失败");
        }
        return Result.success("文件上传成功", fileUrl);

    }
}