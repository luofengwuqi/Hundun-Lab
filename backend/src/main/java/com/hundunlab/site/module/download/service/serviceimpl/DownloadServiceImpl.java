package com.hundunlab.site.module.download.service.serviceimpl;

import com.hundunlab.site.common.context.BaseContext;
import com.hundunlab.site.common.exception.MyException.BizException;
import com.hundunlab.site.common.exception.MyException.OSSException;
import com.hundunlab.site.common.response.ErrorCode;
import com.hundunlab.site.common.util.AliOSSUtil;
import com.hundunlab.site.common.util.IpUtil;
import com.hundunlab.site.module.download.mapper.DownloadRecordMapper;
import com.hundunlab.site.module.download.pojo.Entity.DownloadRecord;
import com.hundunlab.site.module.download.pojo.VO.DownloadTicketVO;
import com.hundunlab.site.module.download.service.DownloadService;
import com.hundunlab.site.module.software.mapper.ReleaseMapper;
import com.hundunlab.site.module.software.pojo.Entity.ReleaseAsset;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
public class DownloadServiceImpl implements DownloadService {

    /** 票据有效期：够用户点开另存为，又不至于被复制出去长期分发 */
    private static final int TICKET_TTL_SECONDS = 300;

    private static final int MAX_UA_LENGTH = 255;

    private final ReleaseMapper releaseMapper;
    private final DownloadRecordMapper downloadRecordMapper;
    private final AliOSSUtil ossUtil;
    private final IpUtil ipUtil;

    public DownloadServiceImpl(ReleaseMapper releaseMapper,
                               DownloadRecordMapper downloadRecordMapper,
                               AliOSSUtil ossUtil,
                               IpUtil ipUtil) {
        this.releaseMapper = releaseMapper;
        this.downloadRecordMapper = downloadRecordMapper;
        this.ossUtil = ossUtil;
        this.ipUtil = ipUtil;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DownloadTicketVO issueTicket(Long assetId, HttpServletRequest request) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            // 拦截器已挡住匿名请求；这里是兜底，防止将来绕过拦截器调用本方法
            throw new BizException(ErrorCode.AUTH_NOT_LOGGED_IN);
        }

        // 只认「已发布软件 + 已发布版本」下的资产，草稿包即使拿到 id 也签不出地址
        ReleaseAsset asset = releaseMapper.selectDownloadableAsset(assetId);
        if (asset == null) {
            throw new BizException(ErrorCode.DOWNLOAD_ASSET_NOT_FOUND);
        }
        if (!StringUtils.hasText(asset.getObjectKey())) {
            throw new BizException(ErrorCode.DOWNLOAD_FILE_MISSING);
        }
        if (!ossUtil.isEnabled()) {
            log.warn("下载请求失败：OSS 未配置，assetId={}", assetId);
            throw new BizException(ErrorCode.DOWNLOAD_STORAGE_UNAVAILABLE);
        }

        String url;
        try {
            url = ossUtil.presignedDownloadUrl(asset.getObjectKey(), TICKET_TTL_SECONDS, asset.getFileName());
        } catch (OSSException e) {
            // 对象存储抖动不应该把业务事务打成 500 堆栈，转成可识别的错误码
            throw new BizException(ErrorCode.DOWNLOAD_STORAGE_UNAVAILABLE, e.getMessage());
        }

        DownloadRecord record = new DownloadRecord();
        record.setUserId(userId);
        record.setSoftwareId(asset.getSoftwareId());
        record.setReleaseId(asset.getReleaseId());
        record.setAssetId(asset.getId());
        record.setPlatform(asset.getPlatform());
        record.setIpHash(sha256Hex(ipUtil.getClientIp(request)));
        record.setUserAgent(truncate(request.getHeader("User-Agent"), MAX_UA_LENGTH));
        downloadRecordMapper.insert(record);

        // 原子自增，避免并发下载时计数丢更新
        releaseMapper.incrementDownloadCount(asset.getId());

        DownloadTicketVO ticket = new DownloadTicketVO();
        ticket.setUrl(url);
        ticket.setFileName(asset.getFileName());
        ticket.setFileSize(asset.getFileSize());
        ticket.setSha256(asset.getSha256());
        ticket.setExpiresIn(TICKET_TTL_SECONDS);
        log.info("签发下载票据 user={} asset={} platform={}", userId, asset.getId(), asset.getPlatform());
        return ticket;
    }

    @Override
    public List<DownloadRecord> myDownloads(int limit) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new BizException(ErrorCode.AUTH_NOT_LOGGED_IN);
        }
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return downloadRecordMapper.selectRecentByUser(userId, safeLimit);
    }

    private String sha256Hex(String value) {
        if (value == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // JVM 一定带 SHA-256，这里不可能触发
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
