package com.hundunlab.site.module.download.pojo.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DownloadRequestDTO {

    @NotNull(message = "安装包不能为空")
    private Long assetId;
}
