package com.hundunlab.site.module.auth.controller;

import com.hundunlab.site.common.anno.PublicApi;
import com.hundunlab.site.common.anno.RateLimit;
import com.hundunlab.site.common.response.Result;
import com.hundunlab.site.module.auth.pojo.dto.LoginEmailCodeDTO;
import com.hundunlab.site.module.auth.pojo.dto.LoginEmailPassWordDTO;
import com.hundunlab.site.module.auth.pojo.dto.LoginPhonePasswordDTO;
import com.hundunlab.site.module.auth.pojo.dto.LoginUsernamePasswordRequestDTO;
import com.hundunlab.site.module.auth.pojo.dto.SendEmailCodeDTO;
import com.hundunlab.site.module.auth.service.LoginService;
import com.hundunlab.site.module.user.pojo.VO.LoginRequestVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录相关接口。
 * 除登出外均为匿名可访问（登出需要已登录才能定位要吊销的令牌）。
 */
@Slf4j
@RestController
@RequestMapping("/user/login")
public class LoginController {

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PublicApi
    @RateLimit(count = 10, time = 60, key = "login:username-password", resetOnSuccess = true)
    @PostMapping("/username-password")
    public Result<LoginRequestVO> loginUsernamePassword(@Valid @RequestBody LoginUsernamePasswordRequestDTO dto) {
        log.info("用户登录，用户名：{}", dto.getUsername());
        return Result.success(loginService.loginUsernamePassword(dto));
    }

    @PublicApi
    @RateLimit(count = 10, time = 60, key = "login:phone-password", resetOnSuccess = true)
    @PostMapping("/phone-password")
    public Result<LoginRequestVO> loginPhonePassword(@Valid @RequestBody LoginPhonePasswordDTO dto) {
        return Result.success(loginService.loginPhonePassword(dto));
    }

    @PublicApi
    @RateLimit(count = 10, time = 60, key = "login:email-code", resetOnSuccess = true)
    @PostMapping("/email-code")
    public Result<LoginRequestVO> loginEmailCode(@Valid @RequestBody LoginEmailCodeDTO dto) {
        return Result.success(loginService.loginEmailCode(dto));
    }

    @PublicApi
    @RateLimit(count = 3, time = 60, key = "login:email-code-send")
    @PostMapping("/email-code/send")
    public Result<Void> sendEmailCode(@Valid @RequestBody SendEmailCodeDTO dto) {
        log.info("发送邮箱验证码，邮箱：{}", dto.getEmail());
        loginService.sendEmailCode(dto.getEmail());
        return Result.success();
    }

    @PublicApi
    @RateLimit(count = 10, time = 60, key = "login:email-password", resetOnSuccess = true)
    @PostMapping("/email-password")
    public Result<LoginRequestVO> loginEmailPassword(@Valid @RequestBody LoginEmailPassWordDTO dto) {
        return Result.success(loginService.loginEmailPassword(dto));
    }

    /**
     * 登出：需要登录态，按当前用户 id 删除 Redis 中的令牌，实现立即吊销
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        loginService.logout();
        return Result.success();
    }
}
