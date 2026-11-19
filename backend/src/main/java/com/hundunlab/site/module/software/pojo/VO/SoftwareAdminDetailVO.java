package com.hundunlab.site.module.software.pojo.VO;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台软件详情 / 编辑回显（F12 + F13）。
 * <p>
 * 与前台 {@link SoftwareDetailVO} 分开：后台要看草稿版本、要看 `status`、要按编辑态给全字段。
 * 子块的字段形状复用了前台的嵌套 VO（`FeatureVO` 等），因为展示字段本来就一样，
 * 再抄一遍只会多一处要同步修改的地方。
 * <p>
 * `objectKey` 刻意**不返回**：后台没有需要它的界面，少一处泄露面。
 */
@Data
public class SoftwareAdminDetailVO {

    private Long id;

    private String slug;

    private String nameZh;

    private String nameEn;

    private String taglineZh;

    private String taglineEn;

    private String introZh;

    private String introEn;

    private List<String> techStack;

    private String license;

    private String coverUrl;

    private String themeColor;

    /** 0-草稿 1-已发布 */
    private Integer status;

    private Integer sortOrder;

    private List<SoftwareDetailVO.FeatureVO> features;

    private List<SoftwareDetailVO.ScreenshotVO> screenshots;

    private List<SoftwareDetailVO.PlatformVO> platforms;

    private List<RepoVO> repos;

    /** 全部版本（含草稿），按 published_at 倒序、id 倒序 */
    private List<ReleaseVO> releases;

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
        /** 0-草稿 1-已发布 */
        private Integer status;
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
