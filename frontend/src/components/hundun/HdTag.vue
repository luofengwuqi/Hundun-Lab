<script setup>
/**
 * 平台胶囊（物料 04）
 * ------------------------------------------------------------------
 * 用于 Windows / macOS / Linux 这类平台标记。
 */
defineProps({
  /** 灰色语气：用于已停更 / 不支持的平台 */
  grey: { type: Boolean, default: false },
})
</script>

<template>
  <span class="hd-tag" :class="{ 'hd-tag--grey': grey }">
    <slot />
  </span>
</template>
