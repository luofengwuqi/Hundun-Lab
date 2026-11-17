<script setup>
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const repoUrl = 'https://github.com/luofengwuqi/Hundun-Lab'
</script>

<template>
  <!-- 全站页脚只有这一份（D043）：左侧品牌，右侧标语 + 三个红方块。 -->
  <footer class="foot">
    <div class="container foot__inner">
      <div class="foot__left">
        <b>HUNDUN LAB</b>
        <span>· {{ t('common.footerBrand') }}</span>
        <a :href="repoUrl" target="_blank" rel="noopener noreferrer" class="foot__source">{{
          t('common.footerSource')
        }}</a>
      </div>
      <div class="foot__right">
        <span class="foot__slogan">{{ t('common.slogan') }}</span>
        <span class="foot__squares" aria-hidden="true"><i></i><i></i><i></i></span>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.foot {
  margin-top: auto;
  border-top: 1px solid var(--ink-a28);
  padding: 13px 0 15px;
}

.foot__inner {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 24px;
}

.foot__left {
  display: flex;
  align-items: baseline;
  gap: 6px;
  font-size: 13px;
  letter-spacing: 0.06em;
  color: var(--ink-soft);
}

.foot__left b {
  color: var(--ink);
  letter-spacing: 0.12em;
}

.foot__source {
  margin-left: 10px;
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--ink-faint);
}

.foot__source:hover {
  color: var(--accent-text);
}

.foot__right {
  display: flex;
  align-items: baseline;
  gap: 14px;
}

.foot__slogan {
  font-size: 13px;
  color: var(--ink);
}

.foot__squares {
  display: inline-flex;
  gap: 4px;
  margin-left: 8px;
}

.foot__squares i {
  width: 6px;
  height: 6px;
  background: var(--red);
}

@media (max-width: 720px) {
  .foot__inner {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
