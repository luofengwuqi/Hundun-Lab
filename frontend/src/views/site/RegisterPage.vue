<script setup>
/**
 * 注册页 · 独立满屏版式（F05）
 *
 * 与 /login 同族：不套 SiteLayout，480px 卡片居中，左侧一条说明栏，
 * 语言／主题固定在视口右上角，版权行贴底。字段顺序参考骨架项目
 * 15-hundun 的 Register.vue（用户名 → 密码 → 确认密码 → 手机号 → 邮箱），
 * 但把「管理员邀请码」从常驻开关收成了折叠区：普通访客不该先看到一把钥匙。
 *
 * 校验只照抄后端 DTO（RegisterDTO / AddAdminDTO），不另立更严的规矩：
 * 用户名 1–10 且 ^[\w\u4e00-\u9fa5-]{1,10}$、密码 6–20、手机号 ^1[3-9]\d{9}$。
 * 「确认密码」是纯前端校验——后端早已不接收第二次输入。
 *
 * 可选字段为空时**不发这个键**：后端虽然会把空串归一化成 NULL，
 * 但少一层「空串算不算填了」的歧义更稳（见 UserServiceImpl#register）。
 *
 * 注册接口不返回令牌，所以成功后回登录页，并带上用户名预填。
 */
import { computed, reactive, ref, useId } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { registerUser, registerAdminByInvite } from '@/api/site'
import { LOCALE_LABELS, setLocale } from '@/i18n'
import BrandMark from '@/components/BrandMark.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'

const { t, te, locale } = useI18n()
const route = useRoute()
const router = useRouter()

const usernameId = useId()
const passwordId = useId()
const passwordAgainId = useId()
const emailId = useId()
const phoneId = useId()
const inviteId = useId()

// 与后端 DTO 一一对应
const USERNAME_RE = /^[\w\u4e00-\u9fa5-]{1,10}$/
const PASSWORD_MIN = 6
const PASSWORD_MAX = 20
const PHONE_RE = /^1[3-9]\d{9}$/
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const form = reactive({
  username: '',
  password: '',
  passwordAgain: '',
  email: '',
  phone: '',
  inviteCode: '',
})

const adminMode = ref(false)
const showPassword = ref(false)
const showPasswordAgain = ref(false)
const submitting = ref(false)
const errorKey = ref('')

const rules = computed(() => [
  t('auth.registerSideRule1'),
  t('auth.registerSideRule2'),
  t('auth.registerSideRule3'),
])

// 从登录页带过来的来源页：注册成功后原样带回去，登录完直接续上
const redirect = computed(() =>
  typeof route.query.redirect === 'string' ? route.query.redirect : '',
)

// 回登录页时只带来源页，别把 username / registered 这类一次性参数再传一遍
const loginQuery = computed(() => (redirect.value ? { redirect: redirect.value } : {}))

function toggleAdminMode() {
  adminMode.value = !adminMode.value
  if (!adminMode.value) form.inviteCode = ''
  errorKey.value = ''
}

/** 按当前模式校验，返回 i18n 键；空串表示通过 */
function validate() {
  if (!USERNAME_RE.test(form.username.trim())) return 'auth.registerUsernameInvalid'
  if (form.password.length < PASSWORD_MIN || form.password.length > PASSWORD_MAX) {
    return 'auth.invalidPassword'
  }
  if (form.password !== form.passwordAgain) return 'auth.registerMismatch'

  const email = form.email.trim()
  if (email && !EMAIL_RE.test(email)) return 'auth.invalidEmail'
  const phone = form.phone.trim()
  if (phone && !PHONE_RE.test(phone)) return 'auth.invalidPhone'

  if (adminMode.value && !form.inviteCode.trim()) return 'auth.registerInviteRequired'
  return ''
}

/** 后端错误码 → i18n 键：先用 errors.* 查，查不到才落回通用文案 */
function errorKeyFor(error) {
  const code = error?.errorCode || ''
  const key = `errors.${code}`
  return code && te(key) ? key : 'auth.registerFailed'
}

async function submit() {
  if (submitting.value) return

  const invalid = validate()
  if (invalid) {
    errorKey.value = invalid
    return
  }

  submitting.value = true
  errorKey.value = ''
  try {
    // 空的可选字段直接不出现，避免把「没填」发成空串
    const payload = { username: form.username.trim(), password: form.password }
    const email = form.email.trim()
    const phone = form.phone.trim()
    if (email) payload.email = email
    if (phone) payload.phone = phone

    if (adminMode.value) {
      payload.importantPassword = form.inviteCode.trim()
      await registerAdminByInvite(payload)
    } else {
      await registerUser(payload)
    }

    const query = { username: payload.username, registered: '1' }
    if (redirect.value) query.redirect = redirect.value
    router.replace({ name: 'login', query })
  } catch (error) {
    errorKey.value = errorKeyFor(error)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="register-page">
    <div class="register-corner">
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

    <aside class="register-side">
      <h2>{{ t('auth.registerSideTitle') }}</h2>
      <ol class="register-rules">
        <li v-for="(rule, index) in rules" :key="index">
          <span class="register-rules__num mono">{{ String(index + 1).padStart(2, '0') }}</span>
          <span>{{ rule }}</span>
        </li>
      </ol>
      <p class="register-side__note">{{ t('auth.registerSideNote') }}</p>
    </aside>

    <form class="register-card" novalidate @submit.prevent="submit">
      <span class="register-card__mark" aria-hidden="true"><BrandMark /></span>
      <div class="register-card__brand">HUNDUN<em>LAB</em></div>
      <h1 class="register-card__title">
        {{ t('auth.registerTitle') }}
        <span v-if="adminMode" class="register-card__badge">{{
          t('auth.registerAdminBadge')
        }}</span>
      </h1>

      <div class="field">
        <label class="field__label" :for="usernameId">{{ t('auth.username') }}</label>
        <input
          :id="usernameId"
          v-model="form.username"
          class="field__input"
          type="text"
          autocomplete="username"
          maxlength="10"
          :placeholder="t('auth.registerUsernamePlaceholder')"
        />
      </div>

      <div class="field">
        <label class="field__label" :for="passwordId">{{ t('auth.password') }}</label>
        <span class="field__control">
          <input
            :id="passwordId"
            v-model="form.password"
            class="field__input"
            :type="showPassword ? 'text' : 'password'"
            autocomplete="new-password"
            maxlength="20"
            :placeholder="t('auth.passwordPlaceholder')"
          />
          <button
            type="button"
            class="field__eye"
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
        </span>
      </div>

      <div class="field">
        <label class="field__label" :for="passwordAgainId">{{
          t('auth.registerPasswordAgain')
        }}</label>
        <span class="field__control">
          <input
            :id="passwordAgainId"
            v-model="form.passwordAgain"
            class="field__input"
            :type="showPasswordAgain ? 'text' : 'password'"
            autocomplete="new-password"
            maxlength="20"
            :placeholder="t('auth.registerPasswordAgainPlaceholder')"
          />
          <button
            type="button"
            class="field__eye"
            :aria-label="showPasswordAgain ? t('auth.hidePassword') : t('auth.showPassword')"
            :aria-pressed="showPasswordAgain"
            @click="showPasswordAgain = !showPasswordAgain"
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
        </span>
      </div>

      <div class="field">
        <label class="field__label" :for="emailId">{{ t('auth.email') }}</label>
        <input
          :id="emailId"
          v-model="form.email"
          class="field__input"
          type="email"
          autocomplete="email"
          :placeholder="t('auth.registerEmailOptional')"
        />
      </div>

      <div class="field">
        <label class="field__label" :for="phoneId">{{ t('auth.phone') }}</label>
        <input
          :id="phoneId"
          v-model="form.phone"
          class="field__input"
          type="tel"
          inputmode="numeric"
          autocomplete="tel"
          maxlength="11"
          :placeholder="t('auth.registerPhoneOptional')"
        />
      </div>

      <p class="register-hint">{{ t('auth.registerOptionalHint') }}</p>

      <!-- 管理员注册是折叠区：普通访客默认看不到邀请码这一层 -->
      <button type="button" class="register-invite-toggle" @click="toggleAdminMode">
        <span aria-hidden="true">{{ adminMode ? '−' : '+' }}</span>
        {{ t('auth.registerInviteToggle') }}
      </button>

      <div v-if="adminMode" class="field">
        <label class="field__label" :for="inviteId">{{ t('auth.registerInviteLabel') }}</label>
        <input
          :id="inviteId"
          v-model="form.inviteCode"
          class="field__input"
          type="text"
          autocomplete="off"
          :placeholder="t('auth.registerInvitePlaceholder')"
        />
        <p class="register-hint">{{ t('auth.registerInviteHint') }}</p>
      </div>

      <p v-if="errorKey" class="register-error mono" role="alert">{{ t(errorKey) }}</p>

      <button type="submit" class="btn btn--primary register-submit" :disabled="submitting">
        {{ submitting ? t('auth.registerSubmitting') : t('auth.registerSubmit') }}
      </button>

      <RouterLink class="register-link" :to="{ name: 'login', query: loginQuery }">
        {{ t('auth.registerHasAccount') }}
      </RouterLink>
    </form>

    <p class="register-copy">{{ t('auth.copyright') }}</p>
  </main>
</template>

<style scoped>
/* 与登录页同一套底：纸底 + 纹理 + 扫描线；注册字段多，这里不强制一屏，允许纵向滚动 */
.register-page {
  position: relative;
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-content: center;
  align-items: start;
  padding: 96px 24px 72px;
  background-color: var(--paper);
  background-image:
    var(--obj-login-texture),
    repeating-linear-gradient(to bottom, var(--scan) 0 1px, transparent 1px 4px);
  background-size:
    300px 300px,
    100% 4px;
}

/* 说明栏贴在卡片左侧，本身不参与卡片居中 */
.register-side {
  justify-self: end;
  width: 220px;
  margin: 8px 56px 0 0;
  display: flex;
  flex-direction: column;
}

.register-side h2 {
  margin: 0 0 22px;
  font-size: 15px;
  font-weight: 800;
  letter-spacing: 0.1em;
  color: var(--ink);
}

.register-rules {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 18px;
}

.register-rules li {
  display: grid;
  grid-template-columns: 26px minmax(0, 1fr);
  gap: 10px;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--ink-soft);
}

.register-rules__num {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--accent-text);
}

.register-side__note {
  margin-top: 22px;
  padding-top: 14px;
  border-top: 1px dashed var(--line-faint);
  font-size: 11px;
  line-height: 1.7;
  color: var(--ink-faint);
}

.register-corner {
  position: absolute;
  top: 26px;
  right: 30px;
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 登录页没有页头，这里单独给一份同尺寸的 46px 胶囊 */
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

.register-card {
  justify-self: center;
  width: 480px;
  max-width: 100%;
  padding: 34px 40px 30px;
  border: 2px solid var(--red);
  border-radius: 10px;
  background: var(--card);
  box-shadow: var(--shadow-card);
}

.register-card__mark {
  display: block;
  width: 56px;
  height: 56px;
  margin: 0 auto 6px;
  color: var(--accent-text);
}

.register-card__mark svg {
  width: 100%;
  height: 100%;
}

.register-card__brand {
  font-family: var(--font-display);
  font-size: 34px;
  font-weight: 400;
  line-height: 1.1;
  text-align: center;
  color: var(--ink);
}

.register-card__brand em {
  font-style: normal;
  color: var(--accent-text);
}

.register-card__title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin: 6px 0 18px;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--ink-soft);
}

.register-card__badge {
  padding: 2px 8px;
  border: 1.5px solid var(--red);
  border-radius: 999px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--accent-text);
}

.field {
  display: block;
  margin-top: 12px;
}

.field__label {
  display: block;
  margin-bottom: 6px;
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--ink-soft);
}

.field__control {
  position: relative;
  display: block;
}

.field__input {
  width: 100%;
  height: 48px;
  padding: 0 14px;
  border: 1.5px solid var(--red);
  border-radius: 6px;
  background: var(--card);
  color: var(--ink);
  font-family: var(--font-ui);
  font-size: 15px;
}

.field__control .field__input {
  padding-right: 46px;
}

.field__input::placeholder {
  color: var(--ink-faint);
}

.field__input:focus {
  outline: 3px solid var(--amber);
  outline-offset: 1px;
}

.field__eye {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  display: grid;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--accent-text);
  cursor: pointer;
}

.field__eye svg {
  width: 22px;
  height: 22px;
}

.register-hint {
  margin-top: 8px;
  font-size: 11px;
  line-height: 1.6;
  color: var(--ink-faint);
}

.register-invite-toggle {
  margin-top: 16px;
  padding: 8px 12px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 1.5px dashed var(--line-soft);
  border-radius: 6px;
  background: transparent;
  color: var(--ink-soft);
  font-family: var(--font-ui);
  font-size: 12px;
  cursor: pointer;
}

.register-invite-toggle:hover {
  border-color: var(--red);
  color: var(--accent-text);
}

.register-error {
  margin-top: 14px;
  font-size: 12px;
  color: var(--accent-text);
}

.register-submit {
  width: 100%;
  height: 54px;
  margin-top: 18px;
  padding: 0;
  border-color: var(--red);
  border-radius: 8px;
  font-family: var(--font-ui);
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.02em;
  text-transform: none;
  box-shadow: 3px 3px 0 var(--ink-a28);
}

.register-submit:hover {
  transform: none;
  box-shadow: 3px 3px 0 var(--ink-a28);
}

.register-link {
  display: block;
  margin-top: 14px;
  font-size: 12px;
  text-align: center;
  color: var(--ink-soft);
}

.register-link:hover {
  color: var(--accent-text);
}

/* 版权行贴视口底部正中；留 44px 避开本地 dev 时底部居中的 DevTools 悬浮块 */
.register-copy {
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

/* 窄屏：说明栏转到卡片上方，整组居中 */
@media (max-width: 1080px) {
  .register-page {
    grid-template-columns: minmax(0, 1fr);
    justify-items: center;
    padding: 84px 20px 64px;
  }

  .register-side {
    justify-self: center;
    width: 100%;
    max-width: 480px;
    margin: 0 0 20px;
  }

  .register-side h2 {
    margin-bottom: 12px;
  }

  .register-rules {
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 10px 18px;
  }

  .register-side__note {
    margin-top: 14px;
  }

  .register-card {
    width: 100%;
    max-width: 480px;
    padding: 26px 22px 24px;
  }

  .register-corner {
    top: 12px;
    right: 12px;
    gap: 8px;
  }

  .register-copy {
    bottom: 40px;
  }
}

/* 矮屏压缩纵向间距：800px 高也要一屏看到提交按钮，别让人滚一半才发现「创建账号」 */
@media (max-height: 860px) {
  .register-page {
    align-content: start;
    padding-block: 64px 30px;
  }

  .register-card {
    padding-block: 24px 20px;
  }

  .register-card__mark {
    width: 44px;
    height: 44px;
    margin-bottom: 4px;
  }

  .register-card__brand {
    font-size: 32px;
  }

  .register-card__title {
    margin: 4px 0 12px;
  }

  .field {
    margin-top: 8px;
  }

  .field__input {
    height: 44px;
  }

  .register-hint {
    margin-top: 6px;
  }

  .register-invite-toggle {
    margin-top: 12px;
  }

  .register-submit {
    height: 50px;
    margin-top: 12px;
  }

  .register-link {
    margin-top: 10px;
  }

  .register-copy {
    display: none;
  }
}
</style>
