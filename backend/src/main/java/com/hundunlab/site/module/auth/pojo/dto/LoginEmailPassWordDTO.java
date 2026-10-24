package com.hundunlab.site.module.auth.pojo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class LoginEmailPassWordDTO {
    @NotEmpty(message = "后端没有接收到邮箱...?")
    @Email(message = "请输入正确的邮箱格式!")
    private String email;

    @ToString.Exclude
    @NotEmpty(message = "后端没有接收到密码...?")
    @Size(min=6,max=20,message="密码长度在6-20之间")
    private String password;
}
