<script setup>
/**
 * F08 扩展详情。
 *
 * 路由 /software/:slug/extensions/:id。
 * - 扩展必须属于 URL 里的软件，否则按 404 处理（D072，防跨软件串门）；
 * - 社区扩展固定显示风险提示；官方扩展显示「官方提供」徽章且不显示提示（D083、Q007）；
 * - permissions 原样展示（F08）；SHA256 只展示前 12 位（schema 注释）；
 * - 下载短链（F10）是 M2 接口，M0 先置灰并说明。
 */
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { localized } from '@/i18n'
import { fetchExtensionDetail } from '@/api/extension'
import { fetchSoftwareDetail } from '@/api/site'

const props = defineProps({
  slug: { type: String, required: true },
  id: { type: String, required: true },
})

const { t } = useI18n()

const loading = ref(true)
const failed = ref(false)
const notFound = ref(false)
const ext = ref(null)
const softwareName = ref('')
const readmeText = ref('')

const name = computed(() => (ext.value ? localized(ext.value.nameZh, ext.value.nameEn) : ''))
const summary = computed(() =>
  ext.value ? localized(ext.value.summaryZh, ext.value.summaryEn) : '',
)
const isOfficial = computed(() => ext.value?.authorType === 'official')
const latestRelease = computed(() => ext.value?.releases?.[0] || null)
const releases = computed(() => ext.value?.releases || [])

/**
 * README：后端只存对象键/URL（readme_url），前端拉 Markdown 文本展示；
 * M0 纯文本 pre-wrap，M2 接渲染器。拉不到（OSS 未配 / 对象缺失）就藏分区，
 * 不让详情页的其他内容陪葬。
 */
async function loadReadme(url) {
  readmeText.value = ''
  if (!url) return
  try {
    const res = await fetch(url)
    if (res.ok) {
      readmeText.value = await res.text()
    }
  } catch {
    // README 属增强内容，失败静默
  }
}

function formatDate(value) {
  return value ? String(value).slice(0, 10) : '—'
}

function formatSize(bytes) {
  if (!bytes) return '—'
  const mb = bytes / 1024 / 1024
  return mb >= 0.1 ? `${mb.toFixed(1)} MB` : `${Math.max(1, Math.round(bytes / 1024))} KB`
}

function formatCount(value) {
  return Number(value || 0).toLocaleString()
}

async function load() {
  loading.value = true
  failed.value = false
  notFound.value = false
  ext.value = null
  readmeText.value = ''
  try {
    ext.value = await fetchExtensionDetail(props.slug, props.id)
    document.title = `${name.value} · Hundun Lab`
    loadReadme(ext.value.readmeUrl)
  } catch (error) {
    if (error?.errorCode === 'EXTENSION_NOT_FOUND') {
      notFound.value = true
    } else {
      failed.value = true
    }
  } finally {
    loading.value = false
  }
  // 面包屑里的软件名：拿不到就用 slug 兜底，不阻塞详情本身
  try {
    const software = await fetchSoftwareDetail(props.slug)
    softwareName.value = localized(software.nameZh, software.nameEn)
  } catch {
    softwareName.value = props.slug
  }
}

onMounted(load)
watch(() => [props.slug, props.id], load)
</script>

<template>
  <div class="ext-page">
    <div class="container">
      <!-- 面包屑：回详情页的扩展分区（F08） -->
      <nav class="crumbs mono" :aria-label="t('extension.title')">
        <RouterLink :to="{ name: 'software-detail', params: { slug } }">
          {{ softwareName || slug }}
        </RouterLink>
        <span aria-hidden="true"> / </span>
        <RouterLink :to="{ name: 'software-detail', params: { slug }, hash: '#extensions' }">
          {{ t('extension.title') }}
        </RouterLink>
        <span aria-hidden="true"> / </span>
        <span v-if="name" class="crumbs__here">{{ name }}</span>
      </nav>

      <p v-if="loading" class="muted mono">{{ t('common.loading') }}</p>

      <div v-else-if="notFound" class="card empty">
        <p>{{ t('extension.notFound') }}</p>
        <RouterLink :to="{ name: 'software-detail', params: { slug } }" class="btn btn--sm">
          {{ t('extension.backToSoftware') }}
        </RouterLink>
      </div>

      <div v-else-if="failed" class="card empty">
        <p>{{ t('common.error') }}</p>
        <button type="button" class="btn btn--sm" @click="load">{{ t('common.retry') }}</button>
      </div>

      <template v-else-if="ext">
        <!-- 头部：图标 / 名称 / 作者 / 版本 / 主操作 -->
        <section class="card hero">
          <div class="hero__main">
            <span class="hero__icon" aria-hidden="true">{{ name.slice(0, 1) }}</span>
            <div>
              <h1 class="hero__title">{{ name }}</h1>
              <div class="hero__meta">
                <span class="mono hero__author">{{
                  ext.authorName || t('extension.official')
                }}</span>
                <span v-if="latestRelease" class="mono hero__version">
                  v{{ latestRelease.version }}
                </span>
                <span class="hero__summary">{{ summary }}</span>
              </div>
              <p class="mono hero__stats">
                {{ formatCount(ext.downloadCount) }} {{ t('extension.downloads') }} ·
                {{ t('extension.updatedAt') }} {{ formatDate(ext.updatedAt) }} · {{ ext.license }}
              </p>
            </div>
          </div>
          <div class="hero__actions">
            <button type="button" class="btn btn--primary hero__download" disabled>
              {{ t('extension.download') }}
            </button>
            <a
              v-if="ext.repoUrl"
              class="btn btn--ghost"
              :href="ext.repoUrl"
              target="_blank"
              rel="noopener noreferrer"
            >
              {{ t('extension.viewSource') }}
            </a>
            <p class="mono muted hero__hint">{{ t('extension.downloadPending') }}</p>
          </div>
        </section>

        <div class="ext-body">
          <div class="ext-body__main">
            <!-- 版本历史 -->
            <section v-if="releases.length" class="block">
              <h2 class="ext-heading">{{ t('extension.versions') }}</h2>
              <ul class="versions">
                <li v-for="release in releases" :key="release.id" class="versions__row">
                  <span class="mono versions__ver">v{{ release.version }}</span>
                  <span class="mono versions__date">{{ formatDate(release.publishedAt) }}</span>
                  <span class="versions__note">
                    {{ localized(release.changelogZh, release.changelogEn) }}
                  </span>
                </li>
              </ul>
            </section>

            <!-- README：后端 readme_url 拉取的 Markdown 文本，M0 纯文本 pre-wrap，M2 接渲染器 -->
            <section v-if="readmeText" class="block">
              <h2 class="ext-heading">{{ t('extension.readme') }}</h2>
              <div class="card readme">
                <p class="prose">{{ readmeText }}</p>
              </div>
            </section>
          </div>

          <aside class="ext-body__side">
            <!-- 权限声明：permissions 原样展示（F08） -->
            <div class="card side">
              <h3 class="side__title">{{ t('extension.permissions') }}</h3>
              <p v-if="!ext.permissions?.length" class="muted side__none">
                {{ t('extension.noPermissions') }}
              </p>
              <ul v-else class="perms">
                <li v-for="perm in ext.permissions" :key="perm" class="mono perms__item">
                  {{ perm }}
                </li>
              </ul>
            </div>

            <div class="card side">
              <h3 class="side__title">{{ t('extension.info') }}</h3>
              <dl class="kv">
                <div class="kv__row">
                  <dt>{{ t('extension.version') }}</dt>
                  <dd class="mono">v{{ latestRelease?.version || '—' }}</dd>
                </div>
                <div class="kv__row">
                  <dt>{{ t('extension.size') }}</dt>
                  <dd class="mono">{{ formatSize(latestRelease?.fileSize) }}</dd>
                </div>
                <div class="kv__row">
                  <dt>{{ t('extension.author') }}</dt>
                  <dd>{{ ext.authorName || t('extension.official') }}</dd>
                </div>
                <div class="kv__row">
                  <dt>{{ t('extension.source') }}</dt>
                  <dd>{{ isOfficial ? t('extension.official') : t('extension.community') }}</dd>
                </div>
                <div class="kv__row">
                  <dt>{{ t('extension.checksum') }}</dt>
                  <dd class="mono" :title="latestRelease?.sha256">
                    {{ latestRelease?.sha256 ? `${latestRelease.sha256.slice(0, 12)}…` : '—' }}
                  </dd>
                </div>
              </dl>

              <!-- Q007：社区扩展固定风险提示；官方扩展只显示徽章不显示提示 -->
              <p v-if="isOfficial" class="badge-official">{{ t('extension.official') }}</p>
              <p v-else class="risk">
                <b>{{ t('extension.riskTitle') }}</b>
                {{ t('extension.riskBody') }}
              </p>
            </div>
          </aside>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.ext-page {
  padding: 24px 0 80px;
}

.crumbs {
  font-size: 12px;
  letter-spacing: 0.06em;
  color: var(--ink-faint);
  padding-bottom: 22px;
}

.crumbs a {
  color: var(--accent-text);
}

.crumbs a:hover {
  text-decoration: underline;
}

.crumbs__here {
  color: var(--ink);
  font-weight: 700;
}

.empty {
  margin-top: 24px;
  padding: 40px;
  display: grid;
  justify-items: start;
  gap: 14px;
}

/* ---------- 头部 ---------- */

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 24px 32px;
  padding: 24px 26px;
}

.hero__main {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.hero__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 88px;
  height: 88px;
  flex: none;
  border: 2px solid var(--line);
  border-radius: 8px;
  background: var(--paper-2);
  font-family: var(--font-mono);
  font-weight: 700;
  font-size: 40px;
}

.hero__title {
  font-size: clamp(28px, 4vw, 42px);
  line-height: 1.15;
}

.hero__meta {
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.hero__author {
  font-weight: 700;
  color: var(--accent-text);
}

.hero__version {
  padding: 2px 10px;
  border: 1.5px solid var(--tag-line);
  font-size: 12px;
}

.hero__summary {
  font-size: 15px;
  color: var(--ink);
}

.hero__stats {
  margin-top: 10px;
  font-size: 13px;
  font-weight: 700;
  color: var(--accent-text);
}

.hero__actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  justify-content: center;
  border-left: 2px dashed var(--line-soft);
  padding-left: 32px;
}

.hero__download {
  height: 52px;
  font-size: 16px;
}

.hero__hint {
  max-width: 220px;
  font-size: 11px;
}

/* ---------- 正文两栏 ---------- */

.ext-body {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(0, 1fr);
  gap: 32px;
  margin-top: 32px;
  align-items: start;
}

.block {
  margin-bottom: 32px;
}

.ext-heading {
  font-size: 20px;
  margin-bottom: 14px;
}

.versions {
  border-top: 2px solid var(--line);
}

.versions__row {
  display: grid;
  grid-template-columns: 90px 110px minmax(0, 1fr);
  gap: 12px;
  align-items: baseline;
  padding: 12px 4px;
  border-bottom: 1px dashed var(--line-faint);
  font-size: 14px;
}

.versions__ver {
  font-weight: 700;
  color: var(--accent-text);
}

.versions__date {
  color: var(--ink-faint);
  font-size: 12px;
}

.versions__note {
  color: var(--ink-soft);
}

.readme {
  padding: 20px 22px;
}

/* ---------- 侧栏 ---------- */

.ext-body__side {
  display: grid;
  gap: 20px;
}

.side {
  padding: 18px 20px;
}

.side__title {
  font-size: 16px;
  margin-bottom: 12px;
}

.perms {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.perms__item {
  padding: 3px 10px;
  border: 1.5px solid var(--tag-line);
  font-size: 12px;
}

.side__none {
  font-size: 13px;
}

.kv {
  display: grid;
  gap: 10px;
}

.kv__row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
}

.kv__row dt {
  color: var(--ink-faint);
}

.kv__row dd {
  margin: 0;
  text-align: right;
  word-break: break-all;
}

.badge-official {
  margin: 0;
  padding: 6px 12px;
  border: 1.5px solid var(--tag-line);
  font-family: var(--font-mono);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--accent-text);
}

/* 风险提示：文案可改，不可删（F08 可配置边界） */
.risk {
  margin: 12px 0 0;
  padding: 10px 12px;
  border: 1.5px dashed var(--line-soft);
  font-size: 12px;
  line-height: 1.6;
  color: var(--ink-soft);
}

.risk b {
  color: var(--accent-text);
}

@media (max-width: 900px) {
  .hero {
    grid-template-columns: minmax(0, 1fr);
  }

  .hero__actions {
    border-left: 0;
    padding-left: 0;
    border-top: 2px dashed var(--line-soft);
    padding-top: 16px;
  }

  .ext-body {
    grid-template-columns: minmax(0, 1fr);
  }

  .versions__row {
    grid-template-columns: 80px minmax(0, 1fr);
  }

  .versions__date {
    display: none;
  }
}
</style>
