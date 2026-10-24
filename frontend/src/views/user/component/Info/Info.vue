<script setup>

import LeftSide from '@/views/user/component/Info/LeftSide.vue'
import RightSide from '@/views/user/component/Info/RightSide.vue'
import { ref } from 'vue'

const operationType = ref(0)
const arr = {
  0: '修改信息',
  1: '修改头像',
  2: '修改密码'
}

</script>

<template>
  <div class="info">
    <el-row :gutter="20">
      <el-col :span="5">
        <div class="left-panel">
          <LeftSide @change-operation-type="operationType = $event"></LeftSide>
        </div>
      </el-col>

      <el-col :span="19">
        <div class="right-panel">
          <RightSide :operation-type="operationType"></RightSide>
        </div>
      </el-col>
    </el-row>
  </div>

</template>

<style scoped lang="less">
.info {
  padding: 20px;
}

.left-panel, .right-panel {
  // 可以添加背景色、内边距等样式
  min-height:  calc(100vh - 120px);
  background-color: #FFF8E8;
  border-radius: 12px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}
</style>
