package com.hundunlab.site.module.admin.pojo.DTO;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class DeleteUsersDTO {
    @NotNull(message = "用户名列表不能为空")
    @NotEmpty(message = "用户名列表不能为空")
    @Size(max = 100, message = "单次最多删除100个用户")
    private List<String> usernames;

    private String password;
}
