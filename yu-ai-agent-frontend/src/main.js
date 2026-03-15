import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

// 防止外部脚本（如浏览器扩展）的未捕获 Promise 错误导致白屏
window.addEventListener('unhandledrejection', (e) => {
  const url = e.reason?.config?.url || e.reason?.message || ''
  if (typeof url === 'string' && (url.includes('version') || url.includes('vhchi') || url.includes('yhchi'))) {
    e.preventDefault()
    e.stopPropagation()
  }
})

const app = createApp(App)
app.use(router)
app.mount('#app')
