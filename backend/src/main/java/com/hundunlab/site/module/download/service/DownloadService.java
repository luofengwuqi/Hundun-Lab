package com.hundunlab.site.module.download.service;

import com.hundunlab.site.module.download.pojo.Entity.DownloadRecord;
import com.hundunlab.site.module.download.pojo.VO.DownloadTicketVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface DownloadService {

    /**
     * 签发下载票据：校验登录态 → 校验资产可下载 → 生成 OSS 预签名地址 → 记账
     *
     * @param assetId 安装包资产 ID
     */
    DownloadTicketVO issueTicket(Long assetId, HttpServletRequest request);

    /**
     * 当前登录用户的下载记录
     */
    List<DownloadRecord> myDownloads(int limit);
}
