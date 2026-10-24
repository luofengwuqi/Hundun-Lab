<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { setLocale } from '@/i18n'
import { useSessionStore } from '@/stores/session'
import { logout } from '@/api/site'

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()
const session = useSessionStore()

const navItems = computed(() => [
  { to: '/', label: t('nav.home') },
  { to: '/software', label: t('nav.software') },
  { to: '/plugins', label: t('nav.plugins') },
])

function isActive(item) {
  if (item.to === '/') return route.path === '/'
  return route.path.startsWith(item.to)
}

function switchLocale(next) {
  setLocale(next)
}

async function handleLogout() {
  try {
    await logout()
  } catch {
    // 服务端令牌可能已经失效，本地照样要清干净
  }
  session.clear()
  router.push('/')
}
</script>

<template>
  <header class="site-header">
    <div class="container site-header__inner">
      <RouterLink to="/" class="brand" :aria-label="t('nav.home')">
        <span class="brand__mark" aria-hidden="true">
          <span></span><span></span><span></span><span></span>
        </span>
        <span class="brand__text">HUNDUN<em>LAB</em></span>
      </RouterLink>

      <nav class="site-nav" :aria-label="t('nav.home')">
        <template v-for="item in navItems" :key="item.to">
          <span v-if="item.disabled" class="site-nav__link is-disabled" :title="t('common.empty')">
            {{ item.label }}
          </span>
          <RouterLink v-else :to="item.to" class="site-nav__link" :class="{ 'is-active': isActive(item) }">
            {{ item.label }}
          </RouterLink>
        </template>
      </nav>

      <div class="site-header__actions">
        <div class="lang-switch" role="group" :aria-label="t('nav.language')">
          <button
            type="button"
            class="lang-switch__btn"
            :class="{ 'is-on': locale === 'zh' }"
            @click="switchLocale('zh')"
          >
            中
          </button>
          <button
            type="button"
            class="lang-switch__btn"
            :class="{ 'is-on': locale === 'en' }"
            @click="switchLocale('en')"
          >
            EN
          </button>
        </div>

        <template v-if="session.isLoggedIn">
          <span class="whoami mono">{{ session.displayName }}</span>
          <button type="button" class="btn btn--ghost btn--sm" @click="handleLogout">
            {{ t('nav.logout') }}
          </button>
        </template>
        <RouterLink v-else to="/login" class="btn btn--primary btn--sm">{{ t('nav.login') }}</RouterLink>
      </div>
    </div>
  </header>
</template>

<style scoped>
.site-header {
  position: fixed;
  inset: 0 0 auto 0;
  height: var(--header-h);
  z-index: 50;
  background: var(--paper);
  border-bottom: 2px solid var(--ink);
}

.site-header__inner {
  height: 100%;
  display: flex;
  align-items: center;
  gap: 24px;
}

/* 四个方块拼成的像素标记，纯 CSS，不用图片 */
.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex: none;
}

.brand__mark {
  display: grid;
  grid-template-columns: repeat(2, 7px);
  grid-template-rows: repeat(2, 7px);
  gap: 2px;
}

.brand__mark span {
  background: var(--ink);
}

.brand__mark span:nth-child(1) {
  background: var(--red);
}

.brand__text {
  font-family: var(--font-mono);
  font-weight: 700;
  font-size: 16px;
  letter-spacing: 0.14em;
}

.brand__text em {
  font-style: normal;
  color: var(--red);
}

.site-nav {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
}

.site-nav__link {
  padding: 6px 12px;
  font-size: 14px;
  font-weight: 600;
  border: 2px solid transparent;
  border-radius: var(--radius);
}

.site-nav__link:hover {
  border-color: var(--ink);
}

.site-nav__link.is-active {
  background: var(--ink);
  color: var(--paper);
}

.site-nav__link.is-disabled {
  color: var(--ink-faint);
  cursor: not-allowed;
}

.site-header__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: none;
}

.whoami {
  font-size: 12px;
  color: var(--ink-soft);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lang-switch {
  display: inline-flex;
  border: 2px solid var(--ink);
  border-radius: var(--radius);
  overflow: hidden;
}

.lang-switch__btn {
  padding: 3px 8px;
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
  background: var(--card);
}

.lang-switch__btn + .lang-switch__btn {
  border-left: 2px solid var(--ink);
}

.lang-switch__btn.is-on {
  background: var(--ink);
  color: var(--paper);
}

@media (max-width: 860px) {
  .site-nav {
    display: none;
  }
  .whoami {
    display: none;
  }
}
</style>
