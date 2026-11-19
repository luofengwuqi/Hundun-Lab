package com.hundunlab.site.module.software.pojo.Entity;

import lombok.Data;

/**
 * 软件仓库地址（D101）。
 * <p>
 * 一个软件可以有 0..n 条：同一个仓库的多个镜像、以及多个不同仓库，各带自己的标签与类型。
 * 这张表取代了 {@code software.repo_url} 单值列——那一列自 D101 起只读不写，旧值由迁移脚本搬进来。
 */
@Data
public class SoftwareRepo {

    private Long id;

    private Long softwareId;

    /** 展示标签中文，如「源码仓库」「Gitee 镜像」 */
    private String labelZh;

    private String labelEn;

    /** 完整地址，含协议 */
    private String url;

    /** source / mirror / release / docs */
    private String repoType;

    private Integer sortOrder;
}
