package com.hundunlab.site.module.software.pojo.Entity;

import lombok.Data;

@Data
public class SoftwarePlatform {

    private Long id;

    private Long softwareId;

    /** win32 / darwin / linux */
    private String platform;

    private String noteZh;

    private String noteEn;

    private Integer sortOrder;
}
