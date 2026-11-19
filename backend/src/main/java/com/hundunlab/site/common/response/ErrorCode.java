package com.hundunlab.site.common.response;

/**
 * 业务错误码。
 * <p>
 * 约定：
 * <ul>
 *   <li>{@code code} 形如 {@code MODULE_REASON}，全局唯一、稳定不变，前端据此查 i18n 文案</li>
 *   <li>{@code httpStatus} 仅用于设置响应状态码与日志聚合</li>
 *   <li>{@code defaultMessage} 是中文兜底，只用于日志与「前端没有对应文案」的场景，
 *       正常展示由前端根据 code 本地化，避免切换语言后残留中文</li>
 * </ul>
 */
public enum ErrorCode {

    // ==================== 通用 ====================
    BAD_REQUEST(400, "请求参数有误"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "没有权限执行该操作"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),
    RATE_LIMITED(429, "操作太频繁，请稍后再试"),
    INTERNAL_ERROR(500, "服务器开小差了，请稍后再试"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),

    // ==================== 认证 ====================
    AUTH_NOT_LOGGED_IN(401, "未登录"),
    AUTH_TOKEN_EXPIRED(401, "登录已过期，请重新登录"),
    AUTH_TOKEN_INVALID(401, "登录信息无效，请重新登录"),
    AUTH_ACCOUNT_DISABLED(403, "账号已被禁用"),
    AUTH_FORBIDDEN(403, "权限不足"),

    // ==================== 用户 ====================
    USER_NOT_FOUND(404, "用户不存在"),
    USER_ALREADY_EXISTS(409, "注册信息不可用，请更换后重试"),
    USER_PASSWORD_WRONG(400, "密码不正确"),
    USER_EMAIL_CODE_EXPIRED(400, "验证码已过期"),
    USER_EMAIL_CODE_WRONG(400, "验证码不正确"),
    USER_EMAIL_SEND_FAILED(500, "验证码发送失败，请稍后再试"),
    /** 管理员邀请码注册失败：码不存在、已被用掉都归这一条，不区分 */
    INVITE_CODE_INVALID(400, "邀请码无效或已被使用"),

    // ==================== 软件与版本 ====================
    SOFTWARE_NOT_FOUND(404, "软件不存在或尚未发布"),
    /** 后台新建/改 slug 时撞上已有行（uk_software_slug） */
    SOFTWARE_SLUG_EXISTS(409, "该 slug 已被占用"),
    /** 软件下还挂着扩展：扩展有自己的 OSS 对象，静默级联会留孤儿，所以拒绝并要求先处理扩展 */
    SOFTWARE_HAS_EXTENSIONS(409, "该软件下还有扩展，请先删除扩展再删软件"),
    RELEASE_NOT_FOUND(404, "版本不存在"),
    RELEASE_VERSION_EXISTS(409, "该版本号已存在"),
    ASSET_NOT_FOUND(404, "安装包不存在"),
    ASSET_VARIANT_EXISTS(409, "该平台/架构/格式的安装包已存在"),
    ASSET_PACKAGE_TYPE_INVALID(400, "不支持的安装包格式"),
    /** 扩展名与 packageType 声明的格式不一致，如选了 dmg 却传 .exe */
    FILE_TYPE_NOT_ALLOWED(400, "文件类型与所选安装包格式不匹配"),
    FILE_TOO_LARGE(413, "文件超过大小上限"),
    /** 装安装包的桶没配好：上传/删除都做不了，但软件资料本身仍可编辑（D102） */
    OSS_NOT_CONFIGURED(503, "对象存储未配置，文件相关操作不可用"),

    // ==================== 扩展（M2 站点侧） ====================
    EXTENSION_NOT_FOUND(404, "扩展不存在或尚未发布"),

    // ==================== 下载 ====================
    DOWNLOAD_ASSET_NOT_FOUND(404, "安装包不存在或尚未发布"),
    DOWNLOAD_STORAGE_UNAVAILABLE(503, "下载服务暂不可用"),
    DOWNLOAD_FILE_MISSING(404, "安装包文件缺失");

    private final int httpStatus;
    private final String defaultMessage;

    ErrorCode(int httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
