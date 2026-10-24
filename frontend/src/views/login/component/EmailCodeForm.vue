<script setup>
import { reactive, ref } from 'vue'
import { loginByEmailAndCode, sendEmailCode } from '@/api/user.js'
import { useUserStore } from '@/stores/user.js'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { loginRouterPush } from '@/utils/login/loginRouterPush.js'

// 定义 emits
const emit = defineEmits(['changeLoginType','changeDialogShow'])

//表单相关
const form = reactive({
  email: '',
  code: '',
})
//表单验证相关
const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
  ]
}
//按钮相关
const userStore = useUserStore()
const router = useRouter()
const formRef = ref(null)
const handleLogin = async () => {
  await formRef.value.validate(async (valid) => {
    if (valid) {
      console.log('验证通过，提交登录', form)
      const res = await loginByEmailAndCode(form.email, form.code)

      //操作pinia
      userStore.setToken(res.data.token)
      userStore.setUserInfo(res.data.user)
      ElMessage.success(`Ciallo～(∠・ω< )⌒★${userStore.userInfo?.nickname || userStore.userInfo?.username || '不知名的朋友'}`)
      await new Promise(resolve => setTimeout(resolve, 1500))
      //路由跳转
      await loginRouterPush()
    } else {
      console.log('表单验证失败')
      return false
    }
  })

}
const handleRegister = async () => {
  // 通知父组件切换到注册模式
  emit('changeLoginType', 'register')
}


const isWaiting = ref(false)
const countdown = ref(60)
let timer = null

const handleSend = async (email) => {
  if(!email){
    ElMessage.warning('请先输入邮箱哦/w\\')
    return
  }

  // 防止重复点击
  if (isWaiting.value) return

  emit('changeDialogShow', [2,true])
  isWaiting.value = true
  countdown.value = 60

  try {
    // 启动倒计时
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
        isWaiting.value = false
        timer = null
      }
    }, 1000)
    // 获取验证码
    await sendEmailCode(email)
    ElMessage.success('验证码发送成功 (∠・ω< )⌒★')
  } catch (error) {
    ElMessage.error('发送失败，请稍后重试')
    isWaiting.value = false
    countdown.value = 60
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }
}




</script>
<template>
  <div class="form">
    <div class="logo-box">
      <img src="../../../assets/img/common/logotext.png" alt="这是备用文案 按理来说不会显示不出来 哈哈">
    </div>
    <el-form
      :model="form"
      :rules="rules"
      ref="formRef"
      label-width="auto"
      style="max-width: 600px">
      <el-form-item prop="username">
        <el-input v-model="form.email" placeholder="请输入邮箱" prefix-icon="message" >
        </el-input>
      </el-form-item>
      <el-form-item prop="password" class="send-box">
        <el-input v-model="form.code" placeholder="请输入验证码" prefix-icon="lock"></el-input>
        <button type="button" class="send" @click="handleSend(form.email)" :disabled="isWaiting">
          <span v-if="isWaiting">请耐心等待 ({{ countdown }}秒)</span>
          <span v-else>发送邀请码</span>
        </button>
      </el-form-item>
      <div class="buttons">
        <el-form-item class="button-group">
          <el-button type="primary" @click="handleLogin">登录</el-button>
          <el-button type="primary" @click="handleRegister">注册</el-button>
        </el-form-item>
      </div>
      <router-link to="/help">忘记密码</router-link>
    </el-form>

  </div>
</template>

<style scoped lang="less">
.form{
  padding: 50px;
  border-radius: 20px;
  background-color: #fff;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.08);
  width: 460px;
  .logo-box{
    width: 100%;
    height: 100px;
    margin-bottom: 25px;
    img{
      width: 100%;
      height: 100%;
      object-fit: contain;
    }

  }
  a{
    display: block;
    width: 20%;
    margin-left: 80%;
    color: #AAA;
  }
  a:hover{
    color: #666;
  }
  .send-box {
    position: relative ;
    .send{
      right:5px;
      position: absolute;
      padding-right:5%;
      padding-left:5%;
      height:80%;

      background-color: #C3272B;
      border: none;
      border-radius: 5px;

      color: #fff;
      text-align: center;
    }
    .send:disabled{
      background-color: #ccc;
      cursor: not-allowed;
    }

  }
  /* 专门放大表单标签字体 */
  :deep(.el-form-item) {
    margin-bottom: 24px;
  }
  :deep(.el-input__inner) {
    font-size: 15px;
    height: 45px;
  }

  .button-group{
    display: flex;
    flex-direction: row;
    align-items: center;
    width: 100%;
    justify-content: space-between;
    .el-button{
      width: 48%;
      background-color: #C3272B;
      border: none;
    }
    .el-button:hover{
      background-color: #D64A4F;
    }



  }
}
</style>
