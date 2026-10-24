import { createRouter, createWebHistory } from 'vue-router'
import SiteLayout from '@/layouts/SiteLayout.vue'

/**
 * 前台路由。全部匿名可访问，登录只影响「下载」这个动作，
 * 所以这里不写任何全局跳转守卫——守卫会把匿名用户挡在首页外面。
 * 管理后台会在阶段 3 以独立布局挂在 /admin 下，单独加守卫。
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: SiteLayout,
      children: [
        { path: '', name: 'home', component: () => import('@/views/site/HomePage.vue') },
        {
          path: 'software',
          name: 'software-list',
          component: () => import('@/views/site/SoftwareListPage.vue'),
        },
        {
          path: 'software/:slug',
          name: 'software-detail',
          component: () => import('@/views/site/SoftwarePage.vue'),
        },
        { path: 'plugins', name: 'plugins', component: () => import('@/views/site/PluginsPage.vue') },
        { path: 'login', name: 'login', component: () => import('@/views/site/LoginPage.vue') },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
  scrollBehavior(to, from, savedPosition) {
    return savedPosition || { top: 0 }
  },
})

router.afterEach((to) => {
  // 详情页拿到数据后会自己覆盖标题，这里只处理静态页
  if (to.name !== 'software-detail') {
    document.title = 'Hundun Lab'
  }
})

export default router
