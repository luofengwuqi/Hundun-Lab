import i18n from '@/i18n'

/**
 * 登录失败 → i18n 键。
 *
 * 后端的登录错误码是「收敛」的：`LoginServiceImpl` 的密码错、账号被封都抛
 * `LoginException`，由 `GlobalExceptionHandler` 统一映射为 `AUTH_NOT_LOGGED_IN`(401)。
 * 也就是说前端**拿不到**「密码错」和「被禁」的区分，只有这一个码。
 * 硬按字面翻成「请先登录」是错的：人正站在登录页上，密码打错了却被告知没登录。
 * 所以登录场景统一说「登录失败，请检查账号与密码」，不猜具体原因，
 * 也顺手不给账号枚举留口子（`USER_NOT_FOUND` 同样并入）。
 *
 * 另外两个后端遗留问题在前端兜一层：
 * - 验证码错 → 走的是 `PasswordException` → `USER_PASSWORD_WRONG`（密码的码，验证码的语义）；
 * - 验证码过期 → `LoginServiceImpl` 抛的是裸 `RuntimeException` → `INTERNAL_ERROR`(500)。
 * 只在验证码场景读成「验证码过期」，别的场景仍照实报服务器错误。
 *
 * scene：'password'（用户名/手机号/邮箱+密码）｜ 'code'（邮箱验证码）
 */
const LOGIN_FAILED_CODES = new Set([
  'AUTH_NOT_LOGGED_IN',
  'AUTH_TOKEN_EXPIRED',
  'AUTH_TOKEN_INVALID',
  'AUTH_ACCOUNT_DISABLED',
  'USER_NOT_FOUND',
])

export function authErrorKey(error, scene = 'password') {
  const code = error?.errorCode || ''

  if (LOGIN_FAILED_CODES.has(code)) return 'auth.failed'

  if (code === 'USER_PASSWORD_WRONG') {
    return scene === 'code' ? 'auth.codeWrong' : 'auth.failed'
  }

  if (code === 'INTERNAL_ERROR' && scene === 'code') return 'auth.codeExpired'

  if (code === 'BAD_REQUEST') return 'auth.invalidInput'

  const key = `errors.${code}`
  return code && i18n.global.te(key) ? key : 'auth.failed'
}
