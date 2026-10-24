# 馄饨骨架

一五馄饨项目的阶段性产物 

## 项目简介

一个 管理员--邮箱--用户 的管理系统 本着最大程度同时保障用户自由与程序健壮性的设计原则 算是本人的处女之作  
大概是一个全栈程序员被AI打倒前最后的倔强

## 技术栈

### 后端

- **框架**: Spring Boot
- **数据库**: MySQL + MyBatis
- **缓存**: Redis
- **认证**: JWT
- **文件存储**: 阿里云 OSS
- **邮件服务**: SMTP 邮件发送

### 前端

- **框架**: Vue 3
- **构建工具**: Vite
- **状态管理**: Pinia
- **路由**: Vue Router

## 功能模块

### 用户模块

- 用户注册/登录（支持用户名、邮箱、手机号）
- 邮箱验证码登录
- 头像上传
- 用户信息管理
- 管理员邀请码生成 

### 反馈系统

- 用户提交反馈
- 管理员处理反馈
- 邮件通知用户处理结果

### 管理员后台

- 用户管理（查看、删除、禁用）
- 反馈处理
- 邀请管理员

## 项目结构

```
15-hundun/
├── back-end/hundun-lab/          # Spring Boot 后端
│   └── src/main/java/com/hundunlab/site/
│       ├── common/                   # 公共组件
│       │   ├── anno/                 # 自定义注解
│       │   ├── config/               # 配置类
│       │   ├── constant/             # 常量定义
│       │   ├── context/              # 上下文
│       │   ├── exception/            # 异常处理
│       │   ├── response/             # 响应封装
│       │   └── util/                 # 工具类
│       ├── framework/                # 框架层
│       │   ├── aspect/               # AOP 切面
│       │   ├── interception/         # 拦截器
│       │   └── task/                 # 定时任务
│       └── module/                   # 业务模块
│           ├── memo/                 # 备忘录模块
│           └── user/                 # 用户模块
├── front-end/hundun-front/           # Vue.js 前端
│   └── src/
│       ├── api/                      # API 接口
│       ├── assets/                   # 静态资源
│       ├── router/                   # 路由配置
│       ├── stores/                   # 状态管理
│       ├── utils/                    # 工具函数
│       └── views/                    # 页面视图
└── file/
    └── hundun.sql                    # 数据库脚本
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 16+
- MySQL 8.0+
- Redis 6.0+

### 后端配置

1. 创建数据库并导入 `file/hundun.sql`

2. 修改配置文件 `back-end/hundun-lab/src/main/resources/application.yml`

3. 配置阿里云 OSS（可选）

4. 运行项目：

```bash
cd back-end/hundun-lab
mvn spring-boot:run
```

### 前端配置

1. 安装依赖：

```bash
cd front-end/hundun-front
npm install
# 或
pnpm install
```

2. 启动开发服务器：

```bash
npm run dev
```

3. 构建生产版本：

```bash
npm run build
```

## API 接口

### 用户接口

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /user/register | 用户注册 |
| POST | /user/login/username-password | 用户名密码登录 |
| POST | /user/login/email-code | 邮箱验证码登录 |
| POST | /user/login/email-password | 邮箱密码登录 |
| POST | /user/login/phone-password | 手机密码登录 |
| GET | /user/info | 获取用户信息 |
| PUT | /user/avatar | 更新头像 |

### 反馈接口

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /feedback | 获取所有反馈 |
| POST | /feedback | 添加反馈 |
| PUT | /feedback/status | 更新反馈状态 |
| DELETE | /feedback/handled | 删除已处理反馈 |

### 管理员接口

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /users/count | 获取用户数量 |
| PUT | /user/status | 修改用户状态 |
| DELETE | /user/{id} | 删除用户 |
| GET | /logs | 查看操作日志 |

## 配置说明

### Redis 配置

系统使用 Redis 进行缓存和会话管理，主要缓存：
- 用户信息缓存
- 反馈列表缓存
- Token 缓存

### 邮件配置

在配置文件中设置 SMTP 相关参数：
- 邮箱地址
- 授权码
- SMTP 主机地址

## 许可证

本项目仅供学习交流使用。