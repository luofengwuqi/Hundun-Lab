<script setup>
/**
 * 邀请管理（视觉沿用本仓已复刻的 15-hundun 后台壳，D094）。
 *
 * 业务规则来自后端现状，不是照抄原型：
 * - 一枚码 = 一行 status=-2 的占位用户；「生成」就是插一行，「撤销」就是删这行。
 * - 注册管理员时这行会被原子消费（AdminServiceImpl#addAdmin 靠 DELETE 行数互斥），
 *   所以列表里的码都是「还没被用过」的；用完即从列表消失。
 * - 两个接口都只放 status=3；普通管理员直接敲地址进来会被后端 403，
 *   这里先按本地角色给一句说明，不假装能用（D036）。
 *
 * User.inviteCode 上是 @JsonIgnore，所以用户管理接口永远带不出码 ——
 * 列表走的是专门的 /admin/invites（InviteCodeVO）。
 */
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import { fetchInvites, makeInviteCode, revokeInvite } from '@/api/admin'
import { useSessionStore } from '@/stores/session'

const { t } = useI18n()
const session = useSessionStore()

const myStatus = computed(() => Number(session.user?.status ?? 0))
const isSuper = computed(() => myStatus.value >= 3)

const invites = ref([])
const loading = ref(false)
const generating = ref(false)
/** 刚生成的那枚码单独放大显示，方便当场复制（列表里也在） */
const freshCode = ref('')

function formatTime(value) {
  if (!value) return t('admin.invites.notRecorded')
  return String(value).slice(0, 19).replace('T', ' ')
}

function errorMessage(error, fallback = 'admin.invites.opFailed') {
  const key = `errors.${error?.errorCode}`
  return error?.errorCode && t(key) !== key ? t(key) : t(fallback)
}

async function load() {
  if (!isSuper.value) return
  loading.value = true
  try {
    const data = await fetchInvites()
    invites.value = Array.isArray(data) ? data : []
  } catch (error) {
    ElMessage.error(errorMessage(error, 'admin.invites.loadFailed'))
    invites.value = []
  } finally {
    loading.value = false
  }
}

async function handleGenerate() {
  if (generating.value) return
  generating.value = true
  try {
    const code = await makeInviteCode()
    freshCode.value = typeof code === 'string' ? code : ''
    if (freshCode.value) ElMessage.success(t('admin.invites.generated'))
    await load()
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    generating.value = false
  }
}

async function handleRevoke(row) {
  try {
    await ElMessageBox.confirm(
      t('admin.invites.confirmRevokeBody', { code: row.code }),
      t('admin.invites.confirmRevokeTitle'),
      {
        confirmButtonText: t('admin.invites.confirmRevoke'),
        cancelButtonText: t('admin.invites.cancel'),
        type: 'warning',
      },
    )
  } catch {
    return
  }

  try {
    await revokeInvite(row.id)
    ElMessage.success(t('admin.invites.revoked'))
    if (freshCode.value === row.code) freshCode.value = ''
    await load()
  } catch (error) {
    ElMessage.error(errorMessage(error))
  }
}

/**
 * 复制：clipboard API 只在安全上下文可用（localhost / https），
 * 本地 http 局域网访问会失败，退回 execCommand 的临时 textarea。
 */
async function copyCode(code) {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(code)
    } else {
      fallbackCopy(code)
    }
    ElMessage.success(t('admin.invites.copied'))
  } catch {
    try {
      fallbackCopy(code)
      ElMessage.success(t('admin.invites.copied'))
    } catch {
      ElMessage.error(t('admin.invites.copyFailed'))
    }
  }
}

function fallbackCopy(text) {
  const area = document.createElement('textarea')
  area.value = text
  area.setAttribute('readonly', '')
  area.style.position = 'fixed'
  area.style.opacity = '0'
  document.body.appendChild(area)
  area.select()
  const ok = document.execCommand('copy')
  document.body.removeChild(area)
  if (!ok) throw new Error('copy failed')
}

onMounted(load)
</script>

<template>
  <div class="invites">
    <!-- 兜底：路由守卫已按 meta.minStatus=3 把普通管理员送回概览，
         这里只覆盖「本地 status 被改过、但后端仍会拒绝」的情况——不摆空表，
         也不假装能点（D036）。 -->
    <el-card v-if="!isSuper" class="notice" shadow="never">
      <p class="notice__title">{{ t('admin.invites.restrictedTitle') }}</p>
      <p class="notice__body">{{ t('admin.invites.restrictedBody') }}</p>
    </el-card>

    <template v-else>
      <div class="panel">
        <div class="panel__head">
          <div>
            <h2 class="panel__title">{{ t('admin.invites.sectionTitle') }}</h2>
            <p class="panel__lead">{{ t('admin.invites.lead') }}</p>
          </div>
          <el-button
            type="primary"
            size="large"
            :loading="generating"
            class="panel__cta"
            @click="handleGenerate"
          >
            {{ t('admin.invites.generate') }}
          </el-button>
        </div>

        <div v-if="freshCode" class="fresh">
          <span class="fresh__label">{{ t('admin.invites.freshLabel') }}</span>
          <code class="fresh__code">{{ freshCode }}</code>
          <el-button size="small" plain @click="copyCode(freshCode)">
            {{ t('admin.invites.copy') }}
          </el-button>
        </div>

        <p class="panel__hint">{{ t('admin.invites.oneTimeHint') }}</p>
      </div>

      <el-table
        v-loading="loading"
        :data="invites"
        :empty-text="t('admin.invites.empty')"
        class="invite-table"
        border
      >
        <el-table-column :label="t('admin.invites.colCode')" min-width="220">
          <template #default="{ row }">
            <code class="code-cell">{{ row.code }}</code>
          </template>
        </el-table-column>
        <el-table-column :label="t('admin.invites.colCreatedAt')" min-width="200">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column
          :label="t('admin.invites.colActions')"
          width="220"
          align="center"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button type="primary" size="small" plain @click="copyCode(row.code)">
              {{ t('admin.invites.copy') }}
            </el-button>
            <el-button type="danger" size="small" plain @click="handleRevoke(row)">
              {{ t('admin.invites.revoke') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </div>
</template>

<style scoped>
/* ---------- 顶部操作卡：与用户管理的搜索卡同一种米色块 ---------- */
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
  max-width: 640px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--adm-ink-soft);
}

.panel__cta {
  flex: none;
}

.panel__hint {
  margin: 14px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--adm-ink-faint);
}

/* 刚生成的码：整块高亮，一眼能选中复制 */
.fresh {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 18px;
  padding: 14px 18px;
  border: 1px solid var(--adm-primary-block-border);
  border-radius: var(--adm-radius-block);
  background: var(--adm-surface-head);
}

.fresh__label {
  font-size: 13px;
  font-weight: 600;
  color: var(--adm-ink-soft);
}

.fresh__code {
  font-family: var(--font-mono);
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: var(--adm-primary);
  user-select: all;
}

/* ---------- 表格：与用户管理同一套令牌 ---------- */
.invite-table {
  border-radius: var(--adm-radius-block);
  overflow: hidden;
  background: var(--adm-surface);
  box-shadow: var(--adm-shadow-table);
}

.invite-table :deep(th.el-table__cell) {
  background: var(--adm-surface-head);
  color: var(--adm-ink-soft);
  font-weight: 600;
}

.invite-table :deep(td.el-table__cell) {
  background: var(--adm-surface);
  padding: 12px 0;
}

.invite-table :deep(.el-button) {
  margin: 2px;
  min-width: 60px;
}

.code-cell {
  font-family: var(--font-mono);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: var(--adm-ink);
  user-select: all;
}

/* ---------- 无权提示 ---------- */
.notice {
  border-radius: var(--adm-radius-block);
  background: var(--adm-surface);
}

.notice__title {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--adm-ink);
}

.notice__body {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--adm-ink-soft);
}
</style>
