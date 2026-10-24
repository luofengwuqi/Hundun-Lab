package com.hundunlab.site.module.software.pojo.Entity;

import lombok.Data;

@Data
public class SoftwareScreenshot {

    private Long id;

    private Long softwareId;

    private String imageUrl;

    private String captionZh;

    private String captionEn;

    private Integer sortOrder;
}
