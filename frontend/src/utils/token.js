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
 *
 * 空闲上限：后端令牌是 Redis 滑动过期（RedisConstant.TOKEN_EXPIRE_TIME = 30 分钟，
 * 每个通过 TokenResolveFilter 的请求都会续期），JWT 自身的 24h 只是上限。
 * 所以「记住我」并不等于一直在线：30 分钟不发一个请求，服务端就把令牌删了。
 * 前端照抄同一口径，本地记「最后一次成功请求的时间」，超时即视为未登录 ——
 * 否则会出现「页面显示已登录、每个接口都 401」的错位。
 */

export const TOKEN_KEY = 'hundunlab.token'
const USER_KEY = 'hundunlab.user'
const ACTIVE_KEY = 'hundunlab.activeAt'

/** 与后端 RedisConstant.TOKEN_EXPIRE_TIME 对齐（30 分钟）。改这里必须同时改后端。 */
export const IDLE_LIMIT_MS = 30 * 60 * 1000

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

/**
 * 找到当前放着令牌的那个 storage。
 * 令牌只会存在一处（saveSession 写入前先清两边），活跃时间跟着它走，
 * 这样 localStorage 与 sessionStorage 不会各留一份时间戳互相打架。
 */
function storeWithToken() {
  try {
    if (localStorage.getItem(TOKEN_KEY)) return localStorage
    if (sessionStorage.getItem(TOKEN_KEY)) return sessionStorage
  } catch {
    // 同上
  }
  return null
}

function isIdleExpired() {
  const store = storeWithToken()
  if (!store) return true
  const raw = store.getItem(ACTIVE_KEY)
  const lastActive = Number(raw)
  // 没有时间戳的老会话按过期处理：让用户重新登录一次，
  // 好过长期挂着一个服务端早已删掉的令牌。
  if (!raw || !Number.isFinite(lastActive)) return true
  return Date.now() - lastActive > IDLE_LIMIT_MS
}

export function readToken() {
  const token = readStored(TOKEN_KEY)
  if (!token) return ''
  if (isIdleExpired()) {
    clearSession()
    return ''
  }
  return token
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
 * 记一次「还活着」。http.js 每个成功响应都会调一次，与后端滑动续期同步。
 * 只改令牌所在的那个 storage，不新造一份。
 */
export function touchSession() {
  const store = storeWithToken()
  if (!store) return
  try {
    store.setItem(ACTIVE_KEY, String(Date.now()))
  } catch {
    // 写不进去不影响本次会话，最多下次判断空闲时提前过期
  }
}

/**
 * 写入登录态。remember 只决定「存哪」，不改变存什么。
 */
export function saveSession(token, user, remember = true) {
  const store = remember ? localStorage : sessionStorage
  clearBoth(TOKEN_KEY)
  clearBoth(USER_KEY)
  clearBoth(ACTIVE_KEY)
  try {
    store.setItem(TOKEN_KEY, token)
    store.setItem(ACTIVE_KEY, String(Date.now()))
    if (user) store.setItem(USER_KEY, JSON.stringify(user))
  } catch {
    // 写不进去就只在内存里保留本次会话，不阻断登录
  }
}

export function clearSession() {
  clearBoth(TOKEN_KEY)
  clearBoth(USER_KEY)
  clearBoth(ACTIVE_KEY)
}
