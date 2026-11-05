import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import i18n from './i18n'

// 全局样式按 D057 的三层顺序引入：
// tokens.css 是全站唯一色值出处（含深浅两套主题），base.css 只做重置，
// components.css 是公共组件类；hundun-kit.css 是原型拆出来的物料库（hd- 前缀）。
import '@/styles/tokens.css'
import '@/styles/base.css'
import '@/styles/components.css'
import '@/assets/css/hundun-kit.css'

const app = createApp(App)

app.use(createPinia())
app.use(i18n)
app.use(router)

app.mount('#app')
