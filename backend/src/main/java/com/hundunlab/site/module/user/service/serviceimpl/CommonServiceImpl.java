package com.hundunlab.site.module.user.service.serviceimpl;

import com.hundunlab.site.common.context.BaseContext;
import com.hundunlab.site.common.util.FileUploadValidator;
import com.hundunlab.site.module.user.service.CommonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class CommonServiceImpl implements CommonService {


    @Autowired
    private com.hundunlab.site.common.util.AliOSSUtil aliOssUtil;
    @Autowired
    private FileUploadValidator fileUploadValidator;

    @Override
    public String addFile(MultipartFile file) {
        // 先做类型、大小和图片内容校验，不合法直接返回 400
        fileUploadValidator.validate(file, false);

        try {
            // 上传到OSS的 {用户名}/files/文件夹下
            String username = BaseContext.getCurrentUsername();
            log.info("当前用户名：{}", username);
            String savePath = username + "/files/";
            String objectName = savePath + aliOssUtil.makeUUIDFileName(file);
            return aliOssUtil.upload(file, objectName);

        } catch (Exception e) {
            log.error("文件上传失败", e);
            return null;
        }
    }
}
