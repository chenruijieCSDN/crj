// 调试脚本：检查前端资源加载
console.log('=== 开始调试 ===');

// 检查基础路径
console.log('BASE_URL:', import.meta.env.BASE_URL);
console.log('NODE_ENV:', import.meta.env.NODE_ENV);

// 检查路由模式
import router from './src/router/index.js';
console.log('路由模式:', router.history.constructor.name);

// 监听路由错误
router.onError((error) => {
  console.error('路由错误:', error);
});

// 监听未处理的Promise拒绝
window.addEventListener('unhandledrejection', (event) => {
  console.error('未处理的Promise拒绝:', event.reason);
});

// 监听资源加载错误
window.addEventListener('error', (event) => {
  console.error('资源加载错误:', event.filename, event.message);
}, true);

// 检查DOM是否正确挂载
window.addEventListener('DOMContentLoaded', () => {
  console.log('DOM加载完成');
  console.log('#app元素存在:', !!document.getElementById('app'));

  // 延迟检查Vue是否正确挂载
  setTimeout(() => {
    console.log('检查Vue挂载状态...');
    const app = document.getElementById('app');
    if (app) {
      console.log('#app内容:', app.innerHTML.substring(0, 200));
      console.log('#app子元素数量:', app.children.length);
    }
  }, 1000);
});

console.log('=== 调试脚本加载完成 ===');