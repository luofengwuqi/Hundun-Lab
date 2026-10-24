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

    private String repoUrl;

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
