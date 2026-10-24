package com.hundunlab.site.module.user.pojo.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PutDTO {
    // 对外仍使用 huh 字段名，保持与前端兼容
    @JsonProperty("huh")
    @NotBlank(message = "修改内容不能为空")
    private String value;

    @NotBlank(message = "密码不能为空")
    private String password;
}
