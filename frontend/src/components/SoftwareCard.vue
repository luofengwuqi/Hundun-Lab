<script setup>
/**
 * 首页 / 列表页的软件卡片
 * ------------------------------------------------------------------
 * 视觉已收敛到物料库：这里只做数据适配，本质是 HdCart 的包装。
 * 想换卡片长相请改 hundun-kit.css 的 .hd-cart，不要在这个文件里写样式。
 */
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { localized } from '@/i18n'
import { HdCart } from '@/components/hundun'

const props = defineProps({
  software: { type: Object, required: true },
})

const { t } = useI18n()

const name = computed(() => localized(props.software.nameZh, props.software.nameEn))
const tagline = computed(() => localized(props.software.taglineZh, props.software.taglineEn))
// 卡片顶部斜纹的主色来自数据库的 theme_color，新增软件不改代码就能换色
const accent = computed(() => props.software.themeColor || '')
const platforms = computed(() => (props.software.platforms || []).map((p) => t(`platform.${p}`)))
</script>

<template>
  <HdCart
    :name="name"
    :version="software.latestVersion"
    :tagline="tagline"
    :platforms="platforms"
    :downloads="software.totalDownloads || 0"
    :downloads-label="t('home.downloads')"
    :accent="accent"
    :detail-label="t('home.viewDetail')"
    :to="`/software/${software.slug}`"
  />
</template>
