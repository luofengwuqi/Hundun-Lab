<script setup>
import { RouterView } from 'vue-router'
</script>

<template>
  <div class="app">
    <!-- 路由过渡动画：使用作用域插槽 -->
    <router-view v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </div>
</template>

<style scoped>
.app {
  width: 100vw;
  height: 100vh;
  background: url("@/assets/img/common/background.jpg") no-repeat center fixed;
  background-size: cover;
  /* 让页面占满，隐藏滚动条（可选优化） */
  overflow: hidden;
}

/* 路由平滑过渡动画（淡入+轻微右滑，高级感拉满） */
:deep(.fade-enter-from) {
  opacity: 0;
  transform: translateX(10px);
}
:deep(.fade-enter-active) {
  transition: all 0.6s ease-out;
}
:deep(.fade-leave-to) {
  opacity: 0;
}
:deep(.fade-leave-active) {
  transition: all 0.5s ease-in;
}
</style>
