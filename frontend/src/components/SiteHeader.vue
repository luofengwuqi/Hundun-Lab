<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { LOCALE_LABELS, setLocale } from '@/i18n'
import ThemeToggle from '@/components/ThemeToggle.vue'
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
        <span class="brand__mark" aria-hidden="true"
          ><svg class="brand-mark" viewBox="0 0 256 256" aria-hidden="true" focusable="false">
            <path
              fill="currentColor"
              fill-rule="evenodd"
              d="M122.8 63 C128.1 63 135.4 63.2 141.3 65.2 C147.3 67.2 152.9 73.6 158.5 75.2 C164.1 76.8 170.7 74.5 174.9 74.8 C179 75 180.8 75.7 183.5 76.7 C186.2 77.7 188.9 78.9 191.2 80.8 C193.5 82.6 195.4 84.9 197.4 88 C199.3 91.1 199.5 96.3 202.9 99.2 C206.3 102 214.4 102.9 217.9 105.1 C221.4 107.2 222.7 109.1 224.1 112.1 C225.6 115.1 225 119.1 226.4 123.3 C227.9 127.4 231.8 133 232.8 137 C233.9 141 233.4 144.5 232.8 147.4 C232.3 150.2 231.2 152.3 229.4 154.2 C227.7 156.2 226.6 157.7 222.2 159.3 C217.8 160.8 209.4 160.1 203.3 163.6 C197.1 167 190.8 175.9 185.4 180 C179.9 184.2 176.2 186.1 170.6 188.2 C165 190.3 160.4 191.6 151.7 192.5 C142.9 193.4 128.3 193.8 118.1 193.4 C107.9 192.9 97.9 191.3 90.6 189.6 C83.3 187.9 78.8 185.8 74.2 183.3 C69.6 180.7 67.2 178.9 63 174.4 C58.9 169.9 54.7 160.9 49.3 156.5 C43.8 152 34.2 149.9 30.4 147.7 C26.5 145.5 27.1 145 25.9 143.1 C24.7 141.1 23.6 139.2 23.2 136.2 C22.7 133.2 22.7 128 23.1 125 C23.4 122 23.4 120.9 25.1 118.1 C26.7 115.3 29.5 111.1 32.9 108.3 C36.4 105.5 42.7 104.4 45.8 101.3 C49 98.2 49.8 92.8 51.7 89.7 C53.7 86.6 54.3 85 57.5 82.8 C60.7 80.6 64.8 77.8 70.8 76.5 C76.7 75.2 86.7 76.8 93.2 74.9 C99.6 73.1 104.6 67.5 109.5 65.5 C114.4 63.5 117.5 63.1 122.8 63 Z M111.2 75.7 C107.4 77.6 103.9 82.2 98.3 83.8 C92.7 85.3 82.7 84.4 77.7 85.1 C72.7 85.8 70.8 86.9 68.2 88.2 C65.6 89.5 64.7 89.6 62.2 93 C59.7 96.4 56.9 104.8 53.1 108.6 C49.4 112.5 43 113.6 39.8 116 C36.6 118.5 35.3 120.6 34 123.3 C32.8 125.9 32.3 129.4 32.5 131.9 C32.7 134.3 31.7 135.4 35.2 137.9 C38.7 140.4 49.5 144.5 53.6 146.7 C57.6 149 56.8 147.9 59.6 151.3 C62.4 154.8 67.6 163.7 70.4 167.1 C73.3 170.6 73.3 170.3 76.6 172.3 C80 174.3 85.7 177.5 90.6 179.3 C95.5 181.1 99 182 106.1 182.9 C113.1 183.9 125.3 184.8 132.7 184.8 C140.2 184.8 143.9 184.6 150.8 182.9 C157.7 181.3 166.3 179.6 174 175 C181.8 170.4 190.2 159.4 197.3 155.3 C204.3 151.2 212 151.8 216.2 150.4 C220.3 148.9 220.9 148.3 222.2 146.7 C223.5 145 224.5 143.4 223.7 140.5 C223 137.6 219.3 133.3 217.8 129.3 C216.3 125.3 215.7 119.1 214.6 116.4 C213.4 113.7 214.1 114.7 211 113.2 C207.9 111.6 199.7 110.1 196 106.9 C192.3 103.7 190.9 97.1 188.8 94 C186.7 90.9 186.1 90.1 183.5 88.5 C180.9 86.9 178.2 85 173.2 84.2 C168.1 83.5 159.7 85.7 153.4 84 C147.1 82.3 140.7 75.7 135.3 73.8 C129.9 71.9 125.1 72.2 121.1 72.5 C117.1 72.8 115 73.9 111.2 75.7 Z M177.5 95.7 C179.3 94.2 179.7 95.3 180.3 95.7 C180.9 96.2 181.9 95.9 181.3 98.3 C180.8 100.8 177.5 107.9 176.9 110.4 C176.2 112.8 175.6 111 177.4 112.9 C179.3 114.9 184.5 120.9 187.8 122 C191.1 123.1 194.5 119.6 197.3 119.4 C200 119.2 203.3 119.9 204.3 120.7 C205.3 121.5 205.2 122.6 203.3 124.1 C201.5 125.7 195.1 127.3 193.3 130.2 C191.4 133 192.9 139.8 192.1 141.5 C191.3 143.2 190.5 142.7 188.7 140.5 C186.8 138.4 184.7 132.6 181.3 128.4 C177.8 124.3 170.2 118.5 167.9 115.5 C165.6 112.5 167.1 112.1 167.4 110.4 C167.6 108.6 167.8 107.6 169.5 105.2 C171.2 102.8 175.7 97.3 177.5 95.7 Z M129 95.7 C130.2 95.1 130.4 97.4 135.3 98.6 C140.2 99.8 153.7 101.6 158.5 102.9 C163.4 104.1 163.4 104.8 164.6 106.1 C165.9 107.3 166.2 109.5 165.9 110.4 C165.6 111.3 168.6 112.3 162.8 111.5 C157.1 110.8 137.4 107.5 131.6 106.1 C125.9 104.6 128.6 104.3 128.1 102.6 C127.7 100.9 127.8 96.4 129 95.7 Z M122 82.8 C122.9 82.1 123.8 82.4 124.4 82.8 C124.9 83.3 125.2 82.5 125.4 85.4 C125.6 88.3 125.8 96.9 125.5 100 C125.1 103.2 125.2 103 123.3 104.2 C121.3 105.4 118 106.9 113.8 107.3 C109.6 107.8 100.8 107.4 98.1 106.9 C95.3 106.4 97 105.2 97.2 104.3 C97.4 103.5 95.9 103.2 99.2 102 C102.5 100.9 113.6 99.9 116.9 97.5 C120.3 95 118.3 89.6 119.1 87.1 C120 84.7 121.1 83.5 122 82.8 Z M68.8 99.2 C69.7 98.6 71.6 98.4 73.4 99.7 C75.1 101 77.2 105.9 79.4 107.2 C81.5 108.5 84.9 107.2 86.3 107.6 C87.6 108 87.7 108.5 87.5 109.5 C87.3 110.5 88 111 84.9 113.8 C81.9 116.6 72 124.3 69.1 126.2 C66.1 128.1 67.5 125.8 67.1 125 C66.8 124.2 66.3 123.6 67 121.5 C67.8 119.5 71.6 116 71.8 112.9 C71.9 109.9 68.4 105.8 67.9 103.5 C67.4 101.2 67.9 99.8 68.8 99.2 Z M47.7 122.4 C49.2 121.9 52.9 121.7 55.3 122.1 C57.7 122.5 60.2 123.4 62.2 124.6 C64.2 125.8 66.5 127.6 67.3 129.3 C68.2 131 68.1 133.7 67.3 134.7 C66.6 135.7 66.3 136.6 63 135.3 C59.7 134 50.3 128.7 47.6 127 C44.8 125.3 46.3 125.8 46.4 125 C46.4 124.2 46.2 122.9 47.7 122.4 Z"
            /></svg
        ></span>
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
.site-header {
  position: fixed;
  inset: 0 0 auto 0;
  height: var(--header-h);
  z-index: 50;
  background: var(--paper);
  border-bottom: 2px solid var(--line);
}

.site-header__inner {
  height: 100%;
  display: flex;
  align-items: center;
  gap: 24px;
}

/* 品牌图形标：docs/design/logo/hundun-lab-mark-plain.svg，内联后随 currentColor 上色 */
.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex: none;
}

.brand__mark {
  display: block;
  width: 40px;
  height: 40px;
  flex: none;
  color: var(--red);
}

.brand__mark svg {
  display: block;
  width: 100%;
  height: 100%;
}

.brand__text {
  font-family: var(--font-mono);
  font-weight: 700;
  font-size: 16px;
  letter-spacing: 0.14em;
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
  padding: 6px 12px;
  font-size: 14px;
  font-weight: 600;
  border: 2px solid transparent;
  border-radius: var(--radius);
}

.site-nav__link:hover {
  border-color: var(--line);
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
  border: 2px solid var(--line);
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
  border-left: 2px solid var(--line);
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
