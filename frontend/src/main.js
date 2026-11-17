import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import i18n from './i18n'
import { AUTH_EXPIRED_EVENT } from '@/api/http'
import { useSessionStore } from '@/stores/session'

// 全局样式按 D057 的三层顺序引入：
// tokens.css 是全站唯一色值出处（含深浅两套主题），base.css 只做重置，
// components.css 是公共组件类；hundun-kit.css 是原型拆出来的物料库（hd- 前缀）。
import '@/styles/tokens.css'
import '@/styles/base.css'
import '@/styles/components.css'
import '@/assets/css/hundun-kit.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(i18n)
app.use(router)

/**
 * 令牌在请求途中失效（过期、被吊销、账号被封）时，http.js 只负责清本地并广播，
 * 导航在这里做：带上当前地址，登录后能回到原处，不用让人重新点一遍。
 * 已经在登录页就不再跳，避免和手写的登录流程打架。
 */
window.addEventListener(AUTH_EXPIRED_EVENT, () => {
  useSessionStore(pinia).clear()
  const current = router.currentRoute.value
  if (current.name === 'login') return
  router.replace({ name: 'login', query: { redirect: current.fullPath } })
})

app.mount('#app')
