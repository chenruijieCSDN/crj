<template>
  <div class="home">
    <header class="header">
      <h1>AI 应用</h1>
      <p>选择下方应用开始对话</p>
    </header>
    <nav class="cards">
      <router-link to="/love" class="card card-love">
        <span class="card-icon">💕</span>
        <h2>AI 恋爱大师</h2>
        <p>情感与恋爱建议助手</p>
      </router-link>
      <router-link to="/manus" class="card card-manus">
        <span class="card-icon">🤖</span>
        <h2>AI 超级智能体</h2>
        <p>多步骤任务与工具调用</p>
      </router-link>
    </nav>
  </div>
</template>

<script setup>
</script>

<style scoped>
.home {
  min-height: 100%;
  padding: 2rem 1rem;
  background: linear-gradient(160deg, #fef7f4 0%, var(--page-bg) 50%);
}
.header {
  text-align: center;
  margin-bottom: 2rem;
}
.header h1 {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 0.5rem;
}
.header p {
  color: var(--text-secondary);
  margin: 0;
  font-size: 0.95rem;
}
.cards {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  justify-content: center;
  max-width: var(--max-content);
  margin: 0 auto;
}
.card {
  flex: 1 1 240px;
  min-width: 0;
  display: block;
  padding: 1.5rem 1.25rem;
  border-radius: var(--radius-md);
  text-decoration: none;
  color: inherit;
  transition: transform 0.2s, box-shadow 0.2s;
  box-shadow: var(--shadow);
}
.card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-hover);
}
.card-love {
  background: var(--love-bg);
  border: 1px solid var(--love-border);
}
.card-manus {
  background: var(--manus-bg);
  border: 1px solid var(--manus-border);
}
.card-icon {
  font-size: 2.25rem;
  display: block;
  margin-bottom: 0.75rem;
}
.card h2 {
  font-size: 1.1rem;
  font-weight: 600;
  margin: 0 0 0.35rem;
  color: var(--text-primary);
}
.card p {
  font-size: 0.875rem;
  color: var(--text-secondary);
  margin: 0;
}

/* 平板 */
@media (min-width: 769px) {
  .home { padding: 3rem 1.5rem; }
  .header { margin-bottom: 3rem; }
  .header h1 { font-size: 1.75rem; }
  .cards { gap: 1.5rem; }
  .card { flex: 1 1 260px; padding: 2rem; }
  .card-icon { font-size: 2.5rem; }
  .card h2 { font-size: 1.15rem; }
}

/* PC 大屏 */
@media (min-width: 1024px) {
  .cards { gap: 2rem; }
}
</style>
