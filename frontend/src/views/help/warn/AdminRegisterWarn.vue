<script setup>
import { ref } from 'vue'
import Card from '@/views/help/component/Card.vue'

const visible = ref(true)

</script>

<template>
  <br>

  <Card v-if="visible">
    <template #head>
      <span style="color: #f00">注意：当前您正在注册为管理员用户</span>
    </template>
    <template #main>
      我们信任您已经从芜师傅那里了解了日常注意事项。<br>
      总结起来无外乎这三点(￣^￣)ゞ：<br>
      <span style="color: #f00">1) 尊重别人的隐私</span><br>
      <small>求你了 别闹。(｡ŏ_ŏ)</small><br>
      <span style="color: #f00">2) 输入前要先考虑 (后果和风险)。</span><br>
      <small>程序炸了怎么办o(￣ヘ￣o#)</small><br>
      <span style="color: #f00">3) 权力越大，责任越大。</span><br>
      <small>瞎改数据删库跑路就<span style="color: #f00 ; font-weight: bold">弄死你ヽ(`Д´)ﾉ︵ ┻━┻</span></small><br>
    </template>
  </Card>

</template>

<style scoped>

</style>
