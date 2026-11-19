package com.hundunlab.site.module.common.service.serviceimpl;

import com.hundunlab.site.common.context.BaseContext;
import com.hundunlab.site.common.exception.MyException.BizException;
import com.hundunlab.site.common.exception.MyException.OSSException;
import com.hundunlab.site.common.response.ErrorCode;
import com.hundunlab.site.common.util.AliOSSUtil;
import com.hundunlab.site.common.util.FileUploadValidator;
import com.hundunlab.site.module.common.service.CommonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
public class CommonServiceImpl implements CommonService {

    private final AliOSSUtil aliOssUtil;
    private final FileUploadValidator fileUploadValidator;

    public CommonServiceImpl(AliOSSUtil aliOssUtil, FileUploadValidator fileUploadValidator) {
        this.aliOssUtil = aliOssUtil;
        this.fileUploadValidator = fileUploadValidator;
    }

    @Override
    public String addFile(MultipartFile file) {
        // 先做类型、大小和图片内容校验，不合法直接返回 400
        // ANY：图片与文档都收（截图/封面走这里），但 zip 不在白名单里（D102）
        fileUploadValidator.validate(file, FileUploadValidator.FileKind.ANY);

        // 桶没配好时明确说清楚，别让用户看到「服务器开小差了」
        if (!aliOssUtil.isEnabled()) {
            throw new BizException(ErrorCode.OSS_NOT_CONFIGURED);
        }

        try {
            // 上传到 OSS 的 {用户名}/files/ 目录下
            String username = BaseContext.getCurrentUsername();
            String objectName = username + "/files/" + aliOssUtil.makeUUIDFileName(file);
            return aliOssUtil.upload(file, objectName);
        } catch (OSSException | IOException e) {
            // 存储抖动 / 读流失败都转成稳定错误码，前端能按 code 出文案
            log.error("通用上传失败：object={}", file.getOriginalFilename(), e);
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, e.getMessage());
        }
    }
}
