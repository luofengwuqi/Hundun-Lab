import request from "@/utils/request.js";

export const addAdmin = async (admin)=>{
  return await request.post('/user/register/admin',admin)
}

export const getInviteCode = async ()=>{
  return await request.get('/admin/invite-code')
}

export const deleteSingleUser = async (id)=> {
  return await request.delete(`/admin/user/${id}`)
}

export const changeStatus = async (username)=>{
  // console.log(useUserStore().token)
  return await request.put(`/admin/user/status?username=${username}`)
}

export const getUserInfoAPI = async (id) => {
  return await request.get('/admin/user/info',{
    params: {
      id
    }
  })
}

// 统一更新用户信息接口
export const updateUserAPI = async (dto) => {
  return await request.put('/admin/user', dto)
}
