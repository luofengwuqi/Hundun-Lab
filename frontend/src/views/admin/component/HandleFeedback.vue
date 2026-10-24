<template>
  <div class="feedback-container">
    <el-empty v-if="!loading && feedbackList.length === 0" description="暂无反馈数据" />
    <div class="button">
      <el-button
        type="primary"
        @click="fetchFeedback"
        :loading="loading"
        :disabled="loading"
      >
        刷新
      </el-button>
      <el-button
        type="danger"
        @click="deleteHandledFeedback"
        >
        一键移除所有已处理反馈
      </el-button>
    </div>
    <div v-loading="loading" class="feedback-grid">
      <transition-group name="feedback-list">
        <el-card
          v-for="(item, index) in feedbackList"
          :key="index"
          class="feedback-card"
          shadow="hover"
        >
          <template #header>
            <div class="card-header">
              <span class="feedback-type">{{ item.type }}</span>
              <el-tag :type="getStatusType(item.status)" size="small">
                {{ getStatusText(item.status) }}
              </el-tag>
            </div>
          </template>

          <div class="card-body">
            <div class="info-item">
              <span class="label">反馈用户：</span>
              <span class="value">{{ item.username }}</span>
            </div>

            <div class="info-item">
              <span class="label">反馈内容：</span>
              <div class="value text-content">
                <!-- 文本超过100字符且未展开时显示截断版本 -->
                <div v-if="isTextLong(item.text) && !item.expanded" class="text-collapsed">
                  {{ truncateText(item.text) }}
                  <el-button
                    type="primary"
                    link
                    size="small"
                    @click="toggleText(item)"
                  >
                    展开
                  </el-button>
                </div>
                <!-- 文本超过100字符且已展开时显示完整版本 -->
                <div v-else-if="isTextLong(item.text) && item.expanded" class="text-full">
                  {{ item.text }}
                  <el-button
                    type="primary"
                    link
                    size="small"
                    @click="toggleText(item)"
                  >
                    收起
                  </el-button>
                </div>
                <!-- 文本不超过100字符时直接显示 -->
                <div v-else class="text-full">
                  {{ item.text }}
                </div>
              </div>
            </div>


            <div v-if="item.createdAt" class="info-item">
              <span class="label">创建时间：</span>
              <span class="value">{{ formatDate(item.createdAt) }}</span>
            </div>

            <div v-if="item.finishedAt" class="info-item">
              <span class="label">处理时间：</span>
              <span class="value">{{ formatDate(item.finishedAt) }}</span>
            </div>
            <div v-if="item.handleUserId" class="info-item">
              <span class="label">处理人id：</span>
              <span class="value">{{ item.handleUserId }}</span>
            </div>
            <div class="button-group" v-if="item.status === '0'">
              <el-button
                type="primary"
                @click="handleFeedback(item, 1)"
                :loading="isProcessing(item.id)"
                :disabled="isProcessing(item.id)"
              >
                完成
              </el-button>
              <el-button
                type="danger"
                @click="handleFeedback(item, -1)"
                :loading="isProcessing(item.id)"
                :disabled="isProcessing(item.id)"
              >
                驳回
              </el-button>
            </div>
          </div>
        </el-card>
      </transition-group>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useFeedbackStore } from '@/stores/feedback.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteHandledFeedbackAPI } from '@/api/feedback.js'

const feedbackStore = useFeedbackStore()

// 使用 storeToRefs 保持响应性
const { feedbackList, loading } = storeToRefs(feedbackStore)
const { fetchFeedback, handleFeedback: updateFeedback } = feedbackStore

// 正在处理的ID集合（用于防重复点击）
const processingIds = ref(new Set())

// 状态映射
const statusMap = {
  '0': { type: 'warning', text: '未处理' },
  '1': { type: 'success', text: '已处理' },
  '-1': { type: 'danger', text: '已驳回' }
}

// 获取状态类型
const getStatusType = (status) => {
  return statusMap[status]?.type || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  return statusMap[status]?.text || '未知'
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 判断文本是否过长（超过100字符）
const isTextLong = (text) => {
  return text && text.length > 100
}

// 截断文本
const truncateText = (text) => {
  if (!text) return ''
  return text.length > 100 ? text.substring(0, 100) + '...' : text
}

// 切换文本展开/收起
const toggleText = (item) => {
  item.expanded = !item.expanded
}

// 处理反馈（完成/驳回）
const handleFeedback = async (item, status) => {
  // 防止重复点击：检查是否正在处理
  if (processingIds.value.has(item.id)) {
    ElMessage.warning('正在处理中，请勿重复点击')
    return
  }

  try {
    // 标记为正在处理
    processingIds.value.add(item.id)

    // 确认操作
    const actionText = status === 1 ? '完成' : '驳回'
    await ElMessageBox.confirm(
      `确定要${actionText}这条反馈吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 调用 store 方法更新反馈状态
    await updateFeedback(item.id, status)
    ElMessage.success(`${actionText}成功(´｡• ᵕ •｡\`)`)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('处理反馈失败:', error)
      ElMessage.error('处理反馈失败，请重试')
    }
  } finally {
    // 无论成功或失败，都移除处理标记
    processingIds.value.delete(item.id)
  }
}

//删除已处理反馈
const deleteHandledFeedback = async () => {
  try {
    // 确认操作
    await ElMessageBox.confirm(
      '在删除前一定要确保反馈都没有后续问题了ヽ(≧□≦)ノ',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    //调用后端api删除反馈
    await deleteHandledFeedbackAPI()
    // store 方法重新获取反馈列表
    await feedbackStore.fetchFeedback()
    ElMessage.success('删除成功(´｡• ᵕ •｡\`)')
  }catch ( error)
    {
      console.error('删除反馈失败:', error)
      ElMessage.error('删除反馈失败，请重试')
    }
}

// 检查是否正在处理
const isProcessing = (id) => {
  return processingIds.value.has(id)
}

onMounted( async () => {
 await fetchFeedback()
})
</script>


<style scoped lang="less">
.button{
  margin-bottom: 20px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
}

.feedback-container {
  padding: 20px;
  background: #FFF8E8;
  height: 100%;
  overflow-y: auto;
}

.feedback-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
}

.feedback-card {
  position: relative;
  border-radius: 8px;
  transition: all 0.3s ease;
  background-color: #FFF8E8;
  border: none;

  &:hover {
    transform: translateY(-4px);
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.feedback-type {
  font-size: 16px;
  font-weight: 600;
  color: #C3272B;
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  line-height: 1.6;
}

.label {
  font-weight: 500;
  color: #606266;
  white-space: nowrap;
  min-width: 80px;
}

.value {
  color: #303133;
  flex: 1;
  word-break: break-all;
}

.text-content {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  min-height: 40px;
}

.text-collapsed,
.text-full {
  line-height: 1.8;
  white-space: pre-wrap;
}

.button-group {
  position: absolute;
  right: 12px;
  bottom: 12px;
}

/* 卡片列表过渡动画 */
.feedback-list-enter-active,
.feedback-list-leave-active {
  transition: all 0.4s ease;
}

.feedback-list-enter-from {
  opacity: 0;
  transform: translateY(20px) scale(0.95);
}

.feedback-list-enter-to {
  opacity: 1;
  transform: translateY(0) scale(1);
}

.feedback-list-leave-from {
  opacity: 1;
  transform: translateY(0) scale(1);
}

.feedback-list-leave-to {
  opacity: 0;
  transform: translateY(-20px) scale(0.95);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .feedback-grid {
    grid-template-columns: 1fr;
  }

  .feedback-container {
    padding: 10px;
  }
}
</style>
