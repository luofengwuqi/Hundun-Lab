<script setup>

import Card from '@/views/help/component/Card.vue'
</script>

<template>
  <Card>
    <template #head>
      不太温馨的提示...
    </template>
    <template #main>
      由于芜师傅没有大米<small>甚至送过半年外卖</small><br>
      所以在开发发送验证码的功能时只能用免费的方法<small>javamail+qq邮箱</small>...<br>
      这导致了没有手机号+验证码的登录方法<br>
      而且邮箱的验证码一般会<b>发的</b>比较<b>慢</b><small>没米是这样的...</small><br>
      <b>如果前端等了半天等不到回话 可能会误以为后端炸了并报错...</b><br>
      在使用此功能时候请忽略那个可能出现的奇奇怪怪的提示框...<br>
      实在抱歉(｡•́︿•̀｡)<br>
      <small>超小声:其实你耐心看完这个验证码就差不多发过来了</small>
    </template>
  </Card>

</template>

<style scoped>

</style>
