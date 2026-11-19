package com.hundunlab.site.module.software.pojo.DTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 软件上下线。单开一个接口而不是塞进编辑 DTO：
 * 改资料时**任何**情况下都不该顺带改变可见性，这条边界值得用一个独立端点表达。
 */
@Data
public class SoftwareStatusDTO {

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status;
}
