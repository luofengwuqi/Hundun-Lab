package com.hundunlab.site.common.exception;

import com.hundunlab.site.common.exception.MyException.*;
import com.hundunlab.site.common.response.Result;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    //DTO属性异常处理
    @ExceptionHandler
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String msg = Objects.requireNonNull(e.getBindingResult().getFieldError()).getDefaultMessage();
        log.warn("DTO接收异常信息:{}", msg);
        return Result.fail(400, msg);
    }

    //jwt过期：具体原因只写日志，不返回给客户端
    @ExceptionHandler
    public Result<Void> handleJwtExpiredException(ExpiredJwtException e) {
        log.warn("JWT 已过期: {}", e.getMessage());
        return Result.fail(401, "登录已过期");
    }

    @ExceptionHandler
    public Result<Void> handleJwtMalformedException(MalformedJwtException e) {
        log.warn("JWT 格式错误: {}", e.getMessage());
        return Result.fail(401, "无效的令牌");
    }

    //其余 JWT 异常（签名错误、不支持的令牌等）
    @ExceptionHandler
    public Result<Void> handleJwtException(JwtException e) {
        log.warn("JWT 校验失败: {}", e.getMessage());
        return Result.fail(401, "无效的令牌");
    }

    //用户异常
    @ExceptionHandler
    public Result<Void> handleUserNotFoundException(UserNotFoundException e) {
        log.warn("用户不存在: {}", e.getMessage());
        return Result.fail(404, e.getMessage());
    }

    @ExceptionHandler
    public Result<Void> handleUserExistedException(UserExistedException e) {
        log.warn("用户已存在: {}", e.getMessage());
        return Result.fail(409, e.getMessage());
    }

    //登录异常
    @ExceptionHandler
    public Result<Void> handleLoginException(LoginException e) {
        log.warn("登录异常: {}", e.getMessage());
        return Result.fail(401, e.getMessage());
    }

    //运行时异常处理
    @ExceptionHandler
    public Result<Void> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常", e);
        return Result.fail(500, e.getMessage());
    }

    //数据库操作异常
    @ExceptionHandler
    public Result<Void> handleSqlOperationException(SqlOperationException e) {
        log.error("数据库操作异常", e);
        return Result.fail(500, e.getMessage());
    }

    //OSS异常
    @ExceptionHandler
    public Result<Void> handleOSSException(OSSException e) {
        log.error("OSS异常: {}", e.getMessage(), e);
        return Result.fail(500, e.getMessage());
    }

    //权限不足异常处理
    @ExceptionHandler
    public Result<Void> handleInsufficientPermissionsException(InsufficientPermissionsException e) {
        log.warn("权限不足: {}", e.getMessage());
        return Result.fail(403, e.getMessage());
    }

    //密码异常处理
    @ExceptionHandler
    public Result<Void> handlePasswordException(PasswordException e) {
        log.warn("密码校验失败: {}", e.getMessage());
        return Result.fail(400, e.getMessage());
    }

    //传参异常处理
    @ExceptionHandler
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("参数异常: {}", e.getMessage());
        return Result.fail(400, e.getMessage());
    }

    //验证码相关的异常
    @ExceptionHandler
    public Result<Void> handleSendCodeException(SendCodeException e) {
        log.warn("验证码发送失败: {}", e.getMessage());
        return Result.fail(400, e.getMessage());
    }

    //限流异常处理
    @ExceptionHandler
    public Result<Void> handleRateLimitException(RateLimitException e) {
        log.warn("接口被限流: {}", e.getMessage());
        return Result.fail(429, e.getMessage());
    }
}
