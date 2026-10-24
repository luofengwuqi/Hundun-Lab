<script setup>
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<template>
  <div class="plugins">
      <section class="section">
        <div class="container soon">
          <div class="soon__card card scanlines">
            <p class="kicker">PLUGINS / WIP</p>
            <h1 class="soon__title">{{ t('plugins.title') }}</h1>
            <p class="soon__lead">{{ t('plugins.lead') }}</p>
            <p class="soon__note mono">{{ t('plugins.note') }}</p>
            <RouterLink to="/software" class="btn btn--primary">{{ t('home.heroPrimary') }}</RouterLink>
          </div>
        </div>
      </section>
  </div>
</template>

<style scoped>
.soon {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

.soon__card {
  position: relative;
  width: min(100%, 560px);
  padding: 36px;
  text-align: center;
  display: grid;
  gap: 14px;
  justify-items: center;
}

.soon__title {
  font-size: clamp(26px, 4vw, 36px);
}

.soon__lead {
  font-size: 15px;
  color: var(--ink-soft);
  max-width: 40ch;
}

.soon__note {
  font-size: 11px;
  letter-spacing: 0.1em;
  color: var(--ink-faint);
}
</style>
