# Hundun Lab · 前端

官网前台的 Vue 3 应用：首页、软件列表与详情、登录、下载。

## 开发

```bash
pnpm install
pnpm dev        # http://localhost:5577
pnpm build      # 产出 dist/
pnpm lint       # oxlint + eslint
pnpm format     # prettier
```

开发服务器把 `/api` 代理到 `http://localhost:5757`（见 `vite.config.js`），跨域无需额外配置。

## 目录

```
src/
├── api/            # http.js（统一出口、ApiError）与 site.js（站点接口）
├── assets/css/     # site.css 设计系统；base.css 为底座遗留
├── components/     # SiteHeader / SiteFooter / SoftwareCard / AuthDialog
├── i18n/           # zh.js / en.js；locale 持久化与 localized() 取值
├── layouts/        # SiteLayout
├── router/         # History 路由，前台全部匿名可访问
├── stores/         # session.js 会话状态
└── views/site/     # HomePage / SoftwareListPage / SoftwarePage / LoginPage / PluginsPage
```

## 约定

- 组件内不写死中文，全部走 `t('key')`，中英文案同时补齐。
- 内容字段由后端成对返回（`*_zh` / `*_en`），用 `localized()` 按当前语言取值。
- 接口失败抛 `ApiError`，页面按 `errorCode` 查文案，不展示后端返回的 message。
- 路由不设全局登录守卫：匿名可以浏览一切，只有下载动作要求登录。
- 接口基地址是写死的 `/api`，开发期靠 `vite.config.js` 代理，线上靠同源反向代理或 CORS。
- 路由是 History 模式，部署时服务器要把未知路径回落到 `index.html`（如 `try_files $uri /index.html`）。

## 遗留文件

`views/{admin,login,user,help}`、`api/{admin,user,feedback,article}.js`、`utils/`、`stores/{user,article,feedback}.js`
是从底座 15-hundun 带过来的旧实现，目前没有挂到路由上，也不会被打包引用。阶段 3 重建后台时替换或删除，
不要在新页面里引用它们。
