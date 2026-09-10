import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/chat' },
  { path: '/chat', component: () => import('@/views/ChatView.vue'), meta: { title: '在线客服' } },
  { path: '/login', component: () => import('@/views/admin/Login.vue'), meta: { title: '登录' } },
  {
    path: '/admin',
    component: () => import('@/views/admin/Layout.vue'),
    redirect: '/admin/dashboard',
    children: [
      { path: 'dashboard', component: () => import('@/views/admin/Dashboard.vue'), meta: { title: '工作台' } },
      { path: 'knowledge', component: () => import('@/views/admin/Knowledge.vue'), meta: { title: '知识库管理' } },
      { path: 'agents', component: () => import('@/views/admin/Agents.vue'), meta: { title: '客服管理' } },
      { path: 'sessions', component: () => import('@/views/admin/Sessions.vue'), meta: { title: '会话管理' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/chat' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (to.path.startsWith('/admin') && !token) {
    return '/login'
  }
  if (to.meta.title) {
    document.title = `${to.meta.title} - 智能客服系统`
  }
  return true
})

export default router
