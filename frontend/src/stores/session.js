import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { TOKEN_KEY } from '@/api/http'

const USER_KEY = 'hundunlab.user'

function readUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

/**
 * 登录态。
 *
 * 只做「令牌 + 用户信息」的存取，不在这里发请求、不在这里跳路由：
 * 首页和软件页是匿名可看的，登录只是一个可选动作。
 */
export const useSessionStore = defineStore('session', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref(readUser())

  const isLoggedIn = computed(() => Boolean(token.value))
  const displayName = computed(() => user.value?.nickname || user.value?.username || '')

  function setSession(nextToken, nextUser) {
    token.value = nextToken || ''
    user.value = nextUser || null
    if (nextToken) {
      localStorage.setItem(TOKEN_KEY, nextToken)
    } else {
      localStorage.removeItem(TOKEN_KEY)
    }
    if (nextUser) {
      localStorage.setItem(USER_KEY, JSON.stringify(nextUser))
    } else {
      localStorage.removeItem(USER_KEY)
    }
  }

  function clear() {
    setSession('', null)
  }

  return { token, user, isLoggedIn, displayName, setSession, clear }
})
