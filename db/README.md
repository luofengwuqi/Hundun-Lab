# 数据库脚本

## 执行顺序

```bash
mysql -u<user> -p -e "CREATE DATABASE hundun_lab CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
mysql -u<user> -p hundun_lab < db/01_schema.sql
mysql -u<user> -p hundun_lab < db/02_seed.sql
mysql -u<user> -p hundun_lab < db/03_seed_extension.sql
```

`01_schema.sql` 与 `02_seed.sql`、`03_seed_extension.sql` 都做了幂等处理（`CREATE TABLE IF NOT EXISTS` / `INSERT IGNORE`），重复执行不会报错，也不会覆盖后台已编辑的内容。存量库升级跑 `10_*.sql` 增量脚本。

## 文件约定

| 文件 | 用途 |
| --- | --- |
| `01_schema.sql` | 全量基础结构，只含 DDL |
| `02_seed.sql` | 初始内容数据（软件、功能特性、平台、首个版本） |
| `03_seed_extension.sql` | 扩展种子（D083：官方扩展预置 + 演示社区扩展，与前台 M0 假数据同一批） |
| `10_extension_license_repo.sql` | 增量：extension 补 `license` / `repo_url` 两列 |
| `11_invite_code_unique.sql` | 增量：`users.invite_code` 补唯一索引（邀请码原子消费的兜底防线） |
| `12_repo_tables.sql` | 增量：新增 `software_repo` / `extension_repo` 子表，并把主表单值 `repo_url` 搬进去（D101） |
| `13_extension_release_fk.sql` | 增量：`extension_release` 外键补 `ON DELETE CASCADE`，与 `software_release` 对齐（D104） |
| `1x_*.sql` | 增量变更，按序号追加，可重复执行 |

## 硬规则

1. **仓库内不出现任何用户行**：不导 `users`、`feedback`、`download_record`、`operation_log` 的数据。
2. **不修改既有列的语义**。需要变更时新增列或新增表，并在增量脚本里写清楚兼容方式。
3. **增量脚本必须可重复执行**。MySQL 8.0 不支持 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`，
   统一用下面的写法包裹：

   ```sql
   SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'software' AND COLUMN_NAME = 'new_col');
   SET @sql := IF(@exists = 0, 'ALTER TABLE `software` ADD COLUMN `new_col` varchar(64) NULL', 'SELECT 1');
   PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
   ```

4. **不写死明文 IP**：`download_record.ip_hash` 存 SHA-256 摘要。

## 当前状态

- `release_asset` 与 `software_screenshot` 故意为空：安装包与截图必须由管理后台上传到 OSS 后入库，
  仓库里不放二进制。
- 管理员账号不通过 SQL 创建。当前代码**还没有**引导逻辑：`/user/register` 只创建 `status = 1` 的普通用户，
  需要管理员权限时手工把该行的 `status` 改为 `2` / `3`。阶段 3 落地后台后再改成
  `ADMIN_BOOTSTRAP_USERNAME` / `ADMIN_BOOTSTRAP_PASSWORD` 首启幂等创建。
