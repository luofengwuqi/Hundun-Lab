<script setup>
import { ref } from 'vue'
import { getInviteCode } from '@/api/admin.js'

const isPressing = ref(false)
const chargeProgress = ref(0)
const isCharged = ref(false)
const showProgress = ref(true)
const inviteCode = ref('')
let chargeTimer = null

const startCharge = () => {
  if (isCharged.value || isPressing.value) return

  isPressing.value = true
  chargeProgress.value = 0
  showProgress.value = true

  const startTime = Date.now()
  const duration = 800

  const animate = async () => {
    const elapsed = Date.now() - startTime
    const progress = Math.min((elapsed / duration) * 100, 100)
    chargeProgress.value = progress

    if (progress < 100 && isPressing.value) {
      requestAnimationFrame(animate)
    } else if (progress >= 100) {
      console.log('充能完成!')
      isCharged.value = true
      isPressing.value = false
      showProgress.value = false

      try {
        const res = await getInviteCode()
        inviteCode.value = res.data
      } catch (error) {
        console.error('获取邀请码失败:', error)
        inviteCode.value = '获取失败，请重试'
      }

      setTimeout(() => {
        chargeProgress.value = 0
      }, 300)
    }
  }

  requestAnimationFrame(animate)
}

const endCharge = () => {
  isPressing.value = false

  if (chargeProgress.value < 100 && !isCharged.value) {
    showProgress.value = false
    setTimeout(() => {
      chargeProgress.value = 0
    }, 300)
  }
}

const resetCharge = () => {
  isCharged.value = false
  chargeProgress.value = 0
  isPressing.value = false
  showProgress.value = true
  inviteCode.value = ''
}
</script>

<template>
  <div class="charge-button-wrapper">
    <div
      class="charge-button"
      :class="{ pressing: isPressing, charged: isCharged }"
      @mousedown="startCharge"
      @mouseup="endCharge"
      @mouseleave="endCharge"
      @touchstart.prevent="startCharge"
      @touchend="endCharge"
    >
      <transition name="progress">
        <div
          v-if="showProgress && chargeProgress > 0 && chargeProgress < 100"
          class="fill-container"
        >
          <div
            class="fill-progress"
            :style="{ width: chargeProgress + '%' }"
          ></div>
        </div>
      </transition>

      <div
        v-if="isCharged"
        class="fill-complete"
      ></div>

      <span class="button-text">
        <template v-if="isCharged">
          <div class="charged-content">
            <div class="success-text">邀请码生成成功！(*´∀`)~♥</div>
            <div class="invite-code-wrapper">
              <span class="invite-code-label">您的邀请码是：</span>
              <span class="invite-code">{{ inviteCode }}</span>
            </div>
            <div class="tip-text">请及时拷贝并保存 并通知受邀请的管理员注册！</div>
          </div>
        </template>
        <template v-else>
          长按以生成邀请码
        </template>
      </span>
    </div>
  </div>
</template>

<style scoped>
.charge-button-wrapper {
  height: 80vh;
  width: 100%;
  padding: 20px;
}

.charge-button {
  height: 80vh;
  width: 100%;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ffffff 0%, #f5f5f5 100%);
  border-radius: 16px;
  cursor: pointer;
  user-select: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1),
              0 2px 4px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  text-align: center;
}

.charge-button:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15),
              0 4px 8px rgba(0, 0, 0, 0.08);
}

.charge-button.pressing {
  transform: scale(0.95) translateY(0);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1),
              0 1px 2px rgba(0, 0, 0, 0.05);
}

.charge-button.charged {
  background: transparent;
  box-shadow: 0 6px 20px rgba(195, 39, 43, 0.4),
              0 0 30px rgba(195, 39, 43, 0.3);
  cursor: default;
}

.fill-container {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  z-index: 1;
  overflow: hidden;
}

.fill-progress {
  height: 100%;
  width: 0;
  background: linear-gradient(to right, #C3272B 0%, #D63A3F 50%, #E84A50 100%);
  transition: width 0.05s linear;
  box-shadow: 0 0 20px rgba(195, 39, 43, 0.4);
}

.progress-enter-active,
.progress-leave-active {
  transition: all 0.3s ease;
}

.progress-enter-from {
  opacity: 0;
}

.progress-enter-to {
  opacity: 1;
}

.progress-leave-from {
  opacity: 1;
}

.progress-leave-to {
  opacity: 0;
  transform: scaleX(0.8);
  transform-origin: left;
}

.fill-complete {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #C3272B 0%, #D63A3F 100%);
  animation: fadeIn 0.3s ease-out;
  z-index: 2;
}

@keyframes fadeIn {
  0% {
    opacity: 0;
  }
  100% {
    opacity: 1;
  }
}

.button-text {
  position: relative;
  z-index: 10;
  font-size: clamp(20px, 4vw, 36px);
  font-weight: 700;
  color: #333333;
  transition: color 0.3s ease;
  letter-spacing: 2px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.charged-button.charged .button-text {
  color: #ffffff;
  text-shadow: 0 2px 8px rgba(255, 255, 255, 0.3);
}

.charged-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 20px;
}

.success-text {
  font-size: clamp(18px, 3vw, 28px);
  font-weight: 600;
}

.invite-code-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
}

.invite-code-label {
  font-size: clamp(16px, 2.5vw, 22px);
  opacity: 0.9;
}

.invite-code {
  font-size: clamp(24px, 5vw, 48px);
  font-weight: 700;
  font-family: 'Courier New', monospace;
  letter-spacing: 4px;
  padding: 12px 24px;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 12px;
  user-select: all;
  cursor: pointer;
  transition: all 0.2s ease;
}

.invite-code:hover {
  background: rgba(255, 255, 255, 0.35);
  transform: scale(1.05);
}

.tip-text {
  font-size: clamp(12px, 2vw, 16px);
  opacity: 0.8;
  margin-top: 8px;
}

.charge-button.charged::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 16px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.4) 0%, transparent 70%);
  animation: pulse 1s ease-in-out infinite;
  pointer-events: none;
  z-index: 3;
}

@keyframes pulse {
  0%, 100% {
    opacity: 0.5;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.05);
  }
}
</style>
