<script setup>
import { ref } from 'vue'
import Aside from '@/views/admin/Aside.vue'
import User from '@/views/admin/component/User.vue'
import InviteAdmin from '@/views/admin/component/InviteAdmin.vue'
import HandleFeedback from '@/views/admin/component/HandleFeedback.vue'
import WriteHelp from '@/views/admin/component/WriteHelp.vue'
import OperationPanel from '@/views/admin/component/OperationPanel.vue'
import ComingSoon from '@/views/admin/component/ComingSoon.vue'

const header = ref('用户管理');
const menu = ref(0);
const menuItems = ['用户管理','邀请管理','处理反馈','撰写帮助','操作面板','敬请期待']

const changeMenu = (index) => {
  // console.log('切换菜单:', index);
  menu.value = index;
  header.value = menuItems[index];
}

</script>

<template>
  <div class="common-layout">
    <el-container>
      <el-aside :span="6">
        <Aside @changeMenu="changeMenu"></Aside>
      </el-aside>
      <el-container>
        <el-header>{{header}}</el-header>
        <el-main :span="18">
          <transition name="slide-fade" mode="out-in">
            <div v-if="menu===0" key="user"><User/></div>
            <div v-else-if="menu===1" key="invite"><InviteAdmin/></div>
            <div v-else-if="menu===2" key="feedback"><HandleFeedback/></div>
            <div v-else-if="menu===3" key="help"><WriteHelp/></div>
            <div v-else-if="menu===4" key="operation"><OperationPanel/></div>
            <div v-else-if="menu===5" key="coming"><ComingSoon/></div>
          </transition>
          <!--     TODO:这里日后会放管理员的其他功能组件     -->
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<style lang="less" scoped>
//  左侧边栏
.el-aside {
  /* 你要求的外边距 + 背景色 */
  margin: 20px;
  background-color: #FFF8E8;
  height: calc(100vh - 40px); /* 铺满屏幕高度（减去上下margin） */
  border-radius: 20px; /* 圆角更柔和，替代直角生硬感 */
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1); /* 轻阴影提升层次感 */
  transition: all 0.3s ease; /* 平滑过渡动画 */
  overflow: hidden; /* 隐藏圆角溢出内容 */
}
// 右边主体
.el-header {
  margin: 20px;
  background-color: #FFF8E8;
  border-radius: 20px; /* 圆角更柔和，替代直角生硬感 */
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1); /* 轻阴影提升层次感 */
  transition: all 0.3s ease; /* 平滑过渡动画 */
  overflow: hidden; /* 隐藏圆角溢出内容 */

  font-size: 20px;
  font-weight: bold;
  line-height: 57px;
}
.el-aside {
  position: relative;
  margin: 20px;
  background-color: #FFF8E8;
}

// 右侧主内容区域 - 设置高度以便子组件内部滚动
.el-main {
  height: calc(100vh - 137px); /* 100vh - header高度(57px) - header上下margin(40px) - main上下margin(40px) */
  overflow-y: auto;
  padding: 0;
}

// 菜单切换过渡动画
.slide-fade-enter-active {
  transition: all 0.3s ease-out;
}

.slide-fade-leave-active {
  transition: all 0.2s ease-in;
}

.slide-fade-enter-from {
  transform: translateX(20px);
  opacity: 0;
}

.slide-fade-leave-to {
  transform: translateX(-20px);
  opacity: 0;
}
</style>
