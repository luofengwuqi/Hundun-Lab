package com.hundunlab.site.module.admin.pojo.DTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetUsersDTO {

    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码不能小于1")
    private Integer page = 1;

    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 1000, message = "每页最多查询1000条")
    private Integer size = 20;

    private String name;

    private Integer status;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime beginTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime endTime;

    //最近一次登录时间距今天数
    private Integer loginDays;



}
