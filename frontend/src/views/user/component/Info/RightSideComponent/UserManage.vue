<script setup>
import { useUserStore } from '@/stores/user.js'
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElNotification } from 'element-plus'
import {
  updateNicknameAPI,
  getUserInfoAPI,
  updateEmailAPI,
  updatePhoneAPI,
  sendEmailCode,
  updatePasswordAPI
} from '@/api/user.js'
import inputpic from '@/assets/img/user/info/inputpic.png'

const router = useRouter()
const userStore = useUserStore()

// 获取用户名
const username = computed(() => userStore.userInfo?.username || '')

// 获取昵称
const nickname = computed(() => userStore.userInfo?.nickname || '')

// 获取邮箱
const email = computed(() => userStore.userInfo?.email || '')

// 获取手机号
const phone = computed(() => userStore.userInfo?.phone || '')

// 当前激活的项
const activeIndex = ref(null)

// 显示提示
const showHint = ref(false)

// 显示图片（用于其他项未掉落完成时）
const showInputPic = ref(false)
const picTimeout = ref(null)
const debugMode = ref(true) // 开启调试模式

// 验证码发送时间记录（用于频率限制）
const lastSendTime = ref(0)

// 表单数据
const formRef = ref(null)
const formData = ref({
  newValue: '',
  password: '',
  emailCode: ''
})

// 表单验证规则
const formRules = {
  newValue: [
    { required: true, message: '请输入新值', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ],
  emailCode: [
    { required: true, message: '请输入邮箱验证码', trigger: 'blur' }
  ]
}

// 获取当前激活项的标题
const getActiveTitle = () => {
  const titles = {
    1: '您更希望被称呼为',
    3: '您的邮箱',
    4: '您的手机号'
  }
  return titles[activeIndex.value] || ''
}

// 获取占位符文本
const getPlaceholder = () => {
  const placeholders = {
    1: '新的名字...',
    2: '新的密码...',
    3: '新的地址...',
    4: '新的数字...'
  }
  return placeholders[activeIndex.value] || '请输入新值'
}

// 图片加载成功
const handleImageLoad = () => {
  console.log('图片加载成功:', inputpic)
  debugMode.value = false
}

// 图片加载失败
const handleImageError = () => {
  console.error('图片加载失败:', inputpic)
  console.log('当前组件所在路径:', import.meta.url)
  debugMode.value = true
}

// 处理点击
const handleItemClick = async (index) => {
  activeIndex.value = index

  // 如果点击的是昵称(1)、密码(2)、邮箱(3)或手机号(4)，同时显示图片和表单
  if (index === 1 || index === 2 || index === 3 || index === 4) {
    showInputPic.value = true
    // 重置表单
    formData.value = {
      newValue: '',
      password: '',
      emailCode: ''
    }

    if (index === 2) {
      // 检查是否在1分钟内重复发送
      const now = Date.now()
      const timeDiff = now - lastSendTime.value

      if (timeDiff < 60000) { // 60秒 = 60000毫秒
        // 1分钟内，显示提示
        const remainingSeconds = Math.ceil((60000 - timeDiff) / 1000)
        ElNotification({
          title: '验证码在路上...',
          message: `请注意查收，${remainingSeconds}秒后才能生成新的验证码`,
          type: 'warning',
          position: 'top-right',
          duration: 3000
        })
        return // 直接返回，不调用API
      }

      try {
        console.log('TODO: 准备发送邮箱验证码')
        console.log('当前用户邮箱:', email.value)
        // 跳过拦截器的默认错误处理，由本组件自行处理
        await sendEmailCode(email.value, true)
        // 记录发送时间
        lastSendTime.value = Date.now()
        ElNotification({
          title: '已发送验证码(・ω< )',
          message: '请耐心等待...',
          type: 'success',
          position: 'top-right'
        })
      } catch (error) {
        console.error('TODO: 发送验证码失败', error)

        // 检查是否为404错误
        if (error.response && error.response.status === 404) {
          ElNotification({
            title: '您未开启邮箱功能...',
            message: '请添加邮箱后再修改密码...',
            type: 'warning',
            position: 'top-right',
            duration: 3000
          })
          // 自动点击复位按钮
          setTimeout(() => {
            handleReset()
          }, 3000)
        }
      }
    }
  } else {
    showInputPic.value = false
  }

  // 如果点击的是名字（索引0），在掉落动画后显示提示
  if (index === 0) {
    setTimeout(() => {
      showHint.value = true
    }, 800)
  } else {
    showHint.value = false
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const type = activeIndex.value
        const newValue = formData.value.newValue
        const password = formData.value.password

        // 根据类型调用不同的API
        if (type === 1) {
          // 更新昵称
          await updateNicknameAPI(newValue, password)
          ElMessage.success('昵称修改成功 (∠・ω< )⌒★')
        } else if (type === 2) {
          await updatePasswordAPI(formData.value.emailCode, newValue)
          console.log('邮箱验证码:', formData.value.emailCode)
          console.log('新密码:', newValue)
          ElMessage.success('密码修改成功 (∠・ω< )⌒★')
        } else if (type === 3) {
          await updateEmailAPI(newValue, password)
          ElMessage.success('邮箱修改成功 (∠・ω< )⌒★')
        } else if (type === 4) {
          await updatePhoneAPI(newValue, password, formData.value.emailCode)
          ElMessage.success('手机号修改成功 (∠・ω< )⌒★')
        }

        // 更新成功后刷新用户信息
        const res = await getUserInfoAPI()
        userStore.setUserInfo(res.data)

        // 重置表单
        handleReset()
      } catch (error) {
        console.error('更新失败:', error)
        // ElMessage 已由 request.js 拦截器处理，无需重复提示
        // 只需重置表单关闭
        handleReset()
      }
    } else {
      console.log('表单验证失败')
      return false
    }
  })
}

// 复位
const handleReset = () => {
  activeIndex.value = null
  showHint.value = false
  showInputPic.value = false
  if (picTimeout.value) clearTimeout(picTimeout.value)
  // 重置表单
  formData.value = {
    newValue: '',
    password: '',
    emailCode: ''
  }
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

// 联系管理员
const handleContactAdmin = () => {
  router.replace('/help')
}

// 禁止滚动
const preventScroll = (e) => {
  e.preventDefault()
}

onMounted(() => {
  // 添加滚动禁止监听
  window.addEventListener('wheel', preventScroll, { passive: false })
  window.addEventListener('touchmove', preventScroll, { passive: false })
})

onUnmounted(() => {
  // 移除滚动禁止监听
  window.removeEventListener('wheel', preventScroll)
  window.removeEventListener('touchmove', preventScroll)
})
</script>

<template>
  <div class="user-manage" :class="{ 'has-active': activeIndex !== null }">
    <div class="reset-btn" v-if="activeIndex !== null" @click.stop="handleReset">
      <span>复位</span>
    </div>
    <div class="info-item" :class="{ 'active': activeIndex === 0, 'fall': activeIndex !== null && activeIndex !== 0 }" @click="handleItemClick(0)">
      <div class="info-label">
        <span class="label-title">您的名字</span>
        <span class="label-value" :class="{ unset: !username }">{{ username }}</span>
      </div>
      <p class="info-desc">名字常常用于锚定存在 不可更改</p>
    </div>

    <div class="info-item" :class="{ 'active': activeIndex === 1, 'fall': activeIndex !== null && activeIndex !== 1 }" @click="handleItemClick(1)">
      <div class="info-label">
        <span class="label-title">您更希望被称呼为</span>
        <span class="label-value" :class="{ unset: !nickname }">{{ nickname || '未设置' }}</span>
      </div>
      <p class="info-desc">名字同样用于记录变革...</p>
    </div>

    <div class="info-item" :class="{ 'active': activeIndex === 2, 'fall': activeIndex !== null && activeIndex !== 2 }" @click="handleItemClick(2)">
      <div class="info-label">
        <span class="label-title">您的密码</span>
        <span class="label-value unset">****</span>
      </div>
      <p class="info-desc">嘘...</p>
    </div>

    <div class="info-item" :class="{ 'active': activeIndex === 3, 'fall': activeIndex !== null && activeIndex !== 3 }" @click="handleItemClick(3)">
      <div class="info-label">
        <span class="label-title">您的邮箱</span>
        <span class="label-value" :class="{ unset: !email }">{{ email || '未设置' }}</span>
      </div>
      <p class="info-desc">
        <span v-if="email">更多的时候不是用于写信...</span>
        <span v-else>或许会错过些什么呢...</span>
      </p>
    </div>

    <div class="info-item" :class="{ 'active': activeIndex === 4, 'fall': activeIndex !== null && activeIndex !== 4 }" @click="handleItemClick(4)">
      <div class="info-label">
        <span class="label-title">您的手机号</span>
        <span class="label-value" :class="{ unset: !phone }">{{ phone || '未设置' }}</span>
      </div>
      <p class="info-desc">
        <span v-if="phone">一串数字...</span>
        <span v-else>或许会错过些什么呢...</span>
      </p>
    </div>

    <!-- 掉落中的图片提示 -->
    <div class="input-pic-box" v-if="showInputPic">
      <img
        :src="inputpic"
        alt="input hint"
        @error="handleImageError"
        @load="handleImageLoad"
      />
      <!-- 调试信息 -->
      <p class="debug-info" v-if="debugMode">图片路径: {{ inputpic }}</p>
    </div>

    <!-- 昵称、密码、邮箱、手机号被选中时的表单 -->
    <div class="edit-form-box" v-if="activeIndex === 1 || activeIndex === 2 || activeIndex === 3 || activeIndex === 4">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="auto"
        class="edit-form"
      >
        <el-form-item prop="newValue" v-if="activeIndex !== 2">
          <el-input
            v-model="formData.newValue"
            :placeholder="getPlaceholder()"
            clearable
          />
        </el-form-item>
        <el-form-item prop="password" v-if="activeIndex !== 2">
          <el-input
            v-model="formData.password"
            type="password"
            placeholder="请输入密码以确认身份"
            show-password
            clearable
          />
        </el-form-item>
        <!-- 密码修改专用表单 -->
        <template v-if="activeIndex === 2">
          <el-form-item prop="emailCode">
            <el-input
              v-model="formData.emailCode"
              placeholder="请输入邮箱验证码"
              clearable
            />
          </el-form-item>
          <el-form-item prop="newValue">
            <el-input
              v-model="formData.newValue"
              type="password"
              :placeholder="getPlaceholder()"
              show-password
              clearable
            />
          </el-form-item>
        </template>
        <!-- 手机号修改时的验证码 -->
        <el-form-item prop="emailCode" v-if="activeIndex === 4">
          <el-input
            v-model="formData.emailCode"
            placeholder="请输入邮箱验证码"
            clearable
          />
        </el-form-item>
        <div class="form-buttons">
          <el-button type="primary" @click="handleSubmit">确认修改</el-button>
        </div>
      </el-form>
    </div>

    <!-- 名字被选中时的提示 -->
    <div class="hint-box" v-if="showHint">
      <p class="hint-text">或许可以考虑修改称呼哦</p>
      <p class="hint-subtext">也可以联系会魔法但很懒的管理员们帮你改改名字 (・ω< )</p>
      <button class="hint-btn" @click.stop="handleContactAdmin">联系管理员</button>
    </div>
  </div>
</template>

<style scoped lang="less">
//@import url('https://fonts.googleapis.com/css2?family=Ma+Shan+Zheng&family=ZCOOL+KuaiLe&family=Caveat:wght@400;600;700&family=Indie+Flower&display=swap');

.user-manage {
  padding: 40px 30px;
  font-family: 'Caveat', 'Indie Flower', 'Ma Shan Zheng', 'ZCOOL KuaiLe', cursive, sans-serif;
  position: relative;
  min-height: 84vh;

  .reset-btn {
    position: absolute;
    top: 20px;
    right: 30px;
    padding: 8px 20px;
    background: linear-gradient(135deg, #C3272B 0%, #B91F24 100%);
    color: #fff;
    font-size: 16px;
    font-weight: 600;
    border-radius: 20px;
    cursor: pointer;
    box-shadow: 0 4px 12px rgba(195, 39, 43, 0.3);
    transition: all 0.3s ease;
    z-index: 100;
    animation: fadeIn 0.3s ease-out;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 16px rgba(195, 39, 43, 0.4);
      background: linear-gradient(135deg, #D63A3F 0%, #C3272B 100%);
    }

    &:active {
      transform: translateY(0);
    }
  }

  .hint-box {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    text-align: center;
    z-index: 20;
    animation: hintFadeIn 0.6s ease-out;

    .hint-text {
      font-size: 28px;
      color: #2c2420;
      font-weight: 700;
      margin-bottom: 10px;
      text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
    }

    .hint-subtext {
      font-size: 18px;
      color: #8b7d6f;
      margin-bottom: 25px;
      font-style: italic;
    }

    .hint-btn {
      padding: 12px 30px;
      background: linear-gradient(135deg, #5a4a42 0%, #6b5b4f 100%);
      color: #fff;
      font-size: 18px;
      font-weight: 600;
      border: none;
      border-radius: 25px;
      cursor: pointer;
      box-shadow: 0 4px 12px rgba(90, 74, 66, 0.3);
      transition: all 0.3s ease;
      font-family: 'Caveat', 'Indie Flower', 'Ma Shan Zheng', 'ZCOOL KuaiLe', cursive, sans-serif;

      &:hover {
        transform: translateY(-2px) scale(1.05);
        box-shadow: 0 6px 16px rgba(90, 74, 66, 0.4);
        background: linear-gradient(135deg, #6b5b4f 0%, #7c6b5f 100%);
      }

      &:active {
        transform: translateY(0) scale(1);
      }
    }
  }

  .input-pic-box {
    opacity: 0.3;
    width: 700px;
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    z-index: 15;
    animation: picFadeIn 0.4s ease-out;

    img {
      width: 100%;
      height: auto;
      filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.15));
    }

    .debug-info {
      margin-top: 10px;
      font-size: 14px;
      color: #C3272B;
      background: rgba(255, 255, 255, 0.9);
      padding: 8px 12px;
      border-radius: 8px;
      word-break: break-all;
    }
  }

  .edit-form-box {
    position: absolute;
    top: 57%;
    left: 51%;
    transform: translate(-50%, -50%);
    z-index: 20;
    animation: formFadeIn 0.5s ease-out;


    .edit-form {
      transform: rotate(-12deg);
      width: 100%;

      :deep(.el-form-item) {
        display: flex;
        flex-direction: column;
        align-items: center;
        margin-bottom: 22px;
      }

      :deep(.el-form-item__content) {
        display: flex;
        justify-content: center;
        width: 100%;
        margin-left: 0 !important;
      }

      :deep(.el-form-item__error) {
        text-align: center;
        position: static;
        margin-top: 8px;
        padding-left: 0;
      }

      :deep(.el-input__wrapper) {
        background: transparent;
        box-shadow: none;
        border-radius: 0;

        &:hover,
        &.is-focus {
          box-shadow: none;
        }
      }

      :deep(.el-input__inner) {
        font-size: 34px;
        font-family: 'Caveat', 'Indie Flower', 'Ma Shan Zheng', 'ZCOOL KuaiLe', cursive, sans-serif;
        color: inherit;
        text-align: center;
      }

      .form-buttons {
        display: flex;
        gap: 15px;
        justify-content: center;
        margin-top: 200px;

        .el-button {
          min-width: 120px;
          font-size: 24px;
          font-weight: 600;
          border-radius: 20px;
          font-family: 'Caveat', 'Indie Flower', 'Ma Shan Zheng', 'ZCOOL KuaiLe', cursive, sans-serif;
          transition: all 0.3s ease;
          background: transparent;
          border: none;
          color: #a8a8a8;

          &:hover {
            transform: translateY(-2px);
            background: transparent;
          }
        }
      }
    }
  }

  .info-item {
    position: absolute;
    padding: 15px 20px;
    opacity: 0;
    animation: fadeIn 0.8s ease-out forwards;
    cursor: pointer;
    transition: all 0.6s cubic-bezier(0.4, 0, 0.2, 1);

    // 掉落动画
    &.fall {
      animation: fallDown 0.8s ease-in forwards !important;

      .info-label .label-title::after {
        opacity: 0;
      }

      .info-desc {
        opacity: 0;
        transform: translateY(-10px);
        transition: all 0.3s ease;
      }
    }

    // 激活状态
    &.active {
      top: 2% !important;
      left: 3% !important;
      right: auto !important;
      bottom: auto !important;
      transform: rotate(0deg) !important;
      animation: activeZoom 0.6s ease-out forwards !important;
      z-index: 10;

      .info-desc {
        opacity: 0;
        transform: translateY(-10px);
        transition: all 0.3s ease;
      }
    }

    &:nth-child(1) {
      top: 6%;
      left: 12%;
      transform: rotate(-3deg);
      animation-delay: 0.1s;
    }

    &:nth-child(2) {
      top: 12%;
      right: 8%;
      transform: rotate(2.5deg);
      animation-delay: 0.2s;
    }

    &:nth-child(3) {
      top: 35%;
      left: 5%;
      transform: rotate(-1.5deg);
      animation-delay: 0.3s;
    }

    &:nth-child(4) {
      top: 48%;
      right: 15%;
      transform: rotate(3deg);
      animation-delay: 0.4s;
    }

    &:nth-child(5) {
      bottom: 10%;
      left: 15%;
      transform: rotate(-2deg);
      animation-delay: 0.5s;
    }

    &:nth-child(6) {
      bottom: 20%;
      right: 15%;
      transform: rotate(2deg);
    }

    &:nth-child(7) {
      bottom: 5%;
      left: 20%;
      transform: rotate(-1.5deg);
    }

    .info-label {
      display: flex;
      align-items: baseline;
      gap: 15px;
      margin-bottom: 10px;
      flex-wrap: wrap;

      .label-title {
        font-size: 22px;
        color: #2c3e50;
        font-weight: 600;
        letter-spacing: 2px;
        position: relative;

        &::after {
          content: '';
          position: absolute;
          bottom: -4px;
          left: 0;
          width: 100%;
          height: 2px;
          background: #d4a574;
          opacity: 0.4;
          transform: rotate(-1deg);
        }
      }

      .label-value {
        font-size: 38px;
        color: #2c2420;
        font-weight: 700;
        position: relative;
        text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.08);
        margin-left: 5px;

        &::before {
          content: '→ ';
          color: #C3272B;
          font-size: 32px;
        }
      }
    }

    .info-desc {
      margin: 0;
      padding-left: 15px;
      font-size: 22px;
      color: #8b7d6f;
      line-height: 1.9;
      font-style: italic;
      letter-spacing: 0.5px;
      position: relative;
      font-weight: 500;

      &::before {
        content: '✎ ';
        color: #c4b5a5;
        font-size: 20px;
      }

      .hint {
        display: block;
        margin-top: 5px;
        color: #C3272B;
        font-size: 20px;
        opacity: 0.8;
      }
    }
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes fallDown {
  0% {
    opacity: 1;
    transform: translateY(0);
  }
  100% {
    opacity: 0;
    transform: translateY(120vh) rotate(20deg);
  }
}

@keyframes activeZoom {
  0% {
    opacity: 0;
    transform: scale(0.8);
  }
  100% {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes hintFadeIn {
  0% {
    opacity: 0;
    transform: translate(-50%, -50%) scale(0.8);
  }
  100% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1);
  }
}

@keyframes formFadeIn {
  0% {
    opacity: 0;
    transform: translate(-50%, -50%) scale(0.9);
  }
  100% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1);
  }
}
</style>
