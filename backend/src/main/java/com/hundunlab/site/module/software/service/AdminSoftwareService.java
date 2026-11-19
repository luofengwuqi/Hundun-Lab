package com.hundunlab.site.module.software.service;

import com.hundunlab.site.module.software.pojo.DTO.ReleaseUpsertDTO;
import com.hundunlab.site.module.software.pojo.DTO.SoftwareUpsertDTO;
import com.hundunlab.site.module.software.pojo.VO.SoftwareAdminDetailVO;
import com.hundunlab.site.module.software.pojo.VO.SoftwareAdminSummaryVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 后台软件管理（F12 + F13）。
 * <p>
 * 与 {@link SoftwareService}（前台只读）分开：那边每条查询都带 {@code status = 1}，
 * 这边要看草稿、要写库。两个接口各自只装一种视角，避免「后台误用前台过滤」这类事故。
 */
public interface AdminSoftwareService {

    List<SoftwareAdminSummaryVO> listSoftware(String keyword, Integer status);

    SoftwareAdminDetailVO getSoftwareDetail(Long id);

    Long createSoftware(SoftwareUpsertDTO dto);

    void updateSoftware(Long id, SoftwareUpsertDTO dto);

    void changeSoftwareStatus(Long id, Integer status);

    void deleteSoftware(Long id);

    Long createRelease(Long softwareId, ReleaseUpsertDTO dto);

    void updateRelease(Long softwareId, Long releaseId, ReleaseUpsertDTO dto);

    void deleteRelease(Long softwareId, Long releaseId);

    /**
     * 上传安装包：流式转存私有桶，边传边算 sha256（D102）
     */
    Long uploadAsset(Long softwareId, Long releaseId,
                     String platform, String arch, String packageType, MultipartFile file);

    void deleteAsset(Long softwareId, Long releaseId, Long assetId);
}
