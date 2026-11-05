<script setup>
/**
 * 按钮（物料 03）
 * ------------------------------------------------------------------
 * 三种落点：to（路由）/ href（外链）/ 都不给（button）。
 */
import { computed } from 'vue'

const props = defineProps({
  /** primary 实底 / ghost 幽灵 / 默认描边 */
  variant: { type: String, default: 'default' },
  /** sm / md / lg */
  size: { type: String, default: 'md' },
  /** 撑满父容器 */
  block: { type: Boolean, default: false },
  /** 路由跳转 */
  to: { type: [String, Object], default: null },
  /** 普通链接 */
  href: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
  /** 原生 button 的 type */
  type: { type: String, default: 'button' },
})

const tag = computed(() => {
  if (props.to) return 'RouterLink'
  if (props.href) return 'a'
  return 'button'
})

const attrs = computed(() => {
  if (props.to) return { to: props.to }
  if (props.href) return { href: props.href }
  return { type: props.type, disabled: props.disabled || undefined }
})

const classes = computed(() => [
  {
    'hd-btn--primary': props.variant === 'primary',
    'hd-btn--ghost': props.variant === 'ghost',
    'hd-btn--sm': props.size === 'sm',
    'hd-btn--lg': props.size === 'lg',
    'hd-btn--block': props.block,
    'hd-btn--disabled': props.disabled,
  },
])
</script>

<template>
  <component :is="tag" v-bind="attrs" class="hd-btn" :class="classes">
    <slot />
  </component>
</template>
