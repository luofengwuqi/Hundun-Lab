package com.hundunlab.site.module.extension.mapper;

import com.hundunlab.site.module.extension.pojo.Entity.Extension;
import com.hundunlab.site.module.extension.pojo.Entity.ExtensionRelease;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 扩展只读查询（站点侧，F07/F08）。写入路径（提交/审核）属 M2 后台，届时另开接口。
 */
@Mapper
public interface ExtensionMapper {

    /** 某软件已上架扩展，featured DESC、download_count DESC（F07 排序口径） */
    List<Extension> selectPublishedBySoftwareSlug(@Param("slug") String slug);

    /** 详情：必须属于该软件且已上架，按数字主键查 */
    Extension selectPublishedBySoftwareAndId(@Param("softwareId") Long softwareId,
                                             @Param("id") Long id);

    /** 详情：必须属于该软件且已上架，按 URL slug 查 */
    Extension selectPublishedBySoftwareAndSlug(@Param("softwareId") Long softwareId,
                                               @Param("slug") String slug);

    /** 已发布版本，published_at 倒序（D064：最新版按发布时间推导） */
    List<ExtensionRelease> selectPublishedReleases(@Param("extensionId") Long extensionId);

    /** 详情页「下载」取最新发布版本；无版本返回 null */
    ExtensionRelease selectLatestPublishedRelease(@Param("extensionId") Long extensionId);

    @Update("UPDATE extension SET download_count = download_count + 1 WHERE id = #{extensionId}")
    int incrementDownloadCount(@Param("extensionId") Long extensionId);

    void insertDownload(@Param("extensionId") Long extensionId,
                        @Param("releaseId") Long releaseId,
                        @Param("ipHash") String ipHash,
                        @Param("userAgent") String userAgent);
}
