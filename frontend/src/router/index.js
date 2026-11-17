import { createRouter, createWebHistory } from 'vue-router'
import { readToken } from '@/utils/token'
import { useSessionStore } from '@/stores/session'
import SiteLayout from '@/layouts/SiteLayout.vue'

/**
 * 前台路由。
 *
 * 导航只有两项：首页 / 软件（D076）。扩展是 HundunDesktop 特有的能力，
 * 没有站点级入口——列表在详情页的 #extensions 分区（D074），
 * 这里只挂扩展的二级页（F07–F09）与用户页（F11）。
 * 管理后台会在 M1 以独立布局挂在 /admin 下，单独加守卫。
 *
 * 登录页不在 SiteLayout 里：原型 08-login.html 是独立的 100vh 满屏页，
 * 刻意不带全站页头页脚，所以它挂在顶级路由上。
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
        // 扩展列表就是详情页的 #extensions 分区，老链接落回锚点
        {
          path: 'software/:slug/extensions',
          redirect: (to) => ({ name: 'software-detail', params: to.params, hash: '#extensions' }),
        },
        {
          path: 'software/:slug/extensions/submit',
          name: 'extension-submit',
          component: () => import('@/views/site/ExtensionSubmitPage.vue'),
          props: true,
        },
        {
          path: 'software/:slug/extensions/:id',
          name: 'extension-detail',
          component: () => import('@/views/site/ExtensionPage.vue'),
          props: true,
        },
        {
          path: 'user/info',
          name: 'user-info',
          component: () => import('@/views/site/UserInfoPage.vue'),
        },
      ],
    },
    // 登录页：独立满屏布局，不套 SiteLayout（无全站页头页脚）
    { path: '/login', name: 'login', component: () => import('@/views/site/LoginPage.vue') },
    // 管理端（D089）：独立布局、不进 SiteLayout；子路由按 F12–F15 逐个挂，接口到了再加
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      children: [
        {
          path: '',
          name: 'admin-overview',
          component: () => import('@/views/admin/AdminOverviewPage.vue'),
          // meta.adminTitle 是 i18n 键：后台标题条跟着路由走（D094）
          meta: { adminTitle: 'admin.navOverview' },
        },
        {
          path: 'users',
          name: 'admin-users',
          component: () => import('@/views/admin/AdminUsersPage.vue'),
          meta: { adminTitle: 'admin.navUsers' },
        },
      ],
    },
    // 「插件」页已被 D074 废弃、并进软件详情的扩展分区；
    // 旧链接与桌面端 COMMUNITY_URL（Q009）都落回 HundunDesktop 的扩展分区，不留死链
    { path: '/plugins', redirect: '/software/hundundesktop#extensions' },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash) {
      // 页头是固定定位的（90px 高），锚点目标上移一截，别藏在页头后面
      return { el: to.hash, top: 106, behavior: 'smooth' }
    }
    return { top: 0 }
  },
})

/** 能进管理端的最低 status（2 = 管理员，3 = 超管，见 UserStatusConstant） */
const ADMIN_MIN_STATUS = 2

// /user/** 与 /admin/** 需要登录；记住来源，登录成功后回跳（§3.2）
router.beforeEach((to) => {
  // readToken() 会把空闲超时的会话直接清掉（口径见 utils/token），
  // 这里同步清内存态，免得页头在令牌已经作废后还挂着用户名
  const token = readToken()
  const session = useSessionStore()
  if (!token && session.isLoggedIn) session.clear()

  if (to.path.startsWith('/admin')) {
    if (!token) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
    // 本地角色只是「别让普通用户看见后台壳」；真正的拒绝在后端 AccessControlInterceptor，
    // 所以这里是回首页，而不是伪造一个 403 页面
    const status = Number(session.user?.status ?? 0)
    if (status < ADMIN_MIN_STATUS) {
      return { name: 'home' }
    }
    return true
  }

  if (to.path.startsWith('/user') && !token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
})

router.afterEach((to) => {
  // 详情页与扩展详情拿到数据后会自己覆盖标题，这里只处理静态页
  if (to.name !== 'software-detail' && to.name !== 'extension-detail') {
    document.title = 'Hundun Lab'
  }
})

export default router
