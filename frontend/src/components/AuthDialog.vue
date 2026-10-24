<script setup>
import { reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { loginWithPassword } from '@/api/site'
import { useSessionStore } from '@/stores/session'

const props = defineProps({
  open: { type: Boolean, default: false },
})
const emit = defineEmits(['update:open', 'success'])

const { t } = useI18n()
const session = useSessionStore()

const form = reactive({ username: '', password: '' })
const submitting = ref(false)
const errorKey = ref('')

watch(
  () => props.open,
  (open) => {
    if (open) {
      errorKey.value = ''
      submitting.value = false
    }
  },
)

function close() {
  emit('update:open', false)
}

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
    emit('success', data.user)
    close()
  } catch (error) {
    // 后端只给错误码，这里翻成当前语言；没有对应文案时用登录失败的通用提示
    const code = error?.errorCode
    errorKey.value = code && t(`errors.${code}`) !== `errors.${code}` ? `errors.${code}` : 'auth.failed'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="auth-overlay" @click.self="close">
      <div class="auth card" role="dialog" aria-modal="true" aria-labelledby="auth-title">
        <div class="auth__head">
          <h2 id="auth-title" class="auth__title">{{ t('auth.title') }}</h2>
          <button type="button" class="auth__close" :aria-label="t('common.close')" @click="close">×</button>
        </div>
        <p class="auth__lead">{{ t('auth.lead') }}</p>

        <form class="auth__form" @submit.prevent="submit">
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

          <p v-if="errorKey" class="auth__error mono">{{ t(errorKey) }}</p>

          <div class="auth__actions">
            <button type="button" class="btn btn--ghost" @click="close">{{ t('auth.cancel') }}</button>
            <button type="submit" class="btn btn--primary" :disabled="submitting">
              {{ submitting ? t('auth.submitting') : t('auth.submit') }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.auth-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(23, 19, 15, 0.55);
  backdrop-filter: blur(2px);
}

.auth {
  width: min(100%, 420px);
  padding: 26px;
  animation: pop 0.16s ease-out;
}

@keyframes pop {
  from {
    transform: translateY(8px);
    opacity: 0;
  }
}

.auth__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.auth__title {
  font-size: 20px;
}

.auth__close {
  font-size: 22px;
  line-height: 1;
  padding: 2px 6px;
  cursor: pointer;
  border: 2px solid transparent;
}

.auth__close:hover {
  border-color: var(--ink);
}

.auth__lead {
  margin-top: 8px;
  font-size: 14px;
  color: var(--ink-soft);
}

.auth__form {
  margin-top: 20px;
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

.auth__error {
  font-size: 12px;
  color: var(--red);
}

.auth__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 6px;
}
</style>
