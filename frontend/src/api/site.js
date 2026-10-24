import http from './http'

/**
 * 站点接口封装。路径与后端模块一一对应，页面不直接拼 URL。
 */

export function fetchSoftwareList() {
  return http.get('/software')
}

export function fetchSoftwareDetail(slug) {
  return http.get(`/software/${encodeURIComponent(slug)}`)
}

/**
 * 换取下载票据。需要登录，未登录会抛 AUTH_NOT_LOGGED_IN。
 * 超时给到 30 秒：签发本身很快，但弱网下不该 5 秒就判死。
 */
export function requestDownloadTicket(assetId) {
  return http.post('/download/ticket', { assetId }, { timeout: 30000 })
}

export function loginWithPassword(username, password) {
  return http.post('/user/login/username-password', { username, password })
}

export function logout() {
  return http.post('/user/login/logout')
}
