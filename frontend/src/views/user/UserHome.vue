<script setup>

import HundunHeader from '@/views/user/component/common/HundunHeader.vue'
</script>

<template>
  <div class="user-home">
    <div class="header">
      <HundunHeader></HundunHeader>
    </div>
    <div class="main">
      <router-view v-slot="{ Component }" :key="$route.fullPath">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </div>
  </div>
</template>



<style scoped lang="less">
  .user-home{
    width: 100%;
    height: 100%;
  }
  .header{
    height: 80px;
  }
  .main{
    height: calc(100vh - 80px);
    overflow: auto;

    &::-webkit-scrollbar {
      display: none;
    }

    -ms-overflow-style: none;
    scrollbar-width: none;
  }

  /* 二级路由过渡动画 */
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
