<script setup>
import { nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { loginWithPassword } from '@/api/site'
import { authErrorKey } from '@/api/authError'
import { useSessionStore } from '@/stores/session'
import FormField from '@/components/FormField.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
})
const emit = defineEmits(['update:open', 'success'])

const { t } = useI18n()
const session = useSessionStore()

const form = reactive({ username: '', password: '' })
const submitting = ref(false)
const errorKey = ref('')
const dialogEl = ref(null)

/**
 * 打开前记住焦点落在哪，关闭时还回去。
 * 键盘用户点「下载」触发弹窗，关掉后焦点若掉回 <body>，Tab 就得从页头重新开始，
 * 等于把人的位置丢了。
 */
let lastFocused = null

function onKeydown(event) {
  if (event.key === 'Escape') {
    event.stopPropagation()
    close()
  }
}

watch(
  () => props.open,
  async (open) => {
    if (open) {
      errorKey.value = ''
      submitting.value = false
      lastFocused = document.activeElement
      document.addEventListener('keydown', onKeydown)
      await nextTick()
      // 焦点进框：弹窗里第一件事就是输入用户名
      dialogEl.value?.querySelector('input')?.focus()
    } else {
      document.removeEventListener('keydown', onKeydown)
      if (lastFocused && typeof lastFocused.focus === 'function') {
        lastFocused.focus()
      }
      lastFocused = null
    }
  },
)

onBeforeUnmount(() => document.removeEventListener('keydown', onKeydown))

function close() {
  emit('update:open', false)
}

async function submit() {
  if (submitting.value) return
  // 先按后端 DTO 的限制拦一道：用户名 1–10 位、密码 6–20 位，
  // 免得填错了走一趟网络，最后只拿到一句「请求参数有误」
  const username = form.username.trim()
  if (!username || !form.password) {
    errorKey.value = 'auth.required'
    return
  }
  if (username.length > 10) {
    errorKey.value = 'auth.invalidUsername'
    return
  }
  if (form.password.length < 6 || form.password.length > 20) {
    errorKey.value = 'auth.invalidPassword'
    return
  }
  submitting.value = true
  errorKey.value = ''
  try {
    const data = await loginWithPassword(username, form.password)
    session.setSession(data.token, data.user)
    emit('success', data.user)
    close()
  } catch (error) {
    // 错误码 → 文案的映射统一走 authErrorKey，登录页与弹窗同一套口径
    errorKey.value = authErrorKey(error, 'password')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="auth-overlay" @click.self="close">
      <div
        ref="dialogEl"
        class="auth card"
        role="dialog"
        aria-modal="true"
        aria-labelledby="auth-title"
        aria-describedby="auth-lead"
      >
        <div class="auth__head">
          <h2 id="auth-title" class="auth__title">{{ t('auth.title') }}</h2>
          <button type="button" class="auth__close" :aria-label="t('common.close')" @click="close">
            ×
          </button>
        </div>
        <p id="auth-lead" class="auth__lead">{{ t('auth.lead') }}</p>

        <form class="auth__form" @submit.prevent="submit">
          <FormField v-model="form.username" :label="t('auth.username')" autocomplete="username" />
          <FormField
            v-model="form.password"
            :label="t('auth.password')"
            type="password"
            autocomplete="current-password"
          />

          <p v-if="errorKey" class="auth__error mono" role="alert">{{ t(errorKey) }}</p>

          <div class="auth__actions">
            <button type="button" class="btn btn--ghost" @click="close">
              {{ t('auth.cancel') }}
            </button>
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
  background: var(--overlay);
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
  border-color: var(--line);
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

.auth__error {
  font-size: 12px;
  color: var(--accent-text);
}

.auth__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 6px;
}
</style>
