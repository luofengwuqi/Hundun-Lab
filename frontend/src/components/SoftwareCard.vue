<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { localized } from '@/i18n'

const props = defineProps({
  software: { type: Object, required: true },
})

const { t } = useI18n()

const name = computed(() => localized(props.software.nameZh, props.software.nameEn))
const tagline = computed(() => localized(props.software.taglineZh, props.software.taglineEn))
const accent = computed(() => props.software.themeColor || 'var(--red)')
const version = computed(() => props.software.latestVersion || '—')
const downloads = computed(() => (props.software.totalDownloads || 0).toLocaleString())
</script>

<template>
  <RouterLink :to="`/software/${software.slug}`" class="soft-card card card--interactive">
    <!-- 卡带顶端的色条：颜色来自数据库的 theme_color，新增软件不改代码就能换色 -->
    <div class="soft-card__stripe" :style="{ background: accent }" aria-hidden="true"></div>

    <div class="soft-card__body">
      <div class="soft-card__head">
        <h3 class="soft-card__name">{{ name }}</h3>
        <span class="soft-card__version mono">v{{ version }}</span>
      </div>

      <p class="soft-card__tagline">{{ tagline }}</p>

      <div class="soft-card__tags">
        <span v-for="p in software.platforms" :key="p" class="tag">{{ t(`platform.${p}`) }}</span>
      </div>
    </div>

    <div class="soft-card__foot">
      <span class="mono soft-card__stat">{{ downloads }} {{ t('home.downloads') }}</span>
      <span class="soft-card__more">{{ t('home.viewDetail') }} →</span>
    </div>
  </RouterLink>
</template>

<style scoped>
.soft-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  color: inherit;
}

.soft-card__stripe {
  height: 12px;
  border-bottom: 2px solid var(--ink);
  /* 斜纹让顶端像卡带的金属触点 */
  background-image: repeating-linear-gradient(
    45deg,
    rgba(255, 255, 255, 0.25) 0 6px,
    transparent 6px 12px
  );
}

.soft-card__body {
  padding: 18px 18px 0;
  flex: 1;
}

.soft-card__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.soft-card__name {
  font-size: 20px;
}

.soft-card__version {
  font-size: 12px;
  color: var(--ink-faint);
}

.soft-card__tagline {
  margin-top: 8px;
  font-size: 14px;
  color: var(--ink-soft);
  min-height: 44px;
}

.soft-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 14px;
}

.soft-card__foot {
  margin-top: 16px;
  padding: 10px 18px;
  border-top: 2px solid var(--ink);
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  background: var(--paper-2);
}

.soft-card__stat {
  color: var(--ink-soft);
}

.soft-card__more {
  font-weight: 700;
  color: var(--red);
}
</style>
