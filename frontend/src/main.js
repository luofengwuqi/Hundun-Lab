import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import i18n from './i18n'

// 全局样式：base.css 只做重置，site.css 是站点自己的设计系统
import '@/assets/css/base.css'
import '@/assets/css/site.css'

const app = createApp(App)

app.use(createPinia())
app.use(i18n)
app.use(router)

app.mount('#app')
