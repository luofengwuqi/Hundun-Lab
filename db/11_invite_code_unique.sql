-- ============================================================
-- 增量 11：users.invite_code 补唯一索引（2026-11-17）
-- 背景：管理员邀请码原靠应用层「先查后插」保证唯一，存在并发 TOCTOU——
--       两个请求持同一枚码可各注册一个管理员。代码侧已改为「DELETE 影响
--       行数」原子消费（AdminServiceImpl#addAdmin），本索引是数据库层的兜底
--       防线，同时挡住邀请码生成的重复碰撞。
-- 新装环境直接跑 01_schema.sql 已包含，本脚本只服务存量库。
-- 可重复执行（information_schema 判断索引存在才 ALTER）。
-- 注意：普通用户的 invite_code 为 NULL，MySQL 唯一索引允许多行 NULL，不受影响。
--       若存量库存在重复码，ALTER 会直接报错——那是脏数据，先人工清理再执行。
-- ============================================================

SET @exists_idx := (SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND INDEX_NAME = 'unique_invite_code');
SET @sql := IF(@exists_idx = 0,
    'ALTER TABLE `users` ADD UNIQUE KEY `unique_invite_code` (`invite_code`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
