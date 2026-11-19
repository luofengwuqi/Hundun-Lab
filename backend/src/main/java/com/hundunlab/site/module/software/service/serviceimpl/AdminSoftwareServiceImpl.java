package com.hundunlab.site.module.software.service.serviceimpl;

import com.hundunlab.site.common.exception.MyException.BizException;
import com.hundunlab.site.common.exception.MyException.OSSException;
import com.hundunlab.site.common.response.ErrorCode;
import com.hundunlab.site.common.util.AliOSSUtil;
import com.hundunlab.site.module.software.mapper.SoftwareAdminMapper;
import com.hundunlab.site.module.software.mapper.SoftwareMapper;
import com.hundunlab.site.module.software.pojo.DTO.ReleaseUpsertDTO;
import com.hundunlab.site.module.software.pojo.DTO.SoftwareUpsertDTO;
import com.hundunlab.site.module.software.pojo.Entity.ReleaseAsset;
import com.hundunlab.site.module.software.pojo.Entity.Software;
import com.hundunlab.site.module.software.pojo.Entity.SoftwareFeature;
import com.hundunlab.site.module.software.pojo.Entity.SoftwarePlatform;
import com.hundunlab.site.module.software.pojo.Entity.SoftwareRelease;
import com.hundunlab.site.module.software.pojo.Entity.SoftwareRepo;
import com.hundunlab.site.module.software.pojo.Entity.SoftwareScreenshot;
import com.hundunlab.site.module.software.pojo.VO.SoftwareAdminDetailVO;
import com.hundunlab.site.module.software.pojo.VO.SoftwareAdminSummaryVO;
import com.hundunlab.site.module.software.pojo.VO.SoftwareDetailVO;
import com.hundunlab.site.module.software.pojo.enums.PackageType;
import com.hundunlab.site.module.software.service.AdminSoftwareService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class AdminSoftwareServiceImpl implements AdminSoftwareService {

    private static final Set<String> PLATFORMS = Set.of("win32", "darwin", "linux");
    private static final Set<String> ARCHS = Set.of("x64", "arm64");
    private static final Set<String> REPO_TYPES = Set.of("source", "mirror", "release", "docs");

    private final SoftwareAdminMapper adminMapper;
    private final SoftwareMapper softwareMapper;
    private final AliOSSUtil ossUtil;

    /** 安装包上限，与 UploadSizeGuardFilter 共用同一份配置（D102） */
    private final long assetMaxBytes;

    public AdminSoftwareServiceImpl(SoftwareAdminMapper adminMapper,
                                    SoftwareMapper softwareMapper,
                                    AliOSSUtil ossUtil,
                                    @Value("${app.upload.asset-max-size-bytes:2147483648}") long assetMaxBytes) {
        this.adminMapper = adminMapper;
        this.softwareMapper = softwareMapper;
        this.ossUtil = ossUtil;
        this.assetMaxBytes = assetMaxBytes;
    }

    // ==================== 软件 ====================

    @Override
    public List<SoftwareAdminSummaryVO> listSoftware(String keyword, Integer status) {
        return adminMapper.selectSummaries(trimToNull(keyword), status);
    }

    @Override
    public SoftwareAdminDetailVO getSoftwareDetail(Long id) {
        Software s = requireSoftware(id);

        SoftwareAdminDetailVO vo = new SoftwareAdminDetailVO();
        vo.setId(s.getId());
        vo.setSlug(s.getSlug());
        vo.setNameZh(s.getNameZh());
        vo.setNameEn(s.getNameEn());
        vo.setTaglineZh(s.getTaglineZh());
        vo.setTaglineEn(s.getTaglineEn());
        vo.setIntroZh(s.getIntroZh());
        vo.setIntroEn(s.getIntroEn());
        vo.setTechStack(splitTechStack(s.getTechStack()));
        vo.setLicense(s.getLicense());
        vo.setCoverUrl(s.getCoverUrl());
        vo.setThemeColor(s.getThemeColor());
        vo.setStatus(s.getStatus());
        vo.setSortOrder(s.getSortOrder());

        // 三个内容块的查询不区分发布状态（SoftwareMapper 的这三个方法本来就没带 status 过滤）
        vo.setFeatures(softwareMapper.selectFeatures(id).stream().map(f -> {
            SoftwareDetailVO.FeatureVO fv = new SoftwareDetailVO.FeatureVO();
            fv.setIcon(f.getIcon());
            fv.setTitleZh(f.getTitleZh());
            fv.setTitleEn(f.getTitleEn());
            fv.setDescZh(f.getDescZh());
            fv.setDescEn(f.getDescEn());
            return fv;
        }).toList());

        vo.setScreenshots(softwareMapper.selectScreenshots(id).stream().map(sc -> {
            SoftwareDetailVO.ScreenshotVO sv = new SoftwareDetailVO.ScreenshotVO();
            sv.setImageUrl(sc.getImageUrl());
            sv.setCaptionZh(sc.getCaptionZh());
            sv.setCaptionEn(sc.getCaptionEn());
            return sv;
        }).toList());

        vo.setPlatforms(softwareMapper.selectPlatforms(id).stream().map(p -> {
            SoftwareDetailVO.PlatformVO pv = new SoftwareDetailVO.PlatformVO();
            pv.setPlatform(p.getPlatform());
            pv.setNoteZh(p.getNoteZh());
            pv.setNoteEn(p.getNoteEn());
            return pv;
        }).toList());

        vo.setRepos(adminMapper.selectRepos(id).stream().map(r -> {
            SoftwareAdminDetailVO.RepoVO rv = new SoftwareAdminDetailVO.RepoVO();
            rv.setLabelZh(r.getLabelZh());
            rv.setLabelEn(r.getLabelEn());
            rv.setUrl(r.getUrl());
            rv.setRepoType(r.getRepoType());
            return rv;
        }).toList());

        vo.setReleases(adminMapper.selectAllReleases(id).stream().map(r -> {
            SoftwareAdminDetailVO.ReleaseVO rv = new SoftwareAdminDetailVO.ReleaseVO();
            rv.setId(r.getId());
            rv.setVersion(r.getVersion());
            rv.setChannel(r.getChannel());
            rv.setNotesZh(r.getNotesZh());
            rv.setNotesEn(r.getNotesEn());
            rv.setPublishedAt(r.getPublishedAt());
            rv.setStatus(r.getStatus());
            rv.setAssets(adminMapper.selectAssets(r.getId()).stream().map(a -> {
                SoftwareAdminDetailVO.AssetVO av = new SoftwareAdminDetailVO.AssetVO();
                av.setId(a.getId());
                av.setPlatform(a.getPlatform());
                av.setArch(a.getArch());
                av.setPackageType(a.getPackageType());
                av.setFileName(a.getFileName());
                av.setFileSize(a.getFileSize());
                av.setSha256(a.getSha256());
                av.setDownloadCount(a.getDownloadCount());
                return av;
            }).toList());
            return rv;
        }).toList());

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSoftware(SoftwareUpsertDTO dto) {
        String slug = dto.getSlug().trim();
        if (adminMapper.countBySlug(slug, null) > 0) {
            throw new BizException(ErrorCode.SOFTWARE_SLUG_EXISTS);
        }

        Software s = new Software();
        applyDto(s, dto, slug);
        // 新建一律草稿：F12 的「发布」是独立动作，不做「建完就上线」
        s.setStatus(0);
        adminMapper.insertSoftware(s);

        replaceBlocks(s.getId(), dto);
        log.info("后台新建软件 id={} slug={}", s.getId(), slug);
        return s.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSoftware(Long id, SoftwareUpsertDTO dto) {
        Software existing = requireSoftware(id);
        String slug = dto.getSlug().trim();
        if (adminMapper.countBySlug(slug, id) > 0) {
            throw new BizException(ErrorCode.SOFTWARE_SLUG_EXISTS);
        }

        Software s = new Software();
        s.setId(existing.getId());
        applyDto(s, dto, slug);
        // status 不在 updateSoftware 的 SQL 里：上下线只走 changeSoftwareStatus
        adminMapper.updateSoftware(s);

        replaceBlocks(id, dto);
        log.info("后台更新软件 id={} slug={}", id, slug);
    }

    @Override
    public void changeSoftwareStatus(Long id, Integer status) {
        requireSoftware(id);
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "状态只能是 0 或 1");
        }
        adminMapper.updateStatus(id, status);
        log.info("后台改软件发布态 id={} status={}", id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSoftware(Long id) {
        requireSoftware(id);

        // 扩展有自己的 zip 对象，静默级联会留下清不掉的孤儿；要求先处理扩展
        int extensionCount = adminMapper.countExtensions(id);
        if (extensionCount > 0) {
            throw new BizException(ErrorCode.SOFTWARE_HAS_EXTENSIONS,
                    "该软件下还有 " + extensionCount + " 个扩展，请先删除扩展再删软件");
        }

        // D103：先删 OSS（幂等）再删库。反过来的话 object_key 随行消失，对象就永远找不回来了
        List<String> objectKeys = adminMapper.selectObjectKeysBySoftwareId(id);
        deleteObjects(objectKeys, "softwareId=" + id);

        adminMapper.deleteById(id);
        log.info("后台删除软件 id={}，连带 {} 个安装包对象", id, objectKeys.size());
    }

    // ==================== 版本 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRelease(Long softwareId, ReleaseUpsertDTO dto) {
        requireSoftware(softwareId);
        String version = dto.getVersion().trim();
        String channel = dto.getChannel().trim().toLowerCase(Locale.ROOT);
        if (adminMapper.countReleaseVersion(softwareId, version, channel, null) > 0) {
            throw new BizException(ErrorCode.RELEASE_VERSION_EXISTS);
        }

        SoftwareRelease r = new SoftwareRelease();
        r.setSoftwareId(softwareId);
        r.setVersion(version);
        r.setChannel(channel);
        r.setNotesZh(dto.getNotesZh());
        r.setNotesEn(dto.getNotesEn());
        r.setStatus(dto.getStatus() == null ? 0 : dto.getStatus());
        r.setPublishedAt(resolvePublishedAt(r.getStatus(), dto.getPublishedAt(), null));

        adminMapper.insertRelease(r);
        log.info("后台新建版本 softwareId={} version={} channel={}", softwareId, version, channel);
        return r.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRelease(Long softwareId, Long releaseId, ReleaseUpsertDTO dto) {
        SoftwareRelease existing = requireRelease(softwareId, releaseId);
        String version = dto.getVersion().trim();
        String channel = dto.getChannel().trim().toLowerCase(Locale.ROOT);
        if (adminMapper.countReleaseVersion(softwareId, version, channel, releaseId) > 0) {
            throw new BizException(ErrorCode.RELEASE_VERSION_EXISTS);
        }

        SoftwareRelease r = new SoftwareRelease();
        r.setId(existing.getId());
        r.setVersion(version);
        r.setChannel(channel);
        r.setNotesZh(dto.getNotesZh());
        r.setNotesEn(dto.getNotesEn());
        r.setStatus(dto.getStatus() == null ? existing.getStatus() : dto.getStatus());
        r.setPublishedAt(resolvePublishedAt(r.getStatus(), dto.getPublishedAt(), existing.getPublishedAt()));

        adminMapper.updateRelease(r);
        log.info("后台更新版本 softwareId={} releaseId={} version={} status={}",
                softwareId, releaseId, version, r.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRelease(Long softwareId, Long releaseId) {
        requireRelease(softwareId, releaseId);

        List<String> objectKeys = adminMapper.selectObjectKeysByReleaseId(releaseId);
        deleteObjects(objectKeys, "releaseId=" + releaseId);

        adminMapper.deleteRelease(releaseId);
        log.info("后台删除版本 softwareId={} releaseId={}，连带 {} 个安装包对象",
                softwareId, releaseId, objectKeys.size());
    }

    // ==================== 安装包资产 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long uploadAsset(Long softwareId, Long releaseId,
                            String platform, String arch, String packageType, MultipartFile file) {
        requireRelease(softwareId, releaseId);

        if (!StringUtils.hasText(platform) || !PLATFORMS.contains(platform.trim().toLowerCase(Locale.ROOT))) {
            throw new BizException(ErrorCode.BAD_REQUEST, "平台只能是 win32 / darwin / linux");
        }
        String normalizedArch = StringUtils.hasText(arch) ? arch.trim().toLowerCase(Locale.ROOT) : "x64";
        if (!ARCHS.contains(normalizedArch)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "架构只能是 x64 / arm64");
        }
        String normalizedPlatform = platform.trim().toLowerCase(Locale.ROOT);

        PackageType type = PackageType.of(packageType);
        if (type == null) {
            throw new BizException(ErrorCode.ASSET_PACKAGE_TYPE_INVALID);
        }
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "安装包文件不能为空");
        }
        if (assetMaxBytes > 0 && file.getSize() > assetMaxBytes) {
            throw new BizException(ErrorCode.FILE_TOO_LARGE,
                    "安装包不能超过 " + (assetMaxBytes / 1024 / 1024) + "MB");
        }
        String originalName = file.getOriginalFilename();
        if (!type.matchesFileName(originalName)) {
            throw new BizException(ErrorCode.FILE_TYPE_NOT_ALLOWED,
                    "所选格式 " + type.getCode() + " 需要 ." + type.getExtension() + " 文件");
        }
        if (adminMapper.countAssetVariant(releaseId, normalizedPlatform, normalizedArch, type.getCode()) > 0) {
            throw new BizException(ErrorCode.ASSET_VARIANT_EXISTS);
        }
        if (!ossUtil.isEnabled()) {
            throw new BizException(ErrorCode.OSS_NOT_CONFIGURED);
        }

        // 对象键带上软件/版本层级，便于按前缀排查；文件名用 UUID，避免中文名与覆盖写
        String objectKey = "software/" + softwareId + "/" + releaseId + "/"
                + UUID.randomUUID() + "." + type.extensionForObjectKey();

        String sha256 = streamToPrivateBucket(file, objectKey);

        ReleaseAsset asset = new ReleaseAsset();
        asset.setReleaseId(releaseId);
        asset.setPlatform(normalizedPlatform);
        asset.setArch(normalizedArch);
        asset.setPackageType(type.getCode());
        asset.setFileName(originalName);
        asset.setObjectKey(objectKey);
        asset.setFileSize(file.getSize());
        asset.setSha256(sha256);

        try {
            adminMapper.insertAsset(asset);
        } catch (RuntimeException e) {
            // 落库失败就把刚传上去的对象删掉，别留一个没人引用的包（F13 异常流程）
            safeDeletePrivate(objectKey);
            throw e;
        }

        log.info("安装包上传完成 softwareId={} releaseId={} variant={}/{}/{} size={} sha256={}",
                softwareId, releaseId, normalizedPlatform, normalizedArch, type.getCode(),
                file.getSize(), sha256);
        return asset.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAsset(Long softwareId, Long releaseId, Long assetId) {
        requireRelease(softwareId, releaseId);
        ReleaseAsset asset = adminMapper.selectAssetById(assetId);
        if (asset == null || !releaseId.equals(asset.getReleaseId())) {
            throw new BizException(ErrorCode.ASSET_NOT_FOUND);
        }

        deleteObjects(Collections.singletonList(asset.getObjectKey()), "assetId=" + assetId);
        adminMapper.deleteAssetById(assetId);
        log.info("后台删除安装包 softwareId={} releaseId={} assetId={}", softwareId, releaseId, assetId);
    }

    // ==================== 内部 ====================

    /**
     * 流式转存私有桶并同步算摘要。
     * <p>
     * 不落临时文件：OSS putObject 本身就是流式读，包一层 DigestInputStream 就能在
     * 同一遍读里拿到 sha256，2GiB 也不会把内存/磁盘撑爆。
     */
    private String streamToPrivateBucket(MultipartFile file, String objectKey) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            // JVM 一定带 SHA-256
            throw new IllegalStateException(e);
        }

        try (InputStream raw = file.getInputStream();
             DigestInputStream digestStream = new DigestInputStream(raw, digest)) {
            ossUtil.uploadToPrivate(objectKey, digestStream);
        } catch (OSSException e) {
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, e.getMessage());
        } catch (Exception e) {
            log.error("安装包上传失败：{}", objectKey, e);
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "安装包上传失败");
        }

        return HexFormat.of().formatHex(digest.digest());
    }

    /**
     * 逐个删私有桶对象。OSS 未配置且确实有待删对象时直接报错——
     * 这时候删库只会把 object_key 抹掉，对象就变成永远清不掉的孤儿（D103）。
     */
    private void deleteObjects(List<String> objectKeys, String scope) {
        if (objectKeys == null || objectKeys.isEmpty()) {
            return;
        }
        if (!ossUtil.isEnabled()) {
            throw new BizException(ErrorCode.OSS_NOT_CONFIGURED,
                    "对象存储未配置，无法删除已有的安装包对象");
        }
        for (String key : objectKeys) {
            if (!StringUtils.hasText(key)) {
                continue;
            }
            ossUtil.deletePrivate(key);
        }
        log.info("已删除 OSS 对象 {} 个（{}）", objectKeys.size(), scope);
    }

    private void safeDeletePrivate(String objectKey) {
        try {
            ossUtil.deletePrivate(objectKey);
        } catch (Exception e) {
            // 这里已经在异常路径上，清理失败只记日志，别把原始异常盖掉
            log.warn("清理未落库的 OSS 对象失败：{}", objectKey, e);
        }
    }

    private Software requireSoftware(Long id) {
        if (id == null) {
            throw new BizException(ErrorCode.SOFTWARE_NOT_FOUND);
        }
        Software s = adminMapper.selectById(id);
        if (s == null) {
            throw new BizException(ErrorCode.SOFTWARE_NOT_FOUND);
        }
        return s;
    }

    /** 版本必须存在且属于该软件，否则拿别的软件的 id 也能传包 */
    private SoftwareRelease requireRelease(Long softwareId, Long releaseId) {
        SoftwareRelease r = releaseId == null ? null : adminMapper.selectReleaseById(releaseId);
        if (r == null || !softwareId.equals(r.getSoftwareId())) {
            throw new BizException(ErrorCode.RELEASE_NOT_FOUND);
        }
        return r;
    }

    private void applyDto(Software s, SoftwareUpsertDTO dto, String slug) {
        s.setSlug(slug);
        s.setNameZh(dto.getNameZh().trim());
        s.setNameEn(dto.getNameEn().trim());
        s.setTaglineZh(trimToNull(dto.getTaglineZh()));
        s.setTaglineEn(trimToNull(dto.getTaglineEn()));
        s.setIntroZh(trimToNull(dto.getIntroZh()));
        s.setIntroEn(trimToNull(dto.getIntroEn()));
        s.setTechStack(joinTechStack(dto.getTechStack()));
        s.setLicense(trimToNull(dto.getLicense()));
        s.setCoverUrl(trimToNull(dto.getCoverUrl()));
        s.setThemeColor(trimToNull(dto.getThemeColor()));
        s.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
    }

    /**
     * 四类子块整块替换：先删后插，`sort_order` 按数组下标重排。
     * <p>
     * 不做逐条 diff 是刻意的——逐条要维护 id 映射、还要处理「同一行既改了又挪了位」，
     * 换来的只是少两次 DELETE，对一个软件十来条子项的场景毫无意义。
     */
    private void replaceBlocks(Long softwareId, SoftwareUpsertDTO dto) {
        adminMapper.deleteFeatures(softwareId);
        List<SoftwareUpsertDTO.FeatureItem> features = safe(dto.getFeatures());
        if (!features.isEmpty()) {
            List<SoftwareFeature> rows = new ArrayList<>(features.size());
            for (int i = 0; i < features.size(); i++) {
                SoftwareUpsertDTO.FeatureItem it = features.get(i);
                SoftwareFeature f = new SoftwareFeature();
                f.setSoftwareId(softwareId);
                f.setIcon(trimToNull(it.getIcon()));
                f.setTitleZh(it.getTitleZh().trim());
                f.setTitleEn(it.getTitleEn().trim());
                f.setDescZh(trimToNull(it.getDescZh()));
                f.setDescEn(trimToNull(it.getDescEn()));
                f.setSortOrder(i);
                rows.add(f);
            }
            adminMapper.insertFeatures(rows);
        }

        adminMapper.deleteScreenshots(softwareId);
        List<SoftwareUpsertDTO.ScreenshotItem> screenshots = safe(dto.getScreenshots());
        if (!screenshots.isEmpty()) {
            List<SoftwareScreenshot> rows = new ArrayList<>(screenshots.size());
            for (int i = 0; i < screenshots.size(); i++) {
                SoftwareUpsertDTO.ScreenshotItem it = screenshots.get(i);
                SoftwareScreenshot sc = new SoftwareScreenshot();
                sc.setSoftwareId(softwareId);
                sc.setImageUrl(it.getImageUrl().trim());
                sc.setCaptionZh(trimToNull(it.getCaptionZh()));
                sc.setCaptionEn(trimToNull(it.getCaptionEn()));
                sc.setSortOrder(i);
                rows.add(sc);
            }
            adminMapper.insertScreenshots(rows);
        }

        adminMapper.deletePlatforms(softwareId);
        List<SoftwareUpsertDTO.PlatformItem> platforms = safe(dto.getPlatforms());
        if (!platforms.isEmpty()) {
            List<SoftwarePlatform> rows = new ArrayList<>(platforms.size());
            for (int i = 0; i < platforms.size(); i++) {
                SoftwareUpsertDTO.PlatformItem it = platforms.get(i);
                SoftwarePlatform p = new SoftwarePlatform();
                p.setSoftwareId(softwareId);
                p.setPlatform(it.getPlatform().trim().toLowerCase(Locale.ROOT));
                p.setNoteZh(trimToNull(it.getNoteZh()));
                p.setNoteEn(trimToNull(it.getNoteEn()));
                p.setSortOrder(i);
                rows.add(p);
            }
            adminMapper.insertPlatforms(rows);
        }

        adminMapper.deleteRepos(softwareId);
        List<SoftwareUpsertDTO.RepoItem> repos = safe(dto.getRepos());
        if (!repos.isEmpty()) {
            List<SoftwareRepo> rows = new ArrayList<>(repos.size());
            Set<String> seenUrls = new HashSet<>();
            for (int i = 0; i < repos.size(); i++) {
                SoftwareUpsertDTO.RepoItem it = repos.get(i);
                String repoType = it.getRepoType().trim().toLowerCase(Locale.ROOT);
                if (!REPO_TYPES.contains(repoType)) {
                    throw new BizException(ErrorCode.BAD_REQUEST,
                            "不支持的仓库类型：" + it.getRepoType());
                }
                String url = it.getUrl().trim();
                if (!seenUrls.add(url)) {
                    // 库里有 UNIQUE(software_id, url)，先在这里给一句人话，别让用户看 500
                    throw new BizException(ErrorCode.BAD_REQUEST, "仓库地址重复：" + url);
                }
                SoftwareRepo r = new SoftwareRepo();
                r.setSoftwareId(softwareId);
                r.setLabelZh(it.getLabelZh().trim());
                r.setLabelEn(it.getLabelEn().trim());
                r.setUrl(url);
                r.setRepoType(repoType);
                r.setSortOrder(i);
                rows.add(r);
            }
            adminMapper.insertRepos(rows);
        }
    }

    /**
     * 发布态由 0 变 1 且没给发布时间时补当前时间。
     * 不补的话「已发布但没有 published_at」既进不了前台的倒序推导（D064），
     * 又会让「最新版」莫名其妙是空的。
     */
    private LocalDateTime resolvePublishedAt(Integer status, LocalDateTime requested, LocalDateTime existing) {
        if (requested != null) {
            return requested;
        }
        if (status != null && status == 1) {
            return existing != null ? existing : LocalDateTime.now();
        }
        return existing;
    }

    private String joinTechStack(List<String> techStack) {
        if (techStack == null || techStack.isEmpty()) {
            return null;
        }
        String joined = techStack.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        return joined.isEmpty() ? null : joined;
    }

    private List<String> splitTechStack(String techStack) {
        if (!StringUtils.hasText(techStack)) {
            return Collections.emptyList();
        }
        return Arrays.stream(techStack.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private <T> List<T> safe(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }
}
