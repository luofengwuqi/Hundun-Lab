import { createI18n } from 'vue-i18n'
import zh from './zh'
import en from './en'

export const SUPPORTED_LOCALES = ['zh', 'en']
export const LOCALE_STORAGE_KEY = 'hundunlab.locale'

/**
 * 语言优先级：本地存储 > 浏览器语言 > 中文。
 * 只认 zh 与 en，其他语言一律回落到英文（比回落中文更容易被非中文用户接受）。
 */
function resolveInitialLocale() {
  const saved = localStorage.getItem(LOCALE_STORAGE_KEY)
  if (saved && SUPPORTED_LOCALES.includes(saved)) {
    return saved
  }
  const browser = (navigator.language || '').toLowerCase()
  if (browser.startsWith('zh')) {
    return 'zh'
  }
  return 'en'
}

const i18n = createI18n({
  legacy: false,
  globalInjection: true,
  locale: resolveInitialLocale(),
  fallbackLocale: 'en',
  messages: { zh, en },
})

/**
 * 切换语言并持久化。
 * 同时更新 <html lang>，一是为了读屏软件，二是让浏览器知道该用哪套断行规则。
 */
export function setLocale(locale) {
  if (!SUPPORTED_LOCALES.includes(locale)) {
    return
  }
  i18n.global.locale.value = locale
  localStorage.setItem(LOCALE_STORAGE_KEY, locale)
  document.documentElement.setAttribute('lang', locale === 'zh' ? 'zh-CN' : 'en')
}

// 首屏就把 lang 修正，避免 index.html 里的空属性一直留着
setLocale(i18n.global.locale.value)

/**
 * 内容字段按语言取值的统一入口。
 * 数据库里中英成对存放（name_zh / name_en），缺英文时回落到中文，不让页面出现空白。
 */
export function localized(zhValue, enValue) {
  return i18n.global.locale.value === 'en' ? (enValue || zhValue) : (zhValue || enValue)
}

export default i18n
