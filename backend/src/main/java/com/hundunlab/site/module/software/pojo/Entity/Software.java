package com.hundunlab.site.module.software.pojo.Entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 软件主表。
 * <p>
 * 中英文字段成对出现：展示层按当前语言取一列，不做「中文存库、英文机器翻译」，
 * 也不把文案写进代码——新增软件只需后台录入数据。
 */
@Data
public class Software {

    private Long id;

    /** URL 标识，如 hundundesktop */
    private String slug;

    private String nameZh;

    private String nameEn;

    private String taglineZh;

    private String taglineEn;

    private String introZh;

    private String introEn;

    /** 逗号分隔的技术栈 */
    private String techStack;

    private String repoUrl;

    private String license;

    private String coverUrl;

    /** 卡片主色，如 #C3272B */
    private String themeColor;

    /** 0-草稿 1-已发布 */
    private Integer status;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
