package com.hundunlab.site.module.extension.pojo.Entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 扩展版本。最新版按 published_at 倒序推导，不设 is_latest（D064）。
 */
@Data
public class ExtensionRelease {

    private Long id;

    private Long extensionId;

    private String version;

    private String changelogZh;

    private String changelogEn;

    /** OSS 对象键，不含域名（D055） */
    private String objectKey;

    private Long fileSize;

    /** zip 摘要，前台只展示前 12 位 */
    private String sha256;

    /** 0-待审 1-通过 2-驳回 */
    private Integer status;

    private Long downloadCount;

    private LocalDateTime publishedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
