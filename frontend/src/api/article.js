import request from '@/utils/request'

// 获取文章列表
export const getArticleListAPI = () => {
  return request({
    url: '/article',
    method: 'GET'
  })
}
export const submitHelpArticle =(title,text,creator)=>{
  return request.post('/article',{
    title,
    text,
    creator,
    belonging: 1
  })
}

export const updateArticleGoodAPI = (id) => {
  return request.post(`/article/good/${id}`)
}
export const updateArticleBadAPI = (id) => {
  return request.post(`/article/bad/${id}`)
}



