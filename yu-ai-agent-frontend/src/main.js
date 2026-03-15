import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

// 添加更详细的错误处理和调试信息
console.log('=== Vue应用启动 ===');
console.log('环境:', import.meta.env.MODE);
console.log('BASE_URL:', import.meta.env.BASE_URL);

// 创建简单的加载指示器
const loadingDiv = document.createElement('div');
loadingDiv.innerHTML = '正在加载应用...';
loadingDiv.style.cssText = 'position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);padding:20px;background:#fff;border:1px solid #ccc;border-radius:8px;z-index:9999;';
document.body.appendChild(loadingDiv);

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
  console.warn('未处理的Promise拒绝:', r)
  e.preventDefault()
})

// 监听资源加载错误
window.addEventListener('error', (e) => {
  console.error('资源加载错误:', e.filename, e.message, e.error)
}, true)

// 确保DOM完全加载后再挂载
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => {
    console.log('DOM加载完成，开始挂载Vue应用')
    mountApp()
  })
} else {
  console.log('DOM已加载，直接挂载Vue应用')
  mountApp()
}

function mountApp() {
  try {
    console.log('创建Vue应用实例...')
    const app = createApp(App)

    console.log('安装路由插件...')
    app.use(router)

    // 监听路由错误
    router.onError((error) => {
      console.error('路由错误:', error)
    })

    console.log('正在挂载到#app元素...')
    const mountPoint = document.getElementById('app')
    if (mountPoint) {
      app.mount('#app')
      console.log('Vue应用挂载成功')

      // 移除加载指示器
      setTimeout(() => {
        if (loadingDiv.parentNode) {
          loadingDiv.parentNode.removeChild(loadingDiv);
        }
      }, 500);
    } else {
      console.error('找不到#app挂载点')
      loadingDiv.innerHTML = '错误：找不到挂载点';
    }
  } catch (error) {
    console.error('Vue应用挂载失败:', error)
    loadingDiv.innerHTML = '应用加载失败：' + error.message;
  }
}
