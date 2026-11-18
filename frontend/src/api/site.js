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

/**
 * 注册（F05）。入参与 RegisterDTO 一致：username / password 必填，email / phone 可选。
 * 可选字段没填时前端直接不发这个键——后端虽然会把空串归一化成 NULL，
 * 但少一层「空串算不算填了」的歧义更稳。
 * 返回 { username }，不带令牌：注册完要回登录页自己登一次。
 */
export function registerUser(payload) {
  return http.post('/user/register', payload)
}

/**
 * 管理员邀请码注册。与普通注册是两个接口，邀请码只走请求体，不进 URL、不进日志。
 * 成功同样只返回 { username }。
 */
export function registerAdminByInvite(payload) {
  return http.post('/user/register/admin', payload)
}

export function logout() {
  return http.post('/user/login/logout')
}
