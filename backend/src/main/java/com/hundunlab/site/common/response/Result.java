package com.hundunlab.site.common.response;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 接口统一返回结果。
 * <p>
 * 字段含义：
 * <ul>
 *   <li>{@code code}：HTTP 风格状态码，200 表示成功（保留旧口径，便于日志与网关判断）</li>
 *   <li>{@code errorCode}：稳定的业务错误码枚举名，失败时非空，前端据此本地化文案</li>
 *   <li>{@code message}：中文兜底文案，仅在 errorCode 缺失时展示，正常不直接呈现给用户</li>
 * </ul>
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;

    private String errorCode;

    private String message;

    private T data;

    private LocalDateTime timestamp;

    private Result() {
        this.timestamp = LocalDateTime.now();
    }

    // ==================== 成功 ====================

    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("OK");
        return result;
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = success();
        result.setData(data);
        return result;
    }

    public static <T> Result<T> success(String message, T data) {
        Result<T> result = success();
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    // ==================== 失败 ====================

    /**
     * 推荐用法：业务错误码优先
     */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        Result<T> result = new Result<>();
        result.setCode(errorCode.getHttpStatus());
        result.setErrorCode(errorCode.name());
        result.setMessage(errorCode.getDefaultMessage());
        return result;
    }

    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        Result<T> result = fail(errorCode);
        if (message != null && !message.isBlank()) {
            result.setMessage(message);
        }
        return result;
    }

    /**
     * 兼容旧调用：没有业务码时前端只能展示通用文案
     */
    public static <T> Result<T> fail(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> fail(int code, String message, T data) {
        Result<T> result = fail(code, message);
        result.setData(data);
        return result;
    }

    public static <T> Result<T> build(int code, String message, T data) {
        Result<T> result = fail(code, message);
        result.setData(data);
        return result;
    }
}
