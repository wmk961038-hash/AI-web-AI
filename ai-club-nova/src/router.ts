import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from './stores/auth'

export const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    { path: '/', component: () => import('./views/Home.vue'), meta: { title: '首页' } },
    {
      path: '/about',
      component: () => import('./views/StaticPage.vue'),
      props: { pageKey: 'about' },
      meta: { title: '关于我们' },
    },
    {
      path: '/join',
      component: () => import('./views/StaticPage.vue'),
      props: { pageKey: 'join' },
      meta: { title: '加入我们' },
    },
    {
      path: '/news',
      component: () => import('./views/Listing.vue'),
      props: { section: 'news' },
      meta: { title: '新闻与活动' },
    },
    {
      path: '/projects',
      component: () => import('./views/Listing.vue'),
      props: { section: 'projects' },
      meta: { title: '项目与成果' },
    },
    {
      path: '/people',
      component: () => import('./views/Listing.vue'),
      props: { section: 'people' },
      meta: { title: '师生风采' },
    },
    {
      path: '/contents/:id',
      component: () => import('./views/Detail.vue'),
      meta: { title: '内容详情' },
    },
    { path: '/login', component: () => import('./views/Login.vue'), meta: { title: '管理员登录' } },
    {
      path: '/admin',
      component: () => import('./views/Admin.vue'),
      meta: { admin: true, title: '内容管理' },
    },
    {
      path: '/admin/pages/:key',
      component: () => import('./views/PageEditor.vue'),
      meta: { admin: true, title: '编辑页面' },
    },
    {
      path: '/admin/section/:section',
      component: () => import('./views/SectionManager.vue'),
      meta: { admin: true, title: '栏目管理' },
    },
    {
      path: '/admin/people/:id',
      component: () => import('./views/PersonEditor.vue'),
      meta: { admin: true, title: '编辑师生' },
    },
    {
      path: '/admin/edit/:id',
      component: () => import('./views/Editor.vue'),
      meta: { admin: true, title: '编辑内容' },
    },
    {
      path: '/admin/preview/:id',
      component: () => import('./views/Detail.vue'),
      props: { preview: true },
      meta: { admin: true, title: '草稿预览' },
    },
    {
      path: '/:pathMatch(.*)*',
      component: () => import('./views/NotFound.vue'),
      meta: { title: '页面未找到' },
    },
  ],
})

router.beforeEach(async (to) => {
  document.title = `${String(to.meta.title)} · 人工智能社`
  if (to.meta.admin) {
    const auth = useAuth()
    await auth.load()
    if (!auth.user) return { path: '/login', query: { redirect: to.fullPath } }
  }
})
