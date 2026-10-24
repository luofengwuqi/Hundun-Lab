package com.hundunlab.site.module.software.service.serviceimpl;

import com.hundunlab.site.common.exception.MyException.BizException;
import com.hundunlab.site.common.response.ErrorCode;
import com.hundunlab.site.module.software.mapper.ReleaseMapper;
import com.hundunlab.site.module.software.mapper.SoftwareMapper;
import com.hundunlab.site.module.software.pojo.Entity.ReleaseAsset;
import com.hundunlab.site.module.software.pojo.Entity.Software;
import com.hundunlab.site.module.software.pojo.Entity.SoftwarePlatform;
import com.hundunlab.site.module.software.pojo.Entity.SoftwareRelease;
import com.hundunlab.site.module.software.pojo.VO.SoftwareDetailVO;
import com.hundunlab.site.module.software.pojo.VO.SoftwareDownloadStat;
import com.hundunlab.site.module.software.pojo.VO.SoftwareSummaryVO;
import com.hundunlab.site.module.software.service.SoftwareService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SoftwareServiceImpl implements SoftwareService {

    private final SoftwareMapper softwareMapper;
    private final ReleaseMapper releaseMapper;

    public SoftwareServiceImpl(SoftwareMapper softwareMapper, ReleaseMapper releaseMapper) {
        this.softwareMapper = softwareMapper;
        this.releaseMapper = releaseMapper;
    }

    @Override
    public List<SoftwareSummaryVO> listPublished() {
        List<Software> list = softwareMapper.selectPublished();
        if (list.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> ids = list.stream().map(Software::getId).toList();

        // 三个维度各查一次，避免「每个软件查一遍平台、查一遍版本、查一遍下载量」
        Map<Long, List<String>> platformMap = softwareMapper.selectPlatformsBySoftwareIds(ids).stream()
                .collect(Collectors.groupingBy(SoftwarePlatform::getSoftwareId,
                        Collectors.mapping(SoftwarePlatform::getPlatform, Collectors.toList())));
        Map<Long, SoftwareRelease> latestMap = releaseMapper.selectLatestPublishedBySoftwareIds(ids).stream()
                .collect(Collectors.toMap(SoftwareRelease::getSoftwareId, Function.identity(), (a, b) -> a));
        Map<Long, Long> downloadMap = releaseMapper.selectDownloadTotals(ids).stream()
                .collect(Collectors.toMap(SoftwareDownloadStat::getSoftwareId,
                        stat -> stat.getTotal() == null ? 0L : stat.getTotal(), (a, b) -> a));

        List<SoftwareSummaryVO> result = new ArrayList<>(list.size());
        for (Software s : list) {
            SoftwareSummaryVO vo = new SoftwareSummaryVO();
            vo.setSlug(s.getSlug());
            vo.setNameZh(s.getNameZh());
            vo.setNameEn(s.getNameEn());
            vo.setTaglineZh(s.getTaglineZh());
            vo.setTaglineEn(s.getTaglineEn());
            vo.setCoverUrl(s.getCoverUrl());
            vo.setThemeColor(s.getThemeColor());
            vo.setPlatforms(platformMap.getOrDefault(s.getId(), Collections.emptyList()));
            SoftwareRelease latest = latestMap.get(s.getId());
            if (latest != null) {
                vo.setLatestVersion(latest.getVersion());
                vo.setLatestPublishedAt(latest.getPublishedAt());
            }
            vo.setTotalDownloads(downloadMap.getOrDefault(s.getId(), 0L));
            result.add(vo);
        }
        return result;
    }

    @Override
    public SoftwareDetailVO getDetail(String slug) {
        Software software = softwareMapper.selectPublishedBySlug(slug);
        if (software == null) {
            throw new BizException(ErrorCode.SOFTWARE_NOT_FOUND);
        }

        SoftwareDetailVO vo = new SoftwareDetailVO();
        vo.setSlug(software.getSlug());
        vo.setNameZh(software.getNameZh());
        vo.setNameEn(software.getNameEn());
        vo.setTaglineZh(software.getTaglineZh());
        vo.setTaglineEn(software.getTaglineEn());
        vo.setIntroZh(software.getIntroZh());
        vo.setIntroEn(software.getIntroEn());
        vo.setTechStack(splitTechStack(software.getTechStack()));
        vo.setRepoUrl(software.getRepoUrl());
        vo.setLicense(software.getLicense());
        vo.setCoverUrl(software.getCoverUrl());
        vo.setThemeColor(software.getThemeColor());

        vo.setFeatures(softwareMapper.selectFeatures(software.getId()).stream().map(f -> {
            SoftwareDetailVO.FeatureVO fv = new SoftwareDetailVO.FeatureVO();
            fv.setIcon(f.getIcon());
            fv.setTitleZh(f.getTitleZh());
            fv.setTitleEn(f.getTitleEn());
            fv.setDescZh(f.getDescZh());
            fv.setDescEn(f.getDescEn());
            return fv;
        }).toList());

        vo.setScreenshots(softwareMapper.selectScreenshots(software.getId()).stream().map(s -> {
            SoftwareDetailVO.ScreenshotVO sv = new SoftwareDetailVO.ScreenshotVO();
            sv.setImageUrl(s.getImageUrl());
            sv.setCaptionZh(s.getCaptionZh());
            sv.setCaptionEn(s.getCaptionEn());
            return sv;
        }).toList());

        vo.setPlatforms(softwareMapper.selectPlatforms(software.getId()).stream().map(p -> {
            SoftwareDetailVO.PlatformVO pv = new SoftwareDetailVO.PlatformVO();
            pv.setPlatform(p.getPlatform());
            pv.setNoteZh(p.getNoteZh());
            pv.setNoteEn(p.getNoteEn());
            return pv;
        }).toList());

        List<SoftwareRelease> releases = releaseMapper.selectPublishedBySoftwareId(software.getId());
        if (!releases.isEmpty()) {
            List<Long> releaseIds = releases.stream().map(SoftwareRelease::getId).toList();
            Map<Long, List<ReleaseAsset>> assetMap = releaseMapper.selectAssetsByReleaseIds(releaseIds).stream()
                    .collect(Collectors.groupingBy(ReleaseAsset::getReleaseId));
            vo.setReleases(releases.stream().map(r -> {
                SoftwareDetailVO.ReleaseVO rv = new SoftwareDetailVO.ReleaseVO();
                rv.setId(r.getId());
                rv.setVersion(r.getVersion());
                rv.setChannel(r.getChannel());
                rv.setNotesZh(r.getNotesZh());
                rv.setNotesEn(r.getNotesEn());
                rv.setPublishedAt(r.getPublishedAt());
                rv.setAssets(assetMap.getOrDefault(r.getId(), Collections.emptyList()).stream().map(a -> {
                    SoftwareDetailVO.AssetVO av = new SoftwareDetailVO.AssetVO();
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
        } else {
            vo.setReleases(Collections.emptyList());
        }

        return vo;
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
}
