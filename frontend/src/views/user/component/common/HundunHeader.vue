<script setup>
import router from '@/router/index.js'
import { useUserStore } from '@/stores/user.js'
import { computed } from 'vue'
import defaultAvatar from '@/assets/img/common/user.jpg'
import { User, SwitchButton } from '@element-plus/icons-vue'
import { logout } from '@/utils/login/logout.js'

const userStore = useUserStore()

// OSS 域名配置
const OSS_DOMAIN = 'https://fifteen-hundun.oss-cn-beijing.aliyuncs.com'

const avatarSrc = computed(() => {
  const avatarUrl = userStore.userInfo?.avatarUrl
  if (!avatarUrl) return defaultAvatar
  // 如果已经是完整 URL，直接返回；否则拼接 OSS 域名
  return avatarUrl.startsWith('http') ? avatarUrl : `${OSS_DOMAIN}/${avatarUrl}`
})

const handleCommand =  async (command) => {
  if (command === 'info') {
    router.push('/user/info')
  } else if (command === 'logout') {
    await logout()
  }
}

const list=[
  { id: 1, name:'首页' ,to:'/user'},
  { id: 2, name:'帮助' ,to:'/help'}
]
const handleRouter = async (to)=>{
  if (to) {
    await router.push(to)
  }
}
</script>

<template>
  <div class="header" >
    <div class="logo-box" @click="router.push('/user')">
      <img src="@/assets/img/common/biglogo.png" alt="Logo">
    </div>
    <div class="main">
      <ul>
        <li v-for="(item) in list"
          @click="handleRouter(item.to)">
          <span>{{item.name}}</span>
        </li>
      </ul>
    </div>
    <el-dropdown v-if="userStore.userInfo" @command="handleCommand" trigger="hover">
      <div class="user-box">
        <img :src="avatarSrc" alt="User">
        <span class="username">{{ userStore.userInfo.nickname||userStore.userInfo.username}}</span>
      </div>
      <template #dropdown>
        <el-dropdown-menu class="custom-dropdown">
          <el-dropdown-item command="info">
            <el-icon><User /></el-icon>
            <span>个人信息</span>
          </el-dropdown-item>
          <el-dropdown-item divided command="logout">
            <el-icon><SwitchButton /></el-icon>
            <span>退出登录</span>
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<style scoped lang="less">
  .header{
    display: flex;
    justify-content: space-between;
    position:fixed;
    height: 80px;
    width: 100%;
    //不透明度
    opacity: 0.95;
    background-color: #FFF8E8;
    overflow: hidden;
    z-index: 999;
    border-radius: 0 0 40px 40px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    .logo-box{
      margin:10px 10px 10px 50px;
      height: 60px;
      img{
        height: 60px;
      }
    }
    .user-box{
      display: flex;
      height: 60px;
      margin:10px 50px 10px 10px;
      cursor: pointer;
      outline: none;

      &:focus {
        outline: none;
      }

      img{
        height: 60px;
        border-radius:30px;
        overflow: hidden;
      }

      .username{
        margin-left: 10px;
        font-size: 20px;
        line-height: 60px;
        //禁止用户选择
        user-select: none;
        -webkit-user-select: none;
        -moz-user-select: none;
        -ms-user-select: none;
      }
    }
    .main{
      ul{
        display: flex;
        flex-direction: row;
        justify-content: center;

        height: 80px;
        line-height: 80px;
        //禁止用户选择
        user-select: none;
        -webkit-user-select: none;
        -moz-user-select: none;
        -ms-user-select: none;
        li{
          margin: 0 20px;
          &:hover{
            color: #C3272B;
          }
        }

      }
    }

    // 移除 el-dropdown 的默认边框
    :deep(.el-dropdown) {
      outline: none;

      &:focus {
        outline: none;
      }

      .el-dropdown-link {
        outline: none;
      }
    }


  }
</style>
