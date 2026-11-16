# Hundun Lab 品牌标识

> **唯一来源（D088，2026-11-16）**：本目录是站点一切 logo 的素材来源。页面、后台、favicon、文档与导出图一律从本目录取用，
> 以 `hundun-lab-mark.svg` 及其变体为准，不再重新生成或另画。具体场景选型见下文「用法」。

## 这套东西从哪来

不是从零画的，是从馄饨系列已有的东西上长出来的：

| 部件 | 来源 | 说明 |
| --- | --- | --- |
| 馄饨轮廓 | `hundunDesktop/build/icons/512x512.png` | 等值线追踪描出来的矢量，与桌面端图标、站点背景图同源，是家族胎记 |
| 中文副标 | 马善政毛笔楷书（`_build/fonts/`） | 渲染后转成路径，保留手写的笔锋和飞白 |
| 英文主标 | 自绘 | 10×12 网格、笔画粗细 2、全直角、宽字距，接站内 `HUNDUN LAB` 的等宽血统 |
| 配色 | 馄饨系列四代一致 | 朱红 / 墨黑 / 馄饨盒米白 |

一句话：**外壳是手作的（毛笔 + 手绘馄饨），内核是技术的（几何等宽 + LAB）**，分裂本身就是设计语言。

## 配色

| 色 | 值 | 用途 |
| --- | --- | --- |
| 朱红 | `#C3272B` | 图形标、中文副标、强调 |
| 墨黑 | `#17130F` | 英文主标、深色底 |
| 米白 | `#FFF8E8` | 底色、反白版线条 |
| 次级底 | `#E9DCC6` | 卡片、分隔（非 logo 本体） |

## 文件

| 文件 | 用途 |
| --- | --- |
| `hundun-lab-lockup.svg` | **主用**。横版组合，README 首图、页头、名片 |
| `hundun-lab-lockup-inverse.svg` | 深色底反白版（页脚、深色主题） |
| `hundun-lab-mark.svg` | 图形标，内含 `LAB`。头像、App 图标、社交预览 |
| `hundun-lab-mark-plain.svg` | 图形标无字版。需要更干净、或要叠字时用 |
| `hundun-lab-mark-a.svg` | 图形标内含单字母 `A`。单字没有字距撑着，层高放到三字版的 1.6 倍才立得住 |
| `hundun-lab-wordmark.svg` | 纯字标，窄条位置（页脚版权行、加载页） |
| `hundun-lab-wordmark-inverse.svg` | 纯字标反白 |
| `hundun-lab-favicon.svg` | 浏览器标签页矢量图标（自动适配） |
| `hundun-lab-favicon.ico` | 16/24/32/48/64/128/256 七档，每档单独光栅化 |
| `png/` | 透明底位图导出，`@2x` 给 retina |
| `brand-sheet.png` | 一页纸总览，给人看的 |

## 用法

- **最小尺寸**：横版组合 ≥ 160px 宽；图形标 ≥ 24px。16/24px 用 `.ico` 里的「粗环无褶」档，别拿大图硬缩。
- **留白**：四周不小于图形高的 25%，谁都别往里塞字。
- **别动的东西**：不拉伸、不改色、不加投影和描边、不把副标换成别的叫法（对外写法见 `docs/02-术语与业务规则.md`）。
- **深色底**只用 `-inverse` 版；正片版压在深色上会糊成一团。

## 重建

```bash
cd docs/design/logo/_build
python3 extract.py   # 从桌面端图标 + 毛笔字体重新抽取几何 → assets.json
python3 build.py     # 生成全部 SVG / PNG / ICO
python3 sheet.py     # 生成 brand-sheet.png 与 preview/
```

改字形只要改 `letters.py`（每个字就是一组多边形），改比例改 `build.py` 里的 `LOCKUP`。

`_build/fonts/MaShanZheng-Regular.ttf`（5.6 MB）只在重新跑 `extract.py` 时用得上；`assets.json` 已经缓存了描好的路径，不需要重抽的话可以把它排除在版本库外。
