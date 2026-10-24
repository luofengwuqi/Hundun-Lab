package com.hundunlab.site.common.util;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.ResponseHeaderOverrides;
import com.hundunlab.site.common.exception.MyException.OSSException;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

/**
 * 阿里云OSS工具类
 * 复用同一个 OSSClient，避免每次上传/删除都重新建立 TCP 连接
 */
@Slf4j
@Component
public class AliOSSUtil {

    private final String bucketName;
    private final String domain;
    private final OSS ossClient;

    public AliOSSUtil(@Value("${aliyun.endpoint}") String endpoint,
                      @Value("${aliyun.access-key-id}") String accessKeyId,
                      @Value("${aliyun.access-key-secret}") String accessKeySecret,
                      @Value("${aliyun.bucket-name}") String bucketName,
                      @Value("${aliyun.domain}") String domain) {
        this.bucketName = bucketName;
        this.domain = domain;
        boolean configured = StringUtils.hasText(endpoint)
                && StringUtils.hasText(accessKeyId)
                && StringUtils.hasText(accessKeySecret)
                && StringUtils.hasText(bucketName);
        if (configured) {
            this.ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        } else {
            // 缺少 OSS 配置时不让应用启动失败：本地开发 / CI 不需要对象存储也能跑
            this.ossClient = null;
            log.warn("OSS 未配置（OSS_ENDPOINT / ALIYUN_ACCESS_KEY_ID / ALIYUN_ACCESS_KEY_SECRET / OSS_BUCKET），"
                    + "对象存储相关能力将不可用");
        }
    }

    /**
     * 对象存储是否可用
     */
    public boolean isEnabled() {
        return ossClient != null;
    }

    @PreDestroy
    public void shutdown() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("OSS 客户端已关闭");
        }
    }

    /**
     * 生成唯一文件名
     */
    public String makeUUIDFileName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String suffix = "";
        if (originalFilename != null) {
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex >= 0) {
                suffix = originalFilename.substring(dotIndex);
            }
        }
        return UUID.randomUUID() + suffix;
    }

    /**
     * 上传文件到OSS
     * @param file 前端上传的文件
     * @param objectName OSS 对象名
     * @return 文件访问URL
     */
    public String upload(MultipartFile file, String objectName) throws IOException {
        requireEnabled();
        try (InputStream inputStream = file.getInputStream()) {
            ossClient.putObject(new PutObjectRequest(bucketName, objectName, inputStream));
            log.info("文件上传OSS成功：{}", objectName);
            return domain + "/" + objectName;
        } catch (Exception e) {
            log.error("文件上传OSS失败", e);
            throw new OSSException("文件上传失败");
        }
    }

    /**
     * 删除OSS文件
     * @param fileUrl 文件访问URL或objectName
     */
    public void delete(String fileUrl) {
        requireEnabled();
        String objectName = fileUrl;
        if (fileUrl.startsWith(domain + "/")) {
            objectName = fileUrl.replace(domain + "/", "");
        } else if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
            int domainEndIndex = fileUrl.indexOf("/", fileUrl.indexOf("//") + 2);
            if (domainEndIndex != -1) {
                objectName = fileUrl.substring(domainEndIndex + 1);
            }
        }

        try {
            ossClient.deleteObject(bucketName, objectName);
            log.info("文件删除OSS成功：{}", objectName);
        } catch (Exception e) {
            log.error("文件删除OSS失败", e);
            throw new OSSException("文件删除失败");
        }
    }

    /**
     * 为私有桶对象生成临时下载地址。
     * <p>
     * 安装包存放在私有读的桶里，任何直链都会被拒绝；这里签发一个短时效的预签名 URL，
     * 让浏览器直接从 OSS/CDN 拉取，不经过应用服务器中转。
     *
     * @param objectKey 对象键
     * @param seconds   有效期（秒），建议 300 左右，够用户点开另存即可
     * @param fileName  浏览器另存时显示的文件名，为空则用对象键
     * @return 预签名下载地址
     */
    public String presignedDownloadUrl(String objectKey, int seconds, String fileName) {
        requireEnabled();
        try {
            GeneratePresignedUrlRequest request =
                    new GeneratePresignedUrlRequest(bucketName, objectKey, HttpMethod.GET);
            request.setExpiration(new Date(System.currentTimeMillis() + seconds * 1000L));
            String dispositionName = StringUtils.hasText(fileName) ? fileName : objectKey;
            ResponseHeaderOverrides headers = new ResponseHeaderOverrides();
            headers.setContentDisposition("attachment; filename=\"" + dispositionName + "\"");
            request.setResponseHeaders(headers);
            URL url = ossClient.generatePresignedUrl(request);
            return url.toString();
        } catch (Exception e) {
            log.error("生成 OSS 预签名地址失败：{}", objectKey, e);
            throw new OSSException("下载地址生成失败");
        }
    }

    private void requireEnabled() {
        if (ossClient == null) {
            throw new OSSException("对象存储未配置，请联系管理员");
        }
    }
}
