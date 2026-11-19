package com.hundunlab.site.module.software.pojo.VO;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 软件详情页聚合视图。
 * <p>
 * 一次请求返回详情页需要的全部内容，避免页面里串行发四五个请求导致首屏抖动。
 */
@Data
public class SoftwareDetailVO {

    private String slug;

    private String nameZh;

    private String nameEn;

    private String taglineZh;

    private String taglineEn;

    private String introZh;

    private String introEn;

    /** 技术栈，已按逗号拆好 */
    private List<String> techStack;

    /**
     * 旧字段，保留是为了不破坏已上线的契约。
     * 自 D101 起由服务端从 {@link #repos} 里取 `repo_type = source` 中 `sort_order` 最小的一条回填，
     * 数据库上的 `software.repo_url` 已停止写入。
     */
    private String repoUrl;

    /**
     * 仓库地址（0..n 条，D101）：同仓多镜像 + 多不同仓库，各带标签与类型。
     * 详情页按 `sort_order` 顺序展示。
     */
    private List<RepoVO> repos;

    private String license;

    private String coverUrl;

    private String themeColor;

    private List<FeatureVO> features;

    private List<ScreenshotVO> screenshots;

    private List<PlatformVO> platforms;

    /** 已发布版本，按发布时间倒序 */
    private List<ReleaseVO> releases;

    @Data
    public static class FeatureVO {
        private String icon;
        private String titleZh;
        private String titleEn;
        private String descZh;
        private String descEn;
    }

    @Data
    public static class ScreenshotVO {
        private String imageUrl;
        private String captionZh;
        private String captionEn;
    }

    @Data
    public static class PlatformVO {
        private String platform;
        private String noteZh;
        private String noteEn;
    }

    @Data
    public static class RepoVO {
        private String labelZh;
        private String labelEn;
        private String url;
        /** source / mirror / release / docs */
        private String repoType;
    }

    @Data
    public static class ReleaseVO {
        private Long id;
        private String version;
        private String channel;
        private String notesZh;
        private String notesEn;
        private LocalDateTime publishedAt;
        private List<AssetVO> assets;
    }

    @Data
    public static class AssetVO {
        private Long id;
        private String platform;
        private String arch;
        private String packageType;
        private String fileName;
        private Long fileSize;
        private String sha256;
        private Long downloadCount;
    }
}
