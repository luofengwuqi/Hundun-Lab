-- ============================================================
-- Hundun Lab 官网 · 扩展种子数据（D083：官方扩展预置进库）
-- 说明：
--   1. 依赖 01_schema.sql（extension 表需含 license / repo_url 两列）
--      与 02_seed.sql（software_id = 1 的 HundunDesktop）
--   2. 全部 INSERT IGNORE，重复执行不覆盖
--   3. 与前台 M0 假数据（api/extension.js 旧 mock）同一批演示内容；
--      extension_release 的 object_key 是占位对象键，真实安装包
--      由管理后台上传（M1），OSS 未配置时下载短链返回
--      DOWNLOAD_STORAGE_UNAVAILABLE，属预期
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 扩展（software_id = 1 HundunDesktop）
-- 精选 4 个：calculator / fasttxt / hundundock / pixelruler（F07 精选 ≤6）
-- ------------------------------------------------------------
INSERT IGNORE INTO `extension`
    (`id`, `software_id`, `plugin_id`, `slug`, `name_zh`, `name_en`,
     `summary_zh`, `summary_en`, `author_type`, `author_name`,
     `permissions`, `platform`, `license`, `repo_url`,
     `status`, `featured`, `download_count`, `sort_order`, `created_at`)
VALUES
    (1, 1, 'calculator', 'calculator', 'Calculator', 'Calculator',
     '内置计算器，支持表达式与单位换算',
     'Built-in calculator with expressions and unit conversion',
     'official', NULL,
     NULL, 'win32,darwin,linux', 'MIT', 'https://github.com/example/hundundesktop',
     1, 1, 0, 1, '2026-10-28 10:00:00'),
    (2, 1, 'fasttxt', 'fasttxt', 'FastTxt', 'FastTxt',
     '随手一段文本，落到桌面就存下',
     'Capture text snippets straight to the desktop',
     'official', NULL,
     NULL, 'win32,darwin,linux', 'MIT', 'https://github.com/example/hundundesktop',
     1, 1, 0, 2, '2026-10-28 10:05:00'),
    (3, 1, 'hundundock', 'hundundock', 'HundunDock', 'HundunDock',
     '桌面底部快捷启动坞',
     'A quick-launch dock at the bottom of the desktop',
     'community', '@ashimmer',
     'launcher', 'win32,linux', 'Apache-2.0', 'https://github.com/ashimmer/hundundock',
     1, 1, 2314, 3, '2026-05-06 09:00:00'),
    (4, 1, 'pixelruler', 'pixelruler', 'PixelRuler', 'PixelRuler',
     '屏幕取色与像素测量',
     'Color picking and pixel measuring on screen',
     'community', '@homo',
     'screen,clipboard', 'win32,darwin,linux', 'MIT', 'https://github.com/homo/pixelruler',
     1, 1, 4021, 4, '2026-05-19 14:00:00'),
    (5, 1, 'sounbar', 'sounbar', 'SounBar', 'SounBar',
     '可视化音量条',
     'A visual volume bar',
     'community', '@chitose',
     'audio', 'win32', 'MIT', 'https://github.com/chitose/sounbar',
     1, 0, 2870, 5, '2026-06-11 11:00:00'),
    (6, 1, 'clipsync', 'clipsync', 'ClipSync', 'ClipSync',
     '剪贴板跨设备同步',
     'Clipboard sync across devices',
     'community', '@nightowl',
     'clipboard,network', 'win32,darwin', 'MIT', 'https://github.com/nightowl/clipsync',
     1, 0, 1560, 6, '2026-10-02 16:00:00');

-- ------------------------------------------------------------
-- 扩展版本（status=1 全部已通过；published_at 倒序即最新版，D064）
-- ------------------------------------------------------------
INSERT IGNORE INTO `extension_release`
    (`id`, `extension_id`, `version`, `changelog_zh`, `changelog_en`,
     `object_key`, `file_size`, `sha256`, `status`, `download_count`, `published_at`)
VALUES
    -- Calculator / FastTxt：随桌面端首个公开版本一起发布
    (11, 1, '1.0.0', '随桌面端首个公开版本一起发布。', 'Shipped with the first public release.',
     'extensions/calculator/1.0.0/calculator-1.0.0.zip', 18432,
     '11ff22aa33bb44cc55dd66ee770011aa22bb33cc44dd55ee66ff778899aa00bb', 1, 0, '2026-10-28 10:00:00'),
    (12, 2, '1.0.0', '随桌面端首个公开版本一起发布。', 'Shipped with the first public release.',
     'extensions/fasttxt/1.0.0/fasttxt-1.0.0.zip', 20976,
     '22ee33dd44cc55bb66aa77ff880011bb33cc44dd55ee66ff778899aa00bb11cc', 1, 0, '2026-10-28 10:05:00'),
    -- HundunDock：两个版本
    (13, 3, '0.4.1', '修复高分屏下图标模糊。', 'Fix blurry icons on HiDPI screens.',
     'extensions/hundundock/0.4.1/hundundock-0.4.1.zip', 1302528,
     '33dd44ee55ff66aa77bb88cc990022cc44dd55ee66ff778899aa00bb11cc22dd', 1, 1502, '2026-09-14 09:30:00'),
    (14, 3, '0.4.0', '支持拖拽排序。', 'Drag to reorder.',
     'extensions/hundundock/0.4.0/hundundock-0.4.0.zip', 1289216,
     '44cc55dd66ee77ff88aa99bb001133dd55ee66ff778899aa00bb11cc22dd33ee', 1, 812, '2026-07-02 09:00:00'),
    -- PixelRuler：三个版本
    (15, 4, '1.2.0', '新增放大镜取色。', 'Magnifier color picking.',
     'extensions/pixelruler/1.2.0/pixelruler-1.2.0.zip', 1258291,
     '55bb66cc77dd88ee99ff00aa112244ee66ff778899aa00bb11cc22dd33ee44ff', 1, 2140, '2026-10-20 15:00:00'),
    (16, 4, '1.1.0', '支持多屏测量。', 'Multi-monitor measuring.',
     'extensions/pixelruler/1.1.0/pixelruler-1.1.0.zip', 1205862,
     '66aa77bb88cc99dd00ee11ff223355ff778899aa00bb11cc22dd33ee44ff55aa', 1, 1218, '2026-08-02 15:00:00'),
    (17, 4, '1.0.0', '首个公开版本。', 'First public release.',
     'extensions/pixelruler/1.0.0/pixelruler-1.0.0.zip', 1179648,
     '779988aa00bb11cc22dd33ee44ff55aa778899aa00bb11cc22dd33ee44ff55aa66', 1, 663, '2026-05-19 14:00:00'),
    -- SounBar / ClipSync
    (18, 5, '2.1.0', '音量条支持自定义配色。', 'Customizable colors.',
     'extensions/sounbar/2.1.0/sounbar-2.1.0.zip', 943718,
     '8877665544332211ffeeddccbbaa99888877665544332211ffeeddccbbaa9988', 1, 2870, '2026-08-30 11:00:00'),
    (19, 6, '0.9.0', '首个公开版本。', 'First public release.',
     'extensions/clipsync/0.9.0/clipsync-0.9.0.zip', 2202009,
     '99aa88bb77cc66dd55ee44ff3322110099aa88bb77cc66dd55ee44ff33221100', 1, 1560, '2026-10-02 16:00:00');
