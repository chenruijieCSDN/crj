<template>
  <div class="app">
    <router-view />
  </div>
</template>

<script setup>
</script>

<style>
/* 全局设计变量：统一风格 */
:root {
  --page-bg: #f5f5f7;
  --surface: #ffffff;
  --text-primary: #1d1d1f;
  --text-secondary: #6e6e73;
  --border: rgba(0, 0, 0, 0.08);
  --radius-sm: 12px;
  --radius-md: 16px;
  --radius-lg: 20px;
  --shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  --shadow-hover: 0 12px 32px rgba(0, 0, 0, 0.1);
  --header-height: 56px;
  --input-area-min: 52px;
  --max-content: 720px;
  /* 恋爱大师主题色 */
  --love-bg: linear-gradient(145deg, #fff5f5 0%, #ffe8ec 100%);
  --love-border: rgba(220, 120, 130, 0.2);
  --love-accent: #c75c67;
  --love-bubble-user: #e8a0a8;
  --love-bubble-ai-bg: #fff;
  --love-bubble-ai-border: rgba(220, 120, 130, 0.25);
  /* 超级智能体主题色 */
  --manus-bg: linear-gradient(145deg, #f0f7ff 0%, #e5eeff 100%);
  --manus-border: rgba(80, 120, 200, 0.2);
  --manus-accent: #4a6fb5;
  --manus-bubble-user: #4a6fb5;
  --manus-bubble-ai-bg: #fff;
  --manus-bubble-ai-border: rgba(80, 120, 200, 0.2);
}

* {
  box-sizing: border-box;
}
html, body, #app, .app {
  height: 100%;
  margin: 0;
}
body {
  font-family: 'Noto Sans SC', -apple-system, BlinkMacSystemFont, sans-serif;
  background: var(--page-bg);
  font-size: 16px;
  -webkit-font-smoothing: antialiased;
}

/* 响应式基准 */
@media (max-width: 768px) {
  body { font-size: 15px; }
}
@media (max-width: 480px) {
  body { font-size: 14px; }
}
</style>
