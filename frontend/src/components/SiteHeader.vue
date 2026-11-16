<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { LOCALE_LABELS, setLocale } from '@/i18n'
import ThemeToggle from '@/components/ThemeToggle.vue'
import BrandMark from '@/components/BrandMark.vue'
import { useSessionStore } from '@/stores/session'
import { logout } from '@/api/site'

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()
const session = useSessionStore()

/**
 * 窄屏抽屉。桌面导航在 860px 以下让位给汉堡按钮——
 * 原来的做法是整条导航 display:none，小屏用户直接没有导航可用。
 */
const menuOpen = ref(false)

// 主导航固定两项（D076）：扩展没有站点级入口，只能从软件详情页进
const navItems = computed(() => [
  { to: '/', label: t('nav.home') },
  { to: '/software', label: t('nav.software') },
])

function isActive(item) {
  if (item.to === '/') return route.path === '/'
  return route.path.startsWith(item.to)
}

function closeMenu() {
  menuOpen.value = false
}

function switchLocale(next) {
  setLocale(next)
  closeMenu()
}

// 换页即收起：抽屉是导航，不是常驻面板
watch(() => route.fullPath, closeMenu)

function onKeydown(event) {
  if (event.key === 'Escape') closeMenu()
}

watch(menuOpen, (open) => {
  if (open) document.addEventListener('keydown', onKeydown)
  else document.removeEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => document.removeEventListener('keydown', onKeydown))

async function handleLogout() {
  try {
    await logout()
  } catch {
    // 服务端令牌可能已经失效，本地照样要清干净
  }
  session.clear()
  closeMenu()
  router.push('/')
}
</script>

<template>
  <header class="site-header">
    <div class="container site-header__inner">
      <RouterLink to="/" class="brand" :aria-label="t('nav.home')">
        <span class="brand__mark" aria-hidden="true"><BrandMark /></span>
        <span class="brand__text">HUNDUN<em>LAB</em></span>
      </RouterLink>

      <nav class="site-nav" :aria-label="t('nav.home')">
        <template v-for="item in navItems" :key="item.to">
          <span v-if="item.disabled" class="site-nav__link is-disabled" :title="t('common.empty')">
            {{ item.label }}
          </span>
          <RouterLink
            v-else
            :to="item.to"
            class="site-nav__link"
            :class="{ 'is-active': isActive(item) }"
          >
            {{ item.label }}
          </RouterLink>
        </template>
      </nav>

      <div class="site-header__actions">
        <button
          type="button"
          class="menu-btn"
          :aria-expanded="menuOpen"
          aria-controls="site-drawer"
          :aria-label="t('nav.menu')"
          @click="menuOpen = !menuOpen"
        >
          <span class="menu-btn__bars" aria-hidden="true"><i></i><i></i><i></i></span>
        </button>

        <ThemeToggle />

        <div class="lang-switch" role="group" :aria-label="t('nav.language')">
          <button
            type="button"
            class="lang-switch__btn"
            :class="{ 'is-on': locale === 'zh' }"
            @click="switchLocale('zh')"
          >
            {{ LOCALE_LABELS.zh }}
          </button>
          <button
            type="button"
            class="lang-switch__btn"
            :class="{ 'is-on': locale === 'en' }"
            @click="switchLocale('en')"
          >
            {{ LOCALE_LABELS.en }}
          </button>
        </div>

        <template v-if="session.isLoggedIn">
          <span class="whoami mono">{{ session.displayName }}</span>
          <button type="button" class="btn btn--ghost btn--sm" @click="handleLogout">
            {{ t('nav.logout') }}
          </button>
        </template>
        <RouterLink v-else to="/login" class="btn btn--primary btn--sm">{{
          t('nav.login')
        }}</RouterLink>
      </div>
    </div>

    <!-- 窄屏抽屉：导航 + 语言 + 登录态都收在这里，宽屏隐藏 -->
    <div v-if="menuOpen" id="site-drawer" class="drawer">
      <nav class="drawer__nav" :aria-label="t('nav.menu')">
        <template v-for="item in navItems" :key="item.to">
          <span v-if="item.disabled" class="drawer__link is-disabled">{{ item.label }}</span>
          <RouterLink
            v-else
            :to="item.to"
            class="drawer__link"
            :class="{ 'is-active': isActive(item) }"
          >
            {{ item.label }}
          </RouterLink>
        </template>
      </nav>

      <div class="drawer__foot">
        <div class="lang-switch" role="group" :aria-label="t('nav.language')">
          <button
            type="button"
            class="lang-switch__btn"
            :class="{ 'is-on': locale === 'zh' }"
            @click="switchLocale('zh')"
          >
            {{ LOCALE_LABELS.zh }}
          </button>
          <button
            type="button"
            class="lang-switch__btn"
            :class="{ 'is-on': locale === 'en' }"
            @click="switchLocale('en')"
          >
            {{ LOCALE_LABELS.en }}
          </button>
        </div>

        <button
          v-if="session.isLoggedIn"
          type="button"
          class="btn btn--ghost btn--sm"
          @click="handleLogout"
        >
          {{ t('nav.logout') }}
        </button>
        <RouterLink v-else to="/login" class="btn btn--primary btn--sm">{{
          t('nav.login')
        }}</RouterLink>
      </div>
    </div>
  </header>
</template>

<style scoped>
/* 尺寸与配色按 M0b 原型 proto.css 校准：90px 高、纸底偏深、主色描边、圆胶囊导航。
   D043：全站头部只有这一份，别在页面里另写一套。 */
.site-header {
  position: fixed;
  inset: 0 0 auto 0;
  height: var(--header-h);
  z-index: 50;
  background: var(--paper-2);
  border-bottom: 2px solid var(--red);
}

.site-header__inner {
  height: 100%;
  display: flex;
  align-items: center;
  gap: 28px 44px;
}

/* 品牌图形标：docs/design/logo/hundun-lab-mark-plain.svg，内联后随 currentColor 上色 */
.brand {
  display: inline-flex;
  align-items: center;
  gap: 14px;
  flex: none;
}

.brand__mark {
  display: block;
  width: 54px;
  height: 54px;
  flex: none;
  color: var(--accent-text);
}

.brand__mark svg {
  display: block;
  width: 100%;
  height: 100%;
}

.brand__text {
  font-family: var(--font-mono);
  font-weight: 700;
  font-size: 22px;
  letter-spacing: 0.06em;
  color: var(--ink);
}

.brand__text em {
  font-style: normal;
  color: var(--accent-text);
}

.site-nav {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
}

.site-nav__link {
  padding: 9px 22px;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.04em;
  border: 2px solid transparent;
  border-radius: 999px;
  color: var(--accent-text);
}

.site-nav__link:hover {
  background: var(--accent-soft);
}

.site-nav__link.is-active {
  background: var(--chip-on-bg);
  border-color: var(--chip-on-border);
  color: var(--chip-on-ink);
}

.site-nav__link.is-disabled {
  color: var(--ink-faint);
  cursor: not-allowed;
}

.site-header__actions {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: none;
}

/* 页头里的登录／退出按原型的 46px 方块；颜色仍走 --btn-primary-* 令牌 */
.site-header__actions .btn--sm {
  height: 46px;
  padding: 0 18px;
  border-color: var(--red);
  border-radius: 8px;
  font-size: 15px;
}

.whoami {
  font-size: 12px;
  color: var(--ink-soft);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 原型是 46px 高的圆胶囊「中 / EN」；Vue 要真切换，所以保留两个按钮 */
.lang-switch {
  display: inline-flex;
  height: 46px;
  border: 2px solid var(--red);
  border-radius: 999px;
  overflow: hidden;
}

.lang-switch__btn {
  padding: 0 20px;
  font-family: var(--font-mono);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  background: transparent;
  color: var(--accent-text);
}

.lang-switch__btn + .lang-switch__btn {
  border-left: 2px solid var(--red);
}

.lang-switch__btn.is-on {
  background: var(--chip-on-bg);
  color: var(--chip-on-ink);
}

/* 汉堡按钮：宽屏不出现，窄屏才接管导航 */
.menu-btn {
  display: none;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: 2px solid var(--line);
  border-radius: var(--radius);
  background: var(--card);
  cursor: pointer;
}

.menu-btn__bars {
  display: grid;
  gap: 4px;
}

.menu-btn__bars i {
  display: block;
  width: 16px;
  height: 2px;
  background: var(--ink);
}

/* 抽屉：贴在页头下沿，整块纸底盖住内容，不做半透明以免字压字 */
.drawer {
  display: none;
  position: fixed;
  top: var(--header-h);
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 45;
  padding: 16px 20px 26px;
  overflow-y: auto;
  background: var(--paper);
  border-top: 2px solid var(--line);
}

.drawer__nav {
  display: grid;
}

.drawer__link {
  display: block;
  padding: 13px 4px;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid var(--ink-faint);
}

.drawer__link.is-active {
  color: var(--accent-text);
}

.drawer__link.is-disabled {
  color: var(--ink-faint);
}

.drawer__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 20px;
}

@media (max-width: 860px) {
  /* 只收页头里那一份；抽屉里的语言切换要留着，所以选择器不能写成 .site-header .lang-switch */
  .site-nav,
  .site-header__actions .lang-switch,
  .site-header__actions .btn,
  .whoami {
    display: none;
  }

  .site-header__inner {
    gap: 12px;
  }

  .site-header__actions {
    margin-left: auto;
  }

  .menu-btn {
    display: inline-flex;
  }

  .drawer {
    display: block;
  }
}
</style>
