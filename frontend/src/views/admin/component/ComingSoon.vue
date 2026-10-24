<script setup>
import { ref } from 'vue'

const creatorInfo = ref({
  name: '芜柒',
  role: '一个快被AI打倒的程序员艺术家',
  // bio: '',
  contact: 'luofengwuqi@gmail.com'
})

// 控制创作者信息显示状态
const showCreatorInfo = ref(false)

const handleClick = () => {
  showCreatorInfo.value = !showCreatorInfo.value
}

const closeCreatorInfo = () => {
  showCreatorInfo.value = false
}
</script>

<template>
  <div class="coming-soon-container">
    <!-- 主要内容区域 -->
    <div class="content-wrapper" @click="handleClick">
      <!-- 火柴人奔跑图标 -->
      <div class="stickman-container">
        <div class="stickman">
          <!-- 头部 -->
          <div class="head"></div>
          <!-- 身体 -->
          <div class="body"></div>
          <!-- 左臂 -->
          <div class="arm left-arm"></div>
          <!-- 右臂 -->
          <div class="arm right-arm"></div>
          <!-- 左腿 -->
          <div class="leg left-leg"></div>
          <!-- 右腿 -->
          <div class="leg right-leg"></div>
        </div>
      </div>

      <!-- 文本内容 -->
      <div class="text-content">
        <h1 class="main-title">一直在路上...</h1>
        <p class="subtitle">点击查看创作者信息</p>
      </div>
    </div>

    <!-- 创作者信息卡片 - 点击后显示 -->
    <transition name="creator-card">
      <div v-if="showCreatorInfo" class="creator-card" @click.stop>
        <!-- TODO: 添加创作者头像 -->
        <div class="card-header">
          <div class="creator-avatar">
            <img src="@/assets/img/common/wuqi.jpg" alt="创作者头像" class="avatar-icon">
          </div>
          <button class="close-btn" @click="closeCreatorInfo" aria-label="关闭">
            <span>×</span>
          </button>
        </div>

        <div class="card-body">
          <h2 class="creator-name">{{ creatorInfo.name }}</h2>
          <p class="creator-role">{{ creatorInfo.role }}</p>
<!--          <p class="creator-bio">{{ creatorInfo.bio }}</p>-->

          <div class="contact-info">
            <span class="contact-label">联系:</span>
            <a :href="`mailto:${creatorInfo.contact}`" class="contact-link">
              {{ creatorInfo.contact }}
            </a>
          </div>
        </div>

        <!-- TODO: 添加装饰性元素或引用语 -->
        <div class="card-footer">
          <p class="quote">"创造是通往未知的旅程"</p>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped lang="less">
.coming-soon-container {
  position: relative;
  width: 100%;
  height: calc(100vh - 137px);
  min-height: 600px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #FFF8E8;
  overflow: hidden;
}

// 内容包装器
.content-wrapper {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 40px;
  cursor: pointer;
  transition: transform 0.3s ease;

  &:hover {
    transform: translateY(-5px);
  }
}

// 火柴人容器
.stickman-container {
  position: relative;
  width: 200px;
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}

// 火柴人主体
.stickman {
  position: relative;
  width: 80px;
  height: 120px;
}

// 头部
.head {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 20px;
  background: #C3272B;
  border-radius: 50%;
  box-shadow: 0 2px 8px rgba(195, 39, 43, 0.3);
}

// 身体
.body {
  position: absolute;
  top: 22px;
  left: 50%;
  transform: translateX(-50%);
  width: 4px;
  height: 50px;
  background: #C3272B;
  border-radius: 2px;
}

// 手臂
.arm {
  position: absolute;
  top: 28px;
  width: 4px;
  height: 35px;
  background: #C3272B;
  border-radius: 2px;
  transform-origin: top center;

  &.left-arm {
    left: 50%;
    margin-left: -2px;
    animation: armSwingLeft 0.8s infinite ease-in-out;
  }

  &.right-arm {
    left: 50%;
    margin-left: -2px;
    animation: armSwingRight 0.8s infinite ease-in-out;
  }
}

@keyframes armSwingLeft {
  0%, 100% {
    transform: rotate(-35deg);
  }
  50% {
    transform: rotate(35deg);
  }
}

@keyframes armSwingRight {
  0%, 100% {
    transform: rotate(35deg);
  }
  50% {
    transform: rotate(-35deg);
  }
}

// 腿部
.leg {
  position: absolute;
  top: 70px;
  width: 4px;
  height: 40px;
  background: #C3272B;
  border-radius: 2px;
  transform-origin: top center;

  &.left-leg {
    left: 50%;
    margin-left: -2px;
    animation: legSwingLeft 0.8s infinite ease-in-out;
  }

  &.right-leg {
    left: 50%;
    margin-left: -2px;
    animation: legSwingRight 0.8s infinite ease-in-out;
  }
}

@keyframes legSwingLeft {
  0%, 100% {
    transform: rotate(30deg);
  }
  50% {
    transform: rotate(-30deg);
  }
}

@keyframes legSwingRight {
  0%, 100% {
    transform: rotate(-30deg);
  }
  50% {
    transform: rotate(30deg);
  }
}

// 文本内容
.text-content {
  text-align: center;
}

.main-title {
  font-size: 42px;
  font-weight: 700;
  color: #C3272B;
  margin: 0 0 12px 0;
  letter-spacing: 4px;
  text-shadow: 2px 2px 4px rgba(195, 39, 43, 0.1);
}

.subtitle {
  font-size: 16px;
  color: #5A4A42;
  margin: 0;
  letter-spacing: 1px;
  opacity: 0.7;
  transition: opacity 0.3s ease;
}

.content-wrapper:hover .subtitle {
  opacity: 1;
}

// 创作者信息卡片
.creator-card {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 90%;
  max-width: 450px;
  background: #FFFFFF;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 8px 32px rgba(195, 39, 43, 0.15);
  border: 2px solid #FFF8E8;
  z-index: 1000;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.creator-avatar {
  width: 80px;
  height: 80px;
  overflow: hidden;
  background: linear-gradient(135deg, #FFF8E8 0%, #FFEFD5 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(195, 39, 43, 0.1);
  border: 2px solid #C3272B;
}

.avatar-icon {
  font-size: 36px;
  color: #C3272B;
}

.close-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: #FFF8E8;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  font-size: 24px;
  color: #C3272B;
  line-height: 1;

  &:hover {
    background: #C3272B;
    color: #FFFFFF;
    transform: rotate(90deg);
  }
}

.card-body {
  margin-bottom: 24px;
}

.creator-name {
  font-size: 28px;
  font-weight: 700;
  color: #333;
  margin: 0 0 8px 0;
}

.creator-role {
  font-size: 14px;
  color: #999;
  margin: 0 0 16px 0;
  letter-spacing: 1px;
}

.creator-bio {
  font-size: 16px;
  color: #5A4A42;
  line-height: 1.6;
  margin: 0 0 20px 0;
}

.contact-info {
  padding: 16px;
  background: #FFF8E8;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  border: 1px solid #FFEFD5;
}

.contact-label {
  font-size: 12px;
  color: #999;
  letter-spacing: 1px;
}

.contact-link {
  font-size: 14px;
  color: #C3272B;
  text-decoration: none;
  font-weight: 600;
  transition: all 0.3s ease;

  &:hover {
    color: #D64A4F;
    text-decoration: underline;
  }
}

.card-footer {
  padding-top: 20px;
  border-top: 2px solid #FFF8E8;
}

.quote {
  font-size: 14px;
  color: #999;
  font-style: italic;
  margin: 0;
  text-align: center;
}

// 卡片过渡动画
.creator-card-enter-active,
.creator-card-leave-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.creator-card-enter-from {
  opacity: 0;
  transform: translate(-50%, -50%) scale(0.9) translateY(20px);
}

.creator-card-leave-to {
  opacity: 0;
  transform: translate(-50%, -50%) scale(0.95) translateY(-10px);
}

// TODO: 添加响应式设计
@media (max-width: 768px) {
  .main-title {
    font-size: 32px;
    letter-spacing: 2px;
  }

  .creator-card {
    padding: 30px;
    width: 95%;
  }

  .creator-name {
    font-size: 24px;
  }

  .stickman-container {
    width: 150px;
    height: 150px;
  }

  .stickman {
    width: 60px;
    height: 90px;
  }
}
</style>
