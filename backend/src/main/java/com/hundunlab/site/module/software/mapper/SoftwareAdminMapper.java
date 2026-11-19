package com.hundunlab.site.module.software.mapper;

import com.hundunlab.site.common.anno.AutoLog;
import com.hundunlab.site.common.constant.ModuleNameConstant;
import com.hundunlab.site.common.constant.type.OperateTypeConstant;
import com.hundunlab.site.module.software.pojo.Entity.ReleaseAsset;
import com.hundunlab.site.module.software.pojo.Entity.Software;
import com.hundunlab.site.module.software.pojo.Entity.SoftwareFeature;
import com.hundunlab.site.module.software.pojo.Entity.SoftwarePlatform;
import com.hundunlab.site.module.software.pojo.Entity.SoftwareRelease;
import com.hundunlab.site.module.software.pojo.Entity.SoftwareRepo;
import com.hundunlab.site.module.software.pojo.Entity.SoftwareScreenshot;
import com.hundunlab.site.module.software.pojo.VO.SoftwareAdminSummaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 后台软件管理的读写（F12/F13，D101–D103）。
 * <p>
 * 与 {@link SoftwareMapper} 分开：那个接口的每条 SQL 都带 `status = 1`，是前台视角；
 * 后台要看草稿，也绝不希望有人误用了前台的「已发布」过滤。两个接口各自只装一种视角。
 */
@Mapper
public interface SoftwareAdminMapper {

    /** 后台列表：含草稿，可按关键词与状态筛 */
    List<SoftwareAdminSummaryVO> selectSummaries(@Param("keyword") String keyword,
                                                 @Param("status") Integer status);

    /** 按 id 取主表行，**不**过滤 status */
    Software selectById(@Param("id") Long id);

    /** slug 撞库检查；excludeId 用于编辑时排除自己 */
    int countBySlug(@Param("slug") String slug, @Param("excludeId") Long excludeId);

    @AutoLog(operateType = OperateTypeConstant.INSERT, moduleName = ModuleNameConstant.SOFTWARE)
    int insertSoftware(Software software);

    int updateSoftware(Software software);

    /**
     * 上下线。审计日志只记「关键操作」——上下架、删除、发版、传包
     * （docs/03 §3.10：下载/浏览这类高频动作不写审计）。
     */
    @AutoLog(operateType = OperateTypeConstant.UPDATE, moduleName = ModuleNameConstant.SOFTWARE)
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @AutoLog(operateType = OperateTypeConstant.DELETE, moduleName = ModuleNameConstant.SOFTWARE)
    int deleteById(@Param("id") Long id);

    /** 软件下挂着的扩展数：>0 时拒绝删除，避免扩展的 OSS 包变成孤儿 */
    int countExtensions(@Param("softwareId") Long softwareId);

    // ==================== 子块（整块替换） ====================

    List<SoftwareRepo> selectRepos(@Param("softwareId") Long softwareId);

    int deleteFeatures(@Param("softwareId") Long softwareId);

    int insertFeatures(@Param("list") List<SoftwareFeature> list);

    int deleteScreenshots(@Param("softwareId") Long softwareId);

    int insertScreenshots(@Param("list") List<SoftwareScreenshot> list);

    int deletePlatforms(@Param("softwareId") Long softwareId);

    int insertPlatforms(@Param("list") List<SoftwarePlatform> list);

    int deleteRepos(@Param("softwareId") Long softwareId);

    int insertRepos(@Param("list") List<SoftwareRepo> list);

    // ==================== 版本 ====================

    List<SoftwareRelease> selectAllReleases(@Param("softwareId") Long softwareId);

    SoftwareRelease selectReleaseById(@Param("id") Long id);

    int countReleaseVersion(@Param("softwareId") Long softwareId,
                            @Param("version") String version,
                            @Param("channel") String channel,
                            @Param("excludeId") Long excludeId);

    @AutoLog(operateType = OperateTypeConstant.INSERT, moduleName = ModuleNameConstant.SOFTWARE)
    int insertRelease(SoftwareRelease release);

    @AutoLog(operateType = OperateTypeConstant.UPDATE, moduleName = ModuleNameConstant.SOFTWARE)
    int updateRelease(SoftwareRelease release);

    @AutoLog(operateType = OperateTypeConstant.DELETE, moduleName = ModuleNameConstant.SOFTWARE)
    int deleteRelease(@Param("id") Long id);

    // ==================== 安装包资产 ====================

    List<ReleaseAsset> selectAssets(@Param("releaseId") Long releaseId);

    ReleaseAsset selectAssetById(@Param("id") Long id);

    int countAssetVariant(@Param("releaseId") Long releaseId,
                          @Param("platform") String platform,
                          @Param("arch") String arch,
                          @Param("packageType") String packageType);

    @AutoLog(operateType = OperateTypeConstant.INSERT, moduleName = ModuleNameConstant.SOFTWARE)
    int insertAsset(ReleaseAsset asset);

    @AutoLog(operateType = OperateTypeConstant.DELETE, moduleName = ModuleNameConstant.SOFTWARE)
    int deleteAssetById(@Param("id") Long id);

    /** 删软件前收集全部对象键（D103：先删 OSS 再删库） */
    List<String> selectObjectKeysBySoftwareId(@Param("softwareId") Long softwareId);

    List<String> selectObjectKeysByReleaseId(@Param("releaseId") Long releaseId);
}
