<script setup>
import { ref, watch } from 'vue'
import { Iphone, User, Van , Lock} from '@element-plus/icons-vue'

//这个是登录方式选择器的配置
const value = ref('username-password')
const direction = ref('vertical')
const size = ref('large')
const options = [
  {
    label: '登录方式',
    disabled: true,
    icon: null,
  },

  {
    label: '用户名+密码',
    value: 'username-password',
    icon: User,
  },
  {
    label:'手机号+密码',
    value: 'phone-password',
    icon: Iphone
  },
  {
    label: '邮箱+验证码',
    value: 'email-code',
    icon: Van,
  },
  {
    label: '邮箱+密码',
    value: 'email-password',
    icon: Lock,
  }
]
//这个是给父组件用的
const emit = defineEmits(['changeLoginType'])
watch(value, (newVal) => {
  emit('changeLoginType', newVal)
})
</script>


<template>
  <div class="loginWay">


    <el-segmented
      v-model="value"
      :options="options"
      :direction="direction"
      :size="size"
    >
      <template #default="scope">
        <div
          :class="[
            'flex',
            'items-center',
            'gap-2',
            'flex-col',
            direction === 'vertical' && 'p-2',
          ]"
        >
          <el-icon size="40" v-if="scope.item.icon!==null">
            <component :is="scope.item.icon" />
          </el-icon >
          <div :class="scope.item.disabled ? 'bold' : ''">
            {{ scope.item.label }}
          </div>
        </div>
      </template>
    </el-segmented>
  </div>
</template>

<style lang="less" scoped>
.loginWay:hover{
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.3);
}

.loginWay{
  //居左中悬浮
  position: absolute;
  top: 50%;
  transform: translate(0%, -50%);
  //加个边框
  border-radius: 0 30px 30px 0;
  overflow: hidden;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.08);

  transition: all 0.5s ease-in-out;

  //为碟醋包了个饺子 专门给题头搞的样式
  .bold {
    font-weight: bold;
    color: black;
  }

  // 使用 CSS 变量覆盖 el-segmented 的选中样式
  --el-segmented-item-selected-bg-color: #C3272B;
  --el-segmented-item-selected-color: white;

  :deep(.el-segmented) {
    --el-segmented-item-selected-bg-color: #C3272B;
    --el-segmented-item-selected-color: white;
  }
}
</style>

// 添加一个全局样式块（不带 scoped）
<style lang="less">
// 全局覆盖 Element Plus 的 CSS 变量
.el-segmented {
  --el-segmented-item-selected-bg-color: #C3272B !important;
}
// 选中项的背景色
.el-segmented__item.is-selected {
  background-color: #C3272B !important;
}
</style>
