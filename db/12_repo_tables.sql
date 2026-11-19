-- ============================================================
-- 增量 12：仓库地址改为子表（D101，2026-11-18）
-- 背景：software.repo_url / extension.repo_url 是单值 varchar(255)，表达不了
--       「同一个仓库多镜像 + 多个不同仓库，各带标签」；作者要求 0..n 条。
-- 做法：新增两张同构子表，再把存量单值搬成一条 repo_type='source' 的行。
--       主表的 repo_url 列**保留但停止写入**（D093：没有必要时不动既有结构）。
-- 可重复执行（CREATE TABLE IF NOT EXISTS + INSERT IGNORE + 唯一键兜底）。
-- ============================================================

CREATE TABLE IF NOT EXISTS `software_repo` (
  `id`          bigint       NOT NULL AUTO_INCREMENT,
  `software_id` bigint       NOT NULL,
  `label_zh`    varchar(32)  NOT NULL COMMENT '展示标签中文，如「源码仓库」「Gitee 镜像」',
  `label_en`    varchar(32)  NOT NULL COMMENT '展示标签英文',
  `url`         varchar(500) NOT NULL COMMENT '完整地址，含协议',
  `repo_type`   varchar(16)  NOT NULL DEFAULT 'source' COMMENT 'source / mirror / release / docs',
  `sort_order`  int          NOT NULL DEFAULT '0' COMMENT '升序展示',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_repo_software_url` (`software_id`, `url`),
  KEY `idx_repo_software` (`software_id`, `sort_order`),
  CONSTRAINT `fk_repo_software` FOREIGN KEY (`software_id`) REFERENCES `software` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='软件仓库地址';

CREATE TABLE IF NOT EXISTS `extension_repo` (
  `id`           bigint       NOT NULL AUTO_INCREMENT,
  `extension_id` bigint       NOT NULL,
  `label_zh`     varchar(32)  NOT NULL,
  `label_en`     varchar(32)  NOT NULL,
  `url`          varchar(500) NOT NULL,
  `repo_type`    varchar(16)  NOT NULL DEFAULT 'source',
  `sort_order`   int          NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_repo_extension_url` (`extension_id`, `url`),
  KEY `idx_repo_extension` (`extension_id`, `sort_order`),
  CONSTRAINT `fk_repo_extension` FOREIGN KEY (`extension_id`) REFERENCES `extension` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='扩展仓库地址';

-- ------------------------------------------------------------
-- 存量搬运：把主表上的单值搬成子表里的一条 source 行
-- INSERT IGNORE 靠 UNIQUE(owner_id, url) 去重，重复执行不会产生第二行
-- ------------------------------------------------------------
INSERT IGNORE INTO `software_repo` (`software_id`, `label_zh`, `label_en`, `url`, `repo_type`, `sort_order`)
SELECT `id`, '源码仓库', 'Source repository', TRIM(`repo_url`), 'source', 0
FROM `software`
WHERE `repo_url` IS NOT NULL AND TRIM(`repo_url`) <> '';

INSERT IGNORE INTO `extension_repo` (`extension_id`, `label_zh`, `label_en`, `url`, `repo_type`, `sort_order`)
SELECT `id`, '源码仓库', 'Source repository', TRIM(`repo_url`), 'source', 0
FROM `extension`
WHERE `repo_url` IS NOT NULL AND TRIM(`repo_url`) <> '';
