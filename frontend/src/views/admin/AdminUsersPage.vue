<script setup>
/**
 * 用户管理（视觉复刻 15-hundun 的 User.vue，逻辑用本仓的，D094）。
 *
 * 复刻的部分（因为用户要「完全复刻」）：
 * - 搜索栏是米色圆角卡片，inline 表单；
 * - el-table 带边框、表头深米色、行米色、超管那行浅蓝底；
 * - 操作列三个 plain 小按钮，动作随状态翻转；
 * - el-pagination 带 total/sizes/jumper，靠右；
 * - 「修改」不是弹层，而是滑入的详情页：字段点一下就地变输入框，
 *   改完不立刻落库，返回列表时一次性提交（原项目这点做得比我上一版好——
 *   它是「批量暂存 + 一次提交」，不是「每改一格就写库」，所以不脏）。
 *
 * 保留的本仓逻辑：
 * - 真接口（/admin/user/page、PUT /admin/user、PUT /admin/user/status、DELETE /admin/user/{id}）；
 * - USER_NOT_FOUND(404) 是「没查到」不是「出错」，当空列表；
 * - toggle 接口是取反语义，返回值就是改完的状态；
 * - 层级软拦与后端 canManage 同一套：超管动 1/-1/2/-2，管理员只动 1/-1，谁都不动 3，也都不动自己。
 *
 * 唯一没照搬的：原项目允许改用户 ID（它自己都标了红字「很危险」）。我们的
 * PUT /admin/user 虽然收了 id 字段，但改主键会让关联表错位，这里只读展示。
 */
import { computed, onMounted, reactive, ref, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import { fetchUsers, updateUser, toggleUserStatus, deleteUser } from '@/api/admin'
import { useSessionStore } from '@/stores/session'

const { t } = useI18n()
const session = useSessionStore()

const PAGE_SIZES = [10, 20, 50, 100]
const STATUS_VALUES = [1, -1, 2, -2, 3]

/** 状态到文案与 el-tag 类型的映射，照原项目的 statusMap */
const STATUS_META = {
  1: { type: 'success', key: 'admin.users.statusUser' },
  '-1': { type: 'danger', key: 'admin.users.statusUserBanned' },
  2: { type: 'warning', key: 'admin.users.statusAdmin' },
  '-2': { type: 'danger', key: 'admin.users.statusAdminBanned' },
  3: { type: 'primary', key: 'admin.users.statusSuper' },
}

const myStatus = computed(() => Number(session.user?.status ?? 0))

/* ---------------- 列表 ---------------- */

const searchForm = reactive({ name: '', status: null })
const pagination = reactive({ page: 1, size: 10, total: 0 })
const tableData = ref([])
const loading = ref(false)

/** 表格整体重挂的 key，用来重放逐行淡入动画（照原项目） */
const tableKey = computed(() => `${pagination.page}-${pagination.size}`)

function statusType(status) {
  return STATUS_META[String(status)]?.type || 'info'
}

function statusText(status) {
  const meta = STATUS_META[String(status)]
  return meta ? t(meta.key) : t('admin.users.statusUnknown')
}

/** 超管那行给个高亮，一眼能看出谁在这一层之上 */
function rowClassName({ row }) {
  return row.status === 3 ? 'super-admin-row' : ''
}

/**
 * 能不能对这个账号动手：与后端 canManage 同一套规则，外加「不能动自己」。
 * 视觉上不把按钮置灰（复刻要求按钮组完整可见），改成点了给一句为什么。
 */
function canManage(row) {
  if (!row) return false
  if (row.id === session.user?.id) return false
  if (row.status === 3) return false
  if (myStatus.value >= 3) return true
  return myStatus.value === 2 && (row.status === 1 || row.status === -1)
}

function denyReason(row) {
  if (row?.id === session.user?.id) return t('admin.users.restrictedSelf')
  if (row?.status === 3) return t('admin.users.restrictedSuper')
  return t('admin.users.restricted')
}

function errorMessage(error, fallback = 'admin.users.opFailed') {
  const key = `errors.${error?.errorCode}`
  return error?.errorCode && t(key) !== key ? t(key) : t(fallback)
}

async function load() {
  loading.value = true
  try {
    const data = await fetchUsers({
      page: pagination.page,
      size: pagination.size,
      name: searchForm.name.trim(),
      status: searchForm.status === null ? null : Number(searchForm.status),
    })
    tableData.value = data?.list || []
    pagination.total = Number(data?.total || 0)
  } catch (error) {
    // 后端对「查不到」抛 USER_NOT_FOUND，语义上就是空列表，不是故障
    if (error?.errorCode !== 'USER_NOT_FOUND') {
      ElMessage.error(errorMessage(error))
    }
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  load()
}

function handleReset() {
  searchForm.name = ''
  searchForm.status = null
  pagination.page = 1
  load()
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  load()
}

function handlePageChange(page) {
  pagination.page = page
  load()
}

/* ---------------- 启禁用 / 删除 ---------------- */

async function handleChangeStatus(row) {
  if (!canManage(row)) {
    ElMessage.warning(denyReason(row))
    return
  }
  const banning = row.status > 0
  try {
    await ElMessageBox.confirm(
      banning
        ? t('admin.users.confirmBan', { name: row.username })
        : t('admin.users.confirmUnban', { name: row.username }),
      t('admin.users.confirmTitle'),
      {
        confirmButtonText: t('admin.users.confirmOk'),
        cancelButtonText: t('admin.users.cancel'),
        type: 'warning',
      },
    )
  } catch {
    return // 取消，什么都不做
  }

  try {
    const next = await toggleUserStatus(row.username)
    ElMessage.success(banning ? t('admin.users.banned') : t('admin.users.unbanned'))
    if (typeof next === 'number') row.status = next
    await load()
  } catch (error) {
    ElMessage.error(errorMessage(error))
  }
}

async function handleDelete(row) {
  if (!canManage(row)) {
    ElMessage.warning(denyReason(row))
    return
  }
  try {
    await ElMessageBox.confirm(
      t('admin.users.confirmDeleteBody', { name: row.username }),
      t('admin.users.confirmDeleteTitle'),
      {
        confirmButtonText: t('admin.users.confirmDelete'),
        cancelButtonText: t('admin.users.cancel'),
        type: 'warning',
      },
    )
  } catch {
    return
  }

  try {
    await deleteUser(row.id)
    ElMessage.success(t('admin.users.deleted'))
    // 删掉当页最后一条时往回退一页，别停在空页上
    if (tableData.value.length === 1 && pagination.page > 1) pagination.page -= 1
    await load()
  } catch (error) {
    ElMessage.error(errorMessage(error))
  }
}

/* ---------------- 详情：点字段就地编辑，返回时一次提交 ---------------- */

const detailVisible = ref(false)
const currentUser = ref({})
const pending = ref({}) // { field: newValue }，还没落库的改动
const editingField = ref(null)
const editValue = ref('')
const savingDetail = ref(false)
const fieldRefs = reactive({})

/** 可编辑字段；id 只读（改主键会让关联表错位），时间字段也不给改 */
const EDITABLE = ['username', 'nickname', 'email', 'phone', 'password']

function openDetail(row) {
  currentUser.value = { ...row }
  pending.value = {}
  editingField.value = null
  editValue.value = ''
  detailVisible.value = true
}

function startEdit(field) {
  if (!canManage(currentUser.value)) {
    ElMessage.warning(denyReason(currentUser.value))
    return
  }
  if (!EDITABLE.includes(field)) return
  editingField.value = field
  // 密码框永远从空开始，不把旧值带出来（接口也不会给）
  editValue.value = field === 'password' ? '' : currentUser.value[field] || ''
  nextTick(() => fieldRefs[field]?.focus?.())
}

function sameValue(field, value) {
  const old = currentUser.value[field]
  if (field === 'password') return value === ''
  return (old ?? '') === (value ?? '')
}

/**
 * 失焦或回车时只记录，不落库。
 * 清空视为「取消这一项」而不是「把邮箱改成空」——后端对空串是「这一列不动」，
 * 前后端口径一致，所以清空等于没改。
 */
function commitField(field) {
  if (editingField.value !== field) return
  const value = (editValue.value ?? '').trim()

  if (field === 'password' && value && (value.length < 6 || value.length > 20)) {
    ElMessage.error(t('admin.users.invalidPassword'))
    return
  }

  if (sameValue(field, value)) {
    resetEdit()
    return
  }

  if (value === '') {
    resetEdit()
    ElMessage.info(t('admin.users.clearIgnored'))
    return
  }

  if (field !== 'password') currentUser.value[field] = value
  pending.value[field] = value
  resetEdit()
  ElMessage.info(t('admin.users.changeRecorded'))
}

function resetEdit() {
  editingField.value = null
  editValue.value = ''
}

/** 返回列表：把暂存的改动一次性提交，失败就留在详情页让人重试 */
async function backToList() {
  const fields = Object.keys(pending.value)
  if (savingDetail.value) return

  if (!fields.length) {
    closeDetail()
    return
  }

  savingDetail.value = true
  try {
    const payload = { oldId: currentUser.value.id }
    for (const field of fields) payload[field] = pending.value[field]
    await updateUser(payload)
    ElMessage.success(t('admin.users.savedCount', { n: fields.length }))
    pending.value = {}
    closeDetail()
    await load()
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    savingDetail.value = false
  }
}

function closeDetail() {
  detailVisible.value = false
  currentUser.value = {}
  pending.value = {}
  resetEdit()
}

const pendingCount = computed(() => Object.keys(pending.value).length)

function formatTime(value) {
  if (!value) return t('admin.users.notRecorded')
  return String(value).slice(0, 19).replace('T', ' ')
}

function avatarText(user) {
  return (user?.nickname || user?.username || '?').slice(0, 1).toUpperCase()
}

onMounted(load)
</script>

<template>
  <div class="users">
    <!-- ============ 列表 ============ -->
    <div v-show="!detailVisible">
      <el-form :inline="true" :model="searchForm" class="search-form" @submit.prevent>
        <el-form-item :label="t('admin.users.searchName')">
          <el-input
            v-model="searchForm.name"
            :placeholder="t('admin.users.searchNamePlaceholder')"
            clearable
            @keydown.enter.prevent="handleSearch"
          />
        </el-form-item>
        <el-form-item :label="t('admin.users.searchStatus')">
          <el-select
            v-model="searchForm.status"
            :placeholder="t('admin.users.searchStatusPlaceholder')"
            clearable
            class="status-select"
            @change="handleSearch"
          >
            <el-option
              v-for="value in STATUS_VALUES"
              :key="value"
              :label="statusText(value)"
              :value="value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            {{ t('admin.users.search') }}
          </el-button>
          <el-button @click="handleReset">{{ t('admin.users.reset') }}</el-button>
        </el-form-item>
      </el-form>

      <transition-group name="table-list" tag="div">
        <el-table
          :key="tableKey"
          v-loading="loading"
          :data="tableData"
          :row-class-name="rowClassName"
          :empty-text="t('admin.users.empty')"
          class="user-table"
          border
        >
          <el-table-column
            prop="username"
            :label="t('admin.users.colUsername')"
            min-width="150"
            show-overflow-tooltip
          />
          <el-table-column
            prop="email"
            :label="t('admin.users.colEmail')"
            min-width="200"
            show-overflow-tooltip
          />
          <el-table-column prop="phone" :label="t('admin.users.colPhone')" min-width="130" />
          <el-table-column
            prop="status"
            :label="t('admin.users.colStatus')"
            width="130"
            align="center"
          >
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small" effect="light">
                {{ statusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            prop="lastLoginAt"
            :label="t('admin.users.colLastLogin')"
            min-width="170"
          >
            <template #default="{ row }">{{ formatTime(row.lastLoginAt) }}</template>
          </el-table-column>
          <el-table-column
            :label="t('admin.users.colActions')"
            fixed="right"
            width="230"
            align="center"
          >
            <template #default="{ row }">
              <el-button type="primary" size="small" plain @click="openDetail(row)">
                {{ t('admin.users.edit') }}
              </el-button>
              <el-button
                v-if="row.status < 0"
                type="success"
                size="small"
                plain
                @click="handleChangeStatus(row)"
              >
                {{ t('admin.users.unban') }}
              </el-button>
              <el-button v-else type="danger" size="small" plain @click="handleChangeStatus(row)">
                {{ t('admin.users.ban') }}
              </el-button>
              <el-button type="danger" size="small" plain @click="handleDelete(row)">
                {{ t('admin.users.remove') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </transition-group>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :page-sizes="PAGE_SIZES"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        background
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

    <!-- ============ 详情：点字段改，返回时一次提交 ============ -->
    <transition name="detail-fade">
      <div v-show="detailVisible" class="user-detail">
        <div class="detail-header">
          <el-button type="primary" :loading="savingDetail" @click="backToList">
            ← {{ t('admin.users.backAndSave') }}
          </el-button>
          <h2 class="detail-title">{{ t('admin.users.detailTitle') }}</h2>
          <p class="detail-warn">{{ t('admin.users.detailWarn') }}</p>
        </div>

        <el-card class="detail-card" shadow="hover">
          <div class="avatar-section">
            <el-avatar :size="120" class="user-avatar">{{ avatarText(currentUser) }}</el-avatar>
            <div class="basic-info">
              <h3 class="username">{{ currentUser.username }}</h3>
              <p v-if="currentUser.nickname" class="nickname">
                {{ t('admin.users.nicknameLine', { name: currentUser.nickname }) }}
              </p>
              <el-tag :type="statusType(currentUser.status)" size="large">
                {{ statusText(currentUser.status) }}
              </el-tag>
              <p v-if="pendingCount" class="pending-count">
                {{ t('admin.users.pendingCount', { n: pendingCount }) }}
              </p>
            </div>
          </div>

          <el-divider />

          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">{{ t('admin.users.fieldId') }}</span>
              <span class="info-value is-locked">{{ currentUser.id }}</span>
            </div>

            <div
              v-for="field in EDITABLE"
              :key="field"
              class="info-item is-editable"
              @click="startEdit(field)"
            >
              <span class="info-label">{{ t(`admin.users.field_${field}`) }}</span>
              <el-input
                v-if="editingField === field"
                :ref="(el) => (fieldRefs[field] = el)"
                v-model="editValue"
                size="small"
                class="edit-input"
                :type="field === 'password' ? 'password' : 'text'"
                :show-password="field === 'password'"
                :placeholder="
                  field === 'password'
                    ? t('admin.users.newPasswordHint')
                    : t('admin.users.emptyHint')
                "
                @click.stop
                @blur="commitField(field)"
                @keyup.enter="commitField(field)"
              />
              <template v-else>
                <span v-if="field === 'password'" class="info-value password-masked"> ****** </span>
                <span v-else class="info-value editable">
                  <template v-if="pending[field]">
                    {{ pending[field] }}
                    <em class="is-pending">{{ t('admin.users.pendingTag') }}</em>
                  </template>
                  <template v-else>{{ currentUser[field] || t('admin.users.notSet') }}</template>
                </span>
              </template>
            </div>

            <div class="info-item">
              <span class="info-label">{{ t('admin.users.fieldCreatedAt') }}</span>
              <span class="info-value">{{ formatTime(currentUser.createdAt) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">{{ t('admin.users.fieldUpdatedAt') }}</span>
              <span class="info-value">{{ formatTime(currentUser.updatedAt) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">{{ t('admin.users.fieldLastLoginAt') }}</span>
              <span class="info-value">{{ formatTime(currentUser.lastLoginAt) }}</span>
            </div>
          </div>
        </el-card>
      </div>
    </transition>
  </div>
</template>

<style scoped>
/* ---------- 搜索栏：米色卡片 ---------- */
.search-form {
  margin-bottom: 20px;
  padding: 24px 24px 4px;
  border-radius: var(--adm-radius-block);
  background: var(--adm-surface);
  box-shadow: var(--adm-shadow-soft);
}

.search-form :deep(.el-form-item__label) {
  color: var(--adm-ink-soft);
  font-weight: 600;
}

.status-select {
  min-width: 150px;
}

/* ---------- 表格 ---------- */
.user-table {
  border-radius: var(--adm-radius-block);
  overflow: hidden;
  background: var(--adm-surface);
  box-shadow: var(--adm-shadow-table);
}

.user-table :deep(th.el-table__cell) {
  background: var(--adm-surface-head);
  color: var(--adm-ink-soft);
  font-weight: 600;
}

.user-table :deep(td.el-table__cell) {
  background: var(--adm-surface);
  padding: 12px 0;
}

/* 超管那行：原项目给浅蓝底 + 蓝字，一眼认出层级 */
.user-table :deep(.super-admin-row td.el-table__cell) {
  background: var(--adm-row-super-bg);
}

.user-table :deep(.super-admin-row .cell) {
  color: var(--adm-row-super-ink);
  font-weight: 500;
}

.user-table :deep(.el-button) {
  margin: 2px;
  min-width: 60px;
}

.user-table :deep(.el-tag--small) {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
}

/* 逐行淡入：原项目给前 10 行各排一个 delay */
.user-table :deep(.el-table__body tr) {
  animation: row-fade-in 0.3s ease-in-out forwards;
  opacity: 0;
}

.user-table :deep(.el-table__body tr:nth-child(1)) {
  animation-delay: 0.05s;
}
.user-table :deep(.el-table__body tr:nth-child(2)) {
  animation-delay: 0.1s;
}
.user-table :deep(.el-table__body tr:nth-child(3)) {
  animation-delay: 0.15s;
}
.user-table :deep(.el-table__body tr:nth-child(4)) {
  animation-delay: 0.2s;
}
.user-table :deep(.el-table__body tr:nth-child(5)) {
  animation-delay: 0.25s;
}
.user-table :deep(.el-table__body tr:nth-child(6)) {
  animation-delay: 0.3s;
}
.user-table :deep(.el-table__body tr:nth-child(7)) {
  animation-delay: 0.35s;
}
.user-table :deep(.el-table__body tr:nth-child(8)) {
  animation-delay: 0.4s;
}
.user-table :deep(.el-table__body tr:nth-child(9)) {
  animation-delay: 0.45s;
}
.user-table :deep(.el-table__body tr:nth-child(10)) {
  animation-delay: 0.5s;
}

@keyframes row-fade-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.table-list-enter-active,
.table-list-leave-active {
  transition: all 0.4s ease;
}

.table-list-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

/* ---------- 分页 ---------- */
.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
  padding: 16px;
  border-radius: var(--adm-radius-block);
  background: var(--adm-surface);
}

.pagination :deep(.el-pager li.is-active) {
  border: 1px solid var(--adm-primary-block-border);
  background: var(--adm-primary-block) !important;
  color: var(--adm-primary-block-ink) !important;
}

.pagination :deep(.el-pager li:not(.is-active):hover) {
  color: var(--adm-primary) !important;
}

/* ---------- 详情页 ---------- */
.user-detail {
  padding: 20px;
}

.detail-header {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 24px;
}

.detail-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: var(--adm-ink);
}

.detail-warn {
  flex: 1;
  min-width: 240px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--adm-primary);
}

.detail-card {
  border-radius: 12px;
  background: var(--adm-surface);
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 30px;
  margin-bottom: 8px;
}

.user-avatar {
  border: 3px solid var(--adm-surface-head);
  background: var(--adm-surface-head);
  color: var(--adm-primary);
  font-size: 42px;
  font-weight: 700;
}

.basic-info {
  flex: 1;
}

.username {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 600;
  color: var(--adm-ink);
}

.nickname {
  margin: 0 0 12px;
  font-size: 16px;
  color: var(--adm-ink-soft);
}

.pending-count {
  margin-top: 10px;
  font-size: 13px;
  color: var(--adm-primary);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-top: 20px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px;
  border-radius: var(--adm-radius-block);
  background: var(--adm-surface-soft);
  transition: all 0.3s ease;
}

.info-item.is-editable {
  cursor: pointer;
}

.info-item.is-editable:hover {
  transform: translateY(-2px);
  background: var(--adm-surface-soft-hover);
  box-shadow: var(--adm-shadow-soft);
}

.info-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--adm-ink-faint);
}

.info-value {
  font-size: 16px;
  font-weight: 500;
  color: var(--adm-ink);
  word-break: break-all;
}

/* 只读字段（ID、三个时间）：不给手型，也不给 hover 反馈 */
.is-locked {
  color: var(--adm-ink-faint);
}

.info-value.editable {
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.info-item.is-editable:hover .info-value.editable {
  background: var(--adm-surface-soft-hover);
  color: var(--adm-primary);
}

.is-pending {
  margin-left: 6px;
  padding: 1px 6px;
  border-radius: 4px;
  background: var(--adm-surface-head);
  font-style: normal;
  font-size: 11px;
  color: var(--adm-primary);
}

.edit-input {
  width: 100%;
}

.edit-input :deep(.el-input__wrapper) {
  padding: 4px 12px;
  background: var(--adm-surface-soft-hover);
  box-shadow: 0 0 0 1px var(--adm-primary) inset;
}

.edit-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--adm-primary) inset;
}

.edit-input :deep(.el-input__inner) {
  font-size: 16px;
  font-weight: 500;
  color: var(--adm-ink);
}

.password-masked {
  font-family: var(--font-mono);
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 2px;
  color: var(--adm-primary);
}

.detail-fade-enter-active,
.detail-fade-leave-active {
  transition: all 0.4s ease;
}

.detail-fade-enter-from,
.detail-fade-leave-to {
  opacity: 0;
  transform: translateX(30px);
}

@media (max-width: 900px) {
  .info-grid {
    grid-template-columns: 1fr;
  }

  .avatar-section {
    gap: 16px;
  }

  .username {
    font-size: 22px;
  }
}
</style>
