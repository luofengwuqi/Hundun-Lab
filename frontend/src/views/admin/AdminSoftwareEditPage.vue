<script setup>
/**
 * 软件新建 / 编辑（F12 + F13）。
 *
 * 页面形态是「同一页两段」（D102 前的 grilling 结论）：
 * - 第一段：基础资料 + 四类内容块（特性 / 截图 / 平台 / 仓库），纯 JSON，秒回；
 * - 第二段：版本与安装包，**软件存下来之后才出现**（安装包要挂 releaseId）。
 *
 * 为什么不做成一次提交：安装包可能有 2GiB，单页长表单一旦上传失败就全丢，
 * 而 F12-02 明确要求「出错时已填内容不丢失」。分两段把「资料」和「文件」解耦，
 * 传包失败只影响那一个包。
 *
 * 子块是整块替换语义：这里不记每行的 id，提交什么就是什么，顺序即 sort_order。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import {
  createRelease,
  createSoftware,
  deleteRelease,
  deleteReleaseAsset,
  fetchSoftwareDetail,
  updateRelease,
  updateSoftware,
  uploadImage,
  uploadReleaseAsset,
} from '@/api/softwareAdmin'

const { t, te } = useI18n()
const route = useRoute()
const router = useRouter()

const softwareId = ref(route.params.id ? Number(route.params.id) : null)
const isNew = computed(() => softwareId.value === null)

const loading = ref(false)
const saving = ref(false)
const coverUploading = ref(false)
const screenshotUploadingIndex = ref(-1)

/** 版本与安装包来自后端（服务端是唯一事实源），每次改完都重新拉一次 */
const releases = ref([])

const form = reactive(emptyForm())

function emptyForm() {
  return {
    slug: '',
    nameZh: '',
    nameEn: '',
    taglineZh: '',
    taglineEn: '',
    introZh: '',
    introEn: '',
    techStackText: '',
    license: '',
    coverUrl: '',
    themeColor: '',
    sortOrder: 0,
    features: [],
    screenshots: [],
    platforms: [],
    repos: [],
  }
}

const repoTypes = computed(() => [
  { value: 'source', label: t('admin.software.repoTypeSource') },
  { value: 'mirror', label: t('admin.software.repoTypeMirror') },
  { value: 'release', label: t('admin.software.repoTypeRelease') },
  { value: 'docs', label: t('admin.software.repoTypeDocs') },
])

const platformOptions = [
  { value: 'win32', label: 'Windows' },
  { value: 'darwin', label: 'macOS' },
  { value: 'linux', label: 'Linux' },
]

const archOptions = [
  { value: 'x64', label: 'x64' },
  { value: 'arm64', label: 'arm64' },
]

const packageTypeOptions = [
  { value: 'nsis', label: 'nsis (.exe)' },
  { value: 'msi', label: 'msi (.msi)' },
  { value: 'dmg', label: 'dmg (.dmg)' },
  { value: 'appimage', label: 'AppImage (.AppImage)' },
  { value: 'deb', label: 'deb (.deb)' },
  { value: 'zip', label: 'zip (.zip)' },
]

function errorMessage(error, fallback = 'admin.software.opFailed') {
  const key = `errors.${error?.errorCode}`
  return error?.errorCode && te(key) ? t(key) : t(fallback)
}

function fillForm(data) {
  form.slug = data.slug || ''
  form.nameZh = data.nameZh || ''
  form.nameEn = data.nameEn || ''
  form.taglineZh = data.taglineZh || ''
  form.taglineEn = data.taglineEn || ''
  form.introZh = data.introZh || ''
  form.introEn = data.introEn || ''
  form.techStackText = Array.isArray(data.techStack) ? data.techStack.join(', ') : ''
  form.license = data.license || ''
  form.coverUrl = data.coverUrl || ''
  form.themeColor = data.themeColor || ''
  form.sortOrder = data.sortOrder ?? 0
  form.features = (data.features || []).map((it) => ({ ...it }))
  form.screenshots = (data.screenshots || []).map((it) => ({ ...it }))
  form.platforms = (data.platforms || []).map((it) => ({ ...it }))
  form.repos = (data.repos || []).map((it) => ({ ...it }))
}

async function loadDetail(id = softwareId.value) {
  if (!id) return
  loading.value = true
  try {
    const data = await fetchSoftwareDetail(id)
    fillForm(data)
    releases.value = Array.isArray(data.releases) ? data.releases : []
  } catch (error) {
    ElMessage.error(errorMessage(error, 'admin.software.loadFailed'))
  } finally {
    loading.value = false
  }
}

// ==================== 基础资料 ====================

function buildPayload() {
  return {
    slug: form.slug.trim(),
    nameZh: form.nameZh.trim(),
    nameEn: form.nameEn.trim(),
    taglineZh: form.taglineZh,
    taglineEn: form.taglineEn,
    introZh: form.introZh,
    introEn: form.introEn,
    techStack: form.techStackText
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean),
    license: form.license,
    coverUrl: form.coverUrl,
    themeColor: form.themeColor,
    sortOrder: Number(form.sortOrder) || 0,
    features: form.features.map((it) => ({
      icon: it.icon,
      titleZh: it.titleZh,
      titleEn: it.titleEn,
      descZh: it.descZh,
      descEn: it.descEn,
    })),
    screenshots: form.screenshots.map((it) => ({
      imageUrl: it.imageUrl,
      captionZh: it.captionZh,
      captionEn: it.captionEn,
    })),
    platforms: form.platforms.map((it) => ({
      platform: it.platform,
      noteZh: it.noteZh,
      noteEn: it.noteEn,
    })),
    repos: form.repos.map((it) => ({
      labelZh: it.labelZh,
      labelEn: it.labelEn,
      url: it.url,
      repoType: it.repoType,
    })),
  }
}

function validateBasics() {
  if (!form.slug.trim() || !form.nameZh.trim() || !form.nameEn.trim()) {
    ElMessage.error(t('admin.software.required'))
    return false
  }
  if (!/^[a-z0-9][a-z0-9-]{0,63}$/.test(form.slug.trim())) {
    ElMessage.error(t('admin.software.invalidSlug'))
    return false
  }
  return true
}

async function save() {
  if (saving.value || !validateBasics()) return
  saving.value = true
  try {
    const payload = buildPayload()
    if (isNew.value) {
      const id = await createSoftware(payload)
      ElMessage.success(t('admin.software.created'))
      // 换成编辑路由：同页展开「版本与安装包」那一段，安装包才有 releaseId 可挂
      await router.replace({ name: 'admin-software-edit', params: { id } })
      softwareId.value = Number(id)
      await loadDetail()
    } else {
      await updateSoftware(softwareId.value, payload)
      ElMessage.success(t('admin.software.saved'))
      await loadDetail()
    }
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    saving.value = false
  }
}

async function pickImage(onPicked) {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.addEventListener('change', async () => {
    const file = input.files?.[0]
    if (!file) return
    try {
      const url = await uploadImage(file)
      onPicked(url)
      ElMessage.success(t('admin.software.imageUploaded'))
    } catch (error) {
      ElMessage.error(errorMessage(error))
    }
  })
  input.click()
}

async function uploadCover() {
  if (coverUploading.value) return
  coverUploading.value = true
  try {
    await pickImage((url) => {
      form.coverUrl = url
    })
  } finally {
    coverUploading.value = false
  }
}

async function uploadScreenshot(index) {
  if (screenshotUploadingIndex.value >= 0) return
  screenshotUploadingIndex.value = index
  try {
    await pickImage((url) => {
      form.screenshots[index].imageUrl = url
    })
  } finally {
    screenshotUploadingIndex.value = -1
  }
}

// ==================== 子块增删 ====================

function addFeature() {
  form.features.push({ icon: '', titleZh: '', titleEn: '', descZh: '', descEn: '' })
}
function addScreenshot() {
  form.screenshots.push({ imageUrl: '', captionZh: '', captionEn: '' })
}
function addPlatform() {
  form.platforms.push({ platform: 'win32', noteZh: '', noteEn: '' })
}
function addRepo() {
  form.repos.push({ labelZh: '', labelEn: '', url: '', repoType: 'source' })
}
function removeAt(list, index) {
  list.splice(index, 1)
}

// ==================== 版本 ====================

const releaseDialog = ref(false)
const releaseSaving = ref(false)
const releaseEditingId = ref(null)
const releaseForm = reactive(emptyRelease())

function emptyRelease() {
  return {
    version: '',
    channel: 'stable',
    notesZh: '',
    notesEn: '',
    publishedAt: '',
    status: 0,
  }
}

function openReleaseDialog(release) {
  Object.assign(releaseForm, emptyRelease())
  releaseEditingId.value = release?.id ?? null
  if (release) {
    releaseForm.version = release.version || ''
    releaseForm.channel = release.channel || 'stable'
    releaseForm.notesZh = release.notesZh || ''
    releaseForm.notesEn = release.notesEn || ''
    releaseForm.publishedAt = release.publishedAt || ''
    releaseForm.status = release.status ?? 0
  }
  releaseDialog.value = true
}

async function saveRelease() {
  if (releaseSaving.value) return
  if (!releaseForm.version.trim()) {
    ElMessage.error(t('admin.software.required'))
    return
  }
  releaseSaving.value = true
  try {
    const payload = {
      version: releaseForm.version.trim(),
      channel: releaseForm.channel,
      notesZh: releaseForm.notesZh,
      notesEn: releaseForm.notesEn,
      publishedAt: releaseForm.publishedAt || null,
      status: releaseForm.status,
    }
    if (releaseEditingId.value) {
      await updateRelease(softwareId.value, releaseEditingId.value, payload)
    } else {
      await createRelease(softwareId.value, payload)
    }
    ElMessage.success(t('admin.software.saved'))
    releaseDialog.value = false
    await loadDetail()
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    releaseSaving.value = false
  }
}

async function handleDeleteRelease(release) {
  try {
    await ElMessageBox.confirm(
      t('admin.software.confirmDeleteReleaseBody', { version: release.version }),
      t('admin.software.confirmDeleteReleaseTitle'),
      {
        confirmButtonText: t('admin.software.confirmDelete'),
        cancelButtonText: t('admin.software.cancel'),
        type: 'warning',
      },
    )
  } catch {
    return
  }
  try {
    await deleteRelease(softwareId.value, release.id)
    ElMessage.success(t('admin.software.deleted'))
    await loadDetail()
  } catch (error) {
    ElMessage.error(errorMessage(error))
  }
}

// ==================== 安装包 ====================

/** 每个版本一份上传草稿（平台/架构/格式 + 进度），互不干扰 */
const assetDrafts = reactive({})

function draftOf(releaseId) {
  if (!assetDrafts[releaseId]) {
    assetDrafts[releaseId] = {
      platform: 'win32',
      arch: 'x64',
      packageType: 'nsis',
      percent: 0,
      uploading: false,
    }
  }
  return assetDrafts[releaseId]
}

async function handleAssetUpload(release, options) {
  const draft = draftOf(release.id)
  if (draft.uploading) return
  draft.uploading = true
  draft.percent = 0
  try {
    await uploadReleaseAsset(softwareId.value, release.id, {
      platform: draft.platform,
      arch: draft.arch,
      packageType: draft.packageType,
      file: options.file,
      onProgress: (percent) => {
        draft.percent = percent
      },
    })
    ElMessage.success(t('admin.software.assetUploaded'))
    options.onSuccess?.()
    await loadDetail()
  } catch (error) {
    options.onError?.(error)
    ElMessage.error(errorMessage(error))
  } finally {
    draft.uploading = false
    draft.percent = 0
  }
}

async function handleDeleteAsset(release, asset) {
  try {
    await ElMessageBox.confirm(
      t('admin.software.confirmDeleteAssetBody', { name: asset.fileName }),
      t('admin.software.confirmDeleteAssetTitle'),
      {
        confirmButtonText: t('admin.software.confirmDelete'),
        cancelButtonText: t('admin.software.cancel'),
        type: 'warning',
      },
    )
  } catch {
    return
  }
  try {
    await deleteReleaseAsset(softwareId.value, release.id, asset.id)
    ElMessage.success(t('admin.software.deleted'))
    await loadDetail()
  } catch (error) {
    ElMessage.error(errorMessage(error))
  }
}

function formatSize(bytes) {
  const size = Number(bytes || 0)
  if (size <= 0) return t('admin.software.notAvailable')
  if (size >= 1024 * 1024 * 1024) return `${(size / (1024 * 1024 * 1024)).toFixed(2)} GB`
  return `${(size / (1024 * 1024)).toFixed(1)} MB`
}

function formatTime(value) {
  if (!value) return t('admin.software.notAvailable')
  return String(value).slice(0, 19).replace('T', ' ')
}

onMounted(() => loadDetail())
</script>

<template>
  <div v-loading="loading" class="editor">
    <div class="editor__bar">
      <el-button plain @click="router.push({ name: 'admin-software' })">
        {{ t('admin.software.backToList') }}
      </el-button>
      <el-button type="primary" :loading="saving" @click="save">
        {{ saving ? t('admin.software.saving') : t('admin.software.save') }}
      </el-button>
    </div>

    <!-- ---------- 第一段：基础资料 ---------- -->
    <div class="panel">
      <h2 class="panel__title">{{ t('admin.software.basicTitle') }}</h2>
      <p class="panel__lead">{{ t('admin.software.basicLead') }}</p>

      <div class="grid">
        <label class="field">
          <span class="field__label">{{ t('admin.software.fieldSlug') }}</span>
          <el-input v-model="form.slug" :disabled="!isNew" />
          <span class="field__hint">{{ t('admin.software.fieldSlugHint') }}</span>
        </label>
        <label class="field">
          <span class="field__label">{{ t('admin.software.fieldSortOrder') }}</span>
          <el-input v-model.number="form.sortOrder" />
          <span class="field__hint">{{ t('admin.software.fieldSortOrderHint') }}</span>
        </label>
        <label class="field">
          <span class="field__label">{{ t('admin.software.fieldNameZh') }}</span>
          <el-input v-model="form.nameZh" />
        </label>
        <label class="field">
          <span class="field__label">{{ t('admin.software.fieldNameEn') }}</span>
          <el-input v-model="form.nameEn" />
        </label>
        <label class="field">
          <span class="field__label">{{ t('admin.software.fieldTaglineZh') }}</span>
          <el-input v-model="form.taglineZh" />
        </label>
        <label class="field">
          <span class="field__label">{{ t('admin.software.fieldTaglineEn') }}</span>
          <el-input v-model="form.taglineEn" />
        </label>
        <label class="field">
          <span class="field__label">{{ t('admin.software.fieldLicense') }}</span>
          <el-input v-model="form.license" placeholder="MIT" />
        </label>
        <label class="field">
          <span class="field__label">{{ t('admin.software.fieldThemeColor') }}</span>
          <el-input v-model="form.themeColor" placeholder="#C3272B" />
        </label>
        <label class="field field--wide">
          <span class="field__label">{{ t('admin.software.fieldTechStack') }}</span>
          <el-input v-model="form.techStackText" />
          <span class="field__hint">{{ t('admin.software.fieldTechStackHint') }}</span>
        </label>
        <div class="field field--wide">
          <span class="field__label">{{ t('admin.software.fieldCover') }}</span>
          <div class="cover">
            <el-input v-model="form.coverUrl" />
            <el-button plain :loading="coverUploading" @click="uploadCover">
              {{ coverUploading ? t('admin.software.uploading') : t('admin.software.upload') }}
            </el-button>
          </div>
          <img v-if="form.coverUrl" :src="form.coverUrl" alt="" class="cover__preview" />
        </div>
        <label class="field field--wide">
          <span class="field__label">{{ t('admin.software.fieldIntroZh') }}</span>
          <el-input v-model="form.introZh" type="textarea" :rows="4" />
        </label>
        <label class="field field--wide">
          <span class="field__label">{{ t('admin.software.fieldIntroEn') }}</span>
          <el-input v-model="form.introEn" type="textarea" :rows="4" />
        </label>
      </div>
    </div>

    <!-- ---------- 内容块：特性 ---------- -->
    <div class="panel">
      <div class="panel__head">
        <div>
          <h2 class="panel__title">{{ t('admin.software.featuresTitle') }}</h2>
          <p class="panel__lead">{{ t('admin.software.featuresLead') }}</p>
        </div>
        <el-button plain @click="addFeature">{{ t('admin.software.addFeature') }}</el-button>
      </div>

      <div v-for="(item, index) in form.features" :key="`feature-${index}`" class="row">
        <el-input
          v-model="item.icon"
          :placeholder="t('admin.software.featureIcon')"
          class="row__icon"
        />
        <el-input v-model="item.titleZh" :placeholder="t('admin.software.featureTitleZh')" />
        <el-input v-model="item.titleEn" :placeholder="t('admin.software.featureTitleEn')" />
        <el-input v-model="item.descZh" :placeholder="t('admin.software.featureDescZh')" />
        <el-input v-model="item.descEn" :placeholder="t('admin.software.featureDescEn')" />
        <el-button plain type="danger" @click="removeAt(form.features, index)">
          {{ t('admin.software.remove') }}
        </el-button>
      </div>
    </div>

    <!-- ---------- 内容块：截图 ---------- -->
    <div class="panel">
      <div class="panel__head">
        <div>
          <h2 class="panel__title">{{ t('admin.software.screenshotsTitle') }}</h2>
          <p class="panel__lead">{{ t('admin.software.screenshotsLead') }}</p>
        </div>
        <el-button plain @click="addScreenshot">{{ t('admin.software.addScreenshot') }}</el-button>
      </div>

      <div v-for="(item, index) in form.screenshots" :key="`shot-${index}`" class="row">
        <div class="row__url">
          <el-input v-model="item.imageUrl" :placeholder="t('admin.software.screenshotUrl')" />
          <el-button
            plain
            :loading="screenshotUploadingIndex === index"
            @click="uploadScreenshot(index)"
          >
            {{
              screenshotUploadingIndex === index
                ? t('admin.software.uploading')
                : t('admin.software.upload')
            }}
          </el-button>
        </div>
        <el-input v-model="item.captionZh" :placeholder="t('admin.software.screenshotCaptionZh')" />
        <el-input v-model="item.captionEn" :placeholder="t('admin.software.screenshotCaptionEn')" />
        <el-button plain type="danger" @click="removeAt(form.screenshots, index)">
          {{ t('admin.software.remove') }}
        </el-button>
      </div>
    </div>

    <!-- ---------- 内容块：平台 ---------- -->
    <div class="panel">
      <div class="panel__head">
        <div>
          <h2 class="panel__title">{{ t('admin.software.platformsTitle') }}</h2>
          <p class="panel__lead">{{ t('admin.software.platformsLead') }}</p>
        </div>
        <el-button plain @click="addPlatform">{{ t('admin.software.addPlatform') }}</el-button>
      </div>

      <div v-for="(item, index) in form.platforms" :key="`platform-${index}`" class="row">
        <el-select v-model="item.platform" class="row__select">
          <el-option
            v-for="option in platformOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
        <el-input v-model="item.noteZh" :placeholder="t('admin.software.platformNoteZh')" />
        <el-input v-model="item.noteEn" :placeholder="t('admin.software.platformNoteEn')" />
        <el-button plain type="danger" @click="removeAt(form.platforms, index)">
          {{ t('admin.software.remove') }}
        </el-button>
      </div>
    </div>

    <!-- ---------- 内容块：仓库（D101） ---------- -->
    <div class="panel">
      <div class="panel__head">
        <div>
          <h2 class="panel__title">{{ t('admin.software.reposTitle') }}</h2>
          <p class="panel__lead">{{ t('admin.software.reposLead') }}</p>
        </div>
        <el-button plain @click="addRepo">{{ t('admin.software.addRepo') }}</el-button>
      </div>

      <div v-for="(item, index) in form.repos" :key="`repo-${index}`" class="row">
        <el-input
          v-model="item.labelZh"
          :placeholder="t('admin.software.repoLabelZh')"
          class="row__label"
        />
        <el-input
          v-model="item.labelEn"
          :placeholder="t('admin.software.repoLabelEn')"
          class="row__label"
        />
        <el-input v-model="item.url" :placeholder="t('admin.software.repoUrl')" />
        <el-select v-model="item.repoType" class="row__select">
          <el-option
            v-for="option in repoTypes"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
        <el-button plain type="danger" @click="removeAt(form.repos, index)">
          {{ t('admin.software.remove') }}
        </el-button>
      </div>
    </div>

    <!-- ---------- 第二段：版本与安装包 ---------- -->
    <div class="panel">
      <div class="panel__head">
        <div>
          <h2 class="panel__title">{{ t('admin.software.releasesTitle') }}</h2>
          <p class="panel__lead">{{ t('admin.software.releasesLead') }}</p>
        </div>
        <el-button v-if="!isNew" type="primary" plain @click="openReleaseDialog(null)">
          {{ t('admin.software.addRelease') }}
        </el-button>
      </div>

      <p v-if="isNew" class="panel__hint">{{ t('admin.software.releaseNeedsSave') }}</p>

      <template v-else>
        <p v-if="!releases.length" class="panel__hint">{{ t('admin.software.noReleases') }}</p>

        <div v-for="release in releases" :key="release.id" class="release">
          <div class="release__head">
            <div class="release__meta">
              <span class="release__version">{{ release.version }}</span>
              <el-tag size="small" disable-transitions>{{ release.channel }}</el-tag>
              <el-tag
                size="small"
                :type="release.status === 1 ? 'success' : 'info'"
                disable-transitions
              >
                {{
                  release.status === 1
                    ? t('admin.software.releasePublished')
                    : t('admin.software.releaseDraft')
                }}
              </el-tag>
              <span class="release__time">{{ formatTime(release.publishedAt) }}</span>
            </div>
            <div class="release__actions">
              <el-button size="small" plain @click="openReleaseDialog(release)">
                {{ t('admin.software.editRelease') }}
              </el-button>
              <el-button size="small" plain type="danger" @click="handleDeleteRelease(release)">
                {{ t('admin.software.deleteRelease') }}
              </el-button>
            </div>
          </div>

          <p v-if="release.notesZh || release.notesEn" class="release__notes">
            {{ release.notesZh || release.notesEn }}
          </p>

          <div class="assets">
            <div class="assets__head">
              <span class="assets__title">{{ t('admin.software.assetsTitle') }}</span>

              <div class="assets__upload">
                <el-select
                  v-model="draftOf(release.id).platform"
                  size="small"
                  class="assets__select"
                >
                  <el-option
                    v-for="option in platformOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
                <el-select v-model="draftOf(release.id).arch" size="small" class="assets__select">
                  <el-option
                    v-for="option in archOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
                <el-select
                  v-model="draftOf(release.id).packageType"
                  size="small"
                  class="assets__select"
                >
                  <el-option
                    v-for="option in packageTypeOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>

                <el-upload
                  :show-file-list="false"
                  :http-request="(options) => handleAssetUpload(release, options)"
                  :disabled="draftOf(release.id).uploading"
                >
                  <el-button size="small" type="primary" :loading="draftOf(release.id).uploading">
                    {{ t('admin.software.assetPick') }}
                  </el-button>
                </el-upload>
              </div>
            </div>

            <p class="assets__hint">{{ t('admin.software.assetFormatHint') }}</p>

            <el-progress
              v-if="draftOf(release.id).uploading"
              :percentage="draftOf(release.id).percent"
              :stroke-width="6"
            />

            <el-table
              v-if="release.assets?.length"
              :data="release.assets"
              size="small"
              border
              class="assets__table"
            >
              <el-table-column :label="t('admin.software.assetFileName')" min-width="220">
                <template #default="{ row }">
                  <code class="code-cell">{{ row.fileName }}</code>
                </template>
              </el-table-column>
              <el-table-column :label="t('admin.software.assetPlatform')" width="100">
                <template #default="{ row }">{{ row.platform }}</template>
              </el-table-column>
              <el-table-column :label="t('admin.software.assetArch')" width="90">
                <template #default="{ row }">{{ row.arch }}</template>
              </el-table-column>
              <el-table-column :label="t('admin.software.assetPackageType')" width="110">
                <template #default="{ row }">{{ row.packageType }}</template>
              </el-table-column>
              <el-table-column :label="t('admin.software.assetSize')" width="110">
                <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
              </el-table-column>
              <el-table-column :label="t('admin.software.assetSha')" min-width="200">
                <template #default="{ row }">
                  <code class="code-cell code-cell--sha">{{
                    (row.sha256 || '').slice(0, 16)
                  }}</code>
                </template>
              </el-table-column>
              <el-table-column
                :label="t('admin.software.assetDownloads')"
                width="100"
                align="center"
              >
                <template #default="{ row }">{{ row.downloadCount ?? 0 }}</template>
              </el-table-column>
              <el-table-column :label="t('admin.software.colActions')" width="110" align="center">
                <template #default="{ row }">
                  <el-button
                    size="small"
                    plain
                    type="danger"
                    @click="handleDeleteAsset(release, row)"
                  >
                    {{ t('admin.software.assetDelete') }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <p v-else class="assets__hint">{{ t('admin.software.noAssets') }}</p>
          </div>
        </div>
      </template>
    </div>

    <!-- ---------- 版本弹层 ---------- -->
    <el-dialog
      v-model="releaseDialog"
      :title="releaseEditingId ? t('admin.software.editRelease') : t('admin.software.addRelease')"
    >
      <div class="dialog">
        <label class="field">
          <span class="field__label">{{ t('admin.software.releaseVersion') }}</span>
          <el-input v-model="releaseForm.version" placeholder="26.2.1024" />
        </label>
        <label class="field">
          <span class="field__label">{{ t('admin.software.releaseChannel') }}</span>
          <el-select v-model="releaseForm.channel">
            <el-option label="stable" value="stable" />
            <el-option label="beta" value="beta" />
          </el-select>
        </label>
        <label class="field">
          <span class="field__label">{{ t('admin.software.releaseStatus') }}</span>
          <el-select v-model="releaseForm.status">
            <el-option :label="t('admin.software.releaseDraft')" :value="0" />
            <el-option :label="t('admin.software.releasePublished')" :value="1" />
          </el-select>
        </label>
        <label class="field">
          <span class="field__label">{{ t('admin.software.releasePublishedAt') }}</span>
          <el-date-picker
            v-model="releaseForm.publishedAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
          <span class="field__hint">{{ t('admin.software.releasePublishedAtHint') }}</span>
        </label>
        <label class="field field--wide">
          <span class="field__label">{{ t('admin.software.releaseNotesZh') }}</span>
          <el-input v-model="releaseForm.notesZh" type="textarea" :rows="3" />
        </label>
        <label class="field field--wide">
          <span class="field__label">{{ t('admin.software.releaseNotesEn') }}</span>
          <el-input v-model="releaseForm.notesEn" type="textarea" :rows="3" />
        </label>
      </div>

      <template #footer>
        <el-button plain @click="releaseDialog = false">
          {{ t('admin.software.cancel') }}
        </el-button>
        <el-button type="primary" :loading="releaseSaving" @click="saveRelease">
          {{ t('admin.software.releaseSave') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.editor {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.editor__bar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.panel {
  padding: 24px;
  border-radius: var(--adm-radius-block);
  background: var(--adm-surface);
  box-shadow: var(--adm-shadow-soft);
}

.panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 8px;
}

.panel__title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 600;
  color: var(--adm-ink);
}

.panel__lead {
  margin: 0;
  max-width: 760px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--adm-ink-soft);
}

.panel__hint {
  margin: 12px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--adm-ink-faint);
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field--wide {
  grid-column: 1 / -1;
}

.field__label {
  font-size: 13px;
  font-weight: 600;
  color: var(--adm-ink-soft);
}

.field__hint {
  font-size: 12px;
  color: var(--adm-ink-faint);
}

.cover {
  display: flex;
  gap: 12px;
}

.cover__preview {
  margin-top: 10px;
  max-height: 120px;
  border-radius: var(--adm-radius-block);
  border: 1px solid var(--adm-surface-head);
  object-fit: contain;
}

/* 一行 = 一个子项。带描边才看得出「这几格属于同一条」，
   否则一屏十几格输入框会长成一堵墙，改第 3 条特性时很容易串行。 */
.row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
  padding: 12px 14px;
  border: 1px solid var(--adm-surface-head);
  border-radius: var(--adm-radius-block);
  background: var(--adm-surface-soft);
}

/* 行内输入按 220px 基准平分：不写这条，每个 el-input 会各自按内容宽度抽风，
   同一列的两条记录宽度都不一样。el-input 是子组件，scoped 的类名仍能命中共根节点。 */
.row > .el-input:not(.row__icon):not(.row__label) {
  flex: 1 1 220px;
  min-width: 0;
}

/* 删除按钮甩到行末，掉行时也贴右，不吊在最后一格输入框下面 */
.row > .el-button {
  flex: none;
  margin-left: auto;
}

.row__icon {
  width: 90px;
  flex: none;
}

.row__label {
  width: 150px;
  flex: none;
}

.row__select {
  width: 140px;
  flex: none;
}

.row__url {
  display: flex;
  gap: 8px;
  flex: 1 1 320px;
}

.release {
  margin-top: 16px;
  padding: 16px;
  border: 1px solid var(--adm-surface-head);
  border-radius: var(--adm-radius-block);
  background: var(--adm-surface-soft);
}

.release__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.release__meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.release__version {
  font-family: var(--font-mono);
  font-size: 16px;
  font-weight: 700;
  color: var(--adm-ink);
}

.release__time {
  font-size: 12px;
  color: var(--adm-ink-faint);
}

.release__actions {
  display: flex;
  gap: 8px;
}

.release__notes {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--adm-ink-soft);
  white-space: pre-wrap;
}

.assets {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed var(--adm-surface-head);
}

.assets__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.assets__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--adm-ink);
}

.assets__upload {
  display: flex;
  align-items: center;
  gap: 8px;
}

.assets__select {
  width: 130px;
}

.assets__hint {
  margin: 8px 0;
  font-size: 12px;
  color: var(--adm-ink-faint);
}

.assets__table {
  margin-top: 8px;
}

.assets__table :deep(th.el-table__cell) {
  background: var(--adm-surface-head);
  color: var(--adm-ink-soft);
  font-weight: 600;
}

.code-cell {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--adm-ink-soft);
  word-break: break-all;
}

.code-cell--sha {
  color: var(--adm-ink-faint);
}

.dialog {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}
</style>
