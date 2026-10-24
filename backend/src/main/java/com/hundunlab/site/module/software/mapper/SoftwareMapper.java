package com.hundunlab.site.module.software.mapper;

import com.hundunlab.site.module.software.pojo.Entity.Software;
import com.hundunlab.site.module.software.pojo.Entity.SoftwareFeature;
import com.hundunlab.site.module.software.pojo.Entity.SoftwarePlatform;
import com.hundunlab.site.module.software.pojo.Entity.SoftwareScreenshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 软件与会话无关的只读查询。
 * <p>
 * 全部只查 status = 1（已发布）的行：草稿对前台不可见，由后台接口另行处理。
 */
@Mapper
public interface SoftwareMapper {

    List<Software> selectPublished();

    Software selectPublishedBySlug(@Param("slug") String slug);

    Software selectPublishedById(@Param("id") Long id);

    List<SoftwareFeature> selectFeatures(@Param("softwareId") Long softwareId);

    List<SoftwareScreenshot> selectScreenshots(@Param("softwareId") Long softwareId);

    List<SoftwarePlatform> selectPlatforms(@Param("softwareId") Long softwareId);

    /**
     * 批量取平台，用于首页卡片，避免 N+1 查询
     */
    List<SoftwarePlatform> selectPlatformsBySoftwareIds(@Param("ids") List<Long> ids);
}
