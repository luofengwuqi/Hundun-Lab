<script setup>
import { useUserStore } from '@/stores/user.js'
import { computed, ref } from 'vue'
import defaultAvatar from '@/assets/img/common/user.jpg'
import { User, Picture, Clock } from '@element-plus/icons-vue'
import { OSS_DOMAIN } from '@/utils/config.js'

const userStore = useUserStore()

// 获取用户头像
const avatarSrc = computed(() => {
  const avatarUrl = userStore.userInfo?.avatarUrl
  if (!avatarUrl) return defaultAvatar
  // 如果已经是完整 URL，直接返回；否则拼接 OSS 域名
  return avatarUrl.startsWith('http') ? avatarUrl : `${OSS_DOMAIN}/${avatarUrl}`
})

//定义 emit 事件
const emit = defineEmits(['change-operation-type'])

// 当前激活的菜单项索引
const activeIndex = ref(0)

// 菜单项列表
const menuItems = [
  { name: '账号管理', icon: User, type: 0 },
  { name: '更改头像', icon: Picture, type: 1 },
  { name: '敬请期待', icon: Clock, type: 2 }
]

// 用户信息相关
const handleChangePic = ()=>{
  // 触发事件,将 operationType 设置为 1
  emit('change-operation-type', 1)
}

// 处理菜单项点击
const handleMenuClick = (index) => {
  activeIndex.value = index
  emit('change-operation-type', menuItems[index].type)
}
</script>

<template>
  <div class="left">
    <div class="user-msg">
      <div class="img-box" @click="handleChangePic">
        <img :src="avatarSrc" alt="" />
        <div class="overlay">
          <span>更改头像</span>
        </div>
      </div>
      <div class="username">
        {{ userStore.userInfo?.username || '' }}
        <span v-if="userStore.userInfo?.nickname">| {{ userStore.userInfo.nickname }}</span>
      </div>
      <div class="email">
        <span v-if="userStore.userInfo?.email">{{ userStore.userInfo.email }}</span>
      </div>
    </div>
    <el-divider />
    <div class="user-operation">
      <ul>
        <li
          v-for="(item, index) in menuItems"
          :key="index"
          :class="{ active: activeIndex === index }"
          @click="handleMenuClick(index)"
        >
          <el-icon :size="20"><component :is="item.icon" /></el-icon>
          <span>{{ item.name }}</span>
        </li>
      </ul>
    </div>
  </div>
</template>
<style scoped lang="less">
.left {
  .user-msg {
    margin-bottom: 15px;
    padding: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-direction: column;
    .img-box {
      position: relative;
      margin-bottom: 15px;
      width: 160px;
      height: 160px;
      border-radius: 50%;
      overflow: hidden;
      cursor: pointer;

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }

      .overlay {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background-color: rgba(0, 0, 0, 0.5);
        display: flex;
        align-items: center;
        justify-content: center;
        opacity: 0;
        transition: opacity 0.3s ease;

        span {
          color: #fff;
          font-size: 20px;
        }
      }

      &:hover {
        .overlay {
          opacity: 1;
        }
      }
    }
    .username {
      font-size: 25px;
      font-weight: 600;
    }
  }
  .user-operation{
    ul{
      display: flex;
      flex-direction: column;

      width: 100%;
      li{
        position: relative;
        margin: 12px 8%;
        padding: 14px 20px;
        border-radius: 12px;
        min-height: 24px;
        width: 84%;

        background: linear-gradient(135deg, #fff5eb 0%, #ffe8d0 100%);
        border: none;
        box-shadow: 0 2px 8px rgba(255, 167, 103, 0.15);

        color: #5A4A42;
        font-weight: 600;
        font-size: 15px;
        text-align: center;
        line-height: 1.5;

        display: flex;
        align-items: center;
        justify-content: center;
        gap: 8px;

        cursor: pointer;
        transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        overflow: hidden;

        &::before {
          content: '';
          position: absolute;
          top: 0;
          left: -100%;
          width: 100%;
          height: 100%;
          background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.6), transparent);
          transition: left 0.5s;
        }

        &:hover {
          transform: translateY(-2px) scale(1.02);
          box-shadow: 0 6px 16px rgba(255, 167, 103, 0.3);
          background: linear-gradient(135deg, #ffe8d0 0%, #ffd9b3 100%);

          &::before {
            left: 100%;
          }
        }

        &.active {
          background: linear-gradient(135deg, #C3272B 0%, #B91F24 100%);
          box-shadow: 0 4px 16px rgba(195, 39, 43, 0.45);
          color: #ffffff;
          font-weight: 700;
          transform: scale(1.05);

          &::before {
            background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
          }

          &:hover {
            transform: translateY(-3px) scale(1.05);
            box-shadow: 0 8px 24px rgba(195, 39, 43, 0.6);
            background: linear-gradient(135deg, #D63A3F 0%, #C3272B 100%);
          }

          &:active {
            transform: translateY(0) scale(1.02);
          }
        }

        &:active {
          transform: translateY(0) scale(0.98);
        }

        span {
          font-size: 15px;
        }
      }
    }
  }

}
</style>
