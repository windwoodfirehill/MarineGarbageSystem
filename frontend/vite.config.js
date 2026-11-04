import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 前端 dev server 端口 5173，代理 /api 和 /static 到后端 8000（避免跨域）
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://127.0.0.1:8000', changeOrigin: true },
      '/static': { target: 'http://127.0.0.1:8000', changeOrigin: true },
    },
  },
})
