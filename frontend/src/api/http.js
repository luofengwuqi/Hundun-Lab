import axios from 'axios'

/**
 * 站点对外接口的统一出口。
 *
 * 与旧 utils/request.js 的区别：
 * 1. 不用 5 秒固定超时——下载票据、大列表都可能超过 5 秒，超时按接口传；
 * 2. 不弹全局 ElMessage、不强制跳登录页——首页是匿名的，跳转逻辑交给调用方；
 * 3. 失败一律抛出带 errorCode 的 ApiError，由页面查 i18n 文案，后端说什么语言都不影响展示。
 */
export const TOKEN_KEY = 'hundunlab.token'

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
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    // 后端统一返回 { code, errorCode, message, data }
    if (body && (body.code === 200 || body.code === 0)) {
      return body.data
    }
    throw new ApiError({
      errorCode: body?.errorCode,
      message: body?.message,
      status: body?.code,
    })
  },
  (error) => {
    const body = error.response?.data
    if (body && typeof body === 'object') {
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
