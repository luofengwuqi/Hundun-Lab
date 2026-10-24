<script setup>
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { fetchSoftwareList } from '@/api/site'
import SoftwareCard from '@/components/SoftwareCard.vue'

const { t } = useI18n()
const loading = ref(true)
const failed = ref(false)
const items = ref([])

async function load() {
  loading.value = true
  failed.value = false
  try {
    items.value = await fetchSoftwareList()
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="software-list">
      <section class="section">
        <div class="container">
          <header class="head">
            <h1 class="section-title">{{ t('home.softwareTitle') }}</h1>
            <p class="muted">{{ t('home.softwareLead') }}</p>
          </header>

          <p v-if="loading" class="muted mono">{{ t('common.loading') }}</p>
          <div v-else-if="failed" class="stack-8">
            <p class="muted">{{ t('common.error') }}</p>
            <button type="button" class="btn btn--sm" @click="load">{{ t('common.retry') }}</button>
          </div>
          <p v-else-if="!items.length" class="muted">{{ t('home.noSoftware') }}</p>

          <div v-else class="grid">
            <SoftwareCard v-for="item in items" :key="item.slug" :software="item" />
          </div>
        </div>
      </section>
  </div>
</template>

<style scoped>
.head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 28px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}
</style>
