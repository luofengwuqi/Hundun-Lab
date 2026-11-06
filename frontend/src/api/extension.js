import http from './http'

/**
 * 扩展接口封装（M2 站点侧，后端 module/extension 已实现）。
 *
 * 扩展是 HundunDesktop 特有的能力（D071–D075），没有站点级入口；
 * 路由挂在所属软件下，字段命名与后端 ExtensionVO（camelCase）直接对齐：
 *   GET /software/{slug}/extensions                列表（只含 status=1，F07）
 *   GET /software/{slug}/extensions/{idOrSlug}     详情（数字主键或 URL slug；
 *                                                  不存在/跨软件 → EXTENSION_NOT_FOUND，D072）
 *   GET /software/{slug}/extensions/{idOrSlug}/download  下载短链 302（F10）
 *
 * 下载短链浏览器直接 302 跳 OSS，前端不包它；F09 提交接口（M2 后台审核）尚未开放，
 * 提交页仍是纯预检演示（判定权在服务端，D051）。
 */

/** F07：某软件已上架的扩展列表；排序 featured DESC, download_count DESC 由后端保证 */
export function fetchExtensions(slug) {
  return http.get(`/software/${encodeURIComponent(slug)}/extensions`)
}

/** F08：扩展详情。id 接受数字主键或 URL slug */
export function fetchExtensionDetail(slug, id) {
  return http.get(`/software/${encodeURIComponent(slug)}/extensions/${encodeURIComponent(id)}`)
}
