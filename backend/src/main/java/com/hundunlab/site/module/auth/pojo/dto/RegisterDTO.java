package com.hundunlab.site.module.auth.pojo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 1, max = 10, message = "用户名长度在1-10之间")
    //用户名会拼进OSS对象路径与各类缓存key，必须限制字符白名单
    @Pattern(regexp = "^[\\w\\u4e00-\\u9fa5-]{1,10}$", message = "用户名仅支持中英文、数字、下划线、连字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度在6-20之间")
    private String password;

//    @NotBlank(message = "测试密码不能为空")
//    @Size(min = 6, max = 20, message = "密码长度在6-20之间")
//    private String checkPassword;

    /**
     * 注册时不采纳：头像只能通过 PUT /user/avatar 上传。
     * 保留字段只为兼容旧客户端，服务端一律忽略（见 UserServiceImpl#register）。
     */
    private String avatarUrl;

  // 手机号（可选）
@Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
@Size(max = 20, message = "手机号过长")
private String phone;

// 邮箱（可选，唯一）。上限对齐 users.email 的 varchar(100)：
// @Email 放行超长地址，不拦就会在 INSERT 时变成 500 而不是 400
@Email(message = "邮箱格式错误")
@Size(max = 100, message = "邮箱过长")
private String email;


    //重写toString方法
    @Override
    public String toString() {
        return "RegisterDTO{" +
                "username='" + username + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
