package com.hundunlab.site.module.extension.pojo.VO;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 扩展列表/详情共用的对外视图，字段命名与前端 api/extension.js 的 M0 假数据同一风格，
 * M2 上线后前端只换数据来源不改页面。
 * <p>
 * {@code bundled} 不是库列：官方扩展随桌面端预置（D083），由 authorType = official 推导。
 */
@Data
public class ExtensionVO {

    private Long id;

    private String pluginId;

    private String slug;

    private String nameZh;

    private String nameEn;

    private String summaryZh;

    private String summaryEn;

    /** official-官方 / community-社区 */
    private String authorType;

    private String authorName;

    /** manifest 权限，已按逗号拆好，原样展示（F08） */
    private List<String> permissions;

    /** 支持平台，已按逗号拆好 */
    private List<String> platforms;

    /** 官方扩展视为随桌面端内置（D083 推导，见类注释） */
    private Boolean bundled;

    private Integer featured;

    private Long downloadCount;

    private String license;

    private String repoUrl;

    /** 详情页侧栏的 SHA256 只展示前 12 位（schema 注释口径），因此不直接返回完整摘要 */
    private String iconUrl;

    private String themeColor;

    /** README 的 Markdown 对象键或 URL；前端拉取文本展示（M2 换 Markdown 渲染） */
    private String readmeUrl;

    private LocalDateTime updatedAt;

    /** 仅详情返回：已发布版本，按发布时间倒序 */
    private List<ReleaseVO> releases;

    @Data
    public static class ReleaseVO {
        private Long id;
        private String version;
        private String changelogZh;
        private String changelogEn;
        private Long fileSize;
        private Long downloadCount;
        private LocalDateTime publishedAt;
    }
}
