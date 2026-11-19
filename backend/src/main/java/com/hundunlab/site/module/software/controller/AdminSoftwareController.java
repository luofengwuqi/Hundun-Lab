package com.hundunlab.site.module.software.controller;

import com.hundunlab.site.common.anno.RequireRole;
import com.hundunlab.site.common.response.Result;
import com.hundunlab.site.module.software.pojo.DTO.ReleaseUpsertDTO;
import com.hundunlab.site.module.software.pojo.DTO.SoftwareStatusDTO;
import com.hundunlab.site.module.software.pojo.DTO.SoftwareUpsertDTO;
import com.hundunlab.site.module.software.pojo.VO.SoftwareAdminDetailVO;
import com.hundunlab.site.module.software.pojo.VO.SoftwareAdminSummaryVO;
import com.hundunlab.site.module.software.service.AdminSoftwareService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 后台软件管理（F12 + F13）。
 * <p>
 * 类级 {@link RequireRole} 与拦截器的 {@code /admin} 前缀规则一致（都是 {@code status ≥ 2}），
 * 写在这里是为了「看控制器就知道谁能调」，不依赖读者去翻拦截器。
 * <p>
 * 安装包上传是**独立端点**，不经过 {@code POST /files}：后者是通用上传口（图片/文档，10MiB），
 * 让它兼职装 2GiB 的包等于把两个完全不同的限额和校验揉在一起（D102）。
 */
@Slf4j
@RequireRole
@RestController
@RequestMapping("/admin/software")
public class AdminSoftwareController {

    private final AdminSoftwareService adminSoftwareService;

    public AdminSoftwareController(AdminSoftwareService adminSoftwareService) {
        this.adminSoftwareService = adminSoftwareService;
    }

    // ==================== 软件 ====================

    /**
     * 列表（含草稿）。软件量级个位数，不分页。
     */
    @GetMapping
    public Result<List<SoftwareAdminSummaryVO>> list(@RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) Integer status) {
        return Result.success(adminSoftwareService.listSoftware(keyword, status));
    }

    @GetMapping("/{id}")
    public Result<SoftwareAdminDetailVO> detail(@PathVariable Long id) {
        return Result.success(adminSoftwareService.getSoftwareDetail(id));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody SoftwareUpsertDTO dto) {
        return Result.success("创建成功", adminSoftwareService.createSoftware(dto));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SoftwareUpsertDTO dto) {
        adminSoftwareService.updateSoftware(id, dto);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody SoftwareStatusDTO dto) {
        adminSoftwareService.changeSoftwareStatus(id, dto.getStatus());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminSoftwareService.deleteSoftware(id);
        return Result.success();
    }

    // ==================== 版本 ====================

    @PostMapping("/{id}/releases")
    public Result<Long> createRelease(@PathVariable Long id, @Valid @RequestBody ReleaseUpsertDTO dto) {
        return Result.success("创建成功", adminSoftwareService.createRelease(id, dto));
    }

    @PutMapping("/{id}/releases/{releaseId}")
    public Result<Void> updateRelease(@PathVariable Long id,
                                      @PathVariable Long releaseId,
                                      @Valid @RequestBody ReleaseUpsertDTO dto) {
        adminSoftwareService.updateRelease(id, releaseId, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}/releases/{releaseId}")
    public Result<Void> deleteRelease(@PathVariable Long id, @PathVariable Long releaseId) {
        adminSoftwareService.deleteRelease(id, releaseId);
        return Result.success();
    }

    // ==================== 安装包 ====================

    /**
     * 上传安装包。multipart，字段：platform / arch / packageType / file。
     * <p>
     * 体积在解析前已由 {@code UploadSizeGuardFilter} 按路径放过 2GiB 额度，
     * 这里再按业务配置与「扩展名 vs packageType」做第二道校验。
     */
    @PostMapping("/{id}/releases/{releaseId}/assets")
    public Result<Long> uploadAsset(@PathVariable Long id,
                                    @PathVariable Long releaseId,
                                    @RequestParam String platform,
                                    @RequestParam(required = false) String arch,
                                    @RequestParam String packageType,
                                    @RequestParam("file") MultipartFile file) {
        Long assetId = adminSoftwareService.uploadAsset(id, releaseId, platform, arch, packageType, file);
        return Result.success("上传成功", assetId);
    }

    @DeleteMapping("/{id}/releases/{releaseId}/assets/{assetId}")
    public Result<Void> deleteAsset(@PathVariable Long id,
                                    @PathVariable Long releaseId,
                                    @PathVariable Long assetId) {
        adminSoftwareService.deleteAsset(id, releaseId, assetId);
        return Result.success();
    }
}
