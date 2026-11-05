<script setup>
/**
 * 主题开关（浅 / 深，D081）。
 *
 * 只做一件事：把 useTheme 的开关接成一个可点的圆形按钮。
 * 图标照原型：一个圆 + 半个实心圆，比太阳/月亮更中性，两套主题下都读得懂。
 */
import { useI18n } from 'vue-i18n'
import { useTheme } from '@/composables/useTheme'

const { t } = useI18n()
const { theme, toggleTheme } = useTheme()
</script>

<template>
  <button
    type="button"
    class="theme-btn"
    :title="t('nav.theme')"
    :aria-label="t('nav.theme')"
    :aria-pressed="theme === 'dark'"
    @click="toggleTheme"
  >
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
      <circle cx="12" cy="12" r="8" />
      <path d="M12 4a8 8 0 0 0 0 16z" fill="currentColor" stroke="none" />
    </svg>
  </button>
</template>

<style scoped>
.theme-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex: none;
  padding: 0;
  border: 2px solid var(--line);
  border-radius: 999px;
  background: var(--card);
  color: var(--accent-text);
  cursor: pointer;
}

.theme-btn:hover {
  background: var(--accent-soft);
}

.theme-btn svg {
  width: 17px;
  height: 17px;
}
</style>
