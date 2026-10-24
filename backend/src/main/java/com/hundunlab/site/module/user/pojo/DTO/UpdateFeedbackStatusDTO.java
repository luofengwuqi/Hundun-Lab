package com.hundunlab.site.module.user.pojo.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class UpdateFeedbackStatusDTO {
    
    @NotNull(message = "反馈ID不能为空")
    private Integer id;
    @NotNull(message = "反馈状态不能为空")
    private Integer status;
    @NotNull(message = "处理人ID不能为空")
    private Integer handleUserId;
}
