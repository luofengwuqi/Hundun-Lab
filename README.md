<img src="frontend/src/assets/img/common/biglogo.png" alt="Hundun Lab" width="320">

# Hundun Lab 官网

馄饨系列软件的官方站点 —— 介绍每个项目、提供安装包下载、承载插件社区。

[![JDK](https://img.shields.io/badge/JDK-17-blue?style=flat-square)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?style=flat-square)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3-42B883?style=flat-square)](https://vuejs.org/)
[![License](https://img.shields.io/badge/License-MulanPSL--2.0-red?style=flat-square)](LICENSE)

站点本身不写死任何软件信息：软件、功能特性、支持平台、版本与安装包全部来自数据库，
设计目标是一个新版本只需在后台录入数据，而不是改代码重新发版（后台尚未实现）。

目前收录 **HundunDesktop**（Electron 28 + Vue 3 的复古掌机风桌面工具）。

## 当前进度

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| 1 | 官网前台：首页、软件列表与详情、登录、需登录的下载 | 已完成 |
| 1 | 管理后台：软件与版本录入、安装包上传、下载数据概览 | 未开始（尚无软件/版本管理接口） |
| 2 | 插件社区：列表与详情、上传校验 manifest、审核、稳定下载地址 | 未开始（前台为占位页） |
| 3 | 用户中心、反馈处理、后台权限收口 | 未开始 |

## 技术栈

| 层 | 选型 |
| --- | --- |
| 后端 | Spring Boot 3.5、JDK 17、MyBatis + PageHelper、Redis |
| 认证 | JWT（jjwt 0.12）、BCrypt、邮箱验证码、Redis 令牌吊销 |
| 存储 | 阿里云 OSS 私有桶 + 预签名下载地址 |
| 前端 | Vue 3、Vite、Pinia、Vue Router（History）、vue-i18n |
| 数据库 | MySQL 8.0 |

## 项目结构

```
chaos-lab/
├── backend/                      # Spring Boot 后端
│   └── src/main/java/com/hundunlab/site/
│       ├── common/               # 统一响应、错误码、上下文、工具、配置
│       ├── framework/            # 令牌解析、访问决策、限流、自动填充
│       └── module/
│           ├── auth/             # 登录方式与邮箱验证码
│           ├── software/         # 软件、特性、平台、版本、安装包
│           ├── download/         # 下载票据与下载记录
│           ├── user/             # 用户资料、头像上传、反馈、通用文件上传
│           └── admin/            # 用户管理、邀请管理员（阶段 3 重建）
├── frontend/                     # 官网前台
│   └── src/
│       ├── views/site/           # 首页、软件列表/详情、登录、插件
│       ├── components/           # 页头页脚、软件卡片、登录弹窗
│       ├── i18n/                 # zh / en 文案与语言持久化
│       ├── api/                  # 接口封装、错误码翻译
│       └── stores/               # Pinia 会话状态
├── db/                           # 建表与种子脚本（可重复执行）
└── docs/                         # 骨架评审报告
```

> [!NOTE]
> `frontend/src/views/{admin,login,user,help}` 与 `api/{admin,user,feedback,article}.js` 是从底座带过来的旧页面，
> 目前没有挂到路由上，也不会被构建产物引用；阶段 3 重建后台时统一替换或删除。

## 快速开始

### 环境要求

- JDK 17+、Maven 3.9+
- MySQL 8.0+、Redis 6+
- Node.js 20.19+ 或 22.12+，pnpm 9/10（也可用 npm）

### 1. 准备数据库

```bash
mysql -uroot -p -e "CREATE DATABASE hundun_lab CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
mysql -uroot -p hundun_lab < db/01_schema.sql
mysql -uroot -p hundun_lab < db/02_seed.sql
```

两个脚本都是幂等的，重复执行不会报错，也不会覆盖后台已经改过的内容。

### 2. 启动后端

全部敏感配置走环境变量（或本地 `backend/src/main/resources/application-secret.yml`，已被 `.gitignore` 忽略）。

| 变量 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `JWT_SECRET` | 是 | 无 | 至少 32 字节；缺失或过短时应用拒绝启动 |
| `DB_URL` | 否 | 本机 `hundun_lab` | JDBC 连接串 |
| `DB_USERNAME` / `DB_PASSWORD` | 是 | `root` / 无 | 口令不设默认值，避免弱口令进环境 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` / `REDIS_DATABASE` | 否 | `localhost` / `6379` / 空 / `0` | 令牌吊销、验证码、缓存 |
| `OSS_ENDPOINT` / `OSS_BUCKET` / `OSS_DOMAIN` | 下载功能必填 | `oss-cn-beijing.aliyuncs.com` / 空 | 私有桶，用于签发下载地址 |
| `ALIYUN_ACCESS_KEY_ID` / `ALIYUN_ACCESS_KEY_SECRET` | 下载功能必填 | 空 | 缺失时应用照常启动，但下载返回 503 |
| `MAIL_SMTP_HOST` / `MAIL_SMTP_PORT` / `MAIL_FROM` / `MAIL_AUTH_CODE` | 否 | `smtp.qq.com` / `587` / 空 | 缺失时只跳过发信，不影响其他功能 |
| `UPLOAD_MAX_FILE_SIZE` / `UPLOAD_MAX_REQUEST_SIZE` | 否 | `20MB` / `25MB` | 上传配额，供插件 `.zip` 使用；安装包走 OSS 不占这里 |
| `CORS_ALLOWED_ORIGINS` | 否 | `http://localhost:5577` | 前后端分离部署时填前端域名；同源部署可留空 |
| `TRUST_PROXY_HEADERS` | 否 | `false` | 只有部署在可信反向代理后面才设 `true`，否则 `X-Forwarded-For` 可被伪造 |
| `SERVER_PORT` | 否 | `5757` | |
| `SPRING_PROFILES_ACTIVE` | 否 | `dev` | 只有 `dev` 会加载 `application-dev.yml` 与本地 `application-secret.yml` |
| `JWT_EXPIRATION_MILLIS` | 否 | `86400000` | 令牌有效期，默认 24 小时 |

除 `JWT_SECRET` 与数据库口令外，其余缺省即可跑通"浏览 + 口令登录"；邮箱验证码登录要配 SMTP，
真正下载安装包才需要配 OSS。

```bash
cd backend
JWT_SECRET="<32 字节以上的随机串>" DB_PASSWORD="<口令>" mvn spring-boot:run
```

### 3. 启动前端

```bash
cd frontend
pnpm install          # 或 npm install
pnpm dev              # http://localhost:5577
```

开发服务器把 `/api` 代理到 `http://localhost:5757`，前后端无需额外配置跨域。

### 4. 关于账号

`POST /user/register` 注册出来的都是普通用户（`users.status = 1`）。仓库不提供内置管理员账号，
也没有管理员引导逻辑 —— 需要用 `/admin/**` 接口时，先注册一个账号，再手工把它的 `status` 改成
`2`（管理员）或 `3`（超级管理员）。这一步会在阶段 3 由后台的账号管理取代。

### 部署前提

前端构建产物里 `/api` 是写死的基地址（`src/api/http.js` 的 `baseURL`），因此线上要么与后端同源，
由 Nginx 把 `/api` 反向代理到 5757；要么在 `CORS_ALLOWED_ORIGINS` 里登记前端域名并让前端指向独立域名。
本期不做部署，但两种方式都不需要改代码。

## 关键设计

### 鉴权：只解析，再决策

`TokenResolveFilter` 只负责解析 Bearer 令牌并写入请求上下文，不做任何放行判断；
`AccessControlInterceptor` 是唯一的放行决策点 —— 默认拒绝，标了 `@PublicApi` 才对外开放，
`/admin` 前缀与 `@RequireRole` 再校验角色。匿名请求的 `getCurrentId()` 返回 `null`，
不会伪造出一个 id=0 的"假用户"。

### 下载：登录换票，文件直出

1. 未登录也能浏览首页、软件列表与详情页；
2. 点击下载且未登录时弹登录框，并记住要下载的那个安装包；
3. 登录成功后调用 `POST /download/ticket`；
4. 后端校验"软件已发布 + 版本已发布 + 安装包属于该版本"，写下载明细、自增下载量，
   返回 5 分钟有效的 OSS 预签名地址；
5. 浏览器直接跳转到 OSS，安装包不经过应用服务器，大文件不会拖垮后端。

> [!IMPORTANT]
> 下载明细写入与 `download_count` 自增在同一事务内完成；记录里的 IP 只存 SHA-256 摘要，
> User-Agent 截断到 255 字符，不落明文。

### 双语：码值在库，文案在前端

- 内容字段成对存放（`name_zh` / `name_en`），接口一次返回两列，前端按当前语言取值，切语言不重新请求；
- 界面文案全部走 `frontend/src/i18n`，组件内不写死中文；
- 后端只返回稳定的 `errorCode`，由前端翻译，接口语言不影响展示；
- 语言选择写入 localStorage，同时更新 `<html lang>`。

## 接口一览

统一响应：`{ code, errorCode, message, data }`。HTTP 状态码表达大类，业务失败看 `errorCode`。

### 站点主链路

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| GET | `/software` | 公开 | 软件列表（含最新版本、支持平台、下载量） |
| GET | `/software/{slug}` | 公开 | 软件详情（特性、截图、版本、安装包） |
| POST | `/download/ticket` | 需登录 | 换取安装包下载地址（5 分钟有效，限流 30 次/分钟） |
| GET | `/download/mine` | 需登录 | 我的下载记录（`limit` 默认 20，上限 100） |

### 账号

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| POST | `/user/register` | 公开 | 注册，限流 5 次/分钟 |
| POST | `/user/register/admin` | 公开 | 凭邀请码注册管理员，限流 5 次/分钟 |
| POST | `/user/login/username-password` | 公开 | 用户名 + 口令 |
| POST | `/user/login/email-password` | 公开 | 邮箱 + 口令 |
| POST | `/user/login/phone-password` | 公开 | 手机号 + 口令 |
| POST | `/user/login/email-code/send` | 公开 | 发送邮箱验证码（限流） |
| POST | `/user/login/email-code` | 公开 | 邮箱验证码登录 |
| POST | `/user/login/logout` | 需登录 | 注销并吊销当前令牌 |
| GET | `/user/info` | 需登录 | 当前用户资料 |
| PUT | `/user/nickname`、`/email`、`/phone`、`/password` | 需登录 | 修改资料，改密码限流 5 次/分钟 |
| PUT | `/user/avatar` | 需登录 | 上传头像（`multipart`，单文件 10MB） |
| DELETE | `/user` | 需登录 | 普通用户凭密码注销自己；管理员可批量删除 |
| POST | `/files` | 需登录 | 通用文件上传，限流 10 次/分钟 |

### 反馈

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| POST | `/feedback` | 需登录 | 提交反馈 |
| GET | `/feedback` | 管理员 | 分页查看反馈 |
| PUT | `/feedback/status` | 管理员 | 更新处理状态 |
| DELETE | `/feedback/handled` | 管理员 | 清除已处理反馈 |

### 管理（前台尚未接入）

`/admin/**` 下还有 9 个底座遗留接口：新建管理员（仅超管）、生成邀请码、用户分页与详情、改状态、删用户、操作日志、用户计数。
它们由 `@RequireRole` 保护，`/admin` 路径前缀在拦截器里再兜一层角色校验。前台没有入口，阶段 3 会重新设计后再开放。

## 数据与内容维护

- `db/01_schema.sql` 只含 DDL，`db/02_seed.sql` 只含首批上线内容，两者都可重复执行；
- 仓库里不放安装包、截图，也不放任何用户数据行；
- 新增软件或版本走管理后台录入（尚未实现），目标是改数据就能上线，不用改代码；
- 结构变更写成 `1x_*.sql` 增量脚本，不改既有列语义，详见 [db/README.md](db/README.md)。

> [!NOTE]
> 种子数据只有 HundunDesktop 一条软件、5 条特性、2 个平台和 1 个版本（`26.2.1024`），
> 安装包与截图为空。所以克隆后首次打开详情页**不会出现主下载按钮**，版本行显示"暂时没有可下载的安装包"——
> 这是数据缺失，不是页面坏了。

## 测试与构建

```bash
cd backend  && mvn test        # 23 个用例：访问决策 10、令牌解析 6、限流 7
cd frontend && pnpm build      # 产出 dist/
cd frontend && pnpm lint       # oxlint + eslint
```

后端测试是纯单元测试，不加载 Spring 上下文，所以不需要 MySQL、Redis，也不需要 `JWT_SECRET`；
CI 里可以直接跑。

## 路线图

- **阶段 1（进行中）**：前台已完成；剩余管理后台 —— 软件与版本录入、安装包上传 OSS、下载数据概览。
- **阶段 2**：插件社区 —— 卡片列表与详情、`.zip` 上传并校验 manifest、待审核 → 通过后可见、每个插件一个稳定下载地址。
- **阶段 3**：用户中心、反馈处理、后台权限收口。

## 常见问题

> [!WARNING]
> 本地未配置 OSS 时，点击下载会返回 `DOWNLOAD_STORAGE_UNAVAILABLE`（503）。这是预期行为 ——
> 宁可明确报"存储不可用"，也不返回一个点开就 404 的假地址。

- **启动直接失败**：`JWT_SECRET` 缺失或不足 32 字节，这是硬性安全要求，不做降级。
- **访问 `/admin/**` 返回 403（`AUTH_FORBIDDEN`）**：当前账号 `status` 不是 2 或 3，见上文「关于账号」。
- **点下载返回 401（`AUTH_NOT_LOGGED_IN`）**：票据接口不对外公开，先登录再点下载。
- **`pnpm install` 后 Vite 报找不到 rollup**：仓库锁文件是 pnpm 9 格式，请使用 pnpm 9/10；pnpm 12 装出的依赖链接不完整。
- **改了组件页面没变化**：Vite 偶发缓存旧 SFC，重启 dev server。

## 文档

- [docs/00-骨架评审报告.md](docs/00-骨架评审报告.md) —— 底座评估、改造清单与三项方案结论
- [db/README.md](db/README.md) —— 脚本执行顺序与硬规则
- [frontend/README.md](frontend/README.md) —— 前端工程说明

本项目基于[木兰宽松许可证第 2 版](LICENSE)开源。
