package com.hundunlab.site.common.exception;

import com.hundunlab.site.common.exception.MyException.*;
import com.hundunlab.site.common.response.ErrorCode;
import com.hundunlab.site.common.response.Result;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

/**
 * 全局异常出口。
 * <p>
 * 每个分支都返回稳定的 {@code errorCode}：前端按 code 查自己的文案，
 * 这里的 message 只作为日志与兜底，不承担展示职责。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    //业务异常：唯一能携带自定义错误码的出口
    @ExceptionHandler
    public Result<Void> handleBizException(BizException e) {
        log.warn("业务异常 [{}]: {}", e.getErrorCode(), e.getMessage());
        return Result.fail(e.getErrorCode(), e.getMessage());
    }

    //DTO属性异常处理
    @ExceptionHandler
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String msg = Objects.requireNonNull(e.getBindingResult().getFieldError()).getDefaultMessage();
        log.warn("DTO接收异常信息:{}", msg);
        return Result.fail(ErrorCode.BAD_REQUEST, msg);
    }

    //jwt过期：具体原因只写日志，不返回给客户端
    @ExceptionHandler
    public Result<Void> handleJwtExpiredException(ExpiredJwtException e) {
        log.warn("JWT 已过期: {}", e.getMessage());
        return Result.fail(ErrorCode.AUTH_TOKEN_EXPIRED);
    }

    @ExceptionHandler
    public Result<Void> handleJwtMalformedException(MalformedJwtException e) {
        log.warn("JWT 格式错误: {}", e.getMessage());
        return Result.fail(ErrorCode.AUTH_TOKEN_INVALID);
    }

    //其余 JWT 异常（签名错误、不支持的令牌等）
    @ExceptionHandler
    public Result<Void> handleJwtException(JwtException e) {
        log.warn("JWT 校验失败: {}", e.getMessage());
        return Result.fail(ErrorCode.AUTH_TOKEN_INVALID);
    }

    //用户异常
    @ExceptionHandler
    public Result<Void> handleUserNotFoundException(UserNotFoundException e) {
        log.warn("用户不存在: {}", e.getMessage());
        return Result.fail(ErrorCode.USER_NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler
    public Result<Void> handleUserExistedException(UserExistedException e) {
        log.warn("用户已存在: {}", e.getMessage());
        return Result.fail(ErrorCode.USER_ALREADY_EXISTS, e.getMessage());
    }

    //登录异常
    @ExceptionHandler
    public Result<Void> handleLoginException(LoginException e) {
        log.warn("登录异常: {}", e.getMessage());
        return Result.fail(ErrorCode.AUTH_NOT_LOGGED_IN, e.getMessage());
    }

    //运行时异常处理
    @ExceptionHandler
    public Result<Void> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常", e);
        //兜底文案不暴露内部细节，避免把 SQL 或堆栈信息带给前端
        return Result.fail(ErrorCode.INTERNAL_ERROR);
    }

    //数据库操作异常
    @ExceptionHandler
    public Result<Void> handleSqlOperationException(SqlOperationException e) {
        log.error("数据库操作异常", e);
        return Result.fail(ErrorCode.INTERNAL_ERROR);
    }

    //OSS异常
    @ExceptionHandler
    public Result<Void> handleOSSException(OSSException e) {
        log.error("OSS异常: {}", e.getMessage(), e);
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, e.getMessage());
    }

    //权限不足异常处理
    @ExceptionHandler
    public Result<Void> handleInsufficientPermissionsException(InsufficientPermissionsException e) {
        log.warn("权限不足: {}", e.getMessage());
        return Result.fail(ErrorCode.FORBIDDEN, e.getMessage());
    }

    //密码异常处理
    @ExceptionHandler
    public Result<Void> handlePasswordException(PasswordException e) {
        log.warn("密码校验失败: {}", e.getMessage());
        return Result.fail(ErrorCode.USER_PASSWORD_WRONG, e.getMessage());
    }

    //传参异常处理
    @ExceptionHandler
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("参数异常: {}", e.getMessage());
        return Result.fail(ErrorCode.BAD_REQUEST, e.getMessage());
    }

    //验证码相关的异常
    @ExceptionHandler
    public Result<Void> handleSendCodeException(SendCodeException e) {
        log.warn("验证码发送失败: {}", e.getMessage());
        return Result.fail(ErrorCode.USER_EMAIL_SEND_FAILED, e.getMessage());
    }

    //限流异常处理
    @ExceptionHandler
    public Result<Void> handleRateLimitException(RateLimitException e) {
        log.warn("接口被限流: {}", e.getMessage());
        return Result.fail(ErrorCode.RATE_LIMITED, e.getMessage());
    }
}
