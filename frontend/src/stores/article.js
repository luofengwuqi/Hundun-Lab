import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getArticleListAPI, updateArticleGoodAPI, updateArticleBadAPI } from '@/api/article'

export const useArticleStore = defineStore('article', () => {

  // 文章列表
  const articleList = ref([])
  
  // 用户投票状态记录 { articleId: 'good' | 'bad' | null }
  const userVoteStatus = ref({})
  
  // 获取文章列表
  const getArticleList = async () => {
    const res = await getArticleListAPI()
    articleList.value = res.data
    return res
  }
  
  // 设置用户投票状态
  const setUserVoteStatus = (articleId, status) => {
    userVoteStatus.value[articleId] = status
  }
  
  // 获取用户投票状态
  const getUserVoteStatus = (articleId) => {
    return userVoteStatus.value[articleId] || null
  }
  
  // 点赞
  const updateArticleGood = async (id) => {
    const res = await updateArticleGoodAPI(id)
    // 更新本地文章列表中的点赞数
    const article = articleList.value.find(item => item.id === id)
    if (article) {
      article.goodCount = (article.goodCount || 0) + 1
    }
    return res
  }
  
  // 点踩
  const updateArticleBad = async (id) => {
    const res = await updateArticleBadAPI(id)
    // 更新本地文章列表中的点踩数
    const article = articleList.value.find(item => item.id === id)
    if (article) {
      article.badCount = (article.badCount || 0) + 1
    }
    return res
  }

  return {
    articleList,
    userVoteStatus,
    getArticleList,
    setUserVoteStatus,
    getUserVoteStatus,
    updateArticleGood,
    updateArticleBad
  }
})
