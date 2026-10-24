package com.hundunlab.site.module.software.pojo.Entity;

import lombok.Data;

@Data
public class SoftwareFeature {

    private Long id;

    private Long softwareId;

    /** emoji 或图标名 */
    private String icon;

    private String titleZh;

    private String titleEn;

    private String descZh;

    private String descEn;

    private Integer sortOrder;
}
