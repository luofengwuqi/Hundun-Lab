<script setup>
/**
 * F11 /user/info：个人资料 + 我的扩展。
 *
 * 本期（F11 / O07）：基本资料只读为主，不做编辑；「我的扩展」列表的数据
 * 要到 M2 才有（扩展提交上线后才有记录），所以现在只有空态引导——
 * 按规格引导去软件详情页，不直连上传页（提交必须先选定软件）。
 * 路由守卫保证进到这里一定已登录。
 */
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useSessionStore } from '@/stores/session'

const { t } = useI18n()
const session = useSessionStore()

const user = computed(() => session.user || {})

const profile = computed(() => [
  { label: t('user.username'), value: user.value.username || '—' },
  { label: t('user.nickname'), value: user.value.nickname || '—' },
  { label: t('user.email'), value: user.value.email || '—' },
  {
    label: t('user.since'),
    value: user.value.createdAt ? String(user.value.createdAt).slice(0, 10) : '—',
  },
])
</script>

<template>
  <div class="user-page">
    <div class="container user-grid">
      <!-- 左：资料卡 + 竖排导航 -->
      <aside class="card profile-card">
        <svg
          class="profile-card__avatar"
          viewBox="0 0 8 8"
          width="84"
          height="84"
          shape-rendering="crispEdges"
          role="img"
          :aria-label="t('user.avatarLabel')"
        >
          <rect width="8" height="8" fill="#fff8e8" />
          <rect x="2" y="0" width="4" height="1" fill="#c3272b" />
          <rect x="1" y="1" width="6" height="5" fill="#e8a33d" />
          <rect x="2" y="2" width="1" height="1" fill="#3b2e2b" />
          <rect x="5" y="2" width="1" height="1" fill="#3b2e2b" />
          <rect x="3" y="4" width="2" height="1" fill="#c3272b" />
          <rect x="1" y="6" width="6" height="2" fill="#3b2e2b" />
          <rect x="3" y="6" width="2" height="1" fill="#fff8e8" />
        </svg>
        <div class="profile-card__name">{{ session.displayName }}</div>
        <nav class="vert-nav" :aria-label="t('user.navLabel')">
          <RouterLink to="/user/info" class="is-active">{{ t('user.profile') }}</RouterLink>
          <RouterLink to="/user/info" class="is-current">{{ t('user.myExtensions') }}</RouterLink>
          <span class="is-disabled" :title="t('common.empty')">{{ t('user.security') }}</span>
        </nav>
      </aside>

      <div class="stack-24">
        <!-- 基本资料：只读为主（F11；完整编辑是 O07 之后的事） -->
        <section class="card panel">
          <h2 class="panel__title">{{ t('user.profile') }}</h2>
          <dl class="kv">
            <div v-for="row in profile" :key="row.label" class="kv__row">
              <dt>{{ row.label }}</dt>
              <dd>{{ row.value }}</dd>
            </div>
          </dl>
          <p class="mono muted panel__note">{{ t('user.readonlyNote') }}</p>
        </section>

        <!-- 我的扩展：M2 才有数据，空态按规格引导去软件详情页 -->
        <section class="card panel">
          <h2 class="panel__title">{{ t('user.myExtensions') }}</h2>
          <div class="empty-ext">
            <p class="muted">{{ t('user.noExtensions') }}</p>
            <RouterLink to="/software" class="btn btn--sm">
              {{ t('user.goSoftware') }}
            </RouterLink>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.user-page {
  padding: 40px 0 80px;
}

.user-grid {
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr);
  gap: 28px;
  align-items: start;
}

.profile-card {
  padding: 24px 20px;
  display: grid;
  justify-items: center;
  gap: 12px;
}

.profile-card__name {
  font-weight: 800;
  font-size: 16px;
}

.vert-nav {
  width: 100%;
  display: grid;
  border-top: 2px solid var(--line);
  margin-top: 8px;
}

.vert-nav a,
.vert-nav span {
  padding: 11px 6px;
  font-size: 14px;
  font-weight: 600;
  border-bottom: 1px dashed var(--line-faint);
}

.vert-nav a:hover {
  color: var(--accent-text);
}

.vert-nav .is-active {
  color: var(--accent-text);
}

.vert-nav .is-current {
  color: var(--ink);
}

.vert-nav .is-disabled {
  color: var(--ink-faint);
  cursor: not-allowed;
}

.panel {
  padding: 22px 24px;
}

.panel__title {
  font-size: 18px;
  margin-bottom: 16px;
}

.kv {
  display: grid;
  gap: 12px;
}

.kv__row {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 12px;
  font-size: 14px;
  padding-bottom: 12px;
  border-bottom: 1px dashed var(--line-faint);
}

.kv__row:last-of-type {
  border-bottom: 0;
  padding-bottom: 0;
}

.kv__row dt {
  color: var(--ink-faint);
}

.kv__row dd {
  margin: 0;
  word-break: break-all;
}

.panel__note {
  margin-top: 16px;
  font-size: 11px;
}

.empty-ext {
  display: grid;
  justify-items: start;
  gap: 12px;
  padding: 8px 0 4px;
}

@media (max-width: 800px) {
  .user-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .profile-card {
    justify-items: start;
  }

  .vert-nav {
    justify-items: start;
  }
}
</style>
