package com.hundunlab.site.module.software.mapper;

import com.hundunlab.site.module.software.pojo.Entity.ReleaseAsset;
import com.hundunlab.site.module.software.pojo.Entity.SoftwareRelease;
import com.hundunlab.site.module.software.pojo.VO.SoftwareDownloadStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ReleaseMapper {

    List<SoftwareRelease> selectPublishedBySoftwareId(@Param("softwareId") Long softwareId);

    /**
     * 最新已发布版本（含发布时间倒序的第一条），无版本返回 null
     */
    SoftwareRelease selectLatestPublished(@Param("softwareId") Long softwareId);

    List<ReleaseAsset> selectAssetsByReleaseIds(@Param("releaseIds") List<Long> releaseIds);

    /**
     * 下载前校验：资产必须属于「已发布版本 + 已发布软件」，一次查齐避免越权下载草稿包
     */
    ReleaseAsset selectDownloadableAsset(@Param("assetId") Long assetId);

    List<ReleaseAsset> selectAssetsByReleaseId(@Param("releaseId") Long releaseId);

    /**
     * 批量取各软件的最新已发布版本（每个软件一条），首页卡片用
     */
    List<SoftwareRelease> selectLatestPublishedBySoftwareIds(@Param("softwareIds") List<Long> softwareIds);

    /**
     * 原子自增下载量，避免「先读再写」的并发丢更新
     */
    @Update("UPDATE release_asset SET download_count = download_count + 1 WHERE id = #{assetId}")
    int incrementDownloadCount(@Param("assetId") Long assetId);

    /**
     * 批量统计累计下载量，首页卡片用，避免逐个软件查一次
     */
    List<SoftwareDownloadStat> selectDownloadTotals(@Param("softwareIds") List<Long> softwareIds);
}
