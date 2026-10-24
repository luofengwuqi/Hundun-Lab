package com.hundunlab.site.module.download.pojo.VO;

import lombok.Data;

/**
 * 下载票据：一张短时效的 OSS 预签名地址。
 * <p>
 * 前端拿到后直接用 {@code location.href} 跳转即可，安装包由 OSS/CDN 直出，
 * 不经过应用服务器，也就不存在「后端 5 秒超时把大文件下载掐断」的问题。
 */
@Data
public class DownloadTicketVO {

    private String url;

    private String fileName;

    private Long fileSize;

    private String sha256;

    /** 地址有效期（秒） */
    private Integer expiresIn;
}
