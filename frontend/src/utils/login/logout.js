import { logoutAPI } from '@/api/user.js'
import { useUserStore } from '@/stores/user.js'
import router from '@/router/index.js'
import { ElMessage } from 'element-plus'

export const logout = async ()=> {
  // 先清除 token,避免 logoutAPI 请求被拦截器误判为登录过期
  const userStore = useUserStore()

  // 先清除 localStorage(在调用 setter 之前)
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')



  try {
    await logoutAPI()
  } catch (error) {
    // 登出接口的错误可以忽略,因为本地已经清除了登录状态
    console.error('Logout API error:', error)
  }


  await router.push('/login')

  ElMessage.success('ヾ(￣▽￣)Bye~Bye~')
  // 使用 store 的方法清除状态(setter 会检查 localStorage,所以先清除)
  userStore.setToken('')
  userStore.setUserInfo({})
}
