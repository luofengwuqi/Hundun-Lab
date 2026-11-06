package com.hundunlab.site.module.extension.pojo.Entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 扩展下载记录（匿名可下，只记计数与时间，不存原始 IP，schema 注释口径）。
 */
@Data
public class ExtensionDownload {

    private Long id;

    private Long extensionId;

    private Long releaseId;

    /** IP 的 SHA-256 哈希，不存原文 */
    private String ipHash;

    private String userAgent;

    private LocalDateTime createdAt;
}
