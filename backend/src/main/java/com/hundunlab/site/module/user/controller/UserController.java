package com.hundunlab.site.module.user.controller;

import com.hundunlab.site.common.anno.PublicApi;
import com.hundunlab.site.common.anno.RateLimit;
import com.hundunlab.site.common.context.BaseContext;
import com.hundunlab.site.common.response.Result;
import com.hundunlab.site.module.admin.pojo.DTO.DeleteUsersDTO;
import com.hundunlab.site.module.auth.pojo.dto.RegisterDTO;
import com.hundunlab.site.module.user.pojo.DTO.*;
import com.hundunlab.site.module.user.pojo.Entity.User;
import com.hundunlab.site.module.user.pojo.VO.RegisterVO;
import com.hundunlab.site.module.user.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PublicApi
    @RateLimit(count = 5, time = 60, key = "register:user")
    @PostMapping("/register")
    public Result<RegisterVO> register(@Valid @RequestBody RegisterDTO dto)
    {
        log.info("用户注册");
        log.info("已经接收到请求:RegisterDTO信息为{}",dto.toString());
        RegisterVO r = userService.register(dto);
        return Result.success(r);
    }



    @DeleteMapping
    public Result<Void> deleteUsers(@Valid @RequestBody DeleteUsersDTO dto) throws Exception {
        log.info("用户删除");
//        log.info("即将进行用户删除 捕获到信息:{}",dto);
        userService.deleteUsers(dto);
        return Result.success();
    }

    //上传用户头像的接口
    @PutMapping("/avatar")
    public Result<Void> updateAvatar(@RequestParam("file") MultipartFile file) {
        log.info("上传用户头像");
        userService.updateAvatar(file);
        return Result.success();
    }

    @GetMapping("/info")
    public Result<User> getUserInfo() {
        Long trueId = BaseContext.getCurrentId();
        log.info("被获取的用户id为{}",trueId);
        User u = userService.selectUserById(trueId);
        return Result.success(u);
    }
    @PutMapping("/nickname")
    public Result<Void> updateNickname(@RequestBody @Valid PutDTO dto) {
        log.info("修改用户昵称");
        userService.updateNickname(dto);
        return Result.success();
    }
    @PutMapping("/email")
    public Result<Void> updateEmail(@RequestBody @Valid PutDTO dto) {
        log.info("修改用户邮箱");
        userService.updateEmail(dto);
        return Result.success();
    }
    @PutMapping("/phone")
    public Result<Void> updatePhone(@RequestBody @Valid PutDTO dto) {
        log.info("修改用户手机号");
        userService.updatePhone(dto);
        return Result.success();
    }
    @RateLimit(count = 5, time = 60, key = "user:password-update")
    @PutMapping("/password")
    public Result<Void> updatePassword(@RequestBody @Valid UpdatePasswordDTO dto) {
        log.info("修改用户密码");
        userService.updatePassword(dto);
        return Result.success();
    }

}
