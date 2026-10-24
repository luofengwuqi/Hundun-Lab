<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

// 定义功能分区配置 - 易于扩展
const operationSections = ref([
  {
    id: 'user-management',
    title: '缓存管理',
    icon: '',
    description: 'redis相关的操作',
    operations: [
      { label: 'test', type: 'success', action: () => handleOperation('test') },
    ]
  },

])

// 处理操作点击
const handleOperation = (operationName) => {
  ElMessage.success(`执行操作: ${operationName}`)
  if(operationName === 'test'){
    ElMessage.warning(`该功能已移除`)
  }
  console.log(`执行操作: ${operationName}`)
}

// 清除本地存储
const handleClearStorage = () => {
  localStorage.clear()
  sessionStorage.clear()
  ElMessage.success('本地存储已清除')
}
</script>

<template>
  <div class="operation-panel">
    <!-- 页面标题 -->
    <div class="panel-header">
      <h1 class="panel-title">操作面板</h1>
      <p class="panel-subtitle">集中管理系统各项功能</p>
    </div>

    <!-- 功能分区网格 -->
    <div class="sections-grid">
      <div
        v-for="section in operationSections"
        :key="section.id"
        class="section-card"
      >
        <!-- 分区头部 -->
        <div class="section-header">
          <span class="section-icon">{{ section.icon }}</span>
          <div class="section-info">
            <h2 class="section-title">{{ section.title }}</h2>
            <p class="section-description">{{ section.description }}</p>
          </div>
        </div>

        <!-- 操作按钮组 -->
        <div class="operations-grid">
          <button
            v-for="(op, index) in section.operations"
            :key="index"
            class="operation-btn"
            :class="`btn-${op.type}`"
            @click="op.action"
          >
            {{ op.label }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="less">
.operation-panel {
  padding: 24px;
  min-height: 100%;
  background: linear-gradient(135deg, #FFFBF8 0%, #FFF8E8 100%);
}

// 页面头部
.panel-header {
  margin-bottom: 32px;
  text-align: center;
  animation: fadeInDown 0.6s ease-out;
}

.panel-title {
  font-size: 32px;
  font-weight: 700;
  color: #C3272B;
  margin: 0 0 8px 0;
  letter-spacing: 2px;
  text-shadow: 2px 2px 4px rgba(195, 39, 43, 0.1);
}

.panel-subtitle {
  font-size: 16px;
  color: #5A4A42;
  margin: 0;
  opacity: 0.8;
}

// 分区网格布局
.sections-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 24px;
  animation: fadeInUp 0.6s ease-out 0.2s both;
}

// 分区卡片
.section-card {
  background: #FFFFFF;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 16px rgba(195, 39, 43, 0.08);
  border: 2px solid transparent;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 4px;
    background: linear-gradient(90deg, #C3272B 0%, #D64A4F 100%);
    transform: scaleX(0);
    transform-origin: left;
    transition: transform 0.3s ease;
  }

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(195, 39, 43, 0.15);
    border-color: rgba(195, 39, 43, 0.2);

    &::before {
      transform: scaleX(1);
    }
  }
}

// 分区头部
.section-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid #FFF8E8;
}

.section-icon {
  font-size: 36px;
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #FFF8E8 0%, #FFEFD5 100%);
  border-radius: 12px;
  flex-shrink: 0;
}

.section-info {
  flex: 1;
}

.section-title {
  font-size: 20px;
  font-weight: 700;
  color: #333;
  margin: 0 0 4px 0;
}

.section-description {
  font-size: 14px;
  color: #999;
  margin: 0;
}

// 操作按钮网格
.operations-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

// 操作按钮基础样式
.operation-btn {
  padding: 14px 20px;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  color: #FFFFFF;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);

  &::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    width: 0;
    height: 0;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.3);
    transform: translate(-50%, -50%);
    transition: width 0.6s, height 0.6s;
  }

  &:active::before {
    width: 300px;
    height: 300px;
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  }

  &:active {
    transform: translateY(0);
  }
}

// 按钮类型样式
.btn-primary {
  background: linear-gradient(135deg, #C3272B 0%, #B91F24 100%);

  &:hover {
    background: linear-gradient(135deg, #D64A4F 0%, #C3272B 100%);
    box-shadow: 0 6px 16px rgba(195, 39, 43, 0.35);
  }
}

.btn-success {
  background: linear-gradient(135deg, #52c41a 0%, #49b018 100%);

  &:hover {
    background: linear-gradient(135deg, #73d13d 0%, #52c41a 100%);
    box-shadow: 0 6px 16px rgba(82, 196, 26, 0.35);
  }
}

.btn-warning {
  background: linear-gradient(135deg, #faad14 0%, #e6a20f 100%);

  &:hover {
    background: linear-gradient(135deg, #ffc542 0%, #faad14 100%);
    box-shadow: 0 6px 16px rgba(250, 173, 20, 0.35);
  }
}

.btn-danger {
  background: linear-gradient(135deg, #ff4d4f 0%, #f5222d 100%);

  &:hover {
    background: linear-gradient(135deg, #ff7875 0%, #ff4d4f 100%);
    box-shadow: 0 6px 16px rgba(255, 77, 79, 0.35);
  }
}

.btn-info {
  background: linear-gradient(135deg, #1890ff 0%, #167ce6 100%);

  &:hover {
    background: linear-gradient(135deg, #40a9ff 0%, #1890ff 100%);
    box-shadow: 0 6px 16px rgba(24, 144, 255, 0.35);
  }
}

// 动画
@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// 响应式设计
@media (max-width: 768px) {
  .sections-grid {
    grid-template-columns: 1fr;
  }

  .operations-grid {
    grid-template-columns: 1fr;
  }

  .panel-title {
    font-size: 24px;
  }
}
</style>
