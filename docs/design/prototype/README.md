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

## 字体

标题用 **Fusion Pixel 12px（缝合像素字体）**，SIL OFL 1.1，可商用。字体文件在 `assets/fonts/fusion-pixel-12px-sc.woff2`（589 KB，含简体中文），许可原文同目录 `LICENSE-OFL.txt`。

- 像素字体的设计网格是 12px，**只在 12 的整数倍字号下最锐利**：现用 96 / 64 / 48 / 40 / 24px。
- 字重固定 400，不要加 `font-weight: bold`——浏览器合成粗体会让像素边缘糊掉。
- 正文、卡片名、按钮用系统无衬线（微软雅黑），小标签与数字用等宽。

## 一屏显示

除登录页（满屏 100vh 设计）外，**9 页全部能在 1536×1024 一屏内完整显示**，页脚不被裁切。实测内容高度：

| 页面 | 内容高度 | 页面 | 内容高度 |
| --- | --- | --- | --- |
| `01-home` | 972 | `04-plugins` | 1013 |
| `02-software-list` | 1016 | `05-plugin-detail` | 1005 |
| `03-software-detail` | 1015 | `06-plugin-submit` | 995 |
| `03b-software-detail-2` | 1007 | `07-user-info` | 937 |

为此收紧的都是**纵向间距**，不改字号与横向尺度：分节间距 22→12、面板内边距 20→13、页脚 18/20→14/16、表格行 13→9、预览缩略图限高 134px、输入框 44→40、卡片最小高 244→232。原 `06` 页那段「单独压缩间距」的内联 `<style>` 已删掉，改由共享样式统一处理。

## 与设计图的差异

1. **像素标题**已用真字体还原，字形与设计图接近；设计图里的字是 AI 直接画的，个别笔画不完全一致属正常。
2. **掌机插画**改用内联 SVG 重绘（`01`、`03` 两页），线条比图里更规整。
3. **06 上传页**表单字段较密，已压到 995px 能在 1024 内放下，代价是纵向留白比列表页小一些。
4. **主题切换按钮**是占位（`title` 已标注「延后实现」）。深色主题的令牌已预留，补一组 `[data-theme="dark"]` 覆盖 `:root` 即可。
5. 尺寸按 `docs/design/mockups/` 的实测坐标校准：版心 1312px、页头 90px、hero 标题 96px、卡片 236px 起、面板圆角 16px。

## 用法

- 直接打开：双击 `index.html`。
- 截图：`chrome --headless --window-size=1536,1024 --screenshot=out.png file:///.../01-home.html`（原型按 1536×1024 视口设计）。
- 后续落地成 Vue 时，`assets/proto.css` 的令牌与组件类可直接沿用，页面结构按组件拆分即可。
