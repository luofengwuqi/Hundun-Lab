package com.hundunlab.site.module.extension.pojo.Entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 扩展（挂在软件下，D071–D075）。
 * <p>
 * 扩展是 HundunDesktop 特有的能力，没有站点级入口；数据模型见 db/01_schema.sql 的 extension 表。
 * 列表/详情只消费 status = 1（已上架）的行。
 */
@Data
public class Extension {

    private Long id;

    private Long softwareId;

    /** manifest 的 id，同一软件下唯一 */
    private String pluginId;

    /** URL 段，同一软件下唯一 */
    private String slug;

    private String nameZh;

    private String nameEn;

    private String summaryZh;

    private String summaryEn;

    /** official-官方 / community-社区 */
    private String authorType;

    /** 社区作者展示名；官方留空 */
    private String authorName;

    private Long authorUserId;

    private String iconUrl;

    private String themeColor;

    /** README 的 Markdown 对象键或 URL */
    private String readmeUrl;

    /** manifest 权限，逗号分隔，原样展示 */
    private String permissions;

    /** win32 / darwin / linux，逗号分隔 */
    private String platform;

    /** 开源许可证（如 MIT）；官方预置扩展通常与宿主软件一致 */
    private String license;

    /** 源码仓库；社区扩展用于详情页「查看源码」 */
    private String repoUrl;

    /** 0-待审 1-已上架 2-已驳回 3-已下架 */
    private Integer status;

    private String rejectReason;

    /** 1-在详情页精选区优先展示 */
    private Integer featured;

    /** 冗余计数，与 extension_download 同事务维护 */
    private Long downloadCount;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
