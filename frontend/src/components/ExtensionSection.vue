<script setup>
/**
 * F07 扩展市场（软件详情页内的分区）。
 *
 * 规则（D073 / D074 / F07）：
 * - 该软件没有已上架扩展时整个分区不渲染，不留空容器；
 * - 精选 ≤ 6 个，「查看全部」内联展开（不换页），每页 12 个；
 * - 接口失败 → 分区内错误态 + 重试，不影响页面其余部分；
 * - 排序 featured DESC, download_count DESC（api 层保证）。
 */
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { localized } from '@/i18n'
import { fetchExtensions } from '@/api/extension'

const props = defineProps({
  slug: { type: String, required: true },
})

const { t } = useI18n()

const FEATURED_MAX = 6
const PAGE_SIZE = 12

const loading = ref(true)
const failed = ref(false)
const extensions = ref([])
const visibleCount = ref(FEATURED_MAX)

/** 精选扩展数（接口已按 featured DESC 排好序，精选在前；F07：精选 ≤ 6） */
const featuredCount = computed(() =>
  Math.min(FEATURED_MAX, extensions.value.filter((ext) => ext.featured).length),
)

/** 计数为 0 时整个分区不渲染（D073）；失败且没有任何数据时渲染错误态 */
const showSection = computed(() => !loading.value && (extensions.value.length > 0 || failed.value))

const visible = computed(() => extensions.value.slice(0, visibleCount.value))
const hasMore = computed(() => extensions.value.length > visibleCount.value)

async function load() {
  loading.value = true
  failed.value = false
  try {
    extensions.value = await fetchExtensions(props.slug)
  } catch {
    failed.value = true
  } finally {
    // 初始只露出精选；「查看全部」按页展开非精选的（F07）
    visibleCount.value = featuredCount.value
    loading.value = false
  }
}

function showMore() {
  visibleCount.value = Math.min(visibleCount.value + PAGE_SIZE, extensions.value.length)
}

function formatCount(value) {
  return Number(value || 0).toLocaleString()
}

onMounted(load)
watch(() => props.slug, load)
</script>

<template>
  <section v-if="showSection" id="extensions" class="ext">
    <div class="ext__head">
      <div>
        <h2 class="ext__title">
          {{ t('extension.title') }}<span class="ext__bullet" aria-hidden="true"></span>
        </h2>
        <p class="ext__sub">{{ t('extension.sectionLead') }}</p>
      </div>
      <div class="ext__actions">
        <RouterLink :to="{ name: 'extension-submit', params: { slug } }" class="btn btn--sm">
          {{ t('extension.submitEntry') }}
        </RouterLink>
      </div>
    </div>

    <div v-if="failed && !extensions.length" class="ext__error">
      <p class="muted">{{ t('common.error') }}</p>
      <button type="button" class="btn btn--sm" @click="load">{{ t('common.retry') }}</button>
    </div>

    <template v-else>
      <div class="ext-grid">
        <RouterLink
          v-for="ext in visible"
          :key="ext.id"
          :to="{ name: 'extension-detail', params: { slug, id: ext.id } }"
          class="ext-card"
        >
          <div class="ext-card__top">
            <span class="ext-card__icon" aria-hidden="true">{{
              (localized(ext.nameZh, ext.nameEn) || '?').slice(0, 1)
            }}</span>
            <span v-if="ext.authorType === 'official'" class="ext-card__badge">
              {{ t('extension.official') }}
            </span>
          </div>
          <div class="ext-card__name">{{ localized(ext.nameZh, ext.nameEn) }}</div>
          <div class="ext-card__author">{{ ext.authorName || t('extension.official') }}</div>
          <p class="ext-card__desc">{{ localized(ext.summaryZh, ext.summaryEn) }}</p>
          <div class="ext-card__foot">
            <span>
              {{
                ext.bundled
                  ? t('extension.bundled')
                  : `${formatCount(ext.downloadCount)} ${t('extension.downloads')}`
              }}
            </span>
            <span class="ext-card__more">{{ t('extension.detail') }} →</span>
          </div>
        </RouterLink>
      </div>

      <div v-if="hasMore" class="ext__more">
        <button type="button" class="btn btn--ghost btn--sm" @click="showMore">
          {{ t('extension.viewAll') }}
        </button>
      </div>
    </template>
  </section>
</template>

<style scoped>
.ext {
  margin-top: 56px;
  padding-top: 40px;
  border-top: 2px solid var(--line);
}

.ext__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 26px;
}

.ext__title {
  font-size: clamp(22px, 3vw, 30px);
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 分区标题前的像素方块，与 .section-title 同一语言 */
.ext__bullet {
  width: 10px;
  height: 10px;
  background: var(--red);
  box-shadow: 0 0 0 2px var(--ink);
  flex: none;
}

.ext__sub {
  margin-top: 8px;
  max-width: 62ch;
  font-size: 14px;
  color: var(--ink-soft);
}

.ext-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 18px;
}

.ext-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 18px 18px 14px;
  background: var(--card);
  border: 2px solid var(--line);
  box-shadow: var(--shadow-pixel-sm);
  border-radius: var(--radius);
  color: inherit;
  transition:
    transform 0.16s ease,
    box-shadow 0.16s ease;
}

.ext-card:hover {
  transform: translate(-2px, -2px);
  box-shadow: var(--shadow-pixel);
}

.ext-card__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  min-height: 44px;
}

/* 图标底板：M0 没有真实图标，先用首字占位，M2 换 icon_url */
.ext-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border: 2px solid var(--line);
  border-radius: 8px;
  background: var(--paper-2);
  font-family: var(--font-mono);
  font-weight: 700;
  font-size: 20px;
}

.ext-card__badge {
  padding: 2px 8px;
  border: 1.5px solid var(--tag-line);
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--accent-text);
  white-space: nowrap;
}

.ext-card__name {
  margin-top: 4px;
  font-size: 18px;
  font-weight: 800;
}

.ext-card__author {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--accent-text);
}

.ext-card__desc {
  flex: 1;
  margin: 0;
  font-size: 14px;
  color: var(--ink-soft);
}

.ext-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 8px;
  padding-top: 10px;
  border-top: 1px dashed var(--line-faint);
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--ink-faint);
}

.ext-card__more {
  font-weight: 700;
  color: var(--accent-text);
  white-space: nowrap;
}

.ext__more {
  margin-top: 20px;
  text-align: center;
}

.ext__error {
  display: grid;
  justify-items: start;
  gap: 10px;
  padding: 24px;
  border: 2px dashed var(--line-soft);
}

@media (max-width: 720px) {
  .ext {
    margin-top: 40px;
    padding-top: 28px;
  }
}
</style>
