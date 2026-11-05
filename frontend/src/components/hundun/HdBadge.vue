<script setup>
/**
 * 状态徽章（物料 05）
 * ------------------------------------------------------------------
 * 版本号、审核状态、可提交等短状态。四种语气。
 */
import { computed } from 'vue'

const props = defineProps({
  /** version 版本号 / ok 通过 / warn 提醒 / muted 失效 */
  variant: { type: String, default: 'version' },
})

const classes = computed(() => [`hd-badge--${props.variant}`])
</script>

<template>
  <span class="hd-badge" :class="classes">
    <slot />
  </span>
</template>
