package com.hundunlab.site.module.auth.controller;

import com.hundunlab.site.common.anno.RateLimit;
import com.hundunlab.site.common.response.Result;
import com.hundunlab.site.module.auth.pojo.dto.LoginEmailCodeDTO;
import com.hundunlab.site.module.auth.pojo.dto.LoginEmailPassWordDTO;
import com.hundunlab.site.module.auth.pojo.dto.LoginPhonePasswordDTO;
import com.hundunlab.site.module.auth.pojo.dto.LoginUsernamePasswordRequestDTO;
import com.hundunlab.site.module.auth.pojo.dto.SendEmailCodeDTO;
import com.hundunlab.site.module.user.pojo.VO.LoginRequestVO;
import com.hundunlab.site.module.auth.service.LoginService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/user/login")
public class LoginController {
    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }
    @RateLimit(count = 10, time = 60, key = "login:username-password", resetOnSuccess = true)
    @PostMapping("/username-password")
    public Result<LoginRequestVO> loginUsernamePassword(@Valid @RequestBody LoginUsernamePasswordRequestDTO dto) {
        log.info("用户登录，用户名：{}", dto.getUsername());
        LoginRequestVO vo =  loginService.loginUsernamePassword(dto);
        return Result.success(vo);
    }

    @RateLimit(count = 10, time = 60, key = "login:phone-password", resetOnSuccess = true)
    @PostMapping("/phone-password")
    public Result<LoginRequestVO> loginPhonePassword(@Valid @RequestBody LoginPhonePasswordDTO dto)
    {
        LoginRequestVO  vo= loginService.loginPhonePassword(dto);
        log.info("手机密码登录成功");
        return Result.success(vo);
    }

    @RateLimit(count = 10, time = 60, key = "login:email-code", resetOnSuccess = true)
    @PostMapping("/email-code")
    public Result<LoginRequestVO> loginEmailCode(@Valid @RequestBody LoginEmailCodeDTO dto)
    {
        LoginRequestVO  vo= loginService.loginEmailCode(dto);
        log.info("邮箱验证码登录成功");
        return Result.success(vo);
    }
    //和上一个方法配套的验证码发送方法
    @RateLimit(count = 3, time = 60, key = "login:email-code-send")
    @PostMapping("/email-code/send")
    public Result<Void> sendEmailCode(@Valid @RequestBody SendEmailCodeDTO dto)
    {
        log.info("开始发送邮箱验证码，邮箱：{}", dto.getEmail());
        loginService.sendEmailCode(dto.getEmail());
        return Result.success();
    }

    //邮箱+验证码登录
    @RateLimit(count = 10, time = 60, key = "login:email-password", resetOnSuccess = true)
    @PostMapping("/email-password")
    public Result<LoginRequestVO> loginEmailPassword(@Valid @RequestBody LoginEmailPassWordDTO dto)
    {
        LoginRequestVO  vo= loginService.loginEmailPassword(dto);
        log.info("邮箱密码登录成功");
        return Result.success(vo);
    }

    //登出
    @PostMapping("/logout")
    public Result<Void> logout(){
        log.info("开始登出");
        loginService.logout();
        return Result.success();
    }
}
