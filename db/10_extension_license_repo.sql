-- ============================================================
-- 增量 10：extension 表补 license / repo_url 两列（2026-11-06）
-- 背景：前台扩展详情页展示「许可证」与「查看源码」，扩展模型缺这两列；
--       新装环境直接跑 01_schema.sql 已包含，本脚本只服务存量库。
-- 可重复执行（information_schema 判断列存在才 ALTER）。
-- ============================================================

SET @exists_license := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'extension' AND COLUMN_NAME = 'license');
SET @sql := IF(@exists_license = 0,
    'ALTER TABLE `extension` ADD COLUMN `license` varchar(64) DEFAULT NULL COMMENT ''开源许可证，如 MIT'' AFTER `platform`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists_repo := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'extension' AND COLUMN_NAME = 'repo_url');
SET @sql := IF(@exists_repo = 0,
    'ALTER TABLE `extension` ADD COLUMN `repo_url` varchar(255) DEFAULT NULL COMMENT ''源码仓库；详情页「查看源码」用'' AFTER `license`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
