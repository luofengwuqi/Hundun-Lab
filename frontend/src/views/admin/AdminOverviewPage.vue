<script setup>
/**
 * 管理端概览（F14 的落地位）。
 *
 * 现在只展示「谁登录了」和「后台还剩什么没接」——F12–F15 的数据接口
 * 都在 M1/M2，没有接口时编数字就是假数据（D036）。等接口上线，
 * 这一页再补下载量、按软件/平台分布与近 30 天趋势。
 */
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useSessionStore } from '@/stores/session'

const { t } = useI18n()
const session = useSessionStore()

const user = computed(() => session.user || {})

const roleName = computed(() =>
  user.value.status >= 3 ? t('admin.roleSuper') : t('admin.roleAdmin'),
)

const lastLogin = computed(() => {
  const raw = user.value.lastLoginAt
  return raw ? String(raw).slice(0, 16).replace('T', ' ') : '—'
})

const profile = computed(() => [
  { label: t('admin.fieldUsername'), value: user.value.username || '—' },
  { label: t('admin.fieldNickname'), value: user.value.nickname || '—' },
  { label: t('admin.fieldRole'), value: roleName.value },
  { label: t('admin.fieldLastLogin'), value: lastLogin.value },
])

/** 尚未接线的后台模块：阶段与依赖按 PRD §5 的 F12–F15 写死，接口到位再改 */
const backlog = computed(() => [
  { id: 'F12', name: t('admin.f12'), stage: 'M1' },
  { id: 'F13', name: t('admin.f13'), stage: 'M1' },
  { id: 'F14', name: t('admin.f14'), stage: 'M1' },
  { id: 'F15', name: t('admin.f15'), stage: 'M2' },
])
</script>

<template>
  <section class="admin-page">
    <p class="admin-page__lead">{{ t('admin.overviewLead') }}</p>

    <div class="admin-grid">
      <article class="admin-panel">
        <h2 class="admin-panel__title">{{ t('admin.accountTitle') }}</h2>
        <dl class="admin-kv">
          <div v-for="row in profile" :key="row.label" class="admin-kv__row">
            <dt>{{ row.label }}</dt>
            <dd>{{ row.value }}</dd>
          </div>
        </dl>
      </article>

      <article class="admin-panel">
        <h2 class="admin-panel__title">{{ t('admin.progressTitle') }}</h2>
        <ul class="admin-backlog">
          <li v-for="item in backlog" :key="item.id">
            <span class="admin-backlog__id">{{ item.id }}</span>
            <span class="admin-backlog__name">{{ item.name }}</span>
            <span class="admin-backlog__stage">{{ item.stage }}</span>
          </li>
        </ul>
      </article>
    </div>

    <p class="admin-page__note">{{ t('admin.overviewNote') }}</p>
  </section>
</template>

<style scoped>
.admin-page {
  max-width: 960px;
}

.admin-page__lead {
  margin-bottom: 20px;
  padding: 16px 20px;
  border-radius: var(--adm-radius-block);
  background: var(--adm-surface);
  box-shadow: var(--adm-shadow-soft);
  font-size: 14px;
  line-height: 1.7;
  color: var(--adm-ink-soft);
}

.admin-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.admin-panel {
  padding: 20px 22px;
  border-radius: var(--adm-radius-block);
  background: var(--adm-surface);
  box-shadow: var(--adm-shadow-soft);
}

.admin-panel__title {
  margin-bottom: 14px;
  font-size: 16px;
  font-weight: 700;
  color: var(--adm-ink);
}

.admin-kv {
  display: grid;
  gap: 10px;
}

.admin-kv__row {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--adm-surface-head);
  font-size: 14px;
}

.admin-kv__row:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.admin-kv dt {
  color: var(--adm-ink-faint);
}

.admin-kv dd {
  margin: 0;
  color: var(--adm-ink);
  word-break: break-all;
}

.admin-backlog {
  display: grid;
  gap: 8px;
  list-style: none;
  margin: 0;
  padding: 0;
}

.admin-backlog li {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 40px;
  padding: 0 12px;
  border-radius: 6px;
  background: var(--adm-surface-soft);
  font-size: 14px;
}

.admin-backlog__id {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--adm-ink-faint);
}

.admin-backlog__name {
  flex: 1;
  min-width: 0;
  color: var(--adm-ink-soft);
}

.admin-backlog__stage {
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 600;
  color: var(--adm-primary);
}

.admin-page__note {
  margin-top: 18px;
  padding: 12px 16px;
  border-radius: var(--adm-radius-block);
  background: var(--adm-surface-soft);
  font-size: 12px;
  line-height: 1.7;
  color: var(--adm-ink-faint);
}

@media (max-width: 900px) {
  .admin-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
