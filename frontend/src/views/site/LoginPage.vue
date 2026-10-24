<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { loginWithPassword } from '@/api/site'
import { useSessionStore } from '@/stores/session'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const session = useSessionStore()

const form = reactive({ username: '', password: '' })
const submitting = ref(false)
const errorKey = ref('')

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
    session.setSession(data.token, data.user)
    // 回到来源页；直接打开登录页时回首页
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.replace(redirect)
  } catch (error) {
    const code = error?.errorCode
    errorKey.value = code && t(`errors.${code}`) !== `errors.${code}` ? `errors.${code}` : 'auth.failed'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-page">
      <div class="container login-wrap">
        <div class="card login">
          <h1 class="login__title">{{ t('auth.title') }}</h1>
          <p class="login__lead">{{ t('auth.lead') }}</p>

          <form class="login__form" @submit.prevent="submit">
            <label class="field">
              <span class="field__label">{{ t('auth.username') }}</span>
              <input v-model="form.username" class="field__input" type="text" autocomplete="username" />
            </label>
            <label class="field">
              <span class="field__label">{{ t('auth.password') }}</span>
              <input
                v-model="form.password"
                class="field__input"
                type="password"
                autocomplete="current-password"
              />
            </label>

            <p v-if="errorKey" class="login__error mono">{{ t(errorKey) }}</p>

            <div class="login__actions">
              <RouterLink to="/" class="btn btn--ghost">{{ t('auth.cancel') }}</RouterLink>
              <button type="submit" class="btn btn--primary" :disabled="submitting">
                {{ submitting ? t('auth.submitting') : t('auth.submit') }}
              </button>
            </div>
          </form>
        </div>
      </div>
  </div>
</template>

<style scoped>
.login-wrap {
  display: flex;
  justify-content: center;
  padding: 80px 0;
}

.login {
  width: min(100%, 440px);
  padding: 28px;
}

.login__title {
  font-size: 24px;
}

.login__lead {
  margin-top: 8px;
  font-size: 14px;
  color: var(--ink-soft);
}

.login__form {
  margin-top: 22px;
  display: grid;
  gap: 14px;
}

.field {
  display: grid;
  gap: 6px;
}

.field__label {
  font-family: var(--font-mono);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--ink-soft);
}

.field__input {
  border: 2px solid var(--ink);
  background: #fffdf7;
  padding: 9px 11px;
  font-size: 15px;
  border-radius: var(--radius);
}

.field__input:focus {
  outline: 3px solid var(--amber);
  outline-offset: 1px;
}

.login__error {
  font-size: 12px;
  color: var(--red);
}

.login__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 6px;
}
</style>
