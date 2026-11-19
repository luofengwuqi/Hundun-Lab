<script setup>
/**
 * 软件列表（F12，视觉沿用已复刻的 15-hundun 后台壳，D094）。
 *
 * 口径来自后端现状：
 * - 列表**含草稿**（后台视角），新建的软件默认 status=0，前台看不见；
 * - 上下线是独立接口（PUT /status），不是「编辑里勾一下」——所以这里单独给按钮；
 * - 删除会连带删版本与 OSS 上的安装包（D103 先删对象再删库），
 *   所以确认框必须说清「不可恢复」，不能只说「确定删除吗」。
 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import { changeSoftwareStatus, deleteSoftware, fetchSoftwareList } from '@/api/softwareAdmin'

const { t, te } = useI18n()
const router = useRouter()

const rows = ref([])
const loading = ref(false)
const keyword = ref('')
const status = ref(null)
/** 正在处理的行，避免连点两次上下线 */
const busyId = ref(null)

const statusOptions = computed(() => [
  { value: null, label: t('admin.software.statusAll') },
  { value: 0, label: t('admin.software.statusDraft') },
  { value: 1, label: t('admin.software.statusPublished') },
])

function formatTime(value) {
  if (!value) return t('admin.software.notAvailable')
  return String(value).slice(0, 19).replace('T', ' ')
}

function errorMessage(error, fallback = 'admin.software.opFailed') {
  const key = `errors.${error?.errorCode}`
  return error?.errorCode && te(key) ? t(key) : t(fallback)
}

async function load() {
  loading.value = true
  try {
    const data = await fetchSoftwareList({ keyword: keyword.value, status: status.value })
    rows.value = Array.isArray(data) ? data : []
  } catch (error) {
    ElMessage.error(errorMessage(error, 'admin.software.loadFailed'))
    rows.value = []
  } finally {
    loading.value = false
  }
}

function reset() {
  keyword.value = ''
  status.value = null
  load()
}

async function toggleStatus(row) {
  if (busyId.value) return
  const next = row.status === 1 ? 0 : 1
  busyId.value = row.id
  try {
    await changeSoftwareStatus(row.id, next)
    ElMessage.success(t(next === 1 ? 'admin.software.published' : 'admin.software.unpublished'))
    await load()
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    busyId.value = null
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      t('admin.software.confirmDeleteBody', { name: row.nameZh || row.slug }),
      t('admin.software.confirmDeleteTitle'),
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
    await deleteSoftware(row.id)
    ElMessage.success(t('admin.software.deleted'))
    await load()
  } catch (error) {
    ElMessage.error(errorMessage(error))
  }
}

function openEditor(row) {
  router.push({ name: 'admin-software-edit', params: { id: row.id } })
}

onMounted(load)
</script>

<template>
  <div class="software">
    <div class="panel">
      <div class="panel__head">
        <div>
          <h2 class="panel__title">{{ t('admin.software.listTitle') }}</h2>
          <p class="panel__lead">{{ t('admin.software.listLead') }}</p>
        </div>
        <el-button
          type="primary"
          size="large"
          class="panel__cta"
          @click="router.push({ name: 'admin-software-new' })"
        >
          {{ t('admin.software.newButton') }}
        </el-button>
      </div>

      <div class="filters">
        <el-input
          v-model="keyword"
          :placeholder="t('admin.software.keywordPlaceholder')"
          clearable
          class="filters__input"
          @keyup.enter="load"
        />
        <el-select v-model="status" class="filters__select">
          <el-option
            v-for="option in statusOptions"
            :key="String(option.value)"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
        <el-button type="primary" plain @click="load">
          {{ t('admin.software.search') }}
        </el-button>
        <el-button plain @click="reset">{{ t('admin.software.reset') }}</el-button>
      </div>
    </div>

    <el-table
      v-loading="loading"
      :data="rows"
      :empty-text="t('admin.software.empty')"
      class="software-table"
      border
    >
      <el-table-column :label="t('admin.software.colName')" min-width="220">
        <template #default="{ row }">
          <div class="name-cell">
            <span class="name-cell__zh">{{ row.nameZh }}</span>
            <span class="name-cell__en">{{ row.nameEn }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column :label="t('admin.software.colSlug')" min-width="160">
        <template #default="{ row }">
          <code class="code-cell">{{ row.slug }}</code>
        </template>
      </el-table-column>
      <el-table-column :label="t('admin.software.colStatus')" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" disable-transitions>
            {{
              row.status === 1
                ? t('admin.software.statusPublished')
                : t('admin.software.statusDraft')
            }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('admin.software.colLatest')" width="140" align="center">
        <template #default="{ row }">{{
          row.latestVersion || t('admin.software.notAvailable')
        }}</template>
      </el-table-column>
      <el-table-column :label="t('admin.software.colReleases')" width="100" align="center">
        <template #default="{ row }">{{ row.releaseCount ?? 0 }}</template>
      </el-table-column>
      <el-table-column :label="t('admin.software.colUpdated')" min-width="180">
        <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column
        :label="t('admin.software.colActions')"
        width="260"
        align="center"
        fixed="right"
      >
        <template #default="{ row }">
          <el-button type="primary" size="small" plain @click="openEditor(row)">
            {{ t('admin.software.edit') }}
          </el-button>
          <el-button size="small" plain :loading="busyId === row.id" @click="toggleStatus(row)">
            {{ row.status === 1 ? t('admin.software.unpublish') : t('admin.software.publish') }}
          </el-button>
          <el-button type="danger" size="small" plain @click="handleDelete(row)">
            {{ t('admin.software.delete') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.panel {
  margin-bottom: 20px;
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
}

.panel__title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 600;
  color: var(--adm-ink);
}

.panel__lead {
  margin: 0;
  max-width: 720px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--adm-ink-soft);
}

.panel__cta {
  flex: none;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 20px;
}

.filters__input {
  width: 240px;
}

.filters__select {
  width: 150px;
}

.software-table {
  border-radius: var(--adm-radius-block);
  overflow: hidden;
  background: var(--adm-surface);
  box-shadow: var(--adm-shadow-table);
}

.software-table :deep(th.el-table__cell) {
  background: var(--adm-surface-head);
  color: var(--adm-ink-soft);
  font-weight: 600;
}

.software-table :deep(td.el-table__cell) {
  background: var(--adm-surface);
  padding: 12px 0;
}

.software-table :deep(.el-button) {
  margin: 2px;
  min-width: 60px;
}

.name-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.name-cell__zh {
  font-size: 14px;
  font-weight: 600;
  color: var(--adm-ink);
}

.name-cell__en {
  font-size: 12px;
  color: var(--adm-ink-faint);
}

.code-cell {
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--adm-ink-soft);
  user-select: all;
}
</style>
