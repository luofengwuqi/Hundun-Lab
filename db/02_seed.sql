-- ============================================================
-- Hundun Lab 官网 · 初始内容数据（schema-only 仓库的例外：仅种子内容）
-- 说明：
--   1. 不含任何用户行、反馈行、日志行
--   2. 全部 INSERT IGNORE，重复执行不会覆盖后台已编辑的内容
--   3. 安装包（release_asset）与截图（software_screenshot）故意留空：
--      二进制与图片必须由管理后台上传，不能写死在仓库里
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 软件：HundunDesktop
-- ------------------------------------------------------------
INSERT IGNORE INTO `software`
(`id`, `slug`, `name_zh`, `name_en`, `tagline_zh`, `tagline_en`, `intro_zh`, `intro_en`,
 `tech_stack`, `repo_url`, `license`, `cover_url`, `theme_color`, `status`, `sort_order`)
VALUES
(1, 'hundundesktop', '馄饨桌面', 'HundunDesktop',
 '复古掌机风格的桌面效率工具',
 'A retro handheld-style desktop productivity tool',
 'HundunDesktop 以游戏掌机为隐喻：母程序是一台 800×540 的固定尺寸掌机，每个功能都是一张可插拔的卡带。插入插槽，子程序就在自己的窗口中启动；拔出一张卡带，其他卡带不受影响。\n\n内置四张卡带——计算器、文本管理器、任务清单、脚本管理器，开箱即用；也可以从 `.zip` 安装第三方插件。\n\n数据默认保存在本机，无需账号与网络。',
 'HundunDesktop borrows the metaphor of a handheld console: the host program is a fixed 800×540 console, and every feature is a cartridge you plug into a slot. Insert a cartridge and its subprogram starts in its own window; eject it and the others keep running.\n\nFour cartridges ship built in — calculator, text manager, task list and script runner. Third-party plugins can also be installed from a `.zip` file.\n\nYour data stays on your machine by default: no account, no network required.',
 'Electron 28, Vue 3, Pinia, Vue Router, electron-vite, electron-builder',
 NULL, 'MIT', NULL, '#C3272B', 1, 1);

-- ------------------------------------------------------------
-- 功能特性
-- ------------------------------------------------------------
INSERT IGNORE INTO `software_feature`
(`id`, `software_id`, `icon`, `title_zh`, `title_en`, `desc_zh`, `desc_en`, `sort_order`)
VALUES
(1, 1, '⚡', '插件化架构', 'Plugin architecture',
 '每个子程序运行在独立窗口与进程中，单个插件崩溃不会拖垮母程序。',
 'Every subprogram runs in its own window and process, so a crashing plugin never takes down the host.', 1),
(2, 1, '🕹️', '复古视觉', 'Retro visuals',
 '像素字体、CRT 扫描线、卡带入仓动画，深色与浅色双主题。',
 'Pixel type, CRT scanlines and a cartridge-insert animation, with dark and light themes.', 2),
(3, 1, '⌨️', '键盘优先', 'Keyboard first',
 'WASD 或方向键在卡带间移动，空格 / Enter 确认，Esc 返回。',
 'Move between cartridges with WASD or the arrow keys, confirm with Space / Enter, go back with Esc.', 3),
(4, 1, '💾', '本地优先', 'Local first',
 '数据保存在本机 JSON 与 localStorage，不依赖账号与网络。',
 'Data lives in local JSON files and localStorage — no account or network required.', 4),
(5, 1, '🧩', '插件安装', 'Plugin install',
 '从 .zip 安装第三方插件，在设置页统一排序与卸载。',
 'Install third-party plugins from a .zip file and manage ordering or removal in Settings.', 5);

-- ------------------------------------------------------------
-- 平台支持
-- ------------------------------------------------------------
INSERT IGNORE INTO `software_platform`
(`id`, `software_id`, `platform`, `note_zh`, `note_en`, `sort_order`)
VALUES
(1, 1, 'win32', 'Windows 10 / 11，NSIS 安装包', 'Windows 10 / 11, NSIS installer', 1),
(2, 1, 'linux', 'AppImage / deb（x64）', 'AppImage / deb (x64)', 2);

-- ------------------------------------------------------------
-- 首个版本（安装包由后台上传后才会出现下载按钮）
-- ------------------------------------------------------------
INSERT IGNORE INTO `software_release`
(`id`, `software_id`, `version`, `channel`, `notes_zh`, `notes_en`, `published_at`, `status`)
VALUES
(1, 1, '26.2.1024', 'stable',
 '首个公开版本：内置计算器、文本管理器、任务清单、脚本管理器四张卡带，支持从 .zip 安装第三方插件。',
 'First public release: ships four built-in cartridges (calculator, text manager, task list, script runner) and supports installing third-party plugins from a .zip file.',
 NOW(), 1);
