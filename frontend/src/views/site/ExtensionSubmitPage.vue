<script setup>
/**
 * F09 扩展提交与预检。
 *
 * 路由 /software/:slug/extensions/submit（URL 携带软件上下文，D075）。
 * M0 阶段提交接口未上线（M2 才有 /software/{slug}/extensions/submit），
 * 所以这一页做三件事：
 * 1. 完整呈现提交流程的 UI（上传要求 / 拖拽 / 预检结果 / 表单）；
 * 2. 把「礼貌预检」做真——大小 / zip 魔数 / manifest 存在 / id 合法，
 *    这是纯前端检查，M2 之后也一样在前端做（判定权在服务端，D051）；
 * 3. 提交按钮在预检通过前置灰，通过后点击给出「M2 开放」的明确提示，不假装成功。
 */
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useSessionStore } from '@/stores/session'
import FormField from '@/components/FormField.vue'

const props = defineProps({
  slug: { type: String, required: true },
})

const { t } = useI18n()
const session = useSessionStore()

const MAX_SIZE = 20 * 1024 * 1024 // D052：20MB，与服务端同一配置口径
const MAGIC = [0x50, 0x4b, 0x03, 0x04] // PK\x03\x04
const ID_PATTERN = /^[a-z][a-z0-9_-]{1,62}$/
/** manifest 文件名（PLUGIN_SPEC），zip 目录条目里按字节原样出现 */
const MANIFEST_NAME = 'plugin.json'

const file = ref(null)
const fileName = ref('')
const checks = ref([])
const precheckPassed = ref(false)
const submitted = ref(false)
const dragging = ref(false)

const form = reactive({
  name: '',
  version: '',
  requires: '',
  summaryZh: '',
  summaryEn: '',
  readme: '',
  color: '#c3272b',
  permissions: [],
})

const canSubmit = computed(() => precheckPassed.value && form.name.trim() && form.version.trim())

function setCheck(key, passed, detail = '') {
  const item = checks.value.find((c) => c.key === key)
  if (item) {
    item.passed = passed
    item.detail = detail
  }
}

async function inspect(fileObj) {
  checks.value = [
    { key: 'size', passed: false, detail: '' },
    { key: 'magic', passed: false, detail: '' },
    { key: 'manifest', passed: false, detail: '' },
    { key: 'id', passed: false, detail: '' },
  ]
  precheckPassed.value = false
  submitted.value = false

  // 1. 大小 ≤ 20MB（D052）
  setCheck('size', fileObj.size <= MAX_SIZE, formatSize(fileObj.size))

  const buffer = new Uint8Array(await fileObj.arrayBuffer())

  // 2. zip 魔数 PK\x03\x04
  setCheck(
    'magic',
    MAGIC.every((byte, i) => buffer[i] === byte),
    'PK\\x03\\x04',
  )

  // 3/4. 读 zip 中央目录找 manifest；能解出内容就顺手校验 id
  try {
    const entry = await findEntry(buffer, MANIFEST_NAME)
    if (!entry) {
      setCheck('manifest', false, t('extSubmit.manifestMissing'))
      return
    }
    setCheck('manifest', true, MANIFEST_NAME)
    const text = await entry.text()
    const manifest = JSON.parse(text)
    applyManifest(manifest)
    const legal = ID_PATTERN.test(String(manifest.id || ''))
    setCheck('id', legal, String(manifest.id || '—'))
    precheckPassed.value = checks.value.every((c) => c.passed)
  } catch {
    // 目录解不开（加密 / 损坏 / 非 zip 结构）：id 这一项交给服务端判定
    setCheck('id', false, t('extSubmit.serverJudged'))
  }
}

/** 从 zip 中央目录定位条目；解压走 DecompressionStream（deflate-raw），不支持时只报存在性 */
async function findEntry(buffer, name) {
  const view = new DataView(buffer.buffer, buffer.byteOffset, buffer.byteLength)
  const text = new TextDecoder('latin1').decode(buffer)

  // 从尾部找 EOCD（0x06054b50）；注释最长 65535
  let eocd = -1
  for (let i = buffer.length - 22; i >= Math.max(0, buffer.length - 22 - 65535); i--) {
    if (view.getUint32(i, true) === 0x06054b50) {
      eocd = i
      break
    }
  }
  if (eocd < 0) throw new Error('no EOCD')

  const count = view.getUint16(eocd + 10, true)
  let offset = view.getUint32(eocd + 16, true)

  for (let i = 0; i < count; i++) {
    if (view.getUint32(offset, true) !== 0x02014b50) throw new Error('bad central directory')
    const method = view.getUint16(offset + 10, true)
    const compressedSize = view.getUint32(offset + 20, true)
    const nameLen = view.getUint16(offset + 28, true)
    const extraLen = view.getUint16(offset + 30, true)
    const commentLen = view.getUint16(offset + 32, true)
    const localOffset = view.getUint32(offset + 42, true)
    const entryName = text.slice(offset + 46, offset + 46 + nameLen)

    if (entryName === name) {
      // 本地头（0x04034b50）里再跳过一次文件名 + extra，才是数据区
      const localNameLen = view.getUint16(localOffset + 26, true)
      const localExtraLen = view.getUint16(localOffset + 28, true)
      const dataStart = localOffset + 30 + localNameLen + localExtraLen
      const data = buffer.subarray(dataStart, dataStart + compressedSize)
      if (method === 0) {
        return { text: () => Promise.resolve(new TextDecoder().decode(data)) }
      }
      if (method === 8 && typeof DecompressionStream === 'function') {
        const stream = new Blob([data]).stream().pipeThrough(new DecompressionStream('deflate-raw'))
        return { text: () => new Response(stream).text() }
      }
      // 压缩且浏览器不支持解压：只确认 manifest 存在，内容校验交给服务端
      return { text: () => Promise.reject(new Error('unsupported compression')) }
    }
    offset += 46 + nameLen + extraLen + commentLen
  }
  return null
}

function applyManifest(manifest) {
  if (manifest?.name && !form.name) form.name = String(manifest.name)
  if (manifest?.version && !form.version) form.version = String(manifest.version)
  if (Array.isArray(manifest?.permissions)) form.permissions = manifest.permissions.map(String)
}

function onPick(event) {
  const fileObj = event.target.files?.[0]
  if (!fileObj) return
  accept(fileObj)
}

function onDrop(event) {
  dragging.value = false
  const fileObj = event.dataTransfer?.files?.[0]
  if (fileObj) accept(fileObj)
}

function accept(fileObj) {
  file.value = fileObj
  fileName.value = `${fileObj.name} · ${formatSize(fileObj.size)}`
  inspect(fileObj)
}

function formatSize(bytes) {
  if (bytes >= 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`
  return `${Math.max(1, Math.round(bytes / 1024))} KB`
}

function submit() {
  if (!canSubmit.value || submitted.value) return
  // M2 才有提交接口；这里明确告知，不假装提交成功
  submitted.value = true
}
</script>

<template>
  <div class="submit-page">
    <div class="container">
      <header class="submit-head">
        <h1 class="submit-head__title">
          {{ t('extSubmit.title') }}<span class="submit-head__bullet" aria-hidden="true"></span>
        </h1>
        <p class="submit-head__sub">{{ t('extSubmit.lead') }}</p>
      </header>

      <div v-if="!session.isLoggedIn" class="card empty">
        <p>{{ t('extSubmit.needLogin') }}</p>
        <RouterLink
          :to="{ name: 'login', query: { redirect: `/software/${slug}/extensions/submit` } }"
          class="btn btn--primary btn--sm"
        >
          {{ t('nav.login') }}
        </RouterLink>
      </div>

      <div v-else class="submit-grid">
        <!-- 左列：上传要求 + manifest 示例 -->
        <div class="submit-side">
          <div class="card side">
            <h3 class="side__title">{{ t('extSubmit.rules') }}</h3>
            <ul class="rules">
              <li>{{ t('extSubmit.ruleSize') }}</li>
              <li>{{ t('extSubmit.ruleManifest') }}</li>
              <li>{{ t('extSubmit.ruleIcon') }}</li>
              <li>{{ t('extSubmit.rulePermissions') }}</li>
              <li>{{ t('extSubmit.ruleOffline') }}</li>
            </ul>
          </div>
          <div class="card side">
            <h3 class="side__title">{{ t('extSubmit.manifestExample') }}</h3>
            <pre class="mono code">{{
              JSON.stringify(
                {
                  id: 'pixelruler',
                  name: 'PixelRuler',
                  version: '1.2.0',
                  entry: 'index.js',
                  permissions: ['screen'],
                },
                null,
                2,
              )
            }}</pre>
          </div>
        </div>

        <!-- 右列：拖拽 + 预检 + 表单 -->
        <div class="submit-main">
          <div
            class="dropzone"
            :class="{ 'is-drag': dragging }"
            role="button"
            tabindex="0"
            :aria-label="t('extSubmit.dropzone')"
            @click="$refs.fileInput.click()"
            @keydown.enter.prevent="$refs.fileInput.click()"
            @dragover.prevent="dragging = true"
            @dragleave.prevent="dragging = false"
            @drop.prevent="onDrop"
          >
            <input
              ref="fileInput"
              type="file"
              accept=".zip"
              class="dropzone__input"
              @change="onPick"
            />
            <p class="dropzone__main">{{ t('extSubmit.dropzone') }}</p>
            <p v-if="fileName" class="mono dropzone__file">{{ fileName }}</p>
          </div>

          <div v-if="checks.length" class="card side">
            <div class="checks">
              <p v-for="check in checks" :key="check.key" class="checks__row">
                <span class="checks__mark" :class="{ 'is-pass': check.passed }" aria-hidden="true">
                  {{ check.passed ? '✓' : '×' }}
                </span>
                <span>{{ t(`extSubmit.check.${check.key}`) }}</span>
                <span class="mono checks__detail">{{ check.detail }}</span>
              </p>
            </div>
            <p class="mono muted checks__note">{{ t('extSubmit.serverJudgedNote') }}</p>
          </div>

          <div class="card side">
            <h3 class="side__title">{{ t('extSubmit.formTitle') }}</h3>
            <div class="form-row3">
              <FormField v-model="form.name" :label="t('extSubmit.name')" />
              <FormField v-model="form.version" :label="t('extSubmit.versionNo')" />
              <FormField v-model="form.requires" :label="t('extSubmit.requires')" />
            </div>
            <div class="form-stack">
              <FormField v-model="form.summaryZh" :label="t('extSubmit.summaryZh')" />
              <FormField v-model="form.summaryEn" :label="t('extSubmit.summaryEn')" />
            </div>
            <div class="form-stack">
              <FormField v-model="form.readme" :label="t('extSubmit.readme')" />
            </div>
            <div class="form-stack">
              <span class="form-label">{{ t('extSubmit.permissions') }}</span>
              <p v-if="form.permissions.length" class="mono perms">
                <span v-for="perm in form.permissions" :key="perm" class="perms__item">
                  {{ perm }}
                </span>
              </p>
              <p v-else class="muted form-hint">{{ t('extSubmit.noPermissions') }}</p>
            </div>

            <div v-if="submitted" class="notice notice--ok" role="status">
              {{ t('extSubmit.pending') }}
            </div>

            <div class="form-actions">
              <button type="button" class="btn btn--primary" :disabled="!canSubmit" @click="submit">
                {{ t('extSubmit.submitBtn') }}
              </button>
              <RouterLink
                :to="{ name: 'software-detail', params: { slug }, hash: '#extensions' }"
                class="btn btn--ghost"
              >
                {{ t('extSubmit.back') }}
              </RouterLink>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.submit-page {
  padding: 24px 0 80px;
}

.submit-head {
  padding-bottom: 18px;
}

.submit-head__title {
  font-size: clamp(26px, 4vw, 34px);
  display: flex;
  align-items: center;
  gap: 12px;
}

.submit-head__bullet {
  width: 12px;
  height: 12px;
  background: var(--red);
  box-shadow: 0 0 0 2px var(--ink);
  flex: none;
}

.submit-head__sub {
  margin-top: 8px;
  color: var(--ink-soft);
}

.empty {
  margin-top: 24px;
  padding: 40px;
  display: grid;
  justify-items: start;
  gap: 14px;
}

.submit-grid {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 26px;
  align-items: start;
}

.submit-side,
.submit-main {
  display: grid;
  gap: 18px;
}

.side {
  padding: 18px 20px;
}

.side__title {
  font-size: 16px;
  margin-bottom: 12px;
}

.rules {
  display: grid;
  gap: 8px;
  font-size: 13px;
  color: var(--ink-soft);
}

.rules li {
  padding-left: 16px;
  position: relative;
}

.rules li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0.55em;
  width: 7px;
  height: 7px;
  background: var(--red);
}

.code {
  margin: 0;
  padding: 12px;
  background: var(--field);
  border: 1.5px solid var(--line-soft);
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}

.dropzone {
  position: relative;
  padding: 34px 24px;
  border: 2px dashed var(--red);
  text-align: center;
  cursor: pointer;
  background: color-mix(in srgb, var(--red) 4%, transparent);
}

.dropzone.is-drag,
.dropzone:hover {
  background: color-mix(in srgb, var(--red) 8%, transparent);
}

.dropzone__input {
  /* 输入框本体不可见也不接收事件：点击与键盘都由外层 role=button 的容器代理 */
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

.dropzone__main {
  font-weight: 700;
  color: var(--accent-text);
}

.dropzone__file {
  margin-top: 8px;
  font-size: 12px;
  color: var(--ink-faint);
}

.checks {
  display: grid;
  gap: 8px;
}

.checks__row {
  display: flex;
  align-items: baseline;
  gap: 10px;
  font-size: 13px;
}

.checks__mark {
  flex: none;
  width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  align-self: center;
  border: 1.5px solid var(--tag-line);
  font-size: 11px;
  font-weight: 700;
  color: var(--ink-faint);
}

.checks__mark.is-pass {
  background: var(--teal);
  border-color: var(--teal);
  color: var(--on-teal);
}

.checks__detail {
  margin-left: auto;
  font-size: 11px;
  color: var(--ink-faint);
  word-break: break-all;
}

.checks__note {
  margin-top: 10px;
  font-size: 11px;
}

.form-row3 {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.form-stack {
  display: grid;
  gap: 14px;
  margin-top: 14px;
}

.form-label {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--ink-soft);
}

.form-hint {
  font-size: 12px;
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

.notice {
  margin-top: 16px;
  font-size: 13px;
}

.notice--ok {
  color: var(--teal-text);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 18px;
}

@media (max-width: 900px) {
  .submit-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .form-row3 {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
