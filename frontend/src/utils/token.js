/**
 * 登录令牌与用户信息的存储。
 *
 * 单独成模块（而不是挂在 http.js 或 store 里）是为了打掉循环依赖：
 * http.js 要读 token，store 要读写 token，router 守卫也要读 token，
 * 三方都依赖它，它不依赖任何项目模块。
 *
 * 「记住我」（原型 08-login.html 的 checkbox）在这里落地：
 * 勾选 → localStorage，关掉浏览器还在；不勾 → sessionStorage，关掉即失效。
 * 两种位置只能有一份，写入前先清干净，避免旧 token 顶掉新 token。
 */

export const TOKEN_KEY = 'hundunlab.token'
const USER_KEY = 'hundunlab.user'

function readStored(key) {
  try {
    return localStorage.getItem(key) || sessionStorage.getItem(key) || ''
  } catch {
    // 隐私模式下 storage 可能直接抛错，当作未登录处理
    return ''
  }
}

function clearBoth(key) {
  try {
    localStorage.removeItem(key)
    sessionStorage.removeItem(key)
  } catch {
    // 同上，读不到也删不掉，忽略
  }
}

export function readToken() {
  return readStored(TOKEN_KEY)
}

export function readUser() {
  const raw = readStored(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

/**
 * 写入登录态。remember 只决定「存哪」，不改变存什么。
 */
export function saveSession(token, user, remember = true) {
  const store = remember ? localStorage : sessionStorage
  clearBoth(TOKEN_KEY)
  clearBoth(USER_KEY)
  try {
    store.setItem(TOKEN_KEY, token)
    if (user) store.setItem(USER_KEY, JSON.stringify(user))
  } catch {
    // 写不进去就只在内存里保留本次会话，不阻断登录
  }
}

export function clearSession() {
  clearBoth(TOKEN_KEY)
  clearBoth(USER_KEY)
}
