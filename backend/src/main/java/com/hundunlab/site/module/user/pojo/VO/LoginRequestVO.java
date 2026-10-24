package com.hundunlab.site.module.user.pojo.VO;

import com.hundunlab.site.module.user.pojo.Entity.User;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class LoginRequestVO {
    @NotBlank(message = "我也不知道为啥但是token没有值")
    private String token;

    private User user;
}
