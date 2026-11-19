package com.hundunlab.site.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

/**
 * 上传文件校验：类型白名单 + 大小限制 + 图片魔数校验。
 * <p>
 * 只服务「图片 / 文档」两类通用文件（头像、封面、截图、反馈附件）。
 * <b>安装包不走这里</b>：它的合法后缀由 {@code PackageType} 决定（nsis→exe、dmg→dmg…），
 * 上限也是另一套配置（D102），两条通道刻意分开，避免互相放宽。
 */
@Component
public class FileUploadValidator {

    /**
     * 文件用途。用枚举而不是 {@code boolean imageOnly}：调用点 {@code validate(file, true)}
     * 读不出语义，也挡不住把两个布尔参数写反。
     */
    public enum FileKind {
        /** 只允许图片，额外校验魔数 */
        IMAGE,
        /** 只允许文档类（pdf/txt/md/office），不含图片 */
        DOCUMENT,
        /** 图片与文档都收（通用附件上传口 `/files`）；图片仍校验魔数 */
        ANY
    }

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of(
            "pdf", "txt", "md", "doc", "docx", "xls", "xlsx", "ppt", "pptx");
    private static final Set<String> ANY_EXTENSIONS = union(IMAGE_EXTENSIONS, DOCUMENT_EXTENSIONS);

    private static Set<String> union(Set<String> a, Set<String> b) {
        Set<String> merged = new java.util.HashSet<>(a);
        merged.addAll(b);
        return Set.copyOf(merged);
    }

    private final long maxSizeBytes;

    public FileUploadValidator(@Value("${app.upload.max-size-bytes:10485760}") long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }

    /**
     * 校验上传文件，不合法时抛出 IllegalArgumentException（由全局异常处理器返回 400）
     *
     * @param file 上传文件
     * @param kind 用途，决定可用后缀
     */
    public void validate(MultipartFile file, FileKind kind) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (maxSizeBytes > 0 && file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("文件大小不能超过 " + (maxSizeBytes / 1024 / 1024) + "MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            throw new IllegalArgumentException("文件缺少扩展名");
        }

        String extension = originalFilename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        Set<String> allowed = switch (kind) {
            case IMAGE -> IMAGE_EXTENSIONS;
            case DOCUMENT -> DOCUMENT_EXTENSIONS;
            case ANY -> ANY_EXTENSIONS;
        };
        if (!allowed.contains(extension)) {
            throw new IllegalArgumentException("不支持的文件类型：" + extension);
        }

        if (IMAGE_EXTENSIONS.contains(extension) && !matchesImageSignature(file, extension)) {
            throw new IllegalArgumentException("文件内容与扩展名不匹配");
        }
    }

    private boolean matchesImageSignature(MultipartFile file, String extension) {
        byte[] header = new byte[12];
        int read;
        try (InputStream inputStream = file.getInputStream()) {
            read = inputStream.read(header);
        } catch (IOException e) {
            throw new IllegalArgumentException("读取文件失败", e);
        }
        if (read < 4) {
            return false;
        }

        return switch (extension) {
            case "jpg", "jpeg" -> (header[0] & 0xFF) == 0xFF
                    && (header[1] & 0xFF) == 0xD8
                    && (header[2] & 0xFF) == 0xFF;
            case "png" -> (header[0] & 0xFF) == 0x89
                    && header[1] == 0x50
                    && header[2] == 0x4E
                    && header[3] == 0x47;
            case "gif" -> header[0] == 'G' && header[1] == 'I' && header[2] == 'F' && header[3] == '8';
            case "bmp" -> header[0] == 'B' && header[1] == 'M';
            case "webp" -> read >= 12
                    && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
            default -> false;
        };
    }
}
