package com.hundunlab.site.module.software.pojo.Entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 安装包资产。
 * <p>
 * 只存 OSS 对象键，不存直链：桶是私有读的，下载必须由后端签发临时地址。
 */
@Data
public class ReleaseAsset {

    private Long id;

    private Long releaseId;

    /** win32 / darwin / linux */
    private String platform;

    /** x64 / arm64 */
    private String arch;

    /** nsis / appimage / deb / dmg / zip */
    private String packageType;

    private String fileName;

    private String objectKey;

    private Long fileSize;

    private String sha256;

    private Long downloadCount;

    private LocalDateTime createdAt;

    /**
     * 冗余字段，仅由下载校验的联表查询填充，不参与写库
     */
    private Long softwareId;
}
