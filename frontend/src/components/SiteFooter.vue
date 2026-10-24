<script setup>
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const year = new Date().getFullYear()
const repoUrl = 'https://github.com/luofengwuqi/Hundun-Lab'
</script>

<template>
  <footer class="site-footer">
    <div class="container site-footer__inner">
      <div class="site-footer__brand">
        <span class="mono">HUNDUN LAB</span>
        <span class="muted">{{ t('common.footerNote') }}</span>
      </div>
      <div class="site-footer__meta mono">
        <span>© {{ year }}</span>
        <a :href="repoUrl" target="_blank" rel="noopener noreferrer">{{ t('common.footerLicense') }}</a>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.site-footer {
  border-top: 2px solid var(--ink);
  background: var(--paper-2);
  padding: 28px 0;
  margin-top: 40px;
}

.site-footer__inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.site-footer__brand {
  display: flex;
  align-items: baseline;
  gap: 12px;
  font-size: 13px;
}

.site-footer__brand .mono {
  font-weight: 700;
  letter-spacing: 0.12em;
}

.site-footer__meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--ink-faint);
}

.site-footer__meta a:hover {
  color: var(--red);
}
</style>
