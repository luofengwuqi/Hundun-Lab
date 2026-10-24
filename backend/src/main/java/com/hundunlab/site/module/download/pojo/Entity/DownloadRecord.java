package com.hundunlab.site.module.download.pojo.Entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 下载明细。
 * <p>
 * IP 只存 SHA-256 摘要：既能做「同一 IP 反复下载」的风控统计，又不把明文 IP 落库。
 */
@Data
public class DownloadRecord {

    private Long id;

    private Long userId;

    private Long softwareId;

    private Long releaseId;

    private Long assetId;

    private String platform;

    private String ipHash;

    private String userAgent;

    private LocalDateTime createdAt;
}
