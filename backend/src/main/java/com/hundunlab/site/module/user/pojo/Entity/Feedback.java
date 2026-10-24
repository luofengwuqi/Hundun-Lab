package com.hundunlab.site.module.user.pojo.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Feedback {
    private Integer id;
    private String type;
    private String text;
    private Integer userId;
    private String userEmail;
    private String username;
    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;
    private Integer status;
    private Integer handleUserId;
}
