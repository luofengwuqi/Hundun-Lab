import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'

import App from './App.vue'
import router from './router'
//引入element-plus图标
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
//引入全局css
import '@/assets/css/base.css'
import 'element-plus/dist/index.css'


const app = createApp(App)


app.use(createPinia())
app.use(router)
app.use(ElementPlus)
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')
