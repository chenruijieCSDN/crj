import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

// 防止未捕获的 Promise 错误导致白屏（扩展/注入脚本或网络失败）
window.addEventListener('unhandledrejection', (e) => {
  const r = e.reason
  const msg = typeof r === 'string' ? r : (r?.message ?? (r && String(r).slice(0, 200)))
  const url = r?.config?.url ?? ''
  const fromExternal = url.includes('yhch') || url.includes('version') || msg.includes('ERR_CONNECTION') || (typeof msg === 'string' && msg.includes('version'))
  if (fromExternal) {
    e.preventDefault()
    e.stopPropagation()
    return
  }
  // 其它未捕获的 rejection 也吞掉，避免整页崩溃（可打开下一行调试）
  // console.warn('unhandledrejection', r)
  e.preventDefault()
})

const app = createApp(App)
app.use(router)
app.mount('#app')
