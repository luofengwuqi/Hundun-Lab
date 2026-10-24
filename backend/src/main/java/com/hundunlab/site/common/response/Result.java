package com.hundunlab.site.common.response;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 接口统一返回结果类
 * 支持序列化，适配各类响应场景
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应状态码（200成功，其他失败）
     */
    private int code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 响应时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 私有化构造方法，通过静态方法创建实例
     */
    private Result() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        return result;
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    /**
     * 成功响应（自定义消息+数据）
     */
    public static <T> Result<T> success(String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    /**
     * 失败响应（默认消息）
     */
    public static <T> Result<T> fail() {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage("操作失败");
        return result;
    }

    /**
     * 失败响应（自定义消息）
     */
    public static <T> Result<T> fail(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }

    /**
     * 失败响应（自定义状态码+消息）
     */
    public static <T> Result<T> fail(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    /**
     * 失败响应（自定义状态码+消息+数据）
     */
    public static <T> Result<T> fail(int code, String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    /**
     * 构建响应（全参数自定义）
     */
    public static <T> Result<T> build(int code, String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(data);
        return result;
    }
}