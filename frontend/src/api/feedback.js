import request from '@/utils/request.js'

// 获取反馈列表
export const getFeedbackAPI = async () => {
  return await request.get('/feedback')
}

// 处理反馈（完成/驳回）
export const handleFeedbackAPI = async (id, status,handleUserId) => {
  console.log(id, status,handleUserId)
  return await request.put('/feedback/status', {
    id,
    status,
    handleUserId
  })
}
//提交反馈
export const addFeedbackAPI = async (type, text) => {
  return await request.post('/feedback', {
    type,
    text
  })
}
//批量删除已处理反馈
export const deleteHandledFeedbackAPI = async () => {
  return await request.delete('/feedback/handled')
}
