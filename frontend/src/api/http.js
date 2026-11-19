import axios from 'axios'
import { readToken, clearSession, touchSession } from '@/utils/token'

/**
 * 站点对外接口的统一出口。
 *
 * 与旧 utils/request.js 的区别：
 * 1. 不用 5 秒固定超时——下载票据、大列表都可能超过 5 秒，超时按接口传；
 * 2. 不弹全局 ElMessage、不强制跳登录页——首页是匿名的，跳转逻辑交给调用方；
 * 3. 失败一律抛出带 errorCode 的 ApiError，由页面查 i18n 文案，后端说什么语言都不影响展示。
 *
 * token 的存取统一走 @/utils/token：登录时选了「记住我」就落在 localStorage，
 * 没选就落在 sessionStorage，这里读的时候两种都认。
 *
 * 会话失效收口在这里：
 * - 成功响应 → touchSession()，与后端 Redis 的滑动续期保持同一节奏；
 * - 令牌类错误 → 清本地登录态并广播 AUTH_EXPIRED_EVENT，由 main.js 跳登录页。
 *   （这里不直接 import router：router → views → api 会绕回来形成循环。）
 */
export { TOKEN_KEY } from '@/utils/token'

/** 登录态真的不能用了的错误码：令牌过期/无效，或账号被禁 */
const SESSION_DEAD_CODES = new Set([
  'AUTH_TOKEN_EXPIRED',
  'AUTH_TOKEN_INVALID',
  'AUTH_ACCOUNT_DISABLED',
])

/** 登录类接口自己的失败不代表会话失效，不能触发全局跳转 */
const AUTH_ENDPOINT = /^\/user\/login\//

export const AUTH_EXPIRED_EVENT = 'hundunlab:auth-expired'

export class ApiError extends Error {
  constructor({ errorCode, message, status }) {
    super(message || 'request failed')
    this.name = 'ApiError'
    this.errorCode = errorCode || 'INTERNAL_ERROR'
    this.status = status
  }
}

const http = axios.create({
  baseURL: '/api',
  timeout: 12000,
  headers: { 'Content-Type': 'application/json;charset=UTF-8' },
})

http.interceptors.request.use((config) => {
  const token = readToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  // 传文件时必须让浏览器自己写 Content-Type，否则 boundary 会丢，
  // 后端只会看到「当前请求不是 multipart」。实例上固定了 JSON 头，这里显式摘掉。
  if (typeof FormData !== 'undefined' && config.data instanceof FormData) {
    delete config.headers['Content-Type']
  }
  return config
})

/**
 * 令牌失效的统一处理：本地登录态已经不成立，别让页面继续装成登录中。
 */
function handleDeadSession(errorCode, url) {
  if (!SESSION_DEAD_CODES.has(errorCode)) return
  if (AUTH_ENDPOINT.test(url || '')) return
  if (!readToken()) return
  clearSession()
  window.dispatchEvent(new CustomEvent(AUTH_EXPIRED_EVENT, { detail: { errorCode } }))
}

http.interceptors.response.use(
  (response) => {
    const body = response.data
    // 后端统一返回 { code, errorCode, message, data }
    if (body && (body.code === 200 || body.code === 0)) {
      // 有请求就有活动，续上本地空闲计时（与后端滑动过期同步）
      touchSession()
      return body.data
    }
    handleDeadSession(body?.errorCode, response.config?.url)
    throw new ApiError({
      errorCode: body?.errorCode,
      message: body?.message,
      status: body?.code,
    })
  },
  (error) => {
    const body = error.response?.data
    if (body && typeof body === 'object') {
      handleDeadSession(body.errorCode, error.config?.url)
      throw new ApiError({
        errorCode: body.errorCode,
        message: body.message,
        status: body.code || error.response.status,
      })
    }
    throw new ApiError({
      errorCode: error.code === 'ECONNABORTED' ? 'SERVICE_UNAVAILABLE' : 'INTERNAL_ERROR',
      message: error.message,
      status: 0,
    })
  },
)

export default http
