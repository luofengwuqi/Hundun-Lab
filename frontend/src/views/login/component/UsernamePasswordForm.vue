<script setup>
import { reactive, ref } from 'vue'
import { loginByUsernameAndPassword } from '@/api/user.js'
import { useUserStore } from '@/stores/user.js'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { loginRouterPush } from '@/utils/login/loginRouterPush.js'
import { View, Hide } from '@element-plus/icons-vue'

// 定义 emits
const emit = defineEmits(['changeLoginType'])

//表单相关
const form = reactive({
  username: '',
  password: '',
})
//表单验证相关
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 1, max: 10, message: '长度在 1 到 10 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6,max:20, message: '长度在 6 到 20 个字符', trigger: 'blur' },
  ]
}
//按钮相关
const userStore = useUserStore()
const router = useRouter()
const formRef = ref(null)
// 密码显示控制
const showPassword = ref(false)
const handleLogin = async () => {
  await formRef.value.validate(async (valid) => {
    if (valid) {
      console.log('验证通过，提交登录', form)
      const res = await loginByUsernameAndPassword(form.username, form.password)
      // res.data:token/user
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
  //TODO: 测试方法 最后要禁用
  const adminLogin = async ()=>{
    const res = await loginByUsernameAndPassword('admin', '15hundun')
    // res.data:token/user
    //操作 pinia
    userStore.setToken(res.data.token)
    userStore.setUserInfo(res.data.user)
    ElMessage.success(`Ciallo～(∠・ω< )⌒★${userStore.userInfo?.nickname || userStore.userInfo?.username || '不知名的朋友'}`)
    // 等待状态更新和消息显示
    await new Promise(resolve => setTimeout(resolve, 1500))
    //路由跳转
      await router.push('/admin')
  }
  const userLogin = async ()=>{
    const res = await loginByUsernameAndPassword('user', '15hundun')
    // res.data:token/user
    //操作 pinia
    userStore.setToken(res.data.token)
    userStore.setUserInfo(res.data.user)
    ElMessage.success(`Ciallo～(∠・ω< )⌒★${userStore.userInfo?.nickname || userStore.userInfo?.username || '不知名的朋友'}`)
    // 等待状态更新和消息显示
    await new Promise(resolve => setTimeout(resolve, 1500))
    //路由跳转
    await router.push('/user')
  }



</script>
<template>
  <div class="test-button">
    警告 这里只是一个测试按钮 正式上线时要删除<br>
    <!--    todo:最终上线的删除  -->
    <el-button @click="adminLogin">管理员登录</el-button>
    <el-button @click="userLogin">用户登录</el-button>
  </div>
  <div class="form">
    <div class="logo-box">
      <img src="../../../assets/img/common/logotext.png" alt="这是logo 按理来说不会显示不出来 哈哈">
    </div>
    <el-form
      :model="form"
      :rules="rules"
      ref="formRef"
      label-width="auto"
      style="max-width: 600px">
    <el-form-item prop="username">
      <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="user" >
      </el-input>
    </el-form-item>
    <el-form-item prop="password">
      <el-input
        v-model="form.password"
        placeholder="请输入密码"
        prefix-icon="lock"
        :type="showPassword ? 'text' : 'password'"
      >
        <template #suffix>
          <el-icon @click="showPassword = !showPassword" style="cursor: pointer; font-size: 18px;">
            <View v-if="!showPassword" />
            <Hide v-else />
          </el-icon>
        </template>
      </el-input>
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
