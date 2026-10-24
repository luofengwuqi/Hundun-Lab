<script setup>

import ChangeAvatar from '@/views/user/component/Info/RightSideComponent/ChangeAvatar.vue'
import Default from '@/views/user/component/Info/RightSideComponent/UserManage.vue'
import ComingSoon from '@/views/admin/component/ComingSoon.vue'

const props = defineProps({
  operationType: {
    type: Number,
    default: 0
  }
})
</script>

<template>
  <div class="right">
    <Transition name="fade">
      <Default v-if="props.operationType === 0" key="default" />
      <ChangeAvatar v-else-if="props.operationType === 1" key="avatar" />
      <ComingSoon v-else-if="props.operationType === 2" key="coming-soon" />
    </Transition>
  </div>
</template>

<style scoped lang="less">
.right {
  opacity: 0.85;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
