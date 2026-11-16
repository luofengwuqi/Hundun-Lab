<script setup>
/**
 * 登录页 · 独立满屏版式（M0b 原型 docs/design/prototype/08-login.html）
 *
 * 它不套 SiteLayout：原型里的登录页刻意没有全站页头页脚。
 * 版式：480px 登录卡相对视口垂直水平居中，登录方式栏作为伴栏贴在卡片左侧，
 * 语言／主题固定在视口右上角，版权行固定在视口底部正中。
 *
 * 只做后端真有的能力：目前只有「用户名 + 密码」这一条接口，
 * 其余三种方式与注册／找回密码保留版式位置但禁用，不假装能点。
 * 「记住我」是真的：勾选存 localStorage，不勾存 sessionStorage（utils/token）。
 */
import { computed, reactive, ref, useId } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { loginWithPassword } from '@/api/site'
import { useSessionStore } from '@/stores/session'
import { LOCALE_LABELS, setLocale } from '@/i18n'
import BrandMark from '@/components/BrandMark.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()
const session = useSessionStore()

// 同一页可能出现多个输入框，label 的 for 必须指向唯一 id
const usernameId = useId()
const passwordId = useId()

const form = reactive({ username: '', password: '' })
const remember = ref(true)
const showPassword = ref(false)
const submitting = ref(false)
const errorKey = ref('')

const methods = computed(() => [
  { id: 'username', label: t('auth.methodUsername'), enabled: true },
  { id: 'phone', label: t('auth.methodPhone'), enabled: false },
  { id: 'email-code', label: t('auth.methodEmailCode'), enabled: false },
  { id: 'email-password', label: t('auth.methodEmailPassword'), enabled: false },
])

async function submit() {
  if (submitting.value) return
  if (!form.username.trim() || !form.password) {
    errorKey.value = 'auth.failed'
    return
  }
  submitting.value = true
  errorKey.value = ''
  try {
    const data = await loginWithPassword(form.username.trim(), form.password)
    session.setSession(data.token, data.user, remember.value)
    // 回到来源页；直接打开登录页时回首页
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.replace(redirect)
  } catch (error) {
    // 后端只给错误码，这里翻成当前语言；没有对应文案时用通用登录失败
    const code = error?.errorCode
    errorKey.value =
      code && t(`errors.${code}`) !== `errors.${code}` ? `errors.${code}` : 'auth.failed'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <div class="login-corner">
      <div class="lang-switch" role="group" :aria-label="t('nav.language')">
        <button
          type="button"
          class="lang-switch__btn"
          :class="{ 'is-on': locale === 'zh' }"
          @click="setLocale('zh')"
        >
          {{ LOCALE_LABELS.zh }}
        </button>
        <button
          type="button"
          class="lang-switch__btn"
          :class="{ 'is-on': locale === 'en' }"
          @click="setLocale('en')"
        >
          {{ LOCALE_LABELS.en }}
        </button>
      </div>
      <ThemeToggle />
    </div>

    <aside class="login-side">
      <h2>{{ t('auth.methodTitle') }}</h2>
      <ul>
        <li v-for="m in methods" :key="m.id">
          <button
            type="button"
            class="login-tab"
            :class="{ 'is-on': m.enabled }"
            :disabled="!m.enabled"
            :title="m.enabled ? m.label : t('auth.methodPending')"
          >
            <svg
              v-if="m.id === 'username'"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <circle cx="12" cy="8" r="4" />
              <path d="M4 21c0-4.4 3.6-7 8-7s8 2.6 8 7" />
            </svg>
            <svg
              v-else-if="m.id === 'phone'"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <rect x="7" y="3" width="10" height="18" rx="2" />
              <path d="M11 18h2" />
            </svg>
            <svg
              v-else-if="m.id === 'email-code'"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <rect x="3" y="6" width="18" height="12" rx="2" />
              <path d="m4 8 8 6 8-6" />
            </svg>
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="5" y="10" width="14" height="10" rx="2" />
              <path d="M8 10V7a4 4 0 0 1 8 0v3" />
            </svg>
            <span>{{ m.label }}</span>
          </button>
        </li>
      </ul>
      <p class="login-side__note">{{ t('auth.methodNote') }}</p>
    </aside>

    <form class="login-card" @submit.prevent="submit">
      <span class="login-card__mark" aria-hidden="true"><BrandMark /></span>
      <div class="login-card__brand">HUNDUN<em>LAB</em></div>
      <p class="login-card__tag">{{ t('auth.tagline') }}</p>

      <div class="input-icon">
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.8"
          aria-hidden="true"
        >
          <circle cx="12" cy="8" r="4" />
          <path d="M4 21c0-4.4 3.6-7 8-7s8 2.6 8 7" />
        </svg>
        <label class="sr-only" :for="usernameId">{{ t('auth.username') }}</label>
        <input
          :id="usernameId"
          v-model="form.username"
          class="input"
          type="text"
          autocomplete="username"
          :placeholder="t('auth.usernamePlaceholder')"
        />
      </div>

      <div class="input-icon">
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.8"
          aria-hidden="true"
        >
          <rect x="5" y="10" width="14" height="10" rx="2" />
          <path d="M8 10V7a4 4 0 0 1 8 0v3" />
        </svg>
        <label class="sr-only" :for="passwordId">{{ t('auth.password') }}</label>
        <input
          :id="passwordId"
          v-model="form.password"
          class="input"
          :type="showPassword ? 'text' : 'password'"
          autocomplete="current-password"
          :placeholder="t('auth.passwordPlaceholder')"
        />
        <button
          type="button"
          class="input-icon__tail"
          :aria-label="showPassword ? t('auth.hidePassword') : t('auth.showPassword')"
          :aria-pressed="showPassword"
          @click="showPassword = !showPassword"
        >
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.8"
            aria-hidden="true"
          >
            <path d="M2 12s3.6-6 10-6 10 6 10 6-3.6 6-10 6-10-6-10-6z" />
            <circle cx="12" cy="12" r="2.6" />
          </svg>
        </button>
      </div>

      <p v-if="errorKey" class="login-error mono" role="alert">{{ t(errorKey) }}</p>

      <div class="login-actions">
        <button type="submit" class="btn btn--primary" :disabled="submitting">
          {{ submitting ? t('auth.submitting') : t('auth.submit') }}
        </button>
        <button type="button" class="btn btn--ghost" disabled :title="t('auth.registerPending')">
          {{ t('auth.register') }}
        </button>
      </div>

      <div class="login-foot">
        <label class="checkbox">
          <input v-model="remember" type="checkbox" />
          {{ t('auth.remember') }}
        </label>
        <button type="button" class="login-foot__link" disabled :title="t('auth.forgotPending')">
          {{ t('auth.forgot') }}
        </button>
      </div>
    </form>

    <p class="login-copy">{{ t('auth.copyright') }}</p>
  </main>
</template>

<style scoped>
/* 登录卡相对视口垂直水平居中：三等分列（左右等宽留白 + 卡片列），
   方式栏贴在卡片左边不参与居中，卡片本身永远落在视口正中 */
.login-page {
  position: relative;
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-content: center;
  align-items: center;
  padding: 72px 24px;
  background-color: var(--paper);
  background-image:
    var(--obj-login-texture),
    repeating-linear-gradient(to bottom, var(--scan) 0 1px, transparent 1px 4px);
  background-size:
    300px 300px,
    100% 4px;
}

.login-side {
  justify-self: end;
  width: 180px;
  margin-right: 64px;
  padding-left: 60px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-side h2 {
  margin: 0 0 26px;
  font-size: 15px;
  font-weight: 800;
  letter-spacing: 0.1em;
  color: var(--ink);
}

.login-side ul {
  list-style: none;
  margin: 0;
  padding: 0;
}

.login-side li {
  margin-bottom: 26px;
}

.login-tab {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  width: 120px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--accent-text);
  font-size: 13px;
  font-weight: 700;
  line-height: 1.4;
  text-align: center;
}

.login-tab svg {
  width: 44px;
  height: 44px;
  padding: 9px;
  flex: none;
  border: 2px solid var(--red);
  border-radius: 12px;
  background: transparent;
  color: var(--accent-text);
}

/* 当前唯一可用的方式：实心主色圆标（深色下按 D087 降级为淡绿底 + 白字） */
.login-tab.is-on svg {
  background: var(--red);
  color: var(--paper);
  border-radius: 999px;
}

/* 未开放的方式：保留版式位置，但灰掉并说明，不假装可点 */
.login-tab:disabled {
  color: var(--ink-faint);
  cursor: not-allowed;
}

.login-tab:disabled svg {
  border-color: var(--line-soft);
  color: var(--ink-faint);
}

.login-side__note {
  max-width: 132px;
  margin-top: 2px;
  font-size: 11px;
  line-height: 1.7;
  color: var(--ink-faint);
}

.login-corner {
  position: absolute;
  top: 26px;
  right: 30px;
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 语言切换：登录页没有页头，这里单独给一份同尺寸的 46px 胶囊 */
.lang-switch {
  display: inline-flex;
  height: 46px;
  border: 2px solid var(--red);
  border-radius: 999px;
  overflow: hidden;
}

.lang-switch__btn {
  padding: 0 20px;
  font-family: var(--font-mono);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  background: transparent;
  color: var(--accent-text);
}

.lang-switch__btn + .lang-switch__btn {
  border-left: 2px solid var(--red);
}

.lang-switch__btn.is-on {
  background: var(--chip-on-bg);
  color: var(--chip-on-ink);
}

.login-card {
  justify-self: center;
  width: 480px;
  max-width: 100%;
  padding: 44px 44px 36px;
  border: 2px solid var(--red);
  border-radius: 10px;
  background: var(--card);
  box-shadow: var(--shadow-card);
  text-align: center;
}

.login-card__mark {
  display: block;
  width: 104px;
  height: 104px;
  margin: 0 auto 14px;
  color: var(--accent-text);
}

.login-card__mark svg {
  width: 100%;
  height: 100%;
}

/* 像素字只在 12 的整数倍字号下锐利，原型写 58px，这里取 60px（12×5） */
.login-card__brand {
  font-family: var(--font-display);
  font-size: 60px;
  font-weight: 400;
  letter-spacing: 0;
  line-height: 1.1;
  color: var(--ink);
}

.login-card__brand em {
  font-style: normal;
  color: var(--accent-text);
}

.login-card__tag {
  margin-top: 8px;
  font-size: 13px;
  color: var(--ink-soft);
  letter-spacing: 0.08em;
}

.input-icon {
  position: relative;
  margin-top: 20px;
}

/* 输入框左侧的圆标图标 */
.input-icon > svg {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  width: 32px;
  height: 32px;
  padding: 7px;
  border: 2px solid var(--red);
  border-radius: 999px;
  background: var(--card);
  color: var(--accent-text);
}

.input-icon .input {
  width: 100%;
  height: 56px;
  padding-left: 58px;
  padding-right: 48px;
  border: 1.5px solid var(--red);
  border-radius: 6px;
  background: var(--card);
  color: var(--ink);
  font-size: 15px;
}

.input-icon .input::placeholder {
  color: var(--ink-faint);
}

.input-icon .input:focus {
  outline: 3px solid var(--amber);
  outline-offset: 1px;
}

.input-icon__tail {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  display: grid;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--accent-text);
  cursor: pointer;
}

.input-icon__tail svg {
  width: 22px;
  height: 22px;
}

.login-error {
  margin-top: 14px;
  font-size: 12px;
  color: var(--accent-text);
  text-align: left;
}

.login-actions {
  margin-top: 26px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

/* 原型登录按钮 56px 高、8px 圆角，不做全站的像素硬阴影 */
.login-actions .btn {
  width: 100%;
  height: 56px;
  padding: 0;
  border-color: var(--red);
  border-radius: 8px;
  font-family: var(--font-ui);
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.02em;
  text-transform: none;
  box-shadow: none;
}

.login-actions .btn--primary {
  box-shadow: 3px 3px 0 var(--ink-a28);
}

.login-actions .btn:hover {
  transform: none;
  box-shadow: none;
}

.login-actions .btn--primary:hover {
  box-shadow: 3px 3px 0 var(--ink-a28);
}

.login-actions .btn:disabled {
  cursor: not-allowed;
}

.login-foot {
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
}

.checkbox {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--ink);
  cursor: pointer;
}

.checkbox input {
  appearance: none;
  width: 18px;
  height: 18px;
  margin: 0;
  border: 1.5px solid var(--red);
  border-radius: 3px;
  background: var(--card);
  position: relative;
  cursor: pointer;
}

.checkbox input:checked {
  background: var(--red);
}

.checkbox input:checked::after {
  content: '';
  position: absolute;
  left: 4px;
  top: 1px;
  width: 6px;
  height: 10px;
  border: 2px solid var(--paper);
  border-top: 0;
  border-left: 0;
  transform: rotate(42deg);
}

.login-foot__link {
  padding: 0;
  border: 0;
  background: transparent;
  font-size: 12px;
  color: var(--ink-soft);
  cursor: pointer;
}

.login-foot__link:hover {
  color: var(--accent-text);
}

.login-foot__link:disabled {
  color: var(--ink-faint);
  cursor: not-allowed;
}

/* 版权行贴视口底部正中；留 44px 是为了避开本地 dev 时底部居中的 Vue DevTools 悬浮块 */
.login-copy {
  position: absolute;
  bottom: 44px;
  left: 50%;
  transform: translateX(-50%);
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.08em;
  color: var(--ink-soft);
  white-space: nowrap;
}

/* 深色：实心圆标降级为淡绿底 + 白字（D087） */
[data-theme='dark'] .login-tab.is-on svg {
  background: var(--accent-soft);
  color: var(--ink);
}

/* 窄屏：方式栏转为卡片上方一行，整组仍居中 */
@media (max-width: 1080px) {
  .login-page {
    grid-template-columns: minmax(0, 1fr);
    justify-items: center;
    padding: 88px 20px 64px;
  }

  .login-side {
    justify-self: center;
    width: 100%;
    max-width: 480px;
    margin: 0 0 24px;
    padding: 0;
  }

  .login-side h2 {
    margin-bottom: 16px;
    text-align: left;
  }

  .login-side ul {
    display: flex;
    gap: 8px;
    overflow-x: auto;
    padding-bottom: 4px;
  }

  .login-side li {
    margin-bottom: 0;
    flex: none;
  }

  .login-tab {
    width: 82px;
    font-size: 11px;
  }

  .login-tab svg {
    width: 40px;
    height: 40px;
  }

  .login-side__note {
    max-width: none;
    margin-top: 12px;
  }

  .login-card {
    width: 100%;
    max-width: 480px;
    padding: 30px 22px 26px;
  }

  .login-card__brand {
    font-size: 48px;
  }

  .login-card__mark {
    width: 84px;
    height: 84px;
    margin-bottom: 10px;
  }

  .login-corner {
    top: 12px;
    right: 12px;
    gap: 8px;
  }

  .login-copy {
    bottom: 40px;
  }
}

/* 矮屏压缩卡片，避免「居中」变成上下溢出（只动纵向尺寸，横向留白交给宽度断点） */
@media (max-height: 820px) {
  .login-page {
    padding-block: 56px;
  }

  .login-card {
    padding-block: 28px 22px;
  }

  .login-card__mark {
    width: 72px;
    height: 72px;
    margin-bottom: 8px;
  }

  .login-card__brand {
    font-size: 48px;
  }

  .input-icon {
    margin-top: 12px;
  }

  .login-actions {
    margin-top: 16px;
  }
}
</style>
