package com.hundunlab.site.module.extension.service.serviceimpl;

import com.hundunlab.site.common.exception.MyException.BizException;
import com.hundunlab.site.common.exception.MyException.OSSException;
import com.hundunlab.site.common.response.ErrorCode;
import com.hundunlab.site.common.util.AliOSSUtil;
import com.hundunlab.site.common.util.IpUtil;
import com.hundunlab.site.module.extension.mapper.ExtensionMapper;
import com.hundunlab.site.module.extension.pojo.Entity.Extension;
import com.hundunlab.site.module.extension.pojo.Entity.ExtensionRelease;
import com.hundunlab.site.module.extension.pojo.VO.ExtensionVO;
import com.hundunlab.site.module.extension.service.ExtensionService;
import com.hundunlab.site.module.software.pojo.Entity.Software;
import com.hundunlab.site.module.software.mapper.SoftwareMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

/**
 * 站点侧扩展查询与下载。作者自己维护的项目，权限口径：下载匿名可下
 * （extension_download 表注释），列表/详情本来就是公开页面内容。
 */
@Slf4j
@Service
public class ExtensionServiceImpl implements ExtensionService {

    /** 扩展包短链有效期：与安装包票据同一口径，够点开另存即可 */
    private static final int DOWNLOAD_TTL_SECONDS = 300;

    private static final int MAX_UA_LENGTH = 255;

    private final ExtensionMapper extensionMapper;
    private final SoftwareMapper softwareMapper;
    private final AliOSSUtil ossUtil;
    private final IpUtil ipUtil;

    public ExtensionServiceImpl(ExtensionMapper extensionMapper,
                                SoftwareMapper softwareMapper,
                                AliOSSUtil ossUtil,
                                IpUtil ipUtil) {
        this.extensionMapper = extensionMapper;
        this.softwareMapper = softwareMapper;
        this.ossUtil = ossUtil;
        this.ipUtil = ipUtil;
    }

    @Override
    public List<ExtensionVO> listPublished(String softwareSlug) {
        Software software = requirePublishedSoftware(softwareSlug);
        List<Extension> list = extensionMapper.selectPublishedBySoftwareSlug(software.getSlug());
        // 计数为 0 时前端整个分区不渲染（D073），这里返回空列表即可
        return list.stream().map(ext -> toVO(ext, false)).toList();
    }

    @Override
    public ExtensionVO getDetail(String softwareSlug, String idOrSlug) {
        Extension ext = requirePublishedExtension(softwareSlug, idOrSlug);
        ExtensionVO vo = toVO(ext, true);
        vo.setReleases(extensionMapper.selectPublishedReleases(ext.getId()).stream()
                .map(this::toReleaseVO)
                .toList());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String issueDownloadUrl(String softwareSlug, String idOrSlug, HttpServletRequest request) {
        Extension ext = requirePublishedExtension(softwareSlug, idOrSlug);
        ExtensionRelease latest = extensionMapper.selectLatestPublishedRelease(ext.getId());
        if (latest == null) {
            // 没有任何已发布版本，等于这个扩展暂时没有可下载的东西
            throw new BizException(ErrorCode.EXTENSION_NOT_FOUND);
        }
        if (!StringUtils.hasText(latest.getObjectKey())) {
            throw new BizException(ErrorCode.DOWNLOAD_FILE_MISSING);
        }
        if (!ossUtil.isEnabled()) {
            log.warn("扩展下载请求失败：OSS 未配置，extensionId={}", ext.getId());
            throw new BizException(ErrorCode.DOWNLOAD_STORAGE_UNAVAILABLE);
        }

        String url;
        try {
            url = ossUtil.presignedDownloadUrl(latest.getObjectKey(), DOWNLOAD_TTL_SECONDS,
                    ext.getSlug() + "-" + latest.getVersion() + ".zip");
        } catch (OSSException e) {
            throw new BizException(ErrorCode.DOWNLOAD_STORAGE_UNAVAILABLE, e.getMessage());
        }

        // 匿名可下：不关联用户，IP 只存哈希（schema 注释口径）
        extensionMapper.insertDownload(ext.getId(), latest.getId(),
                sha256Hex(ipUtil.getClientIp(request)),
                truncate(request.getHeader("User-Agent"), MAX_UA_LENGTH));
        // 原子自增，避免并发下载时计数丢更新
        extensionMapper.incrementDownloadCount(ext.getId());

        log.info("签发扩展下载短链 slug={} version={}", ext.getSlug(), latest.getVersion());
        return url;
    }

    private Software requirePublishedSoftware(String softwareSlug) {
        Software software = softwareMapper.selectPublishedBySlug(softwareSlug);
        if (software == null) {
            throw new BizException(ErrorCode.SOFTWARE_NOT_FOUND);
        }
        return software;
    }

    private Extension requirePublishedExtension(String softwareSlug, String idOrSlug) {
        Software software = requirePublishedSoftware(softwareSlug);
        Extension ext;
        if (idOrSlug.chars().allMatch(Character::isDigit)) {
            ext = extensionMapper.selectPublishedBySoftwareAndId(software.getId(), Long.parseLong(idOrSlug));
            if (ext == null && idOrSlug.length() <= 64) {
                // 数字串也可能是 URL slug；两个入口都试，仍然没有才是真 404
                ext = extensionMapper.selectPublishedBySoftwareAndSlug(software.getId(), idOrSlug);
            }
        } else {
            ext = extensionMapper.selectPublishedBySoftwareAndSlug(software.getId(), idOrSlug);
        }
        if (ext == null) {
            throw new BizException(ErrorCode.EXTENSION_NOT_FOUND);
        }
        return ext;
    }

    private ExtensionVO toVO(Extension ext, boolean withDetailFields) {
        ExtensionVO vo = new ExtensionVO();
        vo.setId(ext.getId());
        vo.setPluginId(ext.getPluginId());
        vo.setSlug(ext.getSlug());
        vo.setNameZh(ext.getNameZh());
        vo.setNameEn(ext.getNameEn());
        vo.setSummaryZh(ext.getSummaryZh());
        vo.setSummaryEn(ext.getSummaryEn());
        vo.setAuthorType(ext.getAuthorType());
        vo.setAuthorName(ext.getAuthorName());
        vo.setPermissions(splitCsv(ext.getPermissions()));
        vo.setPlatforms(splitCsv(ext.getPlatform()));
        // 官方扩展随桌面端预置（D083），前端「内置」标记由此推导；schema 不加 bundled 列
        vo.setBundled("official".equals(ext.getAuthorType()));
        vo.setFeatured(ext.getFeatured());
        vo.setDownloadCount(ext.getDownloadCount());
        vo.setLicense(ext.getLicense());
        vo.setRepoUrl(ext.getRepoUrl());
        vo.setUpdatedAt(ext.getUpdatedAt());
        if (withDetailFields) {
            vo.setIconUrl(ext.getIconUrl());
            vo.setThemeColor(ext.getThemeColor());
            vo.setReadmeUrl(ext.getReadmeUrl());
        }
        return vo;
    }

    private ExtensionVO.ReleaseVO toReleaseVO(ExtensionRelease release) {
        ExtensionVO.ReleaseVO vo = new ExtensionVO.ReleaseVO();
        vo.setId(release.getId());
        vo.setVersion(release.getVersion());
        vo.setChangelogZh(release.getChangelogZh());
        vo.setChangelogEn(release.getChangelogEn());
        vo.setFileSize(release.getFileSize());
        vo.setDownloadCount(release.getDownloadCount());
        vo.setPublishedAt(release.getPublishedAt());
        return vo;
    }

    private List<String> splitCsv(String value) {
        if (!StringUtils.hasText(value)) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String sha256Hex(String value) {
        if (value == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
