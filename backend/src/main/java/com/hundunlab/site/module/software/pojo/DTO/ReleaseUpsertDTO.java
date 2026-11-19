package com.hundunlab.site.module.software.pojo.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台新建 / 编辑版本的入参（F13）。
 * <p>
 * `publishedAt` 为空 = 尚未发布（D064：最新版按此字段倒序推导，不设 is_latest）。
 * 由「草稿 → 已发布」而 `publishedAt` 还空着时，服务端补当前时间——否则发布了却推不出最新版。
 */
@Data
public class ReleaseUpsertDTO {

    @NotBlank(message = "版本号不能为空")
    @Size(max = 32, message = "版本号最多 32 字符")
    @Pattern(regexp = "^[0-9A-Za-z][0-9A-Za-z._-]{0,31}$",
            message = "版本号只能含字母、数字、点、下划线和连字符")
    private String version;

    @NotBlank(message = "通道不能为空")
    @Pattern(regexp = "^(stable|beta)$", message = "通道只能是 stable / beta")
    private String channel;

    private String notesZh;

    private String notesEn;

    private LocalDateTime publishedAt;

    /** 0-草稿 1-已发布；为空按草稿处理 */
    private Integer status;
}
