<script setup>
import { ref,  onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { addFeedbackAPI } from '@/api/feedback.js'
import { updateArticleGoodAPI, updateArticleBadAPI } from '@/api/article.js'
import router from '@/router/index.js'
import { ArrowLeft, CaretTop, CaretBottom } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user.js'
import { useArticleStore } from '@/stores/article.js'
import { marked } from 'marked'

// 配置 marked 选项
marked.setOptions({
  breaks: true,
  gfm: true,
})

const userStore = useUserStore()
const articleStore = useArticleStore()

// 控制蒙层显示状态
const showOverlay = ref(true)

// 点击蒙层隐藏
const hideOverlay = () => {
  showOverlay.value = false
}

const goBack = () => {
  const status = Number(userStore.userInfo.status)
  if(isNaN(status) || status === undefined){
    router.replace('/login')
  }
  else if(status >= 2){
    router.replace('/admin')
  }
  else if(status < 2 && status > 0){
    router.replace('/user')
  }else{
    router.replace('/login')
  }
}
// 反馈表单数据
const feedbackForm = ref({
  type: '',
  content: ''
})

// 反馈类型选项
const feedbackTypes = [
  { label: 'Bug反馈', value: 'Bug反馈' },
  { label: '登录问题', value: '登录问题' },
  // 后续可以继续添加其他类型
]

// 提交反馈
const submitFeedback = async () => {
  if (!feedbackForm.value.type) {
    ElMessage.warning('请选择反馈类型')
    return
  }
  if (!feedbackForm.value.content || feedbackForm.value.content.trim() === '') {
    ElMessage.warning('请填写反馈内容')
    return
  }
  await addFeedbackAPI(feedbackForm.value.type,feedbackForm.value.content)
  ElMessage.success('我已收到!')

  // 重置表单
  feedbackForm.value = {
    type: '',
    content: ''
  }
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  const date = new Date(dateTime)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

// 渲染 Markdown 内容
const renderMarkdown = (text) => {
  if (!text) return ''
  try {
    return marked.parse(text)
  } catch (error) {
    console.error('Markdown 解析错误:', error)
    return text
  }
}

// 处理点赞
const handleGood = async (id) => {
  try {
    console.log('articleStore:', articleStore)
    console.log('getUserVoteStatus method:', typeof articleStore.getUserVoteStatus)
    const currentStatus = articleStore.getUserVoteStatus(id)
    const article = articleStore.articleList.find(item => item.id === id)

    if (!article) return

    // 情况1: 当前没点过赞或者点过踩
    if (currentStatus === null || currentStatus === 'bad') {
      await updateArticleGoodAPI(id)
      article.good += 1
      // 如果之前点过踩，需要减回去
      if (currentStatus === 'bad') {
        article.bad -= 1
      }
      articleStore.setUserVoteStatus(id, 'good')
      ElMessage.success('点赞成功')
    }
    // 情况2: 当前点过赞
    else if (currentStatus === 'good') {
      await updateArticleGoodAPI(id)
      article.good -= 1
      articleStore.setUserVoteStatus(id, null)
      ElMessage.info('已取消点赞')
    }
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error('操作失败')
  }
}

// 处理点踩
const handleBad = async (id) => {
  try {
    const currentStatus = articleStore.getUserVoteStatus(id)
    const article = articleStore.articleList.find(item => item.id === id)

    if (!article) return

    // 情况1: 当前没点过赞或者点过赞
    if (currentStatus === null || currentStatus === 'good') {
      await updateArticleBadAPI(id)
      article.bad += 1
      // 如果之前点过赞，需要减回去
      if (currentStatus === 'good') {
        article.good -= 1
      }
      articleStore.setUserVoteStatus(id, 'bad')
      ElMessage.success('已点踩')
    }
    // 情况3: 当前点过踩
    else if (currentStatus === 'bad') {
      await updateArticleBadAPI(id)
      article.bad -= 1
      articleStore.setUserVoteStatus(id, null)
      ElMessage.info('已取消点踩')
    }
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error('操作失败')
  }
}

// 组件挂载时获取文章列表
onMounted(async () => {
  await articleStore.getArticleList()
})
</script>

<template>
  <div class="help-container">
    <!-- 返回按钮 -->
    <el-button
      class="back-button"
      circle
      @click="goBack"
    >
      <el-icon><ArrowLeft /></el-icon>
    </el-button>

    <!-- 上部：反馈表单区域 -->
    <div class="form-section">
      <!-- 蒙层 -->
      <transition name="fade-overlay">
        <div v-if="showOverlay" class="form-overlay" @click="hideOverlay">
          <div class="overlay-content">
            <span class="overlay-text">
              向管理员求助<br>
              <small style="font-size: 15px">常见问题看下面哦</small>
            </span>
          </div>
        </div>
      </transition>

      <!-- 表单内容 -->
      <transition name="fade-form">
        <el-card v-if="!showOverlay" class="feedback-form-card">
        <template #header>
          <div class="form-header">
            <span>向管理员反馈</span>
          </div>
        </template>

        <el-form :model="feedbackForm" label-width="100px" class="feedback-form">
          <el-form-item label="反馈类型" required>
            <el-select
              v-model="feedbackForm.type"
              placeholder="请选择反馈类型"
              style="width: 100%"
            >
              <el-option
                v-for="item in feedbackTypes"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="反馈内容" required>
            <el-input
              v-model="feedbackForm.content"
              type="textarea"
              :rows="20"
              placeholder="___________________________________________________
以下是一些提示信息 建议在提交时删除
1.您的反馈将以邮件的形式发送给管理员们 在ta们处理后同样会以邮件的形式通知您 我们将尽可能地处理您反馈的问题 但会驳回不合理的请求
2.强烈建议您在注册用户时提交您的邮箱信息以便关注反馈的处理进度
3.建议您使用以下格式来反馈问题...当然也可以删掉自己写 给您最大的自由度进行反馈哦~
___________________________________________________
1.问题位置:
您在什么模块下遇到了问题?登录页面/记账模块/管理员模块/...
2.问题描述:
您遇到了什么样的问题?例如忘记密码无法登录/程序报了什么错...
3.操作流程:
您进行了什么样的操作?"
              maxlength="2000"
              show-word-limit
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="submitFeedback" class="submit-btn">
              提交并肘击管理员!
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
      </transition>
    </div>

    <!-- 下部：文章卡片区域 -->
    <div class="info-section">
      <transition-group name="card-list" tag="div" class="cards-wrapper">
        <el-card
          v-for="article in articleStore.articleList"
          :key="article.id"
          class="info-card"
          shadow="hover"
        >
          <template #header>
            <div class="card-header">
              <span class="article-title">{{ article.title }}</span>
              <span class="article-belonging">{{ article.belonging }}</span>
            </div>
          </template>
          <div class="card-content">
            <div class="article-text markdown-body" v-html="renderMarkdown(article.text)"></div>
            <div class="article-footer">
              <span class="article-creator">作者: {{ article.creator }}</span>
              <span class="article-time">{{ formatDateTime(article.createdAt) }}</span>
              <div class="article-stats">
                <span class="stat-item good" @click="handleGood(article.id)">
                  <el-icon><CaretTop /></el-icon>
                  {{ article.good }}
                </span>
                <span class="stat-item bad" @click="handleBad(article.id)">
                  <el-icon><CaretBottom /></el-icon>
                  {{ article.bad }}
                </span>
              </div>
            </div>
          </div>
        </el-card>
      </transition-group>
    </div>
  </div>
</template>

<style scoped>
.help-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  padding: 20px;
  background-color: transparent;
  overflow-y: auto;
  z-index: 1;
}

/* 返回按钮 */
.back-button {
  position: fixed;
  top: 30px;
  left: 30px;
  width: 50px;
  height: 50px;
  background-color: #C3272B;
  border-color: #C3272B;
  z-index: 10;
  box-shadow: 0 2px 8px rgba(195, 39, 43, 0.3);
}

.back-button:hover {
  background-color: #D64A4F;
  border-color: #D64A4F;
  transform: scale(1.05);
}

.back-button :deep(.el-icon) {
  color: white;
  font-size: 20px;
}

/* 下部：信息卡片区域 */
.info-section {
  order: 2;
  width: 100%;
  margin-top: 20px;
}

.cards-wrapper {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

/* 卡片列表过渡动画 */
.card-list-enter-active {
  transition: all 0.5s ease;
}

.card-list-leave-active {
  transition: all 0.3s ease;
  position: absolute;
}

.card-list-enter-from {
  opacity: 0;
  transform: translateY(30px);
}

.card-list-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}

.card-list-move {
  transition: transform 0.5s ease;
}

.info-card {
  background-color: #FFF8E8;
  border: 1px solid #FFEFD5;
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* Element Plus Card body */
:deep(.info-card .el-card__body) {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: 20px;
}

.card-header {
  font-size: 16px;
  font-weight: bold;
  color: #333333;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.article-title {
  flex: 1;
}

.article-belonging {
  font-size: 12px;
  color: #C3272B;
  background-color: rgba(195, 39, 43, 0.1);
  padding: 2px 8px;
  border-radius: 10px;
  margin-left: 10px;
}

.card-content {
  color: #454545;
  line-height: 1.8;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.card-content p {
  margin: 10px 0;
}

.card-content ul {
  margin: 10px 0;
  padding-left: 20px;
}

.card-content li {
  margin: 5px 0;
  color: #5A4A42;
}

/* 文章样式 */
.article-text {
  line-height: 1.8;
  color: #454545;
  word-break: break-word;
  margin-bottom: 15px;
}

/* Markdown 样式 */
.article-text.markdown-body :deep(h1) {
  font-size: 22px;
  font-weight: 700;
  color: #333333;
  margin: 20px 0 14px 0;
  padding-bottom: 8px;
  border-bottom: 2px solid #FFEFD5;
}

.article-text.markdown-body :deep(h2) {
  font-size: 18px;
  font-weight: 600;
  color: #333333;
  margin: 16px 0 12px 0;
  padding-bottom: 6px;
  border-bottom: 1px solid #FFEFD5;
}

.article-text.markdown-body :deep(h3) {
  font-size: 16px;
  font-weight: 600;
  color: #333333;
  margin: 14px 0 10px 0;
}

.article-text.markdown-body :deep(h4),
.article-text.markdown-body :deep(h5),
.article-text.markdown-body :deep(h6) {
  font-size: 15px;
  font-weight: 600;
  color: #333333;
  margin: 12px 0 8px 0;
}

.article-text.markdown-body :deep(p) {
  margin: 10px 0;
  line-height: 1.8;
}

.article-text.markdown-body :deep(strong) {
  font-weight: 600;
  color: #C3272B;
}

.article-text.markdown-body :deep(em) {
  font-style: italic;
  color: #5A4A42;
}

.article-text.markdown-body :deep(ul),
.article-text.markdown-body :deep(ol) {
  margin: 10px 0;
  padding-left: 20px;
}

.article-text.markdown-body :deep(ul) li,
.article-text.markdown-body :deep(ol) li {
  margin: 5px 0;
  line-height: 1.8;
  color: #5A4A42;
}

.article-text.markdown-body :deep(blockquote) {
  margin: 14px 0;
  padding: 10px 14px;
  background-color: rgba(255, 248, 232, 0.6);
  border-left: 4px solid #C3272B;
  border-radius: 4px;
  color: #5A4A42;
}

.article-text.markdown-body :deep(blockquote) p {
  margin: 0;
}

.article-text.markdown-body :deep(code) {
  padding: 2px 6px;
  background-color: rgba(255, 248, 232, 0.8);
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  color: #C3272B;
}

.article-text.markdown-body :deep(pre) {
  margin: 14px 0;
  padding: 14px;
  background-color: rgba(255, 248, 232, 0.8);
  border-radius: 6px;
  overflow-x: auto;
  border: 1px solid #FFEFD5;
}

.article-text.markdown-body :deep(pre) code {
  background: none;
  padding: 0;
  color: #454545;
}

.article-text.markdown-body :deep(a) {
  color: #C3272B;
  text-decoration: none;
  border-bottom: 1px solid transparent;
  transition: all 0.2s;
}

.article-text.markdown-body :deep(a):hover {
  border-bottom-color: #C3272B;
}

.article-text.markdown-body :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 6px;
  margin: 10px 0;
}

.article-text.markdown-body :deep(hr) {
  margin: 20px 0;
  border: none;
  border-top: 2px solid #FFEFD5;
}

.article-text.markdown-body :deep(table) {
  width: 100%;
  margin: 14px 0;
  border-collapse: collapse;
}

.article-text.markdown-body :deep(table) th,
.article-text.markdown-body :deep(table) td {
  padding: 8px 10px;
  border: 1px solid #FFEFD5;
  text-align: left;
}

.article-text.markdown-body :deep(table) th {
  background-color: rgba(255, 248, 232, 0.8);
  font-weight: 600;
  color: #333333;
}

.article-text.markdown-body :deep(table) tr:nth-child(even) {
  background-color: rgba(255, 251, 248, 0.5);
}

.article-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 10px;
  border-top: 1px solid #FFEFD5;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: auto;
}

.article-creator {
  font-size: 13px;
  color: #666;
}

.article-time {
  font-size: 12px;
  color: #999;
}

.article-stats {
  display: flex;
  gap: 15px;
}

.stat-item {
  font-size: 13px;
  cursor: pointer;
  transition: transform 0.2s;
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat-item:hover {
  transform: scale(1.1);
}

.stat-item.good {
  color: #67C23A;
}

.stat-item.good .el-icon {
  font-size: 16px;
}

.stat-item.bad {
  color: #F56C6C;
}

.stat-item.bad .el-icon {
  font-size: 16px;
}

/* 上部：反馈表单区域 */
.form-section {
  order: 1;
  width: 70%;
  max-width: 900px;
  margin: 15px auto;
  display: flex;
  flex-direction: column;
  position: relative;
  min-height: 660px;
}

.feedback-form-card {
  width: 100%;
  min-height: 600px;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  border: none;
  display: flex;
  flex-direction: column;
}

/* 蒙层样式 */
.form-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 5;
  border-radius: 4px;
  overflow: hidden;
}

.form-overlay::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: url('@/assets/img/help/envelope.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  opacity: 0.5;
  z-index: -1;
}

.overlay-content {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.overlay-text {
  text-align: center;
  font-size: 32px;
  font-weight: bold;
  color: #C3272B;
  text-shadow: 2px 2px 4px rgba(255, 255, 255, 0.8);
  user-select: none;
  margin-top: 250px;
}

/* 蒙层渐变动画 */
.fade-overlay-enter-active,
.fade-overlay-leave-active {
  transition: opacity 0.5s ease;
}

.fade-overlay-enter-from,
.fade-overlay-leave-to {
  opacity: 0;
}

/* 表单渐变动画 */
.fade-form-enter-active,
.fade-form-leave-active {
  transition: opacity 0.5s ease;
}

.fade-form-enter-from,
.fade-form-leave-to {
  opacity: 0;
}

.form-header {
  font-size: 18px;
  font-weight: bold;
  color: #333333;
}

.feedback-form {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.submit-btn {
  width: 100%;
  background-color: #C3272B;
  border-color: #C3272B;
  font-size: 16px;
  padding: 12px 24px;
}

.submit-btn:hover {
  background-color: #D64A4F;
  border-color: #D64A4F;
}

/* 自定义滚动条样式 */
.help-container::-webkit-scrollbar {
  width: 8px;
}

.help-container::-webkit-scrollbar-track {
  background: #FFEFD5;
  border-radius: 4px;
}

.help-container::-webkit-scrollbar-thumb {
  background: #AAA;
  border-radius: 4px;
}

.help-container::-webkit-scrollbar-thumb:hover {
  background: #666;
}

.info-section::-webkit-scrollbar {
  width: 8px;
}

.info-section::-webkit-scrollbar-track {
  background: #FFEFD5;
  border-radius: 4px;
}

.info-section::-webkit-scrollbar-thumb {
  background: #AAA;
  border-radius: 4px;
}

.info-section::-webkit-scrollbar-thumb:hover {
  background: #666;
}

/* 表单项样式优化 */
:deep(.el-form-item__label) {
  color: #333333;
  font-weight: 500;
}

:deep(.el-textarea__inner) {
  background-color: rgba(255, 248, 232, 0.9);
  border-color: #FFEFD5;
  color: #333333;
  resize: none;
}

:deep(.el-textarea__inner:focus) {
  border-color: #C3272B;
}

:deep(.el-select .el-input__wrapper) {
  background-color: rgba(255, 248, 232, 0.9);
  border-color: #FFEFD5;
}

:deep(.el-select .el-input__wrapper:hover) {
  border-color: #AAA;
}

:deep(.el-select .el-input__wrapper.is-focus) {
  border-color: #C3272B;
}
</style>
