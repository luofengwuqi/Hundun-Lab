<script setup>
/**
 * 卡带卡片（物料 01）
 * ------------------------------------------------------------------
 * 立体感来自四层叠加：描边八边形 → 内缩纸面 → 实心红色侧面 → 落地柔影，
 * 具体样式见 @/assets/css/hundun-kit.css 里的 .hd-cart。
 * 这里只负责结构与语义，除 accent 外不写任何视觉样式，
 * 换皮请改 CSS 变量，不要在本组件里加 style。
 */
import { computed } from 'vue'

const props = defineProps({
  /** 软件 / 插件名 */
  name: { type: String, required: true },
  /** 版本号，传 "26.2.1024" 或 "v26.2.1024" 都可以 */
  version: { type: String, default: '' },
  /** 一句话简介 */
  tagline: { type: String, default: '' },
  /** 平台标签，如 ['windows', 'macos']；显示文案由调用方翻译好后传入 */
  platforms: { type: Array, default: () => [] },
  /** 下载量，数字会千分位格式化 */
  downloads: { type: [Number, String], default: null },
  /** 下载量后缀，如「次下载」；文案由调用方翻译后传入，组件不内置任何语言 */
  downloadsLabel: { type: String, default: '' },
  /** 详情链接：给了就走 RouterLink，不给就是静态卡片 */
  to: { type: [String, Object], default: null },
  /** 详情链接文案，同上由调用方翻译后传入 */
  detailLabel: { type: String, default: '' },
  /** 顶部斜纹主色，一般传软件自己的 theme_color */
  accent: { type: String, default: '' },
  /** 已下架：整卡转灰，去掉彩色侧面 */
  off: { type: Boolean, default: false },
  /** 名称与版本号上下堆叠（列表页默认）；false 则同行显示 */
  stack: { type: Boolean, default: true },
  /** 右上角状态角标，如「已下架」「待审核」 */
  badge: { type: String, default: '' },
})

const tag = computed(() => (props.to ? 'RouterLink' : 'div'))

const linkAttrs = computed(() => (props.to ? { to: props.to } : {}))

const styleVars = computed(() => (props.accent ? { '--hd-cart-accent': props.accent } : null))

const versionText = computed(() => {
  if (!props.version) return ''
  return props.version.startsWith('v') ? props.version : `v${props.version}`
})

const downloadText = computed(() => {
  if (props.downloads === null || props.downloads === undefined || props.downloads === '')
    return '—'
  const n = Number(props.downloads)
  return Number.isFinite(n) ? n.toLocaleString() : String(props.downloads)
})
</script>

<template>
  <component
    :is="tag"
    v-bind="linkAttrs"
    class="hd-cart"
    :class="{ 'hd-cart--off': off, 'hd-cart--interactive': !!to }"
    :style="styleVars"
  >
    <!-- 顶部斜纹（卡带金手指） -->
    <div class="hd-cart__stripe" aria-hidden="true"></div>

    <div class="hd-cart__body">
      <div class="hd-cart__head" :class="{ 'hd-cart__head--stack': stack }">
        <span class="hd-cart__name">{{ name }}</span>
        <span v-if="versionText" class="hd-cart__ver">{{ versionText }}</span>
      </div>

      <p v-if="tagline" class="hd-cart__desc">{{ tagline }}</p>

      <div v-if="platforms.length" class="hd-cart__tags">
        <span v-for="p in platforms" :key="p" class="hd-tag">{{ p }}</span>
      </div>

      <slot name="body" />
    </div>

    <div class="hd-cart__foot">
      <slot name="foot">
        <span>{{ downloadText }} {{ downloadsLabel }}</span>
        <span v-if="to && detailLabel" class="hd-cart__link">{{ detailLabel }} →</span>
        <span v-else-if="to" class="hd-cart__link">→</span>
        <span v-else></span>
      </slot>
    </div>

    <span v-if="badge" class="hd-badge hd-badge--muted hd-cart__badge">{{ badge }}</span>
  </component>
</template>
