<script setup>
/**
 * 分节容器（物料 02）
 * ------------------------------------------------------------------
 * 带红框的「货架」：左列放主内容，右列（可选）放附属信息。
 * 不传 aside 插槽时自动变成单列。
 */
import { computed } from 'vue'

const props = defineProps({
  /** 分节标题，如「软件」「最新动态」 */
  title: { type: String, default: '' },
  /** 标题后的等宽小字备注 */
  note: { type: String, default: '' },
  /** 两列比例，如 '58fr 42fr'；'1fr' 为单列 */
  columns: { type: String, default: '58fr 42fr' },
})

const styleVars = computed(() => ({ gridTemplateColumns: props.columns }))
</script>

<template>
  <section class="hd-section" :style="styleVars">
    <div class="hd-section__col">
      <div v-if="title || $slots.head" class="hd-section__head">
        <slot name="head">
          <span class="hd-bullet" aria-hidden="true"></span>
          <span class="hd-section__title">{{ title }}</span>
          <span v-if="note" class="hd-section__note">{{ note }}</span>
        </slot>
      </div>
      <slot />
    </div>

    <div v-if="$slots.aside" class="hd-section__col">
      <slot name="aside" />
    </div>
  </section>
</template>
