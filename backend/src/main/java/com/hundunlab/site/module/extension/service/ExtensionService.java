package com.hundunlab.site.module.extension.service;

import com.hundunlab.site.module.extension.pojo.VO.ExtensionVO;

import java.util.List;

/**
 * 扩展站点侧接口（F07/F08/F10）。扩展挂在软件下（D074/D075），没有站点级入口。
 */
public interface ExtensionService {

    /** 某软件已上架扩展列表；软件不存在或未发布抛 SOFTWARE_NOT_FOUND */
    List<ExtensionVO> listPublished(String softwareSlug);

    /** 扩展详情；不存在、已下架或不属于该软件一律 EXTENSION_NOT_FOUND（D072） */
    ExtensionVO getDetail(String softwareSlug, String idOrSlug);

    /**
     * 最新发布版本的下载短链：签发预签名 URL 并落一条匿名下载记录（F10）。
     * 返回给 Controller 做 302；OSS 未配置抛 DOWNLOAD_STORAGE_UNAVAILABLE。
     */
    String issueDownloadUrl(String softwareSlug, String idOrSlug, jakarta.servlet.http.HttpServletRequest request);
}
