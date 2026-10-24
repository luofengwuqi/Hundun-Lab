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

    // ==================== 软件与版本 ====================
    SOFTWARE_NOT_FOUND(404, "软件不存在或尚未发布"),

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
