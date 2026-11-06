# 更新日志

> 格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。
> 本项目尚未打 release tag，版本段落以「阶段」为单位；首次正式发版后改用语义化版本，并把阶段段落归档。
> 类别：`新增` / `变更` / `修复` / `安全` / `文档` / `数据` / `移除`。
> 相关文档：[项目计划书](docs/01-项目计划书.md) ｜ [术语与业务规则](docs/02-术语与业务规则.md) ｜ [数据字典](docs/03-数据字典.md) ｜ [接口文档](docs/04-接口文档.md)

## [未发布]

### 新增

- 2026-11-06 后端扩展站点侧接口（M2 站点侧提前落地，D075 路径）：`module/extension` 新增 `GET /software/{slug}/extensions`（列表，featured DESC、download_count DESC，只含 status=1）、`GET /software/{slug}/extensions/{idOrSlug}`（详情含已发布版本；数字主键或 URL slug，跨软件 404/D072）、`GET /software/{slug}/extensions/{idOrSlug}/download`（302 到最新版预签名 URL + 匿名计数，OSS 未配置 503）；`ErrorCode` 新增 `EXTENSION_NOT_FOUND`；整个控制器 `@PublicApi`（extension_download 匿名可下口径），提交类接口将来单独开控制器
- 2026-11-06 扩展种子数据 `db/03_seed_extension.sql`（D083：官方预置 + 演示社区扩展，与前台 M0 假数据同一批 6 个扩展 9 个版本）；`extension` 表补 `license`/`repo_url` 两列（01_schema 更新 + 存量库走 `db/10_extension_license_repo.sql` 增量）；`bundled` 不加列，由 `authorType=official` 推导
- 2026-11-06 前端 `api/extension.js` 从 M0 假数据切到真接口（页面组件零改动）；扩展详情 README 改从 `readme_url` 拉取 Markdown 文本展示（M0 纯文本，M2 换渲染器；拉不到自动隐藏分区）；i18n 补齐后端已有而前端缺失的 6 个错误码（`USER_NOT_FOUND` / `USER_ALREADY_EXISTS` / `USER_EMAIL_CODE_*` / `USER_EMAIL_SEND_FAILED` / `CONFLICT`）
- 2026-11-06 后端测试补 `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker = mock-maker-subclass`：受限 CI/沙盒不允许 byte-buddy 自附着（self-attach）时 inline maker 全量崩，subclass maker 无此依赖且对现有 23 个用例等效
- 2026-11-06 M0c④ 扩展前端（F07/F08）：`components/ExtensionSection.vue`（软件详情页 `#extensions` 分区，精选 ≤6、计 0 不渲染、失败分区内重试）与 `views/site/ExtensionPage.vue`（面包屑回锚点、版本历史、README、permissions 原样、SHA256 前 12 位；官方徽章与社区风险提示按 Q007 切换）；`api/extension.js` 以 M0 假数据提供与 schema 对齐的 6 个扩展（D083），M2 只换实现层
- 2026-11-06 M0c④ 扩展提交（F09）：`views/site/ExtensionSubmitPage.vue` 落地 4 项礼貌预检（≤20MB / zip 魔数 / manifest 存在 / id 合法，D051/D052），zip 走手写中央目录解析 + `DecompressionStream('deflate-raw')`，manifest 内容自动预填表单；提交按钮明确提示「M2 开放」，不假装成功
- 2026-11-06 M0c④ 用户页（F11）：`views/site/UserInfoPage.vue` 只读基本资料 + 我的扩展空态引导（不直连上传页，按 D074 回软件详情页）
- 2026-11-06 新增路由 `/software/:slug/extensions/submit|:id` 与 `/user/info`（F08/F09/F11 路由表落地），`scrollBehavior` 支持锚点（页头 68px 固定所以上移 84px）；`beforeEach` 守卫 `/user/**` 未登录跳登录并带 `redirect` 回跳
- 2026-11-06 i18n 新增 `extension` / `extSubmit` / `user` 文案块与 `errors.EXTENSION_NOT_FOUND`；中英键表同步
- 2026-11-05 新增 `composables/useTheme.js` 与 `components/ThemeToggle.vue`：浅/深切换写 `hundunlab.theme`，同步 `<html data-theme>` 与 `color-scheme`（原生滚动条、表单控件一起换）；页头常驻，窄屏也不收
- 2026-11-05 新增 `frontend/src/styles/{tokens,base,components}.css` 三层：`tokens.css` 是全站唯一色值出处，同时提供浅色与 `[data-theme="dark"]` 两套令牌（含物料层 `--hd-*` 与「实物」组 `--hd-cart-*` / `--obj-console-*`）
- 2026-11-05 新增语义令牌：`--accent-text`（浅=主色/深=白）、`--accent-soft`、`--on-accent`、`--teal-text`、`--chip-on-{bg,ink,border}`、`--btn-primary-*`、`--tag-*`（含 `--tag-{red,teal}-{bg,ink,border}`）、`--line-soft`/`--line-faint`/`--dot`/`--scan`、`--shadow-pixel-{xs,sm,lg}`
- 2026-11-05 新增 `components/FormField.vue`：登录页与登录弹窗共用的表单字段，样式只留一处；`label` 走 `for/id` 关联，`useId` 保证同页多表单不撞 id
- 2026-11-05 页头新增窄屏汉堡菜单与抽屉（≤860px），导航、语言切换、登录态收进抽屉；换页与 ESC 自动收起，`aria-expanded` 跟随
- 2026-11-05 新增 `frontend/scripts/check-color.mjs`、`check-i18n.mjs`（O11）：前者禁样式里的十六进制与 `rgb()`/`hsl()` 字面量（`*tokens.css` 豁免），后者禁组件里的 CJK 字面量（`src/i18n/` 与注释豁免）；配套 `lint-baseline.json` 做基线棘轮，违规只许减不许增
- 2026-11-05 `lint` 改为 `oxlint + prettier --check + check-i18n + check-color`（D059），并补装 `oxlint`（此前脚本引用了但没装）；新增 `pnpm lint:baseline` 用于违规减少后收窄基线

### 变更

- 2026-11-06 M0c④ 撤 `/plugins`（D074/D076）：导航收为两项（首页 / 软件），首页 hero 次按钮改指 `/software/hundundesktop#extensions`；`/plugins` 重定向回该锚点（Q009 的 301 兜底，桌面端 `COMMUNITY_URL` 切换前的落点）
- 2026-11-06 F07 展示口径修正：初始只露出精选扩展（≤6），非精选的进「查看全部」内联展开（每页 12），与 PRD F07 一致（此前实现会把非精选直接平铺）
- 2026-11-05 M0c③ 换肤：`assets/css/site.css` 与 `assets/css/base.css` 拆成 `styles/{tokens,base,components}.css` 后删除（`main.js` 按新顺序引入），余下 72 处色值字面量全部收进令牌；`check-color` 基线归零，两个质量脚本进入严格模式
- 2026-11-05 深色主题在前端正式落地（D087）：状态件拆成 `bg/ink/border` 三个令牌，深色下由令牌整组换成「绿描边 + 白字 + ≤12% 绿底」，组件里不写 `[data-theme]` 判断；硬投影 `--shadow-pixel*` 在深色改绿光晕（原来固定 `var(--ink)`，会变成白投影）
- 2026-11-05 「实物」单独成组：卡带与首页掌机不随主题漂绿，深色下只换卡内文字墨色（`--hd-cart-ink` 系）与掌机塑料底色一档；`.hd-cart` 内的平台标签与角标改用 `--hd-cart-tag-*`，否则深色的白字会压在米纸上
- 2026-11-05 结构线统一走 `--line`（浅色与 `--ink` 同值，深色为绿描边）：页头、页脚、hero、详情页分节线、表单输入框、汉堡按钮
- 2026-11-05 登录弹窗无障碍：ESC 关闭、打开时焦点进首个输入框、关闭后焦点归还触发元素，补 `aria-describedby` 与错误行 `role="alert"`
- 2026-11-05 `index.html` 增加内联同步脚本，首屏绘制前读 `hundunlab.theme` 设 `data-theme`，消除深色用户刷新时的 FOUC 闪白；隐私模式下 `localStorage` 抛错则回落浅色
- 2026-11-05 新增令牌 `--field`（输入框底色）与 `--overlay`（弹窗遮罩），替换组件里写死的 `#fffdf7` 与 `rgba(...)`
- 2026-11-05 深色主题用色改为「绿只做描边、文字用白」（D087）：`--ink` 系列改白/灰白，`--line` 系列改绿描边；导航选中、筛选胶囊、主按钮、状态徽章、版本标签、扩展图标底板、上传校验块等原「实心绿底 + 反白字」组件统一降为「绿描边 + 白字」（层级用 ≤12% 绿透明底）
- 2026-11-05 新增 `--accent-text` 令牌（浅色=主色，深色=白），替换 34 处 `color: var(--red)` 与页内红字内联样式（改用 `.text-accent`、`.check-mark--on`、`.badge--ready`）
- 2026-11-05 原型页脚统一：首页的 `.foot`（品牌 + 标语 + 三个红方块）提到 `assets/proto.css`，02/03/03b/05/06/07 六页换掉旧单行 `.site-footer`
- 2026-11-05 网页文案统一「扩展」（D071）：03 分区标题与按钮、05 面包屑与下载按钮、06 标题与字段名、07「我的扩展」、目录页卡片名；文件名保留 `05/06-plugin-*`
- 2026-11-05 05 扩展详情按 Q007 落地：社区扩展加「来源」行与固定风险提示，官方扩展只留「官方提供」徽章不显示提示

### 修复

- 2026-11-06 修复扩展二级页路由没配 `props: true`，`slug`/`id` 传不进页面组件、详情页恒 404 的问题（`extension-detail` 与 `extension-submit` 两条路由）
- 2026-11-06 修复 `ExtensionSubmitPage` 漏 `import FormField`——未注册组件被渲染成空的自定义元素，提交表单整体不可见（Playwright 实测暴露）
- 2026-11-05 修复深色下卡片内平台标签/角标不可读：`.hd-cart__tags .hd-tag` 与 `.hd-cart__badge` 收回卡带自己的色（白字压米纸的问题与原型 `.cart` 是同一个坑）
- 2026-11-05 深色主题修复：13 处 `rgba(195, 39, 43, ·)` / 3 处 `rgba(29, 22, 19, ·)` 字面量改为 `color-mix(var(--red|--ink))`，否则深色下是看不见的暗红边；06 上传区底色字面量同样令牌化；卡带（`.cart`）在深色下就地重声明一套浅色令牌，避免卡内文字被全局白字吞掉
- 2026-11-05 02 列表页删掉「已下架」演示卡（违反 F03 只展示 `status = 1`）、平台标签由 macOS 对齐为 Linux、HundunDesktop 卡补「有扩展」标记（Q008）、卡片数与「N 款软件」对齐为 5
- 2026-11-05 版式修复：卡片栅格底部留 14px 让落影不被页脚切断；`.ext` 补末留白；`.page-section`／页脚纵向微调使 03b 回到一屏（03 因并入扩展分区改为长页，属预期）

### 移除

- 2026-11-06 删除 `views/site/PluginsPage.vue`（旧 `/plugins` 占位页，D074 废弃站点级入口；路由改为重定向）
- 2026-11-05 M0c① 删旧层：`views/{admin,user,login,help}` 27 文件 / 7,252 行、`api/{user,admin,feedback,article}.js`、`stores/{user,article,feedback}.js`、`utils/`（含旧 `request.js`）、`assets/img/` 13 张旧图 1.4MB；依赖移除 `element-plus`、`@element-plus/icons-vue`、`less`、`marked`（构建产物逐字节未变，确认均为死代码）
- 2026-11-05 `HdCart` 不再内置中文字面量：`downloadsLabel` 默认值置空、新增 `detailLabel` prop，文案由 `SoftwareCard` 传 `t('home.downloads')` / `t('home.viewDetail')`；语言自称移入 `i18n` 的 `LOCALE_LABELS`

### 文档

- 2026-11-06 接口文档修订（D071 后置）：§2 矩阵补扩展站点侧三接口；§5.2 改「M2 扩展社区」并标记已实现/设计态，旧 `/plugin/**` 设计稿作废；§4.5 注明命名 `PLUGIN_*` → `EXTENSION_*`；db/README 执行顺序补 03/10 脚本
- 2026-11-06 M0c④ 收尾：`00-CONTEXT` 标记四步全完成；台账新增 **O14**（Vue 前台版式向 M0b 原型迁移，`HomePage` 仍是旧版）；本文件补 M0c④ 各条
- 2026-11-05 O13 登记：浅色 `--ink-faint` 压纸底实测 2.96:1，低于 AA 小字 4.5:1（深色 4.93:1 无问题），是否加深留作者裁
- 2026-11-05 D041／D057／D087 各补一条「落地注」：令牌从物料库搬进 `styles/tokens.css`、层级顺序、状态件拆三令牌与「实物」不换肤的口径
- 2026-11-05 `docs/design/kit/index.html` 补引 `tokens.css`（物料库自身已不含色值）
- 2026-11-05 新增决策 D087（深色主题绿色只做描边、文字用白），同步原型 README 的深色用色说明
- 2026-11-05 O11 关闭：质量脚本已落地，台账与 `00-CONTEXT` 同步；M0c 第①步「删」完成，下一步第②步「修」
- 2026-11-05 G09 八问集中裁决：Q001–Q003、Q005–Q009 全部已答，待答归零；O09 关闭，O08／O12 定案（`docs/prd/02` / `03` / `04`）
- 2026-11-05 原型 README 增补「细节收口」段与一屏口径（7 页一屏、03 长页）
- 2026-10-29 新增 `docs/01-项目计划书.md`（立项书：M0 前台重构 / M1 管理后台+上线 / M2 插件社区 / M3 桌面端对接）
- 2026-10-29 新增 `docs/02-术语与业务规则.md`（命名硬规则、术语词典、角色权限矩阵、业务不变量）
- 2026-10-29 新增 `docs/03-数据字典.md`（ER 图、字段说明、枚举字典、外键与索引、增量脚本规则）
- 2026-10-29 新增 `docs/04-接口文档.md`（已实现接口 + M1/M2 设计态接口 + 错误码总表）
- 2026-10-29 新增本文件，并回填 2026-10-14 起的全部提交记录

## [阶段 1 · 官网前台] - 2026-10-24

骨架改造完成，官网前台（首页 / 软件列表 / 软件详情 / 登录 / 需登录的下载）可跑通。

### 新增

- `1fcbcbd` 软件与版本的匿名只读接口（`GET /software`、`GET /software/{slug}`）
- `4df352b` 需登录的下载票据与下载记录（`POST /download/ticket`、`GET /download/mine`）
- `776e158` 官网前台首页、软件页与登录下载流程
- `3ab6ddc` 数据库重建为 schema-only 脚本与种子内容（`01_schema.sql` / `02_seed.sql`，幂等）
- `427eb12` 鉴权改为匿名/登录双通道，修正匿名身份语义（不再伪造 id=0 的假用户）

### 变更

- `e041c9a` 移除死代码、历史数据与冗余依赖
- `ebc1247` 统一仓库目录与包名为 `hundun-lab`（包名 `com.hundunlab.site`）
- `e0380f7` 修正限流测试与当前接口签名、信任策略不一致
- `115a266` 数据：首个版本的更新说明改为基于 README 的真实内容

### 文档

- `f5dba5c` 补充阶段 0 骨架评审报告（`docs/00-骨架评审报告.md`）
- `a7a7dea` 重写 README 为中英双份站点说明
- `1744a71` 按当前实现校正 README 与脚本说明

## [底座继承 · 安全加固] - 2026-10-14 ~ 2026-10-22

以下提交继承自开发底座 `15-hundun`，是本站安全能力的来源，**不要因为它们"不是本站写的"而回退**。

### 安全

- `65e15f4`（10-22）P0/P1：令牌吊销（登出即失效）、注册限流、日志脱敏
- `99dd9ff`（10-21）修复删除越权、收紧 OPTIONS 预检、外置数据库凭据
- `a182c8c`（10-21）P2：IP 伪造、JSON/SQL 注入、分页与管理员层级
- `dc2e06e`（10-21）P1：日志脱敏、CORS 白名单、上传校验、Redis 降级
- `142200c`（10-21）P0：凭据外置、RBAC、Redis 反序列化白名单
- `202a914`（10-15）登录与验证码发送端点补限流（防暴力破解与邮件轰炸）

### 变更

- `799bb73`（10-20）移除阿里云 AK/SK 硬编码，改用本地忽略配置
- `27ddbac`（10-14）前端本地端口固定为 5577，忽略 npm 生成的 package-lock

---

## 维护说明

- 每个改动先写进本文件的「未发布」，发版/阶段完成时改名并加日期。
- 破坏性变更（接口语义、字段含义、脚本不兼容）必须在条目开头标 `**破坏性**`。
- 条目写法：动词开头 + 影响范围 + 一句话原因；不写"优化了一些东西"。
