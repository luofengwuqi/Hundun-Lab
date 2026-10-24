import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getFeedbackAPI, handleFeedbackAPI } from '@/api/feedback.js'
import { useUserStore } from '@/stores/user.js'

export const useFeedbackStore = defineStore('feedback', () => {
  // 反馈列表
  const feedbackList = ref([])
  const loading = ref(false)

  // 获取反馈列表
  const fetchFeedback = async () => {
    loading.value = true
    try {
      const res = await getFeedbackAPI()

      if (res.code === 200 || res.code === 0) {
        feedbackList.value = (res.data || []).map(item => ({
          ...item,
          status: String(item.status),
          expanded: false
        }))
      }
      return res
    } catch (error) {
      console.error('获取反馈列表失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  // 处理反馈（完成/驳回）
  const userStore = useUserStore()
  const handleFeedback = async (id, status) => {
    try {

      const res = await handleFeedbackAPI(id, status, userStore.userInfo.id)

      if (res.code === 200 || res.code === 0) {
        // 更新本地状态
        const index = feedbackList.value.findIndex(item => item.id === id)
        if (index !== -1) {
          feedbackList.value[index].status = String(status)
          feedbackList.value[index].finishedAt = new Date().toISOString()
        }
        return res
      }
    } catch (error) {
      console.error('处理反馈失败:', error)
      throw error
    }
  }

  // 重置反馈列表
  const resetFeedbackList = () => {
    feedbackList.value = []
  }

  return {
    feedbackList,
    loading,
    fetchFeedback,
    handleFeedback,
    resetFeedbackList
  }
})
