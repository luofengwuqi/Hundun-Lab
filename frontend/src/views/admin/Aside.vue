<script setup>
import { ref } from 'vue';
import { useUserStore } from '@/stores/user.js'
import router from '@/router/index.js'
import { logout } from '@/utils/login/logout.js'

const emit = defineEmits(['changeMenu']);

const activeIndex = ref(0);
const menuItems = ref(['用户管理', '邀请管理','处理反馈','撰写帮助','操作面板','敬请期待']);

const handleChoose = (index) => {
  activeIndex.value = index;
  emit('changeMenu', index);
}

//用户相关
const userStore = useUserStore();
const userStatus = ref(userStore.userInfo.status);
//底部按钮
const handleHelp = async ()=>{
  await router.push('/help')
}
const handleLogout = async ()=>{
  await logout();
}
</script>

<template>
  <div class="logo-box">
    <img src="../../assets/img/admin/logo-admin.png" alt="">
  </div>
  <el-divider />
  <div class="choose">
    <div
      v-for="(item, index) in menuItems" :key="index">
      <p
        v-if="!(index===1 && userStatus !== 3)"
        class="choose-item"
        :class="{ active: activeIndex === index }"
        @click="handleChoose(index)"
      >
        {{ item }}
      </p>
    </div>

  </div>
  <div class="footer">

    <el-divider></el-divider>
    <p>Ciallo～(∠・ω< )⌒★ <span>{{userStore.userInfo.nickname || userStore.userInfo.username}}</span> sama</p>
    <button @click="handleHelp" class="footer-btn">寻求帮助</button>
    <button @click="handleLogout" class="footer-btn">退出登录</button>

  </div>


</template>

<style scoped lang="less">

  .logo-box{
    margin-top: 20px;
    margin-bottom: 20px;
    width: 100%;
    img{
      width: 100%;
      height: 100%;
      object-fit: contain;
    }
  }
  .choose{
    margin-bottom: 20px;
    padding: 10px 0;
    .choose-item{
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
    }
  }
  .footer{
    position: absolute;
    bottom: 20px;
    width: 100%;
    text-align: center;
    span{
      color: #5A4A42;
      font-weight: 600;
    }
    .footer-btn{
      width: 45%;
      margin: 2.5%;
      border-radius: 20px;

      height: 30px;
      background-color:  #C3272B;

      color: #fff;
      font-weight: 600;
      line-height: 30px;

      transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
    }
    .footer-btn:hover{
      background-color:  #D63A3F;
    }
  }
</style>
