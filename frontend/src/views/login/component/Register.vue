<script setup>
import { reactive, ref, watch } from 'vue'
import { useUserStore } from '@/stores/user.js'
import { useRouter } from 'vue-router'
import { ArrowLeft, QuestionFilled, Message, View, Hide } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { addUser } from '@/api/user.js'
import { addAdmin } from '@/api/admin.js'

// 定义 emits
const emit = defineEmits(['changeLoginType','changeDialogShow'])

const handleBack = () => {
  emit('changeLoginType', 'username-password')
}


//表单相关
const form = reactive({
  importantPassword: '',
  username: '',
  password: '',
  passwordAgain:'',
  phone:'',
  email: ''
})
//表单验证相关
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 1, max: 10, message: '长度在 1 到 10 个字符', trigger: 'blur' },
  ],
  importantPassword: [
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' },
  ],
  passwordAgain: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' },
    {
      trigger: 'blur',
      validator: (rule, value, callback) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      }
    }
  ],

  phone: [
    {
      trigger: 'blur',
      validator: (rule, value, callback) => {
        // 如果为空，直接通过验证
        if (!value || value === '') {
          callback()
          return
        }
        const phoneStr = String(value).replace(/\s+/g, '')
        if (!/^1[3456789]\d{9}$/.test(phoneStr)) {
          callback(new Error('请输入正确的手机号'))
        } else {
          callback()
        }
      }
    }
  ],
  email: [
    {
      trigger: 'blur',
      validator: (rule, value, callback) => {
        // 如果为空，直接通过验证
        if (!value) {
          callback()
          return
        }
        if (!/^[a-zA-Z0-9._-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/.test(value)) {
          callback(new Error('请输入正确的邮箱'))
        } else {
          callback()
        }
      }
    }
  ]
}

//按钮相关
const userStore = useUserStore()
const router = useRouter()
const formRef = ref(null)
const adminOrNot =ref(false)
// 密码显示控制
const showPassword = ref(false)
const showPasswordAgain = ref(false)
watch(adminOrNot, (newVal) => {
  // 0:index
  emit('changeDialogShow', [0,newVal])
})
const dialogVisible = ref(false)
watch(dialogVisible, (newVal) => {
    emit('changeDialogShow', [1,newVal])
})


const handleRegister = async () => {
  const valid = await formRef.value.validate()
  if (!valid) {
    console.log('表单验证失败')
    return false
  }
  console.log(form.importantPassword)
  console.log(adminOrNot.value)
  //管理员注册
  if (adminOrNot.value){
    const dto = {
      username: form.username,
      password: form.password,
      importantPassword: form.importantPassword
    }
    if (form.phone) {
      dto.phone = form.phone
    }
    if (form.email) {
      dto.email = form.email
    }
    await addAdmin(dto)
  }
  //用户注册
  else {
    const dto = {
      username: form.username,
      password: form.password
    }

    // 只有当手机号不为空时才添加
    if (form.phone) {
      dto.phone = form.phone
    }

    // 只有当邮箱不为空时才添加
    if (form.email) {
      dto.email = form.email
    }
    await addUser(dto)

  }
  ElMessage.success(`欢迎!${form.username}`)
  await new Promise(resolve => setTimeout(resolve, 1000))
  handleBack()
}
</script>
<template>

  <div class="form">
    <el-form
      :model="form"
      :rules="rules"
      ref="formRef"
      label-width="auto"
      style="max-width: 600px">
      <ArrowLeft class="back-icon" @click="handleBack"/>
<!--   表头部分   -->
      <QuestionFilled class="help" @click="dialogVisible = true"/>
      <div class="logo-box">
        <img src="@/assets/img/login/zhuce.png" alt="">
      </div>
<!--   表单项   -->
      <el-form-item prop="importantPassword" v-if="adminOrNot">
        <el-input v-model.trim="form.importantPassword" placeholder="请输入管理员邀请码" prefix-icon="lock"></el-input>
      </el-form-item>
      <el-form-item prop="username">
        <el-input v-model.trim="form.username" placeholder="请输入用户名" prefix-icon="user" >
        </el-input>
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          v-model.trim="form.password"
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
      <el-form-item prop="passwordAgain">
        <el-input
          v-model.trim="form.passwordAgain"
          placeholder="请再次输入密码"
          prefix-icon="lock"
          :type="showPasswordAgain ? 'text' : 'password'"
        >
          <template #suffix>
            <el-icon @click="showPasswordAgain = !showPasswordAgain" style="cursor: pointer; font-size: 18px;">
              <View v-if="!showPasswordAgain" />
              <Hide v-else />
            </el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item prop="phone">
        <el-input v-model.trim="form.phone" placeholder="请输入手机号" prefix-icon="phone"></el-input>
      </el-form-item>
      <el-form-item prop="email">
        <el-input v-model.trim="form.email" placeholder="请输入邮箱" :prefix-icon="Message"></el-input>
      </el-form-item>
<!--   管理员注册开关   -->
      <el-switch
        v-model="adminOrNot"
        class="ml-2"
        inline-prompt
        style="--el-switch-on-color:  #C3272B; --el-switch-off-color: #AAA"
        active-text="以管理员身份注册"
        inactive-text="以用户身份注册"
      />
<!--   注册按钮    -->
      <el-button type="primary" @click="handleRegister">注册</el-button>
      <router-link to="/help">
        寻求帮助
      </router-link>
    </el-form>
  </div>
</template>

<style scoped lang="less">

.form{
  position: relative;
  padding: 50px;
  border-radius: 20px;
  background-color: #fff;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.08);
  width: 460px;
  .back-icon{
    position: absolute;

    height: 30px;
    width: 30px;
  }
  .help{
    width: 2em;
    height: 1em;
    position: absolute;
    right: 10%;
    color: #666666;
  }
  .back-icon:hover,
  .help:hover{
    color: #C3272B
  }
  .logo-box{
    width: 50%;
    margin-right: 25%;
    margin-left: 25%;
    height: 100px;
    img{
      width: 100%;
      height: 100%;
      object-fit: contain;
    }

  }
  a{
    display: block;
    width: 25%;
    margin-left: 80%;
    color: #AAA;
  }
  a:hover{
    color: #666;
  }
  .el-switch{
    margin-left: 65%;
    margin-bottom: 10px;
  }
  /* 专门放大表单标签字体 */
  :deep(.el-form-item) {
    margin-bottom: 15px;
  }
  :deep(.el-input__inner) {
    font-size: 13px;
    height: 30px;
  }
    .el-button{
      width: 98%;
      background-color: #C3272B;
      border: none;
      margin-bottom: 20px;
    }
    .el-button:hover{
      background-color: #D64A4F;
    }
 }
</style>
