<script setup>
/**
 * 表单字段（label + input）
 * ------------------------------------------------------------------
 * 抽出来的唯一理由：登录页与登录弹窗各抄了一份 `.field*` 样式，
 * 改一个字的字号要改两处，且两处已经开始不一致（弹窗的 input 背景写死 #fffdf7）。
 * 现在样式只在这里定义一份，页面只负责传入 label 与 v-model。
 *
 * 不做的：校验、错误提示。错误属于表单整体（一次提交只有一个错误码），
 * 不该由单个字段承担，所以错误行仍由调用方渲染。
 */
import { useId } from 'vue'

const model = defineModel({ type: String, default: '' })

defineProps({
  /** 字段名，显示在输入框上方，调用方先翻译好再传 */
  label: { type: String, required: true },
  /** 输入类型：text / password / email 等 */
  type: { type: String, default: 'text' },
  /** 浏览器自动填充提示，登录表单必须给，否则密码管理器认不出 */
  autocomplete: { type: String, default: 'off' },
})

// 同一页可能出现多个表单，id 必须唯一，label 的 for 才点得准
const id = useId()
</script>

<template>
  <label class="field" :for="id">
    <span class="field__label">{{ label }}</span>
    <input
      :id="id"
      v-model="model"
      class="field__input"
      :type="type"
      :autocomplete="autocomplete"
    />
  </label>
</template>

<style scoped>
.field {
  display: grid;
  gap: 6px;
}

.field__label {
  font-family: var(--font-mono);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--ink-soft);
}

.field__input {
  border: 2px solid var(--ink);
  background: var(--field);
  padding: 9px 11px;
  font-size: 15px;
  border-radius: var(--radius);
}

.field__input:focus {
  outline: 3px solid var(--amber);
  outline-offset: 1px;
}
</style>
