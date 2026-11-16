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
/* 与原型 .icon-btn 对齐：46px 圆按钮、主色描边、透明底 */
.theme-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  flex: none;
  padding: 0;
  border: 2px solid var(--red);
  border-radius: 999px;
  background: transparent;
  color: var(--accent-text);
  cursor: pointer;
}

.theme-btn:hover {
  background: var(--accent-soft);
}

.theme-btn svg {
  width: 20px;
  height: 20px;
}
</style>
