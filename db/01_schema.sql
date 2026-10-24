-- ============================================================
-- Hundun Lab 官网 · 基础结构（schema only）
-- 目标库：MySQL 8.0+
-- 说明：
--   1. 本文件只含 DDL，不含任何 INSERT，可安全提交到公开仓库
--   2. 重复执行安全（CREATE TABLE IF NOT EXISTS）
--   3. 后续结构变更放入 1x_*.sql 增量脚本，不修改本文件的既有列语义
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- 用户（沿用底座结构，字段语义不变）
-- status: 3-超级管理员 / 2-管理员 / 1-普通用户 / -1-用户禁用 / -2-管理员禁用
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `users` (
  `id`            bigint       NOT NULL AUTO_INCREMENT,
  `username`      varchar(50)  NOT NULL,
  `email`         varchar(100) DEFAULT NULL,
  `password_hash` varchar(255) NOT NULL,
  `nickname`      varchar(50)  DEFAULT NULL,
  `avatar_url`    varchar(255) DEFAULT NULL,
  `phone`         varchar(20)  DEFAULT NULL,
  `status`        tinyint      DEFAULT '1' COMMENT '3-超管 2-管理员 1-用户 -1-用户禁用 -2-管理员禁用',
  `invite_code`   varchar(32)  DEFAULT NULL COMMENT '管理员邀请码，普通用户为 NULL',
  `created_at`    datetime     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    datetime     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `last_login_at` datetime     DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_username` (`username`),
  UNIQUE KEY `unique_email` (`email`),
  UNIQUE KEY `unique_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户';

-- ------------------------------------------------------------
-- 软件（系列内每个软件一行，新增软件只插数据、不改代码）
-- 文案字段成对存在：*_zh / *_en，读取时按请求语言取其一
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `software` (
  `id`          bigint       NOT NULL AUTO_INCREMENT,
  `slug`        varchar(64)  NOT NULL COMMENT 'URL 标识，如 hundundesktop',
  `name_zh`     varchar(64)  NOT NULL,
  `name_en`     varchar(64)  NOT NULL,
  `tagline_zh`  varchar(160) DEFAULT NULL COMMENT '一句话简介',
  `tagline_en`  varchar(160) DEFAULT NULL,
  `intro_zh`    mediumtext   COMMENT '详细介绍，支持 Markdown',
  `intro_en`    mediumtext,
  `tech_stack`  varchar(255) DEFAULT NULL COMMENT '技术栈，逗号分隔',
  `repo_url`    varchar(255) DEFAULT NULL,
  `license`     varchar(64)  DEFAULT NULL,
  `cover_url`   varchar(500) DEFAULT NULL,
  `theme_color` varchar(16)  DEFAULT NULL COMMENT '卡片主色，如 #C3272B',
  `status`      tinyint      NOT NULL DEFAULT '0' COMMENT '0-草稿 1-已发布',
  `sort_order`  int          NOT NULL DEFAULT '0' COMMENT '升序展示',
  `created_at`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_software_slug` (`slug`),
  KEY `idx_software_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='软件';

-- 功能特性（详情页「功能特性」区块）
CREATE TABLE IF NOT EXISTS `software_feature` (
  `id`          bigint       NOT NULL AUTO_INCREMENT,
  `software_id` bigint       NOT NULL,
  `icon`        varchar(16)  DEFAULT NULL COMMENT 'emoji 或图标名',
  `title_zh`    varchar(64)  NOT NULL,
  `title_en`    varchar(64)  NOT NULL,
  `desc_zh`     varchar(500) DEFAULT NULL,
  `desc_en`     varchar(500) DEFAULT NULL,
  `sort_order`  int          NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_feature_software` (`software_id`, `sort_order`),
  CONSTRAINT `fk_feature_software` FOREIGN KEY (`software_id`) REFERENCES `software` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='软件功能特性';

-- 截图
CREATE TABLE IF NOT EXISTS `software_screenshot` (
  `id`          bigint       NOT NULL AUTO_INCREMENT,
  `software_id` bigint       NOT NULL,
  `image_url`   varchar(500) NOT NULL,
  `caption_zh`  varchar(160) DEFAULT NULL,
  `caption_en`  varchar(160) DEFAULT NULL,
  `sort_order`  int          NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_screenshot_software` (`software_id`, `sort_order`),
  CONSTRAINT `fk_screenshot_software` FOREIGN KEY (`software_id`) REFERENCES `software` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='软件截图';

-- 平台支持（详情页「平台支持」区块，与实际安装包分离）
CREATE TABLE IF NOT EXISTS `software_platform` (
  `id`          bigint      NOT NULL AUTO_INCREMENT,
  `software_id` bigint      NOT NULL,
  `platform`    varchar(16) NOT NULL COMMENT 'win32 / darwin / linux',
  `note_zh`     varchar(120) DEFAULT NULL,
  `note_en`     varchar(120) DEFAULT NULL,
  `sort_order`  int         NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_software` (`software_id`, `platform`),
  CONSTRAINT `fk_platform_software` FOREIGN KEY (`software_id`) REFERENCES `software` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='软件平台支持';

-- ------------------------------------------------------------
-- 版本与安装包
-- 「最新版本」不落冗余标记，按 (software_id, channel) 内 published_at 倒序推导
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `software_release` (
  `id`           bigint       NOT NULL AUTO_INCREMENT,
  `software_id`  bigint       NOT NULL,
  `version`      varchar(32)  NOT NULL COMMENT '语义化版本或产品版本号',
  `channel`      varchar(16)  NOT NULL DEFAULT 'stable' COMMENT 'stable / beta',
  `notes_zh`     mediumtext   COMMENT '更新说明',
  `notes_en`     mediumtext,
  `published_at` datetime     DEFAULT NULL COMMENT '发布时间，为空表示尚未发布',
  `status`       tinyint      NOT NULL DEFAULT '0' COMMENT '0-草稿 1-已发布',
  `created_at`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_release_version` (`software_id`, `version`, `channel`),
  KEY `idx_release_published` (`software_id`, `status`, `published_at`),
  CONSTRAINT `fk_release_software` FOREIGN KEY (`software_id`) REFERENCES `software` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='软件版本';

-- 安装包资产：一个版本可有多平台多格式，object_key 指向私有 OSS，不存直链
CREATE TABLE IF NOT EXISTS `release_asset` (
  `id`             bigint       NOT NULL AUTO_INCREMENT,
  `release_id`     bigint       NOT NULL,
  `platform`       varchar(16)  NOT NULL COMMENT 'win32 / darwin / linux',
  `arch`           varchar(16)  NOT NULL DEFAULT 'x64' COMMENT 'x64 / arm64',
  `package_type`   varchar(16)  NOT NULL COMMENT 'nsis / appimage / deb / dmg / zip',
  `file_name`      varchar(255) NOT NULL COMMENT '用户下载时看到的文件名',
  `object_key`     varchar(500) NOT NULL COMMENT 'OSS 对象键，不含域名',
  `file_size`      bigint       NOT NULL DEFAULT '0' COMMENT '字节',
  `sha256`         char(64)     DEFAULT NULL,
  `download_count` bigint       NOT NULL DEFAULT '0',
  `created_at`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_asset_variant` (`release_id`, `platform`, `arch`, `package_type`),
  KEY `idx_asset_release` (`release_id`),
  CONSTRAINT `fk_asset_release` FOREIGN KEY (`release_id`) REFERENCES `software_release` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='安装包资产';

-- ------------------------------------------------------------
-- 下载明细
-- ip 只存 SHA-256 摘要，避免明文 IP 落库
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `download_record` (
  `id`          bigint       NOT NULL AUTO_INCREMENT,
  `user_id`     bigint       DEFAULT NULL,
  `software_id` bigint       NOT NULL,
  `release_id`  bigint       NOT NULL,
  `asset_id`    bigint       NOT NULL,
  `platform`    varchar(16)  NOT NULL,
  `ip_hash`     char(64)     DEFAULT NULL,
  `user_agent`  varchar(255) DEFAULT NULL,
  `created_at`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_download_user` (`user_id`, `created_at`),
  KEY `idx_download_software` (`software_id`, `created_at`),
  KEY `idx_download_asset` (`asset_id`, `created_at`),
  CONSTRAINT `fk_download_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='下载明细';

-- ------------------------------------------------------------
-- 反馈（沿用底座结构，仅追加列，不改既有字段语义）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `feedback` (
  `id`             int          NOT NULL AUTO_INCREMENT,
  `type`           varchar(10)  DEFAULT NULL COMMENT '反馈类型',
  `text`           text         COMMENT '反馈内容',
  `user_id`        bigint       NOT NULL,
  `user_email`     varchar(100) DEFAULT NULL,
  `username`       varchar(50)  DEFAULT NULL,
  `status`         tinyint      DEFAULT NULL COMMENT '0-未处理 1-已处理 -1-驳回',
  `created_at`     datetime     DEFAULT NULL,
  `finished_at`    datetime     DEFAULT NULL,
  `handle_user_id` int          DEFAULT NULL,
  `software_id`    bigint       DEFAULT NULL COMMENT '关联软件（可空）',
  `contact`        varchar(100) DEFAULT NULL COMMENT '补充联系方式（可空）',
  `lang`           varchar(8)   NOT NULL DEFAULT 'zh' COMMENT '提交时语言',
  PRIMARY KEY (`id`),
  KEY `feedback_users_email_fk` (`user_email`),
  KEY `feedback_users_id_fk` (`user_id`),
  KEY `idx_feedback_status_created` (`status`, `created_at`),
  CONSTRAINT `feedback_users_id_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户反馈';

-- ------------------------------------------------------------
-- 操作审计（仅记录后台关键操作）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `operation_log` (
  `id`                    bigint      NOT NULL AUTO_INCREMENT,
  `operate_user_id`       bigint      DEFAULT NULL,
  `operate_user_name`     varchar(50) DEFAULT NULL,
  `operate_type`          varchar(20) DEFAULT NULL,
  `module_name`           varchar(50) DEFAULT NULL,
  `ip_address`            varchar(50) DEFAULT NULL,
  `operate_function_name` varchar(50) DEFAULT NULL,
  `operate_at`            datetime    DEFAULT NULL,
  `operate_time`          varchar(30) DEFAULT NULL,
  `operate_status`        tinyint     DEFAULT NULL COMMENT '1-成功 0-失败',
  `executed_sql`          text,
  PRIMARY KEY (`id`),
  KEY `idx_operate_user_id` (`operate_user_id`),
  KEY `idx_operate_at` (`operate_at`),
  KEY `idx_module_name` (`module_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='后台操作审计';

SET FOREIGN_KEY_CHECKS = 1;
