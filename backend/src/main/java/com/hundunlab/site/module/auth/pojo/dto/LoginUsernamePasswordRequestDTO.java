package com.hundunlab.site.module.auth.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginUsernamePasswordRequestDTO {

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度在6-20之间")
    private String password;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 1, max = 10, message = "用户名长度在1-10之间")
    private String username;

    //重写toString方法
    @Override
    public String toString() {
        return "LoginRequestDTO{" +
                "username='" + username + '\'' +
                '}';
    }
}
