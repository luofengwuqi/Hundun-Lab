package com.hundunlab.site.module.software.pojo.Entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SoftwareRelease {

    private Long id;

    private Long softwareId;

    private String version;

    /** stable / beta */
    private String channel;

    private String notesZh;

    private String notesEn;

    private LocalDateTime publishedAt;

    /** 0-草稿 1-已发布 */
    private Integer status;

    private LocalDateTime createdAt;
}
