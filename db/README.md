# 数据库脚本

## 执行顺序

```bash
mysql -u<user> -p -e "CREATE DATABASE hundun_lab CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
mysql -u<user> -p hundun_lab < db/01_schema.sql
mysql -u<user> -p hundun_lab < db/02_seed.sql
```

`01_schema.sql` 与 `02_seed.sql` 都做了幂等处理（`CREATE TABLE IF NOT EXISTS` / `INSERT IGNORE`），重复执行不会报错，也不会覆盖后台已编辑的内容。

## 文件约定

| 文件 | 用途 |
| --- | --- |
| `01_schema.sql` | 全量基础结构，只含 DDL |
| `02_seed.sql` | 初始内容数据（软件、功能特性、平台、首个版本） |
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
- 管理员账号不通过 SQL 创建：首次启动时用 `ADMIN_BOOTSTRAP_USERNAME` / `ADMIN_BOOTSTRAP_PASSWORD`
  环境变量引导创建（幂等，库中已有管理员则跳过）。
