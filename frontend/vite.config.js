import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
    /**
     * Element Plus 只服务管理端（D094）。按需引入，样式跟着用到的组件走，
     * 全部落进 /admin 那个懒加载 chunk，公开站点一个字节都不多背。
     * dts 关掉：本仓是纯 JS，不需要生成 .d.ts。
     */
    AutoImport({
      resolvers: [ElementPlusResolver()],
      dts: false,
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: false,
      // 只让 admin 目录享受自动注册，公开站点的组件仍显式 import，保持原样
      dirs: [],
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    port: 5577,
    proxy: {
      '/api': {
        target: 'http://localhost:5757/',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
})
