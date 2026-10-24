<script setup>
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { fetchSoftwareList } from '@/api/site'
import { localized } from '@/i18n'
import SoftwareCard from '@/components/SoftwareCard.vue'

const { t } = useI18n()

const loading = ref(true)
const failed = ref(false)
const softwareList = ref([])

const totalDownloads = computed(() =>
  softwareList.value.reduce((sum, item) => sum + (item.totalDownloads || 0), 0),
)

const platformCount = computed(() => {
  const set = new Set()
  softwareList.value.forEach((item) => (item.platforms || []).forEach((p) => set.add(p)))
  return set.size
})

const latestItems = computed(() =>
  softwareList.value
    .filter((item) => item.latestVersion)
    .sort((a, b) => String(b.latestPublishedAt || '').localeCompare(String(a.latestPublishedAt || ''))),
)

async function load() {
  loading.value = true
  failed.value = false
  try {
    softwareList.value = await fetchSoftwareList()
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
}

function formatDate(value) {
  if (!value) return '—'
  return String(value).slice(0, 10)
}

onMounted(load)
</script>

<template>
  <div class="home">
      <!-- 首屏：左文右「掌机」，掌机是纯 CSS 拼的，不依赖图片资源 -->
      <section class="hero scanlines">
        <div class="container hero__inner">
          <div class="hero__copy">
            <p class="kicker">{{ t('home.heroKicker') }}</p>
            <h1 class="hero__title">{{ t('home.heroTitle') }}</h1>
            <p class="hero__lead">{{ t('home.heroLead') }}</p>

            <div class="hero__actions">
              <RouterLink to="/software" class="btn btn--primary">{{ t('home.heroPrimary') }}</RouterLink>
              <RouterLink to="/plugins" class="btn btn--ghost">{{ t('home.heroSecondary') }}</RouterLink>
            </div>

            <dl class="hero__stats">
              <div>
                <dt class="kicker">{{ t('home.statSoftware') }}</dt>
                <dd class="mono">{{ softwareList.length }}</dd>
              </div>
              <div>
                <dt class="kicker">{{ t('home.statDownloads') }}</dt>
                <dd class="mono">{{ totalDownloads.toLocaleString() }}</dd>
              </div>
              <div>
                <dt class="kicker">{{ t('home.statPlatforms') }}</dt>
                <dd class="mono">{{ platformCount }}</dd>
              </div>
            </dl>
          </div>

          <div class="hero__visual" aria-hidden="true">
            <div class="console">
              <div class="console__screen">
                <div class="console__slots">
                  <span></span><span></span><span></span><span></span>
                </div>
                <p class="console__label mono">HUNDUN / OS</p>
                <div class="console__pixels">
                  <i v-for="n in 48" :key="n"></i>
                </div>
              </div>
              <div class="console__controls">
                <div class="console__dpad">
                  <span class="console__dpad-h"></span>
                  <span class="console__dpad-v"></span>
                </div>
                <div class="console__buttons">
                  <span class="console__btn console__btn--a"></span>
                  <span class="console__btn console__btn--b"></span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 软件列表 -->
      <section class="section">
        <div class="container">
          <header class="section-head">
            <h2 class="section-title">{{ t('home.softwareTitle') }}</h2>
            <p class="muted">{{ t('home.softwareLead') }}</p>
          </header>

          <p v-if="loading" class="muted mono">{{ t('common.loading') }}</p>
          <div v-else-if="failed" class="stack-8">
            <p class="muted">{{ t('common.error') }}</p>
            <button type="button" class="btn btn--sm" @click="load">{{ t('common.retry') }}</button>
          </div>
          <p v-else-if="!softwareList.length" class="muted">{{ t('home.noSoftware') }}</p>

          <div v-else class="soft-grid">
            <SoftwareCard v-for="item in softwareList" :key="item.slug" :software="item" />
          </div>
        </div>
      </section>

      <!-- 最新动态：版本列表本身就是内容，不额外维护一份公告表 -->
      <section class="section section--tight">
        <div class="container">
          <header class="section-head">
            <h2 class="section-title">{{ t('home.latestTitle') }}</h2>
            <p class="muted">{{ t('home.latestLead') }}</p>
          </header>

          <p v-if="!loading && !latestItems.length" class="muted">{{ t('home.latestEmpty') }}</p>

          <ul v-else class="news">
            <li v-for="item in latestItems" :key="item.slug" class="news__item">
              <span class="news__date mono">{{ formatDate(item.latestPublishedAt) }}</span>
              <span class="news__name">{{ localized(item.nameZh, item.nameEn) }}</span>
              <span class="tag tag--red mono">{{ item.latestVersion }}</span>
              <RouterLink :to="`/software/${item.slug}`" class="news__link">
                {{ t('home.viewDetail') }} →
              </RouterLink>
            </li>
          </ul>
        </div>
      </section>
  </div>
</template>

<style scoped>
.hero {
  position: relative;
  border-bottom: 2px solid var(--ink);
  background: linear-gradient(180deg, #f6efe2 0%, var(--paper) 100%);
  overflow: hidden;
}

.hero__inner {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 0.85fr);
  gap: 48px;
  align-items: center;
  padding: 76px 0 84px;
}

.hero__title {
  font-size: clamp(34px, 5.4vw, 60px);
  margin-top: 10px;
  letter-spacing: -0.02em;
}

.hero__lead {
  margin-top: 18px;
  /* ch 单位在中文里偏宽，必须同时受容器约束，否则窄屏会把页面撑出横向滚动 */
  max-width: min(44ch, 100%);
  font-size: 17px;
  color: var(--ink-soft);
}

.hero__actions {
  display: flex;
  gap: 12px;
  margin-top: 28px;
  flex-wrap: wrap;
}

.hero__stats {
  display: flex;
  gap: 40px;
  margin-top: 40px;
  padding-top: 20px;
  border-top: 2px dashed rgba(23, 19, 15, 0.25);
}

.hero__stats dd {
  font-size: 26px;
  font-weight: 700;
  margin-top: 2px;
}

/* ---------- 纯 CSS 掌机 ---------- */

.hero__visual {
  display: flex;
  justify-content: center;
}

.console {
  width: min(100%, 320px);
  background: var(--ink);
  border-radius: 14px 14px 26px 26px;
  padding: 18px 18px 22px;
  box-shadow: 10px 10px 0 rgba(23, 19, 15, 0.18);
  transform: rotate(-2deg);
}

.console__screen {
  position: relative;
  background: #cfd8b6;
  border: 3px solid #0b0908;
  border-radius: 6px;
  padding: 14px 12px 16px;
  overflow: hidden;
}

/* 屏幕上的扫描线 */
.console__screen::after {
  content: "";
  position: absolute;
  inset: 0;
  background-image: repeating-linear-gradient(
    to bottom,
    rgba(11, 9, 8, 0.16) 0 1px,
    transparent 1px 3px
  );
}

.console__slots {
  display: flex;
  gap: 5px;
}

.console__slots span {
  width: 18px;
  height: 8px;
  background: #0b0908;
  opacity: 0.55;
}

.console__label {
  margin-top: 12px;
  font-size: 9px;
  letter-spacing: 0.22em;
  color: #2c3421;
}

.console__pixels {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 4px;
}

.console__pixels i {
  height: 8px;
  background: #3d4a2c;
  opacity: 0.5;
}

.console__pixels i:nth-child(3n) {
  background: var(--red);
  opacity: 0.85;
}

.console__controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20px;
}

.console__dpad {
  position: relative;
  width: 54px;
  height: 54px;
}

.console__dpad-h,
.console__dpad-v {
  position: absolute;
  background: #3a332d;
  border-radius: 2px;
}

.console__dpad-h {
  top: 21px;
  left: 0;
  width: 54px;
  height: 12px;
}

.console__dpad-v {
  left: 21px;
  top: 0;
  width: 12px;
  height: 54px;
}

.console__buttons {
  display: flex;
  gap: 10px;
  transform: rotate(-18deg);
}

.console__btn {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--red);
}

.console__btn--b {
  background: var(--amber);
}

/* ---------- 区块 ---------- */

.section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 26px;
}

.section-head .muted {
  font-size: 14px;
}

.soft-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}

.news {
  border-top: 2px solid var(--ink);
}

.news__item {
  display: grid;
  grid-template-columns: 110px 1fr auto auto;
  align-items: center;
  gap: 14px;
  padding: 14px 4px;
  border-bottom: 1px solid rgba(23, 19, 15, 0.2);
  font-size: 14px;
}

.news__date {
  font-size: 12px;
  color: var(--ink-faint);
}

.news__name {
  font-weight: 700;
}

.news__link {
  font-weight: 700;
  color: var(--red);
  white-space: nowrap;
}

@media (max-width: 900px) {
  .hero__inner {
    /* 必须写 minmax(0, 1fr)：纯 1fr 的下限是 auto，长中文段落会把整列撑出屏幕 */
    grid-template-columns: minmax(0, 1fr);
    gap: 40px;
    padding: 52px 0 60px;
  }
  .hero__visual {
    order: -1;
    justify-content: flex-start;
  }
  .console {
    width: 240px;
  }
  .news__item {
    grid-template-columns: 1fr auto;
    gap: 6px 12px;
  }
  .news__date {
    grid-column: 1 / -1;
  }
}
</style>
