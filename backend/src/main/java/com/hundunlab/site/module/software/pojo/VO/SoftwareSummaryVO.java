package com.hundunlab.site.module.software.pojo.VO;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 首页 / 列表页的软件卡片。
 * <p>
 * 中英文案一并返回，前端按当前语言取用：切换语言不需要重新请求接口，
 * 也就不存在「切了语言还有一半中文」的问题。
 */
@Data
public class SoftwareSummaryVO {

    private String slug;

    private String nameZh;

    private String nameEn;

    private String taglineZh;

    private String taglineEn;

    private String coverUrl;

    private String themeColor;

    /** 支持的平台标识，如 win32 / linux */
    private List<String> platforms;

    /** 最新已发布版本号，无版本时为空 */
    private String latestVersion;

    private LocalDateTime latestPublishedAt;

    private Long totalDownloads;
}
