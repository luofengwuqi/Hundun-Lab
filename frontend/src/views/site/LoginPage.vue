<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { loginWithPassword } from '@/api/site'
import { useSessionStore } from '@/stores/session'
import FormField from '@/components/FormField.vue'

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
    errorKey.value =
      code && t(`errors.${code}`) !== `errors.${code}` ? `errors.${code}` : 'auth.failed'
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
          <FormField v-model="form.username" :label="t('auth.username')" autocomplete="username" />
          <FormField
            v-model="form.password"
            :label="t('auth.password')"
            type="password"
            autocomplete="current-password"
          />

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
