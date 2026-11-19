package com.hundunlab.site.module.common.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 通用文件上传（图片/文档，非安装包）。
 * <p>
 * 独立成 common 模块而不是挂在 user 下：它服务的是「页面随便传个图」这类通用需求，
 * 与账号业务无关。安装包走 {@code /admin/software/**} 的专用通道（D102），不经过这里。
 */
public interface CommonService {

    /**
     * 校验并上传文件，返回可访问 URL
     */
    String addFile(MultipartFile file);
}
