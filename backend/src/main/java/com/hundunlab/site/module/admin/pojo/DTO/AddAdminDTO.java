package com.hundunlab.site.module.admin.pojo.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddAdminDTO {
    @NotBlank(message = "重要密码不能为空")
    private String importantPassword;
    @NotBlank(message = "用户名不能为空")
    @Size(min = 1, max = 10, message = "用户名长度在1-10之间")
    //用户名会拼进OSS对象路径与各类缓存key，必须限制字符白名单
    @Pattern(regexp = "^[\\w\\u4e00-\\u9fa5-]{1,10}$", message = "用户名仅支持中英文、数字、下划线、连字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度在6-20之间")
    private String password;

//    密码校验已在前端完成 后端不再接受而第二次输入的密码
//    @NotBlank(message = "测试密码不能为空")
//    @Size(min = 6, max = 20, message = "密码长度在6-20之间")
//    private String checkPassword;

    private String avatarUrl;

    /**
     * 手机号（可选，唯一）
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
    private String phone;

    /**
     * 邮箱（可选，唯一）
     */
    @Email(message = "邮箱格式错误")
    private String email;

    //重写toString方法
    @Override
    public String toString() {
        return "AddAdminDTO{" +
                "username='" + username + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
