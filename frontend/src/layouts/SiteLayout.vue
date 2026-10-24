<script setup>
import SiteHeader from '@/components/SiteHeader.vue'
import SiteFooter from '@/components/SiteFooter.vue'
import { RouterView } from 'vue-router'
</script>

<template>
  <div class="page">
    <SiteHeader />
    <main class="page__main">
      <RouterView v-slot="{ Component }">
        <component :is="Component" />
      </RouterView>
    </main>
    <SiteFooter />
  </div>
</template>
