import { createRouter, createWebHashHistory } from 'vue-router'

console.log('初始化路由...')
console.log('BASE_URL:', import.meta.env.BASE_URL)

const routes = [
  { path: '/', name: 'Home', component: () => import('../views/Home.vue'), meta: { title: 'AI 应用' } },
  { path: '/love', name: 'LoveApp', component: () => import('../views/LoveAppChat.vue'), meta: { title: 'AI 恋爱大师' } },
  { path: '/manus', name: 'Manus', component: () => import('../views/ManusChat.vue'), meta: { title: 'AI 超级智能体' } },
]

const router = createRouter({
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes,
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - AI 应用` : 'AI 应用'
})

export default router
