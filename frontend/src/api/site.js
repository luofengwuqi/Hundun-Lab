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

/**
 * 登录。四条路径对应后端 LoginController 已上线的四种方式，入参字段名与 DTO 一致：
 * LoginUsernamePasswordRequestDTO / LoginPhonePasswordDTO / LoginEmailPassWordDTO / LoginEmailCodeDTO。
 * 成功都返回 LoginRequestVO（{ token, user }），前端只需看这两种字段。
 */
export function loginWithPassword(username, password) {
  return http.post('/user/login/username-password', { username, password })
}

export function loginWithPhone(phone, password) {
  return http.post('/user/login/phone-password', { phone, password })
}

export function loginWithEmailPassword(email, password) {
  return http.post('/user/login/email-password', { email, password })
}

export function loginWithEmailCode(email, code) {
  return http.post('/user/login/email-code', { email, code })
}

/** 发送邮箱验证码：后端 3 次/60 秒，且要求邮箱已在账号上 */
export function sendLoginEmailCode(email) {
  return http.post('/user/login/email-code/send', { email })
}

export function logout() {
  return http.post('/user/login/logout')
}
