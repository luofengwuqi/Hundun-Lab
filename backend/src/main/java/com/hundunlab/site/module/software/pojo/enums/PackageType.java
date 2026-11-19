package com.hundunlab.site.module.software.pojo.enums;

import java.util.Locale;

/**
 * 安装包格式与文件扩展名的对应关系（D102）。
 * <p>
 * 后台声明「这是 dmg」却传了 `.exe` 时必须拒绝：`file_name` 是给用户看的，
 * 用户按它决定双击还是解压，名字与内容不符比传错包更糟。
 * <p>
 * 注意 {@link #zip} 只用于安装包格式，**不是**通用上传白名单的一部分——
 * `POST /files` 依然拒绝 zip（D102）。
 */
public enum PackageType {

    NSIS("nsis", "exe"),
    MSI("msi", "msi"),
    APPIMAGE("appimage", "appimage"),
    DMG("dmg", "dmg"),
    DEB("deb", "deb"),
    ZIP("zip", "zip");

    private final String code;
    private final String extension;

    PackageType(String code, String extension) {
        this.code = code;
        this.extension = extension;
    }

    public String getCode() {
        return code;
    }

    public String getExtension() {
        return extension;
    }

    /**
     * @return 匹配的枚举；不认识返回 null（调用方据此报「不支持的安装包格式」）
     */
    public static PackageType of(String code) {
        if (code == null) {
            return null;
        }
        String normalized = code.trim().toLowerCase(Locale.ROOT);
        for (PackageType type : values()) {
            if (type.code.equals(normalized)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 文件名后缀是否与格式一致。比较时统一小写，所以 `.AppImage` 与 `.appimage` 都算对。
     */
    public boolean matchesFileName(String fileName) {
        if (fileName == null) {
            return false;
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return false;
        }
        String actual = fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        return actual.equals(extension);
    }

    /**
     * 对象键里用的扩展名（保持小写，下载时浏览器靠 file_name 还原真实名字）
     */
    public String extensionForObjectKey() {
        return extension;
    }
}
