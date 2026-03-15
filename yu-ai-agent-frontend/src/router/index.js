import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'Home', component: () => import('../views/Home.vue'), meta: { title: 'AI 应用' } },
  { path: '/love', name: 'LoveApp', component: () => import('../views/LoveAppChat.vue'), meta: { title: 'AI 恋爱大师' } },
  { path: '/manus', name: 'Manus', component: () => import('../views/ManusChat.vue'), meta: { title: 'AI 超级智能体' } },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - AI 应用` : 'AI 应用'
})

export default router
