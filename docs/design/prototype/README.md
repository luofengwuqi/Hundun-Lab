# 前台原型（M0a 浅色）

`docs/design/mockups/` 下 9 张设计图的 HTML 落地版。**双击 `index.html` 即可浏览**，不需要 Node、不需要构建、不依赖网络。

## 文件

| 文件 | 对应设计图 |
| --- | --- |
| `index.html` | 目录页，9 张图的入口 |
| `01-home.html` | `01-home.png` |
| `02-software-list.html` | `02-software-list.png` |
| `03-software-detail.html` | `03-software-detail.png` |
| `03b-software-detail-2.html` | `03b-software-detail-2.png` |
| `04-plugins.html` | `04-plugins.png` |
| `05-plugin-detail.html` | `05-plugin-detail.png` |
| `06-plugin-submit.html` | `06-plugin-submit.png` |
| `07-user-info.html` | `07-user-info.png` |
| `08-login.html` | `08-login.png` |

## 结构

- `assets/proto.css` — 唯一一份样式表，顶部是设计令牌（色板 / 字体 / 尺度 / 阴影），下面按组件分节：页头、按钮、卡带卡、插件卡、徽标、表格、表单、登录页。
- 页面之间靠普通 `<a>` 互链，导航可点：首页 ↔ 软件列表 ↔ 详情 ↔ 插件列表 ↔ 插件详情 ↔ 上传 ↔ 个人信息。登录页刻意不带页头，被设计为独立入口。

## 与设计图的差异

1. **像素标题字体缺失。** 设计图里的中文像素字是 AI 生成时画的，本机没有对应字体文件，原型用 `Arial Black` + 微软雅黑加粗近似，字面比图里更「印刷体」一些。要还原像素感，需引入 Zpix 或方正像素类字体（ttf 丢进 `assets/`，改 `--font-display`）。
2. **掌机插画**改用内联 SVG 重绘（`01`、`03` 两页），线条比图里更规整。
3. **06 上传页**信息密度高，1024 高度内放不下，底部按钮需滚动一点。这是有意的——表单页按真实滚动处理，不硬压。
4. **主题切换按钮**是占位（`title` 已标注「延后实现」）。深色主题的令牌已预留，补一组 `[data-theme="dark"]` 覆盖 `:root` 即可。

## 用法

- 直接打开：双击 `index.html`。
- 截图：`chrome --headless --window-size=1536,1024 --screenshot=out.png file:///.../01-home.html`（原型按 1536×1024 视口设计）。
- 后续落地成 Vue 时，`assets/proto.css` 的令牌与组件类可直接沿用，页面结构按组件拆分即可。
