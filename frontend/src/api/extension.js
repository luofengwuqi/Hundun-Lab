import { ApiError } from './http'

/**
 * 扩展接口封装（M0 假数据版）。
 *
 * 扩展是 HundunDesktop 特有的能力（D071–D075），没有站点级入口；
 * 数据模型见 db/01_schema.sql 的 `extension` / `extension_release`。
 *
 * M0 阶段接口未上线，这里返回内置假数据，字段命名与 schema 对齐
 * （snake_case → camelCase，与 site.js 的响应同一风格），
 * 到 M2 只换这一层的实现，页面组件不动：
 *   GET /software/{slug}/extensions                列表（只含 status=1，F07）
 *   GET /software/{slug}/extensions/{id}           详情（software_id 不匹配 → EXTENSION_NOT_FOUND，D072）
 *   GET /software/{slug}/extensions/{id}/download  下载短链 302（F10）
 */

/** 演示数据与原型 03 的扩展分区同一批（D083：官方扩展预置进库、status=1） */
const MOCK_EXTENSIONS = [
  {
    id: 1,
    softwareSlug: 'hundundesktop',
    slug: 'calculator',
    pluginId: 'calculator',
    nameZh: 'Calculator',
    nameEn: 'Calculator',
    summaryZh: '内置计算器，支持表达式与单位换算',
    summaryEn: 'Built-in calculator with expressions and unit conversion',
    authorType: 'official',
    authorName: 'Hundun Lab',
    permissions: [],
    platforms: ['win32', 'darwin', 'linux'],
    featured: 1,
    downloadCount: 0,
    bundled: true,
    license: 'MIT',
    repoUrl: 'https://github.com/example/hundundesktop',
    updatedAt: '2026-10-28',
    releases: [
      {
        id: 11,
        version: '1.0.0',
        changelogZh: '随桌面端首个公开版本一起发布。',
        changelogEn: 'Shipped with the first public release.',
        fileSize: 18432,
        sha256: '11ff22aa33bb44cc55dd66ee770011aa22bb33cc44dd55ee66ff778899aa00bb',
        downloadCount: 0,
        publishedAt: '2026-10-28',
      },
    ],
  },
  {
    id: 2,
    softwareSlug: 'hundundesktop',
    slug: 'fasttxt',
    pluginId: 'fasttxt',
    nameZh: 'FastTxt',
    nameEn: 'FastTxt',
    summaryZh: '随手一段文本，落到桌面就存下',
    summaryEn: 'Capture text snippets straight to the desktop',
    authorType: 'official',
    authorName: 'Hundun Lab',
    permissions: [],
    platforms: ['win32', 'darwin', 'linux'],
    featured: 1,
    downloadCount: 0,
    bundled: true,
    license: 'MIT',
    repoUrl: 'https://github.com/example/hundundesktop',
    updatedAt: '2026-10-28',
    releases: [
      {
        id: 12,
        version: '1.0.0',
        changelogZh: '随桌面端首个公开版本一起发布。',
        changelogEn: 'Shipped with the first public release.',
        fileSize: 20976,
        sha256: '22ee33dd44cc55bb66aa77ff880011bb33cc44dd55ee66ff778899aa00bb11cc',
        downloadCount: 0,
        publishedAt: '2026-10-28',
      },
    ],
  },
  {
    id: 3,
    softwareSlug: 'hundundesktop',
    slug: 'hundundock',
    pluginId: 'hundundock',
    nameZh: 'HundunDock',
    nameEn: 'HundunDock',
    summaryZh: '桌面底部快捷启动坞',
    summaryEn: 'A quick-launch dock at the bottom of the desktop',
    authorType: 'community',
    authorName: '@ashimmer',
    permissions: ['launcher'],
    platforms: ['win32', 'linux'],
    featured: 1,
    downloadCount: 2314,
    bundled: false,
    license: 'Apache-2.0',
    repoUrl: 'https://github.com/ashimmer/hundundock',
    updatedAt: '2026-09-14',
    releases: [
      {
        id: 13,
        version: '0.4.1',
        changelogZh: '修复高分屏下图标模糊。',
        changelogEn: 'Fix blurry icons on HiDPI screens.',
        fileSize: 1302528,
        sha256: '33dd44ee55ff66aa77bb88cc990022cc44dd55ee66ff778899aa00bb11cc22dd',
        downloadCount: 1502,
        publishedAt: '2026-09-14',
      },
      {
        id: 14,
        version: '0.4.0',
        changelogZh: '支持拖拽排序。',
        changelogEn: 'Drag to reorder.',
        fileSize: 1289216,
        sha256: '44cc55dd66ee77ff88aa99bb001133dd55ee66ff778899aa00bb11cc22dd33ee',
        downloadCount: 812,
        publishedAt: '2026-07-02',
      },
    ],
  },
  {
    id: 4,
    softwareSlug: 'hundundesktop',
    slug: 'pixelruler',
    pluginId: 'pixelruler',
    nameZh: 'PixelRuler',
    nameEn: 'PixelRuler',
    summaryZh: '屏幕取色与像素测量',
    summaryEn: 'Color picking and pixel measuring on screen',
    authorType: 'community',
    authorName: '@homo',
    permissions: ['screen', 'clipboard'],
    platforms: ['win32', 'darwin', 'linux'],
    featured: 1,
    downloadCount: 4021,
    bundled: false,
    license: 'MIT',
    repoUrl: 'https://github.com/homo/pixelruler',
    readmeZh:
      'PixelRuler 是一款屏幕测量小工具。\n\n## 功能\n\n- 任意区域像素级测量\n- 放大镜取色（HEX / RGB）\n- 多屏坐标换算\n\n## 安装\n\n在 HundunDesktop 的扩展面板中搜索 PixelRuler，点击安装即可；离线环境下可从 Releases 页下载 .hdext 手动导入。\n\n## 说明\n\n取色与测量均在本地完成，不会上传任何屏幕内容。',
    readmeEn:
      'PixelRuler is a small screen measuring tool.\n\n## Features\n\n- Pixel-level measurement of any region\n- Magnifier color picking (HEX / RGB)\n- Multi-monitor coordinate mapping\n\n## Install\n\nSearch PixelRuler in the HundunDesktop extension panel, or download the .hdext package from Releases and import it manually.\n\n## Notes\n\nAll picking and measuring happens locally; nothing is uploaded.',
    updatedAt: '2026-10-20',
    releases: [
      {
        id: 15,
        version: '1.2.0',
        changelogZh: '新增放大镜取色。',
        changelogEn: 'Magnifier color picking.',
        fileSize: 1258291,
        sha256: '55bb66cc77dd88ee99ff00aa112244ee66ff778899aa00bb11cc22dd33ee44ff',
        downloadCount: 2140,
        publishedAt: '2026-10-20',
      },
      {
        id: 16,
        version: '1.1.0',
        changelogZh: '支持多屏测量。',
        changelogEn: 'Multi-monitor measuring.',
        fileSize: 1205862,
        sha256: '66aa77bb88cc99dd00ee11ff223355ff778899aa00bb11cc22dd33ee44ff55aa',
        downloadCount: 1218,
        publishedAt: '2026-08-02',
      },
      {
        id: 17,
        version: '1.0.0',
        changelogZh: '首个公开版本。',
        changelogEn: 'First public release.',
        fileSize: 1179648,
        sha256: '779988aa00bb11cc22dd33ee44ff55aa778899aa00bb11cc22dd33ee44ff55aa66',
        downloadCount: 663,
        publishedAt: '2026-05-19',
      },
    ],
  },
  {
    id: 5,
    softwareSlug: 'hundundesktop',
    slug: 'sounbar',
    pluginId: 'sounbar',
    nameZh: 'SounBar',
    nameEn: 'SounBar',
    summaryZh: '可视化音量条',
    summaryEn: 'A visual volume bar',
    authorType: 'community',
    authorName: '@chitose',
    permissions: ['audio'],
    platforms: ['win32'],
    featured: 0,
    downloadCount: 2870,
    bundled: false,
    license: 'MIT',
    repoUrl: 'https://github.com/chitose/sounbar',
    updatedAt: '2026-08-30',
    releases: [
      {
        id: 18,
        version: '2.1.0',
        changelogZh: '音量条支持自定义配色。',
        changelogEn: 'Customizable colors.',
        fileSize: 943718,
        sha256: '8877665544332211ffeeddccbbaa99888877665544332211ffeeddccbbaa9988',
        downloadCount: 2870,
        publishedAt: '2026-08-30',
      },
    ],
  },
  {
    id: 6,
    softwareSlug: 'hundundesktop',
    slug: 'clipsync',
    pluginId: 'clipsync',
    nameZh: 'ClipSync',
    nameEn: 'ClipSync',
    summaryZh: '剪贴板跨设备同步',
    summaryEn: 'Clipboard sync across devices',
    authorType: 'community',
    authorName: '@nightowl',
    permissions: ['clipboard', 'network'],
    platforms: ['win32', 'darwin'],
    featured: 0,
    downloadCount: 1560,
    bundled: false,
    license: 'MIT',
    repoUrl: 'https://github.com/nightowl/clipsync',
    updatedAt: '2026-10-02',
    releases: [
      {
        id: 19,
        version: '0.9.0',
        changelogZh: '首个公开版本。',
        changelogEn: 'First public release.',
        fileSize: 2202009,
        sha256: '99aa88bb77cc66dd55ee44ff3322110099aa88bb77cc66dd55ee44ff33221100',
        downloadCount: 1560,
        publishedAt: '2026-10-02',
      },
    ],
  },
]

function normalize(ext) {
  const { softwareSlug: _softwareSlug, ...rest } = ext
  return rest
}

/** F07：某软件已上架的扩展列表；精选在前（featured DESC, download_count DESC，F07 排序口径） */
export function fetchExtensions(slug) {
  const target = String(slug || '').toLowerCase()
  const list = MOCK_EXTENSIONS.filter((ext) => ext.softwareSlug === target)
  list.sort((a, b) => b.featured - a.featured || b.downloadCount - a.downloadCount)
  return Promise.resolve(list.map(normalize))
}

/**
 * F08：扩展详情。id 接受数字主键或 URL slug。
 * 扩展不存在、已下架、或不属于 :slug 所指软件 → EXTENSION_NOT_FOUND（D072）。
 */
export function fetchExtensionDetail(slug, id) {
  const target = String(slug || '').toLowerCase()
  const key = String(id || '').toLowerCase()
  const ext = MOCK_EXTENSIONS.find(
    (item) =>
      item.softwareSlug === target &&
      (String(item.id) === String(id) || item.slug.toLowerCase() === key),
  )
  if (!ext) {
    return Promise.reject(new ApiError({ errorCode: 'EXTENSION_NOT_FOUND' }))
  }
  return Promise.resolve(normalize(ext))
}
