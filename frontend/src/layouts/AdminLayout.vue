<script setup>
/**
 * 管理端布局（D089 两端结构 + D094 视觉复刻）。
 *
 * 视觉照 15-hundun：整页白底上浮着三块米色圆角卡片（侧栏 / 标题条 / 内容），
 * 卡片之间 20px 留白，轻阴影，内容区独立滚动。形态语言与公开站点
 * （硬阴影、直角、像素字体）有意不同——后台是工具，不是门面。
 *
 * 逻辑仍是本仓的：路由驱动（不是 ref 切组件）、i18n、真接口、守卫在后端之上再软拦一道。
 */
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'
import AsideNav from '@/views/admin/AsideNav.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'

// 令牌必须在布局之前加载：AdminUsersPage 等子页面的样式全都引用 --adm-*
import '@/styles/admin-tokens.css'

const { t, locale } = useI18n()
const route = useRoute()

/** 标题跟着路由走，和原项目 header 显示当前模块名一个意思 */
const pageTitle = computed(() =>
  route.meta?.adminTitle ? t(route.meta.adminTitle) : t('admin.navOverview'),
)

/**
 * Element Plus 自己的文案（分页的「共 N 条」、空表格、确认框按钮）不归 vue-i18n 管，
 * 得用 el-config-provider 喂它一份 locale，否则中文界面里会蹦出 "Total 3"。
 */
const epLocale = computed(() => (locale.value === 'en' ? en : zhCn))
</script>

<template>
  <el-config-provider :locale="epLocale">
    <div class="admin-shell">
      <el-container class="admin-shell__body">
        <el-aside class="admin-aside">
          <AsideNav />
        </el-aside>

        <el-container class="admin-column">
          <el-header class="admin-header">
            <h1 class="admin-header__title">{{ pageTitle }}</h1>
            <ThemeToggle />
          </el-header>

          <el-main class="admin-main">
            <RouterView v-slot="{ Component }">
              <transition name="slide-fade" mode="out-in">
                <component :is="Component" />
              </transition>
            </RouterView>
          </el-main>
        </el-container>
      </el-container>
    </div>
  </el-config-provider>
</template>

<style scoped>
.admin-shell {
  height: 100vh;
  overflow: hidden;
  /* 浅色铺原项目的馄饨线稿底图，深色由 --adm-canvas-image: none 关掉 */
  background-color: var(--adm-canvas);
  background-image: var(--adm-canvas-image);
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
  background-attachment: fixed;
  color: var(--adm-ink);
}

.admin-shell__body {
  height: 100%;
}

/* 侧栏：20px 外边距 + 大圆角 + 轻阴影，内部自己滚 */
.admin-aside {
  margin: 20px;
  height: calc(100vh - 40px);
  border-radius: var(--adm-radius-card);
  background: var(--adm-surface);
  box-shadow: var(--adm-shadow-card);
  overflow: hidden;
  transition: all 0.3s ease;
}

.admin-column {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

/* 标题条与侧栏同款卡片，右边留 20px */
.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  height: 57px;
  margin: 20px 20px 0;
  padding: 0 24px;
  border-radius: var(--adm-radius-card);
  background: var(--adm-surface);
  box-shadow: var(--adm-shadow-card);
  overflow: hidden;
  transition: all 0.3s ease;
}

.admin-header__title {
  font-size: 20px;
  font-weight: 700;
  line-height: 1;
  color: var(--adm-ink);
}

.admin-main {
  flex: 1;
  min-height: 0;
  padding: 20px;
  overflow-y: auto;
}

/* 模块切换：原项目的 slide-fade，位移 20px 而不是淡入淡出 */
.slide-fade-enter-active {
  transition: all 0.3s ease-out;
}

.slide-fade-leave-active {
  transition: all 0.2s ease-in;
}

.slide-fade-enter-from {
  transform: translateX(20px);
  opacity: 0;
}

.slide-fade-leave-to {
  transform: translateX(-20px);
  opacity: 0;
}

@media (max-width: 900px) {
  .admin-shell {
    height: auto;
    overflow: visible;
  }

  .admin-shell__body {
    flex-direction: column;
    height: auto;
  }

  .admin-aside {
    height: auto;
    margin: 16px;
  }

  .admin-header {
    margin: 0 16px;
  }

  .admin-main {
    padding: 16px;
    overflow-y: visible;
  }
}
</style>
