package com.hundunlab.site.module.software.pojo.VO;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台软件列表项（F12）。
 * <p>
 * 与前台 {@code SoftwareSummaryVO} 刻意分开：后台要看到草稿、要看 `status` 与 `sortOrder`，
 * 前台那些字段（下载量、平台标签）后台列表不需要。混用一个 VO 迟早会在前台接口里漏出后台字段。
 */
@Data
public class SoftwareAdminSummaryVO {

    private Long id;

    private String slug;

    private String nameZh;

    private String nameEn;

    /** 0-草稿 1-已发布 */
    private Integer status;

    private Integer sortOrder;

    private LocalDateTime updatedAt;

    /** 版本数（含草稿） */
    private Integer releaseCount;

    /** 已发布版本里 published_at 最新的那条，无则为 null */
    private String latestVersion;
}
