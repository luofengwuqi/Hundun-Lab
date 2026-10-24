<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { fetchSoftwareDetail, requestDownloadTicket } from '@/api/site'
import { localized } from '@/i18n'
import { useSessionStore } from '@/stores/session'
import AuthDialog from '@/components/AuthDialog.vue'

const { t, te } = useI18n()
const route = useRoute()
const session = useSessionStore()

const loading = ref(true)
const failed = ref(false)
const notFound = ref(false)
const detail = ref(null)

const authOpen = ref(false)
const pendingAsset = ref(null)
const busyAssetId = ref(null)
const notice = ref({ type: '', key: '' })

const name = computed(() => (detail.value ? localized(detail.value.nameZh, detail.value.nameEn) : ''))
const tagline = computed(() =>
  detail.value ? localized(detail.value.taglineZh, detail.value.taglineEn) : '',
)
const introParagraphs = computed(() => {
  if (!detail.value) return []
  const text = localized(detail.value.introZh, detail.value.introEn) || ''
  return text
    .split(/\n{2,}/)
    .map((p) => p.trim())
    .filter(Boolean)
})
const latestRelease = computed(() => detail.value?.releases?.[0] || null)
// 主按钮优先给 Windows 安装包，没有则退回第一个包
const primaryAsset = computed(() => {
  const assets = latestRelease.value?.assets || []
  return assets.find((a) => a.platform === 'win32') || assets[0] || null
})

async function load(slug) {
  loading.value = true
  failed.value = false
  notFound.value = false
  try {
    detail.value = await fetchSoftwareDetail(slug)
    document.title = `${localized(detail.value.nameZh, detail.value.nameEn)} · Hundun Lab`
  } catch (error) {
    if (error?.errorCode === 'SOFTWARE_NOT_FOUND') {
      notFound.value = true
    } else {
      failed.value = true
    }
  } finally {
    loading.value = false
  }
}

function formatSize(bytes) {
  if (!bytes) return '—'
  const mb = bytes / 1024 / 1024
  return `${mb.toFixed(1)} MB`
}

function formatDate(value) {
  return value ? String(value).slice(0, 10) : '—'
}

function assetLabel(asset) {
  const platform = t(`platform.${asset.platform}`)
  return `${platform} · ${asset.packageType.toUpperCase()} · ${asset.arch}`
}

// 后端只返回错误码，翻译交给前端；没有对应文案时回落到通用错误，绝不把 errors.XXX 甩给用户
function errorTextKey(code) {
  const key = `errors.${code || 'INTERNAL_ERROR'}`
  return te(key) ? key : 'common.error'
}

function openAuth(asset) {
  pendingAsset.value = asset || null
  authOpen.value = true
}

async function issueTicket(asset) {
  if (!asset || busyAssetId.value) return
  busyAssetId.value = asset.id
  notice.value = { type: '', key: '' }
  try {
    const ticket = await requestDownloadTicket(asset.id)
    // OSS 预签名地址：直接跳转，安装包不经过应用服务器
    window.location.href = ticket.url
    notice.value = { type: 'ok', key: 'download.started' }
  } catch (error) {
    if (error?.errorCode === 'AUTH_NOT_LOGGED_IN' || error?.errorCode === 'AUTH_TOKEN_EXPIRED') {
      session.clear()
      openAuth(asset)
    } else if (error?.errorCode === 'DOWNLOAD_STORAGE_UNAVAILABLE') {
      notice.value = { type: 'error', key: 'download.storageUnavailable' }
    } else {
      notice.value = { type: 'error', key: errorTextKey(error?.errorCode) }
    }
  } finally {
    busyAssetId.value = null
  }
}

function startDownload(asset) {
  if (!asset) return
  if (!session.isLoggedIn) {
    openAuth(asset)
    return
  }
  issueTicket(asset)
}

// 登录成功 → 自动续上刚才被打断的那次下载，用户不用再点一次
function onAuthSuccess() {
  const asset = pendingAsset.value
  pendingAsset.value = null
  if (asset) {
    issueTicket(asset)
  }
}

async function copyChecksum(text) {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    notice.value = { type: 'ok', key: 'common.copied' }
  } catch {
    notice.value = { type: 'error', key: 'common.error' }
  }
}

onMounted(() => load(route.params.slug))
watch(
  () => route.params.slug,
  (slug) => {
    if (slug) load(slug)
  },
)
</script>

<template>
  <div class="software-page">
      <div class="container detail-wrap">
        <p v-if="loading" class="muted mono">{{ t('common.loading') }}</p>

        <div v-else-if="notFound" class="empty-state card">
          <p>{{ t('software.notFound') }}</p>
          <RouterLink to="/software" class="btn btn--sm">{{ t('software.back') }}</RouterLink>
        </div>

        <div v-else-if="failed" class="empty-state card">
          <p>{{ t('common.error') }}</p>
          <button type="button" class="btn btn--sm" @click="load(route.params.slug)">
            {{ t('common.retry') }}
          </button>
        </div>

        <template v-else-if="detail">
          <RouterLink to="/software" class="back-link mono">← {{ t('software.back') }}</RouterLink>

          <!-- 顶部：名称 + 下载入口 -->
          <header class="detail-head">
            <div class="detail-head__copy">
              <div class="detail-head__tags">
                <span v-if="latestRelease" class="tag tag--red mono">v{{ latestRelease.version }}</span>
                <span v-for="p in detail.platforms" :key="p.platform" class="tag">
                  {{ t(`platform.${p.platform}`) }}
                </span>
              </div>
              <h1 class="detail-head__title">{{ name }}</h1>
              <p class="detail-head__tagline">{{ tagline }}</p>

              <div class="detail-head__actions">
                <button
                  v-if="primaryAsset"
                  type="button"
                  class="btn btn--primary"
                  :disabled="busyAssetId === primaryAsset.id"
                  @click="startDownload(primaryAsset)"
                >
                  {{ busyAssetId === primaryAsset.id ? t('download.preparing') : t('software.download') }}
                  <span class="mono detail-head__size">{{ formatSize(primaryAsset.fileSize) }}</span>
                </button>
                <span v-if="!session.isLoggedIn" class="muted detail-head__hint">
                  {{ t('software.downloadHint') }}
                </span>
              </div>

              <p v-if="notice.key" class="notice mono" :class="`notice--${notice.type}`">
                {{ t(notice.key) }}
              </p>
            </div>

            <div class="detail-head__side card">
              <dl>
                <div v-if="detail.license">
                  <dt class="kicker">{{ t('software.license') }}</dt>
                  <dd class="mono">{{ detail.license }}</dd>
                </div>
                <div v-if="detail.repoUrl">
                  <dt class="kicker">{{ t('software.repo') }}</dt>
                  <dd>
                    <a :href="detail.repoUrl" target="_blank" rel="noopener noreferrer" class="mono">
                      {{ detail.repoUrl.replace('https://', '') }}
                    </a>
                  </dd>
                </div>
                <div v-if="detail.techStack.length">
                  <dt class="kicker">{{ t('software.techStack') }}</dt>
                  <dd class="tech">
                    <span v-for="tech in detail.techStack" :key="tech" class="tag">{{ tech }}</span>
                  </dd>
                </div>
              </dl>
            </div>
          </header>

          <!-- 简介 -->
          <section v-if="introParagraphs.length" class="block">
            <h2 class="section-title">{{ t('software.overview') }}</h2>
            <div class="prose block__body">
              <p v-for="(para, i) in introParagraphs" :key="i">{{ para }}</p>
            </div>
          </section>

          <!-- 功能特性 -->
          <section v-if="detail.features.length" class="block">
            <h2 class="section-title">{{ t('software.features') }}</h2>
            <div class="feature-grid">
              <article v-for="(f, i) in detail.features" :key="i" class="feature card">
                <span class="feature__icon" aria-hidden="true">{{ f.icon }}</span>
                <h3 class="feature__title">{{ localized(f.titleZh, f.titleEn) }}</h3>
                <p class="feature__desc">{{ localized(f.descZh, f.descEn) }}</p>
              </article>
            </div>
          </section>

          <!-- 截图（后台没上传就不显示这一块，不留空壳） -->
          <section v-if="detail.screenshots.length" class="block">
            <h2 class="section-title">{{ t('software.screenshots') }}</h2>
            <div class="shots">
              <figure v-for="(s, i) in detail.screenshots" :key="i" class="shot card">
                <img :src="s.imageUrl" :alt="localized(s.captionZh, s.captionEn) || name" loading="lazy" />
                <figcaption class="mono">{{ localized(s.captionZh, s.captionEn) }}</figcaption>
              </figure>
            </div>
          </section>

          <!-- 版本记录 -->
          <section class="block">
            <h2 class="section-title">{{ t('software.releases') }}</h2>

            <p v-if="!detail.releases.length" class="muted">{{ t('software.noRelease') }}</p>

            <div v-for="release in detail.releases" :key="release.id" class="release card">
              <header class="release__head">
                <h3 class="mono">v{{ release.version }}</h3>
                <span class="tag">{{ release.channel }}</span>
                <span class="mono release__date">{{ formatDate(release.publishedAt) }}</span>
              </header>

              <div v-if="release.notesZh || release.notesEn" class="release__notes prose">
                <p>{{ localized(release.notesZh, release.notesEn) }}</p>
              </div>

              <p v-if="!release.assets.length" class="muted release__empty">{{ t('software.noAsset') }}</p>

              <ul v-else class="assets">
                <li v-for="asset in release.assets" :key="asset.id" class="assets__row">
                  <div class="assets__info">
                    <span class="assets__label mono">{{ assetLabel(asset) }}</span>
                    <span class="assets__file mono">{{ asset.fileName }}</span>
                  </div>
                  <div class="assets__meta mono">
                    <span>{{ formatSize(asset.fileSize) }}</span>
                    <button
                      v-if="asset.sha256"
                      type="button"
                      class="assets__sha"
                      :title="asset.sha256"
                      @click="copyChecksum(asset.sha256)"
                    >
                      {{ asset.sha256.slice(0, 12) }}…
                    </button>
                  </div>
                  <button
                    type="button"
                    class="btn btn--sm"
                    :disabled="busyAssetId === asset.id"
                    @click="startDownload(asset)"
                  >
                    {{ busyAssetId === asset.id ? t('download.preparing') : t('software.download') }}
                  </button>
                </li>
              </ul>
            </div>
          </section>
        </template>
      </div>

    <AuthDialog v-model:open="authOpen" @success="onAuthSuccess" />
  </div>
</template>

<style scoped>
.detail-wrap {
  padding: 40px 0 80px;
}

.back-link {
  font-size: 12px;
  color: var(--ink-faint);
  letter-spacing: 0.08em;
}

.back-link:hover {
  color: var(--red);
}

.detail-head {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 0.6fr);
  gap: 36px;
  margin-top: 18px;
  padding-bottom: 36px;
  border-bottom: 2px solid var(--ink);
}

.detail-head__tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.detail-head__title {
  font-size: clamp(30px, 4.6vw, 48px);
  margin-top: 14px;
}

.detail-head__tagline {
  margin-top: 10px;
  font-size: 17px;
  color: var(--ink-soft);
}

.detail-head__actions {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 24px;
  flex-wrap: wrap;
}

.detail-head__size {
  font-size: 11px;
  opacity: 0.85;
}

.detail-head__hint {
  font-size: 13px;
}

.detail-head__side {
  padding: 20px;
  align-self: start;
}

.detail-head__side dl {
  display: grid;
  gap: 16px;
}

.detail-head__side a {
  color: var(--red);
  font-size: 13px;
  word-break: break-all;
}

.tech {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}

.notice {
  margin-top: 14px;
  font-size: 12px;
}

.notice--ok {
  color: var(--teal);
}

.notice--error {
  color: var(--red);
}

.block {
  margin-top: 48px;
}

.block__body {
  margin-top: 18px;
  max-width: min(68ch, 100%);
}

.feature-grid {
  margin-top: 20px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 18px;
}

.feature {
  padding: 18px;
}

.feature__icon {
  font-size: 22px;
  line-height: 1;
}

.feature__title {
  margin-top: 10px;
  font-size: 16px;
}

.feature__desc {
  margin-top: 6px;
  font-size: 14px;
  color: var(--ink-soft);
}

.shots {
  margin-top: 20px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 18px;
}

.shot {
  overflow: hidden;
}

.shot img {
  width: 100%;
  border-bottom: 2px solid var(--ink);
}

.shot figcaption {
  padding: 8px 12px;
  font-size: 12px;
  color: var(--ink-faint);
}

.release {
  margin-top: 18px;
  padding: 18px;
}

.release__head {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.release__head h3 {
  font-size: 18px;
}

.release__date {
  margin-left: auto;
  font-size: 12px;
  color: var(--ink-faint);
}

.release__notes {
  margin-top: 10px;
  font-size: 14px;
  white-space: pre-line;
}

.release__empty {
  margin-top: 10px;
  font-size: 13px;
}

.assets {
  margin-top: 14px;
  border-top: 1px dashed rgba(23, 19, 15, 0.28);
}

.assets__row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px dashed rgba(23, 19, 15, 0.18);
}

.assets__info {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.assets__label {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.assets__file {
  font-size: 11px;
  color: var(--ink-faint);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.assets__meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: var(--ink-soft);
}

.assets__sha {
  font-family: var(--font-mono);
  font-size: 11px;
  border-bottom: 1px dotted var(--ink-faint);
  cursor: pointer;
}

.assets__sha:hover {
  color: var(--red);
  border-color: var(--red);
}

.empty-state {
  margin-top: 60px;
  padding: 32px;
  text-align: center;
  display: grid;
  gap: 16px;
  justify-items: center;
}

@media (max-width: 860px) {
  .detail-head {
    grid-template-columns: minmax(0, 1fr);
    gap: 24px;
  }
  .assets__row {
    grid-template-columns: minmax(0, 1fr) auto;
  }
  .assets__meta {
    grid-column: 1 / -1;
  }
}
</style>
