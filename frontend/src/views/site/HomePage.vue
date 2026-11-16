<script setup>
/**
 * 首页 · 扉页版式（M0b 原型 docs/design/prototype/01-home.html）
 *
 * 这一轮只换版式骨架，已落地的逻辑一律保留：软件列表走真接口、加载／失败／空态、
 * i18n 双语、RouterLink 路由。原型里的静态假数据不搬，主产品卡用接口第一条填充。
 * 插画的所有色值已收敛到 styles/tokens.css 的 --obj-art-* 实物令牌。
 */
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { fetchSoftwareList } from '@/api/site'

const { t, te, locale } = useI18n()

const loading = ref(true)
const failed = ref(false)
const softwareList = ref([])

const primary = computed(() => softwareList.value[0] || null)
// 品牌名按设计稿固定显示英文，不随界面语言翻译；其余内容仍取真接口
const primaryName = computed(
  () => primary.value?.nameEn || primary.value?.nameZh || 'HundunDesktop',
)
const primaryPlatforms = computed(() => primary.value?.platforms || [])
const primaryLink = computed(() =>
  primary.value ? `/software/${primary.value.slug}` : '/software',
)

function platformLabel(value) {
  const key = `platform.${value}`
  return te(key) ? t(key) : value
}

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

onMounted(load)
</script>

<template>
  <div class="home container" :class="{ 'home--en': locale === 'en' }">
    <!-- 扉页：左排版型大标题，右四块工具瓦片与积木 -->
    <section class="hero">
      <div class="hero__text">
        <h1 class="hero__title">
          {{ t('home.heroLine1') }}<br />{{ t('home.heroLine2') }}<span class="hero__dot"></span>
        </h1>
        <p class="hero__lead">
          {{ t('home.heroLead1') }}
          <b>{{ t('home.heroLead2') }}</b>
        </p>
        <div class="hero__actions">
          <RouterLink to="/software" class="btn btn--primary">{{
            t('home.heroPrimary')
          }}</RouterLink>
          <a href="#shelf" class="hero__link">{{ t('home.heroSecondary') }}</a>
        </div>
      </div>

      <figure class="hero__art">
        <svg viewBox="0 0 662 408" role="img" :aria-label="t('home.artLabel')">
          <g transform="translate(30 4)">
            <defs>
              <filter id="tileShadow" x="-30%" y="-30%" width="180%" height="180%">
                <feDropShadow
                  dx="9"
                  dy="13"
                  stdDeviation="9"
                  flood-color="var(--ink)"
                  flood-opacity="0.3"
                />
              </filter>
            </defs>
            <g filter="url(#tileShadow)">
              <g transform="translate(16 296)">
                <path
                  d="M0 26 L28 12 L56 26 L28 40 Z"
                  fill="var(--obj-art-yellow)"
                  stroke="var(--ink)"
                  stroke-width="1.6"
                />
                <path
                  d="M0 26 L28 40 L28 68 L0 54 Z"
                  fill="var(--obj-art-yellow-2)"
                  stroke="var(--ink)"
                  stroke-width="1.6"
                />
                <path
                  d="M56 26 L28 40 L28 68 L56 54 Z"
                  fill="var(--obj-art-yellow-3)"
                  stroke="var(--ink)"
                  stroke-width="1.6"
                />
              </g>
              <g transform="translate(10 342)">
                <path
                  d="M0 26 L28 12 L56 26 L28 40 Z"
                  fill="var(--obj-art-green)"
                  stroke="var(--ink)"
                  stroke-width="1.6"
                />
                <path
                  d="M0 26 L28 40 L28 68 L0 54 Z"
                  fill="var(--obj-art-green-2)"
                  stroke="var(--ink)"
                  stroke-width="1.6"
                />
                <path
                  d="M56 26 L28 40 L28 68 L56 54 Z"
                  fill="var(--teal)"
                  stroke="var(--ink)"
                  stroke-width="1.6"
                />
              </g>
            </g>
            <g filter="url(#tileShadow)">
              <rect
                x="126"
                y="34"
                width="150"
                height="150"
                fill="var(--paper)"
                stroke="var(--ink)"
                stroke-width="1.6"
              />
              <path
                d="M126 184 L140 196 L290 196 L276 184 Z"
                fill="var(--obj-art-tan)"
                stroke="var(--ink)"
                stroke-width="1.6"
                stroke-linejoin="round"
              />
              <path
                d="M276 34 L290 46 L290 196 L276 184 Z"
                fill="var(--obj-art-tan-2)"
                stroke="var(--ink)"
                stroke-width="1.6"
                stroke-linejoin="round"
              />
              <g fill="none" stroke="var(--obj-art-red)" stroke-width="8">
                <path d="M172 88 H232 V148 H172 Z" />
              </g>
              <rect x="204" y="120" width="28" height="28" fill="var(--obj-art-red)" />
              <rect x="172" y="88" width="24" height="24" fill="var(--obj-art-red)" opacity="0" />
            </g>
            <g filter="url(#tileShadow)">
              <rect
                x="330"
                y="16"
                width="164"
                height="164"
                fill="var(--obj-art-red)"
                stroke="var(--ink)"
                stroke-width="1.6"
              />
              <path
                d="M330 180 L344 194 L494 194 L494 180 Z"
                fill="var(--obj-art-red-deep)"
                stroke="var(--ink)"
                stroke-width="1.6"
                stroke-linejoin="round"
              />
              <path
                d="M494 16 L508 30 L508 194 L494 180 Z"
                fill="var(--obj-art-red-mid)"
                stroke="var(--ink)"
                stroke-width="1.6"
                stroke-linejoin="round"
              />
              <circle cx="412" cy="98" r="42" fill="none" stroke="var(--paper)" stroke-width="7" />
              <path
                d="M390 100 a22 22 0 0 1 44 0"
                fill="none"
                stroke="var(--paper)"
                stroke-width="7"
                stroke-linecap="round"
              />
              <circle cx="412" cy="100" r="8" fill="var(--paper)" />
            </g>
            <g filter="url(#tileShadow)">
              <rect
                x="112"
                y="222"
                width="150"
                height="150"
                fill="var(--paper)"
                stroke="var(--ink)"
                stroke-width="1.6"
              />
              <path
                d="M112 372 L126 384 L276 384 L262 372 Z"
                fill="var(--obj-art-tan)"
                stroke="var(--ink)"
                stroke-width="1.6"
                stroke-linejoin="round"
              />
              <path
                d="M262 222 L276 234 L276 384 L262 372 Z"
                fill="var(--obj-art-tan-2)"
                stroke="var(--ink)"
                stroke-width="1.6"
                stroke-linejoin="round"
              />
              <rect x="154" y="264" width="66" height="66" fill="var(--obj-art-red)" />
              <g stroke="var(--paper)" stroke-width="9" stroke-linecap="square">
                <path d="M170 280 L204 314" />
                <path d="M204 280 L170 314" />
              </g>
            </g>
            <g filter="url(#tileShadow)">
              <rect
                x="326"
                y="204"
                width="168"
                height="168"
                fill="var(--obj-art-red)"
                stroke="var(--ink)"
                stroke-width="1.6"
              />
              <path
                d="M326 372 L340 386 L494 386 L494 372 Z"
                fill="var(--obj-art-red-deep)"
                stroke="var(--ink)"
                stroke-width="1.6"
                stroke-linejoin="round"
              />
              <path
                d="M494 204 L508 218 L508 386 L494 372 Z"
                fill="var(--obj-art-red-mid)"
                stroke="var(--ink)"
                stroke-width="1.6"
                stroke-linejoin="round"
              />
              <g fill="none" stroke="var(--paper)" stroke-width="6">
                <rect x="364" y="242" width="92" height="92" />
                <path d="M364 273h92M364 304h92M395 242v92M426 242v92" />
                <path d="M364 334 L456 242" stroke-width="7" />
              </g>
            </g>
            <g filter="url(#tileShadow)">
              <g transform="translate(556 252)">
                <path
                  d="M0 22 L24 10 L48 22 L24 34 Z"
                  fill="var(--obj-art-paper)"
                  stroke="var(--ink)"
                  stroke-width="1.5"
                />
                <path
                  d="M0 22 L24 34 L24 56 L0 44 Z"
                  fill="var(--obj-art-paper-2)"
                  stroke="var(--ink)"
                  stroke-width="1.5"
                />
                <path
                  d="M48 22 L24 34 L24 56 L48 44 Z"
                  fill="var(--obj-art-paper-3)"
                  stroke="var(--ink)"
                  stroke-width="1.5"
                />
              </g>
              <g transform="translate(550 292)">
                <path
                  d="M0 22 L24 10 L48 22 L24 34 Z"
                  fill="var(--obj-art-paper)"
                  stroke="var(--ink)"
                  stroke-width="1.5"
                />
                <path
                  d="M0 22 L24 34 L24 56 L0 44 Z"
                  fill="var(--obj-art-paper-2)"
                  stroke="var(--ink)"
                  stroke-width="1.5"
                />
                <path
                  d="M48 22 L24 34 L24 56 L48 44 Z"
                  fill="var(--obj-art-paper-3)"
                  stroke="var(--ink)"
                  stroke-width="1.5"
                />
              </g>
              <g transform="translate(546 332)">
                <path
                  d="M0 22 L24 10 L48 22 L24 34 Z"
                  fill="var(--obj-art-yellow)"
                  stroke="var(--ink)"
                  stroke-width="1.5"
                />
                <path
                  d="M0 22 L24 34 L24 56 L0 44 Z"
                  fill="var(--obj-art-yellow-2)"
                  stroke="var(--ink)"
                  stroke-width="1.5"
                />
                <path
                  d="M48 22 L24 34 L24 56 L48 44 Z"
                  fill="var(--obj-art-yellow-3)"
                  stroke="var(--ink)"
                  stroke-width="1.5"
                />
              </g>
            </g>
          </g>
        </svg>
        <figcaption class="v3-mono">{{ t('home.artCaption') }}</figcaption>
      </figure>
    </section>

    <hr class="hero-rule" />

    <!-- 软件与实验：一张主产品卡 + 两张实验状态卡 -->
    <section class="shelf" id="shelf">
      <div class="shelf__head">
        <h2>{{ t('home.shelfTitle') }}</h2>
        <span class="v3-mono">{{ t('home.shelfIndex') }}</span>
      </div>

      <div class="shelf__grid">
        <article class="product">
          <div class="product__tape"></div>
          <div class="product__fold"></div>
          <span class="v3-mono product__label">{{ t('home.productLabel') }}</span>
          <h3>{{ primaryName }}</h3>
          <p>{{ t('home.productDesc') }}</p>
          <div class="product__tags">
            <span v-for="p in primaryPlatforms" :key="p">{{ platformLabel(p) }}</span>
          </div>

          <p v-if="loading" class="product__status mono">{{ t('common.loading') }}</p>
          <div v-else-if="failed" class="product__status">
            <span class="muted">{{ t('common.error') }}</span>
            <button type="button" class="btn btn--sm" @click="load">{{ t('common.retry') }}</button>
          </div>
          <p v-else-if="!primary" class="product__status muted">{{ t('home.noSoftware') }}</p>
          <RouterLink v-else :to="primaryLink" class="btn btn--primary">{{
            t('home.productCta')
          }}</RouterLink>

          <div class="product__art" aria-hidden="true">
            <svg viewBox="0 0 360 440">
              <rect x="26" y="24" width="308" height="392" rx="26" fill="var(--obj-art-wood)" />
              <rect
                x="26"
                y="24"
                width="308"
                height="392"
                rx="26"
                fill="none"
                stroke="var(--obj-art-ink)"
                stroke-width="5"
              />
              <path d="M26 96 h308" stroke="var(--obj-art-wood-line)" stroke-width="3" />
              <rect x="44" y="48" width="272" height="40" rx="6" fill="var(--obj-art-red)" />
              <rect x="44" y="104" width="272" height="196" rx="8" fill="var(--obj-art-shell)" />
              <rect x="60" y="120" width="240" height="164" rx="4" fill="var(--obj-art-screen)" />
              <defs>
                <pattern id="p3-olive" width="20" height="20" patternUnits="userSpaceOnUse">
                  <rect width="20" height="20" fill="var(--obj-art-screen-a)" />
                  <rect width="10" height="10" fill="var(--obj-art-screen-b)" />
                  <rect x="10" y="10" width="10" height="10" fill="var(--obj-art-screen-b)" />
                </pattern>
                <pattern id="p3-red" width="20" height="20" patternUnits="userSpaceOnUse">
                  <rect width="20" height="20" fill="var(--obj-art-red)" />
                  <rect width="10" height="10" fill="var(--obj-art-red-light)" />
                  <rect x="10" y="10" width="10" height="10" fill="var(--obj-art-red-light)" />
                </pattern>
              </defs>
              <rect
                x="76"
                y="136"
                width="100"
                height="132"
                fill="url(#p3-olive)"
                stroke="var(--obj-art-ink)"
                stroke-width="3"
              />
              <rect
                x="188"
                y="136"
                width="100"
                height="132"
                fill="url(#p3-red)"
                stroke="var(--obj-art-ink)"
                stroke-width="3"
              />
              <g fill="var(--obj-art-shell)">
                <rect x="66" y="318" width="34" height="88" rx="7" />
                <rect x="40" y="344" width="86" height="34" rx="7" />
              </g>
              <circle
                cx="236"
                cy="338"
                r="23"
                fill="var(--obj-art-red)"
                stroke="var(--obj-art-ink)"
                stroke-width="4"
              />
              <circle
                cx="288"
                cy="372"
                r="23"
                fill="var(--obj-art-amber)"
                stroke="var(--obj-art-ink)"
                stroke-width="4"
              />
              <g fill="var(--obj-art-detail)">
                <rect x="206" y="398" width="38" height="7" rx="3.5" />
                <rect x="256" y="398" width="38" height="7" rx="3.5" />
              </g>
            </svg>
          </div>
        </article>

        <div class="stack">
          <article class="mini mini--green">
            <div class="mini__bar"></div>
            <span class="mini__icon" aria-hidden="true">
              <svg
                width="26"
                height="26"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.2"
              >
                <rect x="3.6" y="3.6" width="16.8" height="16.8" />
                <path d="M3.6 12h16.8M12 3.6v16.8" />
              </svg>
            </span>
            <span class="mini__state">{{ t('home.miniGreenState') }}</span>
            <h4>{{ t('home.miniGreenTitle') }}</h4>
            <p>{{ t('home.miniGreenDesc') }}</p>
          </article>

          <article class="mini mini--amber">
            <div class="mini__bar"></div>
            <span class="mini__icon" aria-hidden="true">
              <svg
                width="26"
                height="26"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.2"
              >
                <rect x="3.6" y="3.6" width="16.8" height="16.8" />
                <path d="M5.4 5.4 L18.6 18.6M18.6 5.4 L5.4 18.6" />
              </svg>
            </span>
            <span class="mini__state">{{ t('home.miniAmberState') }}</span>
            <h4>{{ t('home.miniAmberTitle') }}</h4>
            <p>{{ t('home.miniAmberDesc') }}</p>
          </article>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* 一屏应用外壳：首页吃掉头部与页脚之间的全部高度，多出来的给 hero */
.home {
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.hero {
  flex: 1 1 auto;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.1fr);
  align-items: center;
  gap: 16px;
  padding: 24px 0 12px;
}

.hero__title {
  margin: 0;
  font-size: 84px;
  line-height: 1.03;
  font-weight: 900;
  letter-spacing: 0.01em;
  color: var(--ink);
}

/* 英文比中文长：降一档字号，并把文字列放宽一点，保持扉页的两行节奏 */
.home--en .hero {
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 0.9fr);
}

.home--en .hero__title {
  font-size: 72px;
}

.hero__dot {
  display: inline-block;
  width: 14px;
  height: 21px;
  margin-left: 9px;
  vertical-align: 3px;
  background: var(--red);
}

.hero__lead {
  margin: 24px 0 0;
  font-size: 19px;
  line-height: 2.05;
  color: var(--ink-soft);
}

.hero__lead b {
  display: block;
  font-weight: 400;
}

.hero__actions {
  display: flex;
  align-items: center;
  gap: 34px;
  margin-top: 24px;
}

.hero__link {
  font-size: 18px;
  color: var(--ink);
  text-decoration: none;
}

.hero__link:hover {
  color: var(--accent-text);
}

/* 原型 01 的按钮：52px 高、3px 圆角、主色描边，不做全站像素硬阴影 */
.hero__actions .btn,
.product .btn {
  height: 52px;
  padding: 0 26px;
  border-color: var(--red);
  border-radius: 3px;
  font-family: var(--font-ui);
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.02em;
  text-transform: none;
  box-shadow: none;
}

.hero__actions .btn--primary,
.product .btn--primary {
  box-shadow: 3px 3px 0 var(--ink-a28);
}

.hero__actions .btn:hover,
.product .btn:hover {
  transform: none;
  box-shadow: 3px 3px 0 var(--ink-a28);
}

.hero__art {
  margin: 0;
  padding-top: 6px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.hero__art svg {
  display: block;
  width: auto;
  height: 372px;
  max-width: 100%;
}

.hero__art figcaption {
  align-self: stretch;
  margin-top: 8px;
  text-align: center;
  font-size: 16px;
  letter-spacing: 0.16em;
  color: var(--ink-soft);
}

.v3-mono {
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

/* 通栏分隔线：从版心里溢到窗口两边 */
.hero-rule {
  border: 0;
  border-top: 1px solid var(--ink-a28);
  margin: 0;
  margin-inline: calc(50% - 50vw);
}

/* ---------- 软件与实验 ---------- */

.shelf__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding-top: 26px;
}

.shelf__head h2 {
  margin: 0;
  font-size: 44px;
  line-height: 1.1;
  font-weight: 900;
  letter-spacing: 0.02em;
  color: var(--ink);
}

.shelf__head .v3-mono {
  color: var(--accent-text);
  font-size: 13px;
  letter-spacing: 0.16em;
  padding-bottom: 8px;
}

.shelf__grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 0.4fr) minmax(0, 0.4fr);
  gap: 44px;
  margin-top: 22px;
  margin-bottom: 4px;
  align-items: stretch;
}

.product {
  position: relative;
  background: var(--card);
  border: 1px solid var(--ink-a22);
  border-radius: 3px;
  box-shadow: 14px 16px 24px var(--ink-a14);
  min-height: 320px;
  padding: 30px 34px 28px;
}

.product__tape {
  position: absolute;
  top: -1px;
  left: -1px;
  width: 44%;
  height: 11px;
  background: var(--red);
  clip-path: polygon(0 0, 100% 0, 93% 100%, 0 100%);
}

.product__fold {
  position: absolute;
  top: -1px;
  right: -1px;
  width: 46px;
  height: 46px;
  background: linear-gradient(
    225deg,
    var(--paper) 0 49.4%,
    var(--ink-a22) 50%,
    var(--card) 51% 100%
  );
  clip-path: polygon(100% 0, 100% 100%, 0 0);
  border-top-right-radius: 3px;
  filter: drop-shadow(-2px 2px 2px var(--ink-a14));
}

.product__label {
  display: block;
  margin-top: 6px;
  color: var(--accent-text);
  font-size: 13px;
  letter-spacing: 0.1em;
}

.product h3 {
  margin: 8px 0 0;
  font-size: 44px;
  line-height: 1.05;
  font-weight: 900;
  letter-spacing: 0.005em;
  color: var(--ink);
}

.product p {
  margin: 12px 0 0;
  font-size: 16px;
  color: var(--ink-soft);
  max-width: 16em;
}

.product__tags {
  display: flex;
  gap: 8px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.product__tags span {
  padding: 4px 12px;
  border: 1px solid var(--ink-a30);
  border-radius: 2px;
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.04em;
  color: var(--ink-soft);
  background: var(--sheen-strong);
}

.product .btn {
  margin-top: 18px;
  padding: 13px 24px;
  font-size: 16px;
}

.product__status {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 18px;
  min-height: 52px;
  font-size: 14px;
}

.product__status .btn {
  margin-top: 0;
  height: 38px;
  font-size: 14px;
}

.product__art {
  position: absolute;
  right: -22px;
  bottom: -10px;
  width: 200px;
  transform: rotate(-2deg);
  pointer-events: none;
  filter: drop-shadow(10px 14px 18px var(--ink-a28));
}

.product__art svg {
  display: block;
  width: 100%;
  height: auto;
}

.stack {
  display: contents;
}

.mini {
  display: flex;
  flex-direction: column;
  position: relative;
  background: var(--card);
  border: 1px solid var(--ink-a22);
  border-radius: 3px;
  box-shadow: 10px 12px 20px var(--ink-a12);
  padding: 24px 24px 22px;
}

.mini__bar {
  position: absolute;
  top: -1px;
  left: -1px;
  right: -1px;
  height: 7px;
  border-radius: 3px 3px 0 0;
}

.mini--green .mini__bar {
  background:
    repeating-linear-gradient(90deg, var(--sheen) 0 5px, transparent 5px 12px), var(--teal);
}

.mini--amber .mini__bar {
  background:
    repeating-linear-gradient(90deg, var(--sheen) 0 5px, transparent 5px 12px), var(--amber);
}

.mini__icon {
  width: 52px;
  height: 52px;
  display: grid;
  place-items: center;
  border-radius: 3px;
  margin-top: 4px;
}

.mini--green .mini__icon {
  background: var(--mini-green-bg);
  color: var(--mini-green-ink);
  box-shadow: 0 0 0 2px var(--mini-green-line);
}

.mini--amber .mini__icon {
  background: var(--mini-amber-bg);
  color: var(--mini-amber-ink);
  box-shadow: 0 0 0 2px var(--mini-amber-line);
}

.mini__state {
  display: block;
  margin-top: 26px;
  font-size: 13.5px;
  font-weight: 700;
  color: var(--accent-text);
  letter-spacing: 0.04em;
}

.mini h4 {
  margin: 10px 0 0;
  font-size: 21px;
  font-weight: 900;
  letter-spacing: 0.02em;
  color: var(--ink);
}

.mini p {
  margin: 12px 0 0;
  font-size: 14px;
  line-height: 1.75;
  color: var(--ink-soft);
}

/* 插画是画出来的实物：深色下不漂绿，只整体压一档亮度（同原型 D087 处理） */
[data-theme='dark'] .hero__art svg,
[data-theme='dark'] .product__art svg {
  filter: brightness(0.82);
}

@media (max-width: 1120px) {
  .hero,
  .home--en .hero {
    grid-template-columns: minmax(0, 1fr);
  }

  .hero {
    padding-bottom: 30px;
  }

  .hero__title {
    font-size: 62px;
  }

  .home--en .hero__title {
    font-size: 54px;
  }

  .shelf__grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .product__art {
    position: static;
    width: 180px;
    margin: 22px auto 0;
  }

  .stack {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 20px;
  }
}
</style>
