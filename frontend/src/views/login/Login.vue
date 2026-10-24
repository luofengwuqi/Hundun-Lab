<script setup>
import LoginWay from '@/views/login/component/LoginWay.vue'
import { ref } from 'vue'
import EmailCodeForm from '@/views/login/component/EmailCodeForm.vue'
import EmailPasswordForm from '@/views/login/component/EmailPasswordForm.vue'
import UsernamePasswordForm from '@/views/login/component/UsernamePasswordForm.vue'
import PhonePasswordForm from '@/views/login/component/PhonePasswordForm.vue'
import Register from '@/views/login/component/Register.vue'
import AdminRegisterWarn from '@/views/help/warn/AdminRegisterWarn.vue'
import RegisterHelp from '@/views/help/card/RegisterHelp.vue'
import SendEmailCodeHelp from '@/views/help/card/SendEmailCodeHelp.vue'
//登录模式选择器相关的东西(子传父)
const loginType = ref(`username-password`)
const changeLoginType = (type) => {
  console.log(`登录模式切换:${type}`)
  loginType.value = type
}
//提示相关的东西
// (这里的逻辑写的比较糟糕 一用数组基本上就完蛋了哈哈哈 希望以后还能看懂)
const changeDialogShow = ([index, value])=>{
  console.log(`提示框切换:[${index},${value}]`)

  dialogShow.value[index] = value
}
//index: 0:管理员警告 1:管理员提示 2:验证码发送提示
//value: false禁用 true启用

const dialogShow=ref([false,false,false])


</script>
<template>
  <div class="login">
    <login-way @changeLoginType="changeLoginType" v-if="loginType !== 'register'"></login-way>
    <div class="loginForm" v-if="loginType==='username-password'">
      <UsernamePasswordForm @changeLoginType="changeLoginType"></UsernamePasswordForm>
    </div>
    <div class="loginForm" v-else-if="loginType==='phone-password'">
      <PhonePasswordForm @changeLoginType="changeLoginType"></PhonePasswordForm>
    </div>
    <div class="loginForm" v-else-if="loginType==='email-code'">
      <EmailCodeForm
        @changeLoginType="changeLoginType"
        @changeDialogShow="changeDialogShow"></EmailCodeForm>
    </div>
    <div class="loginForm" v-else-if="loginType==='email-password'">
      <EmailPasswordForm
        @changeLoginType="changeLoginType"
        @changeDialogShow="changeDialogShow"></EmailPasswordForm>
    </div>
    <div class="loginForm" v-else-if="loginType==='register'">
      <Register
        @changeLoginType="changeLoginType"
        @changeDialogShow="changeDialogShow"
      ></Register>
    </div>
    <div class="loginForm" v-else>
      纳尼
    </div>
    <div class="help">
      <div class="admin-register-warn" v-if="loginType==='register' && dialogShow[0]">
        <AdminRegisterWarn/>
      </div>
      <div class="register-help" v-if="loginType==='register' && dialogShow[1]">
        <RegisterHelp/>
      </div>
      <div class="send-email-code-help" v-if="loginType==='email-code' && dialogShow[2]">
        <SendEmailCodeHelp/>
      </div>
    </div>


  </div>



</template>
<style scoped lang="less">
.login{
  width: 100%;
  height: 100vh;
  position: relative;
  .loginForm{
    position:absolute;
    //居中
    top:50%;
    left:50%;
    transform:translate(-50%,-50%);
  }
}
.help{
  //注册相关提示
  .admin-register-warn{
    width: 25%;
    position:absolute;
    bottom:10%;
    right:5%;
  }
  .register-help{
    width: 25%;
    position:absolute;
    top:10%;
    right:5%;
  }
  //登录相关提示
  .send-email-code-help{
    width: 25%;
    position:absolute;
    bottom:10%;
    right:5%;
  }
 }
</style>
