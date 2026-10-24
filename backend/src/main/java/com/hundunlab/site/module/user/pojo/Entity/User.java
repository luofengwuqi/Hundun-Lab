package com.hundunlab.site.module.user.pojo.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User implements Serializable {
        /**
         * 用户ID（主键）
         */
        private Long id;

        /**
         * 用户名（唯一）
         */
        private String username;

        /**
         * 邮箱（唯一）
         */
        private String email;

        /**
         * 密码哈希值
         */
        // 密码哈希永远不参与 JSON 序列化（HTTP 响应和 Redis 缓存都不落哈希）
        @JsonIgnore
        private String passwordHash;

        /**
         * 昵称
         */
        private String nickname;

        /**
         * 头像URL
         */
        private String avatarUrl;

        /**
         * 手机号
         */
        private String phone;

        /**
         * 用户状态：1-正常，0-禁用
         */
        private Integer status;

        //邀请码属于管理端敏感字段，不随任何JSON响应外泄
        @JsonIgnore
        private String inviteCode;

        /**
         * 创建时间
         */
        private LocalDateTime createdAt;

        /**
         * 更新时间
         */
        private LocalDateTime updatedAt;

        /**
         * 最后登录时间
         */
        private LocalDateTime lastLoginAt;

        //重写ToString方法
        @Override
        public String toString() {
            return "User{" +
                    "id=" + id +
                    ", username='" + username + '\'' +
                    ", email='" + email + '\'' +
                    ", nickname='" + nickname + '\'' +
                    ", avatarUrl='" + avatarUrl + '\'' +
                    ", phone='" + phone + '\'' +
                    ", status=" + status +
                    ", createdAt=" + createdAt +
                    ", updatedAt=" + updatedAt +
                    ", lastLoginAt=" + lastLoginAt +
                    '}';
        }

}
