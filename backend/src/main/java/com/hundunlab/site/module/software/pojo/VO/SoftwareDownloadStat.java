package com.hundunlab.site.module.software.pojo.VO;

import lombok.Data;

/**
 * 单个软件的累计下载量（按安装包 download_count 求和）
 */
@Data
public class SoftwareDownloadStat {

    private Long softwareId;

    private Long total;
}
