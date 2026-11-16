import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { readToken, readUser, saveSession, clearSession } from '@/utils/token'

/**
 * 登录态。
 *
 * 只做「令牌 + 用户信息」的存取，不在这里发请求、不在这里跳路由：
 * 首页和软件页是匿名可看的，登录只是一个可选动作。
 *
 * 第三个参数 remember 来自登录页的「记住我」：true 存 localStorage，
 * false 存 sessionStorage（关掉浏览器就失效）。存哪由 utils/token 决定。
 */
export const useSessionStore = defineStore('session', () => {
  const token = ref(readToken())
  const user = ref(readUser())

  const isLoggedIn = computed(() => Boolean(token.value))
  const displayName = computed(() => user.value?.nickname || user.value?.username || '')

  function setSession(nextToken, nextUser, remember = true) {
    token.value = nextToken || ''
    user.value = nextUser || null
    if (nextToken) saveSession(nextToken, nextUser, remember)
    else clearSession()
  }

  function clear() {
    setSession('', null)
  }

  return { token, user, isLoggedIn, displayName, setSession, clear }
})
