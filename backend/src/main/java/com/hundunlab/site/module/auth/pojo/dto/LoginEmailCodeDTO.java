package com.hundunlab.site.module.auth.pojo.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class LoginEmailCodeDTO {
    @NotEmpty
    @Email
    private String email;
    //六位非空
    @ToString.Exclude
    @NotEmpty
    @Size(min = 6, max = 6, message = "验证码长度不对哦...")
    private String code;

}
