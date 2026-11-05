import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import i18n from './i18n'

// 全局样式：base.css 只做重置，site.css 是站点自己的设计系统，
// hundun-kit.css 是原型拆出来的物料库（hd- 前缀，与 site.css 互不干扰）
import '@/assets/css/base.css'
import '@/assets/css/site.css'
import '@/assets/css/hundun-kit.css'

const app = createApp(App)

app.use(createPinia())
app.use(i18n)
app.use(router)

app.mount('#app')
