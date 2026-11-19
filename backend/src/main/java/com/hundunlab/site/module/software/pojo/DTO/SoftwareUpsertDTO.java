package com.hundunlab.site.module.software.pojo.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 后台新建 / 编辑软件的入参（F12，D101）。
 * <p>
 * 四个子块（特性、截图、平台、仓库）都是<b>整块替换</b>语义：提交什么就是什么，
 * 不在数组里的旧行会被删掉，`sort_order` 按数组下标重排。不做逐条 CRUD——
 * 逐条要额外记 id、处理「先删后加」的顺序，换来的能力这里用不上。
 * <p>
 * 刻意<b>不含</b>：`repoUrl`（已改子表）、`status`（走独立的状态接口，避免「改资料顺手提权/上线」）。
 */
@Data
public class SoftwareUpsertDTO {

    @NotBlank(message = "slug 不能为空")
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,63}$",
            message = "slug 只能是小写字母、数字和连字符，且以字母或数字开头")
    private String slug;

    @NotBlank(message = "中文名称不能为空")
    @Size(max = 64, message = "中文名称最多 64 字")
    private String nameZh;

    @NotBlank(message = "英文名称不能为空")
    @Size(max = 64, message = "英文名称最多 64 字")
    private String nameEn;

    @Size(max = 160, message = "标语最多 160 字")
    private String taglineZh;

    @Size(max = 160, message = "标语最多 160 字")
    private String taglineEn;

    private String introZh;

    private String introEn;

    /** 技术栈，逗号分隔存库；入参用数组，避免前端拼字符串 */
    @Size(max = 20, message = "技术栈最多 20 项")
    private List<@Size(max = 32, message = "单项技术栈最多 32 字") String> techStack;

    @Size(max = 64, message = "许可证最多 64 字")
    private String license;

    @Size(max = 500, message = "封面地址过长")
    private String coverUrl;

    @Pattern(regexp = "^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$", message = "主题色必须是 #RGB 或 #RRGGBB")
    private String themeColor;

    private Integer sortOrder;

    @Valid
    @Size(max = 20, message = "功能特性最多 20 条")
    private List<FeatureItem> features;

    @Valid
    @Size(max = 20, message = "截图最多 20 张")
    private List<ScreenshotItem> screenshots;

    @Valid
    @Size(max = 3, message = "平台最多 3 条")
    private List<PlatformItem> platforms;

    @Valid
    @Size(max = 8, message = "仓库地址最多 8 条")
    private List<RepoItem> repos;

    @Data
    public static class FeatureItem {
        @Size(max = 16, message = "图标最多 16 字符")
        private String icon;
        @NotBlank(message = "特性标题不能为空")
        @Size(max = 64, message = "特性标题最多 64 字")
        private String titleZh;
        @NotBlank(message = "特性标题不能为空")
        @Size(max = 64, message = "特性标题最多 64 字")
        private String titleEn;
        @Size(max = 500, message = "特性描述最多 500 字")
        private String descZh;
        @Size(max = 500, message = "特性描述最多 500 字")
        private String descEn;
    }

    @Data
    public static class ScreenshotItem {
        @NotBlank(message = "截图地址不能为空")
        @Size(max = 500, message = "截图地址过长")
        private String imageUrl;
        @Size(max = 160, message = "截图说明最多 160 字")
        private String captionZh;
        @Size(max = 160, message = "截图说明最多 160 字")
        private String captionEn;
    }

    @Data
    public static class PlatformItem {
        @NotBlank(message = "平台不能为空")
        @Pattern(regexp = "^(win32|darwin|linux)$", message = "平台只能是 win32 / darwin / linux")
        private String platform;
        @Size(max = 120, message = "平台说明最多 120 字")
        private String noteZh;
        @Size(max = 120, message = "平台说明最多 120 字")
        private String noteEn;
    }

    @Data
    public static class RepoItem {
        @NotBlank(message = "仓库标签不能为空")
        @Size(max = 32, message = "仓库标签最多 32 字")
        private String labelZh;
        @NotBlank(message = "仓库标签不能为空")
        @Size(max = 32, message = "仓库标签最多 32 字")
        private String labelEn;
        @NotBlank(message = "仓库地址不能为空")
        @Size(max = 500, message = "仓库地址过长")
        @Pattern(regexp = "^https?://\\S+$", message = "仓库地址必须以 http:// 或 https:// 开头")
        private String url;
        /** source / mirror / release / docs */
        @NotBlank(message = "仓库类型不能为空")
        private String repoType;
    }
}
