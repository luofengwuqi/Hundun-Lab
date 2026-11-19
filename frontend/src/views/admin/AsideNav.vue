<script setup>
/**
 * 管理端侧栏（复刻 15-hundun 的 Aside.vue 视觉，D094）。
 *
 * 结构照原项目：logo → 分割线 → 菜单条 → 底部（问候 + 帮助 + 退出）。
 * 差别只有两处，都是本仓硬约束：
 * - logo 只能取 docs/design/logo 那一套（D088），所以用 BrandMark 而不是原项目的 png；
 * - 菜单跳的是路由（/admin/users），不是原项目的 ref 下标切组件——
 *   地址栏要能直接贴给同事，刷新不能掉回第一页。
 *
 * 未实现的模块不隐藏，灰着并说明原因（D036「不假装能点」）。
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { logout } from '@/api/site'
import { useSessionStore } from '@/stores/session'
import BrandMark from '@/components/BrandMark.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const session = useSessionStore()

const myStatus = computed(() => Number(session.user?.status ?? 0))

/**
 * 邀请管理是超管专属：后端的 /admin/invite-code 与 /admin/invites 都是
 * @RequireRole(status=3)，普通管理员在这里看不到入口，
 * 和原项目的 userStatus !== 3 判断一致。
 */
const items = computed(() =>
  [
    { key: 'overview', to: '/admin', label: t('admin.navOverview'), ready: true },
    // 软件与内容块（F12/F13）：列表 + 新建 + 编辑，安装包在编辑页第二段
    { key: 'software', to: '/admin/software', label: t('admin.navSoftware'), ready: true },
    { key: 'users', to: '/admin/users', label: t('admin.navUsers'), ready: true },
    {
      key: 'invite',
      to: '/admin/invites',
      label: t('admin.navInvite'),
      ready: true,
      superOnly: true,
    },
    { key: 'feedback', label: t('admin.navFeedback'), ready: false },
    { key: 'panel', label: t('admin.navPanel'), ready: false },
  ].filter((item) => !item.superOnly || myStatus.value >= 3),
)

function isActive(item) {
  if (!item.to) return false
  // /admin 是父路由，精确匹配，否则用户管理页会把概览也点亮
  return item.to === '/admin' ? route.path === '/admin' : route.path.startsWith(item.to)
}

async function handleLogout() {
  try {
    await logout()
  } catch {
    // 服务端令牌可能已经失效，本地照样要清干净
  }
  session.clear()
  router.push({ name: 'login' })
}
</script>

<template>
  <div class="aside">
    <RouterLink to="/admin" class="aside__logo">
      <span class="aside__mark" aria-hidden="true"><BrandMark /></span>
      <span class="aside__wordmark">HUNDUN<em>LAB</em></span>
    </RouterLink>
    <p class="aside__badge">{{ t('admin.badge') }}</p>

    <el-divider class="aside__rule" />

    <nav class="aside__menu" :aria-label="t('admin.navLabel')">
      <template v-for="item in items" :key="item.key">
        <RouterLink
          v-if="item.ready"
          :to="item.to"
          class="aside__item"
          :class="{
            'is-active': isActive(item),
          }"
        >
          {{ item.label }}
        </RouterLink>
        <span v-else class="aside__item is-off" :title="t('admin.pending')">
          {{ item.label }}
        </span>
      </template>
    </nav>

    <div class="aside__foot">
      <el-divider class="aside__rule" />
      <p class="aside__hello">
        {{ t('admin.greeting') }}<span>{{ session.displayName }}</span>
      </p>
      <div class="aside__actions">
        <button type="button" class="aside__btn" @click="router.push('/')">
          {{ t('admin.backToSite') }}
        </button>
        <button type="button" class="aside__btn" @click="handleLogout">
          {{ t('admin.signOut') }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.aside {
  position: relative;
  height: 100%;
  padding: 0 16px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.aside__logo {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 20px;
  color: var(--adm-ink);
}

.aside__mark {
  width: 44px;
  height: 44px;
  flex: none;
  color: var(--adm-primary);
}

.aside__mark :deep(svg) {
  width: 100%;
  height: 100%;
}

.aside__wordmark {
  font-family: var(--font-display);
  font-size: 19px;
  line-height: 1;
}

.aside__wordmark em {
  font-style: normal;
  color: var(--adm-primary);
}

.aside__badge {
  margin-top: 6px;
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.14em;
  color: var(--adm-ink-faint);
}

.aside__rule {
  margin: 16px 0;
  border-color: var(--adm-surface-head);
}

.aside__menu {
  display: grid;
  gap: 4px;
  padding: 4px 0;
}

/* 菜单条：渐变米色 + 光带扫过 + 上浮，全部照原项目的手感 */
.aside__item {
  position: relative;
  display: block;
  margin: 6px 4%;
  padding: 14px 20px;
  width: 92%;
  border-radius: var(--adm-radius-nav);
  background: linear-gradient(135deg, var(--adm-nav-bg-from) 0%, var(--adm-nav-bg-to) 100%);
  box-shadow: var(--adm-shadow-nav);
  color: var(--adm-nav-ink);
  font-size: 15px;
  font-weight: 600;
  line-height: 1.5;
  text-align: center;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.aside__item::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, var(--adm-nav-glow), transparent);
  transition: left 0.5s;
}

.aside__item:hover {
  transform: translateY(-2px) scale(1.02);
  box-shadow: var(--adm-shadow-nav-hover);
  background: linear-gradient(135deg, var(--adm-nav-bg-to) 0%, var(--adm-nav-bg-hover-to) 100%);
}

.aside__item:hover::before {
  left: 100%;
}

.aside__item:active {
  transform: translateY(0) scale(0.98);
}

.aside__item.is-active {
  transform: scale(1.05);
  border: 1px solid var(--adm-primary-block-border);
  background: var(--adm-primary-block);
  box-shadow: var(--adm-shadow-primary);
  color: var(--adm-primary-block-ink);
  font-weight: 700;
}

.aside__item.is-active::before {
  background: linear-gradient(90deg, transparent, var(--adm-nav-shine), transparent);
}

.aside__item.is-active:hover {
  transform: translateY(-3px) scale(1.05);
  box-shadow: var(--adm-shadow-primary-hover);
  background: var(--adm-primary-block-hover);
}

.aside__item.is-active:active {
  transform: translateY(0) scale(1.02);
}

/* 未接模块：不发光、不响应，鼠标是禁止符 */
.aside__item.is-off {
  color: var(--adm-ink-faint);
  background: transparent;
  box-shadow: none;
  cursor: not-allowed;
}

.aside__item.is-off:hover {
  transform: none;
  background: transparent;
  box-shadow: none;
}

.aside__item.is-off::before {
  display: none;
}

.aside__foot {
  margin-top: auto;
  padding-bottom: 20px;
  text-align: center;
}

.aside__hello {
  margin-bottom: 6px;
  font-size: 13px;
  color: var(--adm-ink-faint);
}

.aside__hello span {
  color: var(--adm-nav-ink);
  font-weight: 600;
}

.aside__actions {
  display: flex;
  gap: 5%;
}

.aside__btn {
  flex: 1;
  height: 32px;
  border: 1px solid var(--adm-primary-block-border);
  border-radius: var(--adm-radius-pill);
  background: var(--adm-primary-block);
  color: var(--adm-primary-block-ink);
  font-size: 13px;
  font-weight: 600;
  line-height: 32px;
  cursor: pointer;
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

.aside__btn:hover {
  background: var(--adm-primary-block-hover);
}

.aside__btn:focus-visible {
  outline: 3px solid var(--adm-primary);
  outline-offset: 2px;
}
</style>
