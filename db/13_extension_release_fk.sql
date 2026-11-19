-- ============================================================
-- 增量 13：extension_release 外键补 ON DELETE CASCADE（D104，2026-11-18）
-- 背景：01_schema.sql 里这条例外键是裸 FK（无 CASCADE），而 software_release 的
--       同名外键是 ON DELETE CASCADE。两侧不对称——删有版本的扩展会直接 FK 报错。
-- 做法：读 information_schema.REFERENTIAL_CONSTRAINTS 的 DELETE_RULE，
--       不是 CASCADE 就先 DROP 再 ADD。可重复执行。
-- 注意：CASCADE 只作兜底，业务路径仍按 D103「先删 OSS 再删库」，
--       否则删库后 object_key 消失，OSS 对象就成了永久孤儿。
-- ============================================================

SET @fk_exists := (SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS
                   WHERE CONSTRAINT_SCHEMA = DATABASE()
                     AND TABLE_NAME = 'extension_release'
                     AND CONSTRAINT_NAME = 'release_extension_fk');

SET @fk_cascade := (SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS
                    WHERE CONSTRAINT_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'extension_release'
                      AND CONSTRAINT_NAME = 'release_extension_fk'
                      AND DELETE_RULE = 'CASCADE');

-- 先删掉不带 CASCADE 的旧外键（存在且规则不对时）
SET @sql := IF(@fk_exists > 0 AND @fk_cascade = 0,
    'ALTER TABLE `extension_release` DROP FOREIGN KEY `release_extension_fk`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 再按 CASCADE 重建（已带 CASCADE 或刚删掉都会走到这里；已存在则跳过）
SET @fk_exists := (SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS
                   WHERE CONSTRAINT_SCHEMA = DATABASE()
                     AND TABLE_NAME = 'extension_release'
                     AND CONSTRAINT_NAME = 'release_extension_fk');

SET @sql := IF(@fk_exists = 0,
    'ALTER TABLE `extension_release` ADD CONSTRAINT `release_extension_fk` FOREIGN KEY (`extension_id`) REFERENCES `extension` (`id`) ON DELETE CASCADE',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
